/**
 * The ARTist Project (https://artist.cispa.saarland)
 *
 * Copyright (C) 2017 CISPA (https://cispa.saarland), Saarland University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author "Sebastian Weisgerber <weisgerber@cispa.saarland>"
 *
 */
package saarland.cispa.dexterous.cli;

import com.android.apksig.ApkSigner;
import com.android.apksig.apk.ApkFormatException;
import com.android.apksig.apk.MinSdkVersionException;
import com.android.apksigner.PasswordRetriever;
import trikita.log.Log;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

public class ApkSir {

    private static final String VERSION = "0.1";

    public static class SignerParams {
        String name;

        String keystoreFile;
        String keystoreKeyAlias;
        String keystorePasswordSpec;
        String keyPasswordSpec;
        String keystoreType;
        String keystoreProviderName;
        String keystoreProviderClass;
        String keystoreProviderArg;

        String keyFile;
        String certFile;

        String v1SigFileBasename;

        PrivateKey privateKey;
        List<X509Certificate> certs;

        private boolean isEmpty() {
            return (name == null)
                    && (keystoreFile == null)
                    && (keystoreKeyAlias == null)
                    && (keystorePasswordSpec == null)
                    && (keyPasswordSpec == null)
                    && (keystoreType == null)
                    && (keystoreProviderName == null)
                    && (keystoreProviderClass == null)
                    && (keystoreProviderArg == null)
                    && (keyFile == null)
                    && (certFile == null)
                    && (v1SigFileBasename == null)
                    && (privateKey == null)
                    && (certs == null);
        }

        private void loadPrivateKeyAndCerts(PasswordRetriever passwordRetriever) throws Exception {
            if (keystoreFile != null) {
                if (keyFile != null) {
                    throw new ApkSir.ParameterException(
                            "--ks and --key may not be specified at the same time");
                } else if (certFile != null) {
                    throw new ApkSir.ParameterException(
                            "--ks and --cert may not be specified at the same time");
                }
                loadPrivateKeyAndCertsFromKeyStore(passwordRetriever);
            } else if (keyFile != null) {
                loadPrivateKeyAndCertsFromFiles(passwordRetriever);
            } else {
                throw new ApkSir.ParameterException(
                        "KeyStore (--ks) or private key file (--key) must be specified");
            }
        }

        private void loadPrivateKeyAndCertsFromKeyStore(PasswordRetriever passwordRetriever)
                throws Exception {
            if (keystoreFile == null) {
                throw new ApkSir.ParameterException("KeyStore (--ks) must be specified");
            }

            // 1. Obtain a KeyStore implementation
            String ksType = (keystoreType != null) ? keystoreType : KeyStore.getDefaultType();
            KeyStore ks;
            if (keystoreProviderName != null) {
                // Use a named Provider (assumes the provider is already installed)
                ks = KeyStore.getInstance(ksType, keystoreProviderName);
            } else if (keystoreProviderClass != null) {
                // Use a new Provider instance (does not require the provider to be installed)
                Class<?> ksProviderClass = Class.forName(keystoreProviderClass);
                if (!Provider.class.isAssignableFrom(ksProviderClass)) {
                    throw new ApkSir.ParameterException(
                            "Keystore Provider class " + keystoreProviderClass + " not subclass of "
                                    + Provider.class.getName());
                }
                Provider ksProvider;
                if (keystoreProviderArg != null) {
                    // Single-arg Provider constructor
                    ksProvider =
                            (Provider) ksProviderClass.getConstructor(String.class)
                                    .newInstance(keystoreProviderArg);
                } else {
                    // No-arg Provider constructor
                    ksProvider = (Provider) ksProviderClass.getConstructor().newInstance();
                }
                ks = KeyStore.getInstance(ksType, ksProvider);
            } else {
                // Use the highest-priority Provider which offers the requested KeyStore type
                ks = KeyStore.getInstance(ksType);
            }

            // 2. Load the KeyStore
            char[] keystorePwd = null;
            if ("NONE".equals(keystoreFile)) {
                ks.load(null);
            } else {
                String keystorePasswordSpec =
                        (this.keystorePasswordSpec != null)
                                ? this.keystorePasswordSpec : PasswordRetriever.SPEC_STDIN;
                String keystorePwdString =
                        passwordRetriever.getPassword(
                                keystorePasswordSpec, "Keystore password for " + name);
                keystorePwd = keystorePwdString.toCharArray();
                try (FileInputStream in = new FileInputStream(keystoreFile)) {
                    ks.load(in, keystorePwd);
                }
            }

            // 3. Load the PrivateKey and cert chain from KeyStore
            char[] keyPwd;
            if (keyPasswordSpec == null) {
                keyPwd = keystorePwd;
            } else {
                keyPwd =
                        passwordRetriever.getPassword(keyPasswordSpec, "Key password for " + name)
                                .toCharArray();
            }
            String keyAlias = null;
            PrivateKey key = null;
            try {
                if (keystoreKeyAlias == null) {
                    // Private key entry alias not specified. Find the key entry contained in this
                    // KeyStore. If the KeyStore contains multiple key entries, return an error.
                    Enumeration<String> aliases = ks.aliases();
                    if (aliases != null) {
                        while (aliases.hasMoreElements()) {
                            String entryAlias = aliases.nextElement();
                            if (ks.isKeyEntry(entryAlias)) {
                                keyAlias = entryAlias;
                                if (keystoreKeyAlias != null) {
                                    throw new ApkSir.ParameterException(
                                            keystoreFile + " contains multiple key entries"
                                                    + ". --ks-key-alias option must be used to specify"
                                                    + " which entry to use.");
                                }
                                keystoreKeyAlias = keyAlias;
                            }
                        }
                    }
                    if (keystoreKeyAlias == null) {
                        throw new ApkSir.ParameterException(
                                keystoreFile + " does not contain key entries");
                    }
                }

                // Private key entry alias known. Load that entry's private key.
                keyAlias = keystoreKeyAlias;
                if (!ks.isKeyEntry(keyAlias)) {
                    throw new ApkSir.ParameterException(
                            keystoreFile + " entry \"" + keyAlias + "\" does not contain a key");
                }
                Key entryKey;
                if (keyPwd != null) {
                    // Key password specified -- load this key as a password-protected key
                    entryKey = ks.getKey(keyAlias, keyPwd);
                } else {
                    // Key password not specified -- try to load this key without using a password
                    try {
                        entryKey = ks.getKey(keyAlias, null);
                    } catch (UnrecoverableKeyException expected) {
                        // Looks like this might be a password-protected key. Prompt for password
                        // and try loading the key using the password.
                        keyPwd =
                                passwordRetriever.getPassword(
                                        PasswordRetriever.SPEC_STDIN,
                                        "Password for key with alias \"" + keyAlias + "\"")
                                        .toCharArray();
                        entryKey = ks.getKey(keyAlias, keyPwd);
                    }
                }
                if (entryKey == null) {
                    throw new ApkSir.ParameterException(
                            keystoreFile + " entry \"" + keyAlias + "\" does not contain a key");
                } else if (!(entryKey instanceof PrivateKey)) {
                    throw new ApkSir.ParameterException(
                            keystoreFile + " entry \"" + keyAlias + "\" does not contain a private"
                                    + " key. It contains a key of algorithm: "
                                    + entryKey.getAlgorithm());
                }
                key = (PrivateKey) entryKey;
            } catch (UnrecoverableKeyException e) {
                throw new IOException(
                        "Failed to obtain key with alias \"" + keyAlias + "\" from " + keystoreFile
                                + ". Wrong password?",
                        e);
            }
            this.privateKey = key;
            java.security.cert.Certificate[] certChain = ks.getCertificateChain(keyAlias);
            if ((certChain == null) || (certChain.length == 0)) {
                throw new ApkSir.ParameterException(
                        keystoreFile + " entry \"" + keyAlias + "\" does not contain certificates");
            }
            this.certs = new ArrayList<>(certChain.length);
            for (java.security.cert.Certificate cert : certChain) {
                this.certs.add((X509Certificate) cert);
            }
        }

        private void loadPrivateKeyAndCertsFromFiles(PasswordRetriever passwordRetriver)
                throws Exception {
            if (keyFile == null) {
                throw new ApkSir.ParameterException("Private key file (--key) must be specified");
            }
            if (certFile == null) {
                throw new ApkSir.ParameterException("Certificate file (--cert) must be specified");
            }
            byte[] privateKeyBlob = readFully(new File(keyFile));

            PKCS8EncodedKeySpec keySpec;
            // Potentially encrypted key blob
            try {
                EncryptedPrivateKeyInfo encryptedPrivateKeyInfo =
                        new EncryptedPrivateKeyInfo(privateKeyBlob);

                // The blob is indeed an encrypted private key blob
                String passwordSpec =
                        (keyPasswordSpec != null) ? keyPasswordSpec : PasswordRetriever.SPEC_STDIN;
                String keyPassword =
                        passwordRetriver.getPassword(
                                passwordSpec, "Private key password for " + name);

                PBEKeySpec decryptionKeySpec = new PBEKeySpec(keyPassword.toCharArray());
                SecretKey decryptionKey =
                        SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName())
                                .generateSecret(decryptionKeySpec);
                keySpec = encryptedPrivateKeyInfo.getKeySpec(decryptionKey);
            } catch (IOException e) {
                // The blob is not an encrypted private key blob
                if (keyPasswordSpec == null) {
                    // Given that no password was specified, assume the blob is an unencrypted
                    // private key blob
                    keySpec = new PKCS8EncodedKeySpec(privateKeyBlob);
                } else {
                    throw new InvalidKeySpecException(
                            "Failed to parse encrypted private key blob " + keyFile, e);
                }
            }

            // Load the private key from its PKCS #8 encoded form.
            try {
                privateKey = loadPkcs8EncodedPrivateKey(keySpec);
            } catch (InvalidKeySpecException e) {
                throw new InvalidKeySpecException(
                        "Failed to load PKCS #8 encoded private key from " + keyFile, e);
            }

            // Load certificates
            Collection<? extends java.security.cert.Certificate> certs;
            try (FileInputStream in = new FileInputStream(certFile)) {
                certs = CertificateFactory.getInstance("X.509").generateCertificates(in);
            }
            List<X509Certificate> certList = new ArrayList<>(certs.size());
            for (java.security.cert.Certificate cert : certs) {
                certList.add((X509Certificate) cert);
            }
            this.certs = certList;
        }

        private static PrivateKey loadPkcs8EncodedPrivateKey(PKCS8EncodedKeySpec spec)
                throws InvalidKeySpecException, NoSuchAlgorithmException {
            try {
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (InvalidKeySpecException expected) {
            }
            try {
                return KeyFactory.getInstance("EC").generatePrivate(spec);
            } catch (InvalidKeySpecException expected) {
            }
            try {
                return KeyFactory.getInstance("DSA").generatePrivate(spec);
            } catch (InvalidKeySpecException expected) {
            }
            throw new InvalidKeySpecException("Not an RSA, EC, or DSA private key");
        }
    }

    /**
     * Indicates that there is an issue with command-line parameters provided to this tool.
     */
    private static class ParameterException extends Exception {
        private static final long serialVersionUID = 1L;

        ParameterException(String message) {
            super(message);
        }
    }

    private File inputApk;
    private File tmpOutputApk;
    private File outputApk;

    public ApkSir() {
    }

    private static byte[] readFully(File file) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(file)) {
            drain(in, result);
        }
        return result.toByteArray();
    }

    private static void drain(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[65536];
        int chunkSize;
        while ((chunkSize = in.read(buf)) != -1) {
            out.write(buf, 0, chunkSize);
        }
    }


    public void signApk(final String apkPath)
            throws ApkFormatException, IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        boolean v1SigningEnabled = true;
        boolean v2SigningEnabled = true;

        final String signedApkPath = apkPath.replace(".apk", "-signed.apk");
        final String tmpApkPath = apkPath.replace(".apk", "-temp.apk");

        this.outputApk = new File(signedApkPath);
        this.tmpOutputApk = new File(tmpApkPath);
        this.inputApk = new File(apkPath);

        final List<SignerParams> signers = buildDefaultSignerParams();
        final List<ApkSigner.SignerConfig> signerConfigs = buildSignerConfigs(signers);

        if (signerConfigs == null) {
            Log.e("No SignerConfig found.");
            return;
        }

        ApkSigner.Builder apkSignerBuilder =
                new ApkSigner.Builder(signerConfigs)
                        .setInputApk(inputApk)
                        .setOutputApk(tmpOutputApk)
                        .setOtherSignersSignaturesPreserved(false)
                        .setV1SigningEnabled(v1SigningEnabled)
                        .setV2SigningEnabled(v2SigningEnabled)
                        .setCreatedBy(VERSION + " (Dexterously apksigner)");
        // apkSignerBuilder.setMinSdkVersion(minSdkVersion);
        ApkSigner apkSigner = apkSignerBuilder.build();
        try {
            apkSigner.sign();
        } catch (MinSdkVersionException e) {
            throw new MinSdkVersionException(
                    "Failed to determine APK's minimum supported platform version."
                    + " Use --min-sdk-version to override: ",
                    e);
        }
        if (!tmpOutputApk.getCanonicalPath().equals(outputApk.getCanonicalPath())) {
            renameOutputApk();
        }
        Log.i("Signed: " + outputApk.getName());
    }

    private List<SignerParams> buildDefaultSignerParams() {
        final List<SignerParams> signers = new ArrayList<>(1);
        final SignerParams signerParams = new SignerParams();

        final String KEYSTORE_NAME = "artist-debug.keystore";
        final String KEYSTORE_PATH = "res/" + KEYSTORE_NAME;

        final String KEYSTORE_PASSWORD = "pass:android";
        final String CERTIFICATE_ALIAS = "androiddebugkey";
        final String CERTIFICATE_PASSWORD = KEYSTORE_PASSWORD;

        signerParams.keystoreFile = KEYSTORE_PATH;
        signerParams.keystoreKeyAlias = CERTIFICATE_ALIAS;
        signerParams.keystorePasswordSpec = KEYSTORE_PASSWORD;
        signerParams.keyPasswordSpec = CERTIFICATE_PASSWORD;

        signers.add(signerParams);

        return signers;
    }

    private List<ApkSigner.SignerConfig> buildSignerConfigs(final List<SignerParams> signers) {
        List<ApkSigner.SignerConfig> signerConfigs = new ArrayList<>(signers.size());

        int signerNumber = 0;
        try (PasswordRetriever passwordRetriever = new PasswordRetriever()) {
            for (final SignerParams signer : signers) {
                signerNumber++;
                signer.name = "signer #" + signerNumber;
                try {
                    signer.loadPrivateKeyAndCerts(passwordRetriever);
                } catch (ParameterException e) {
                    System.err.println("Failed to load signer \"" + signer.name + "\": " + e.getMessage());
                    System.exit(2);
                    return null;
                } catch (Exception e) {
                    System.err.println("Failed to load signer \"" + signer.name + "\"");
                    e.printStackTrace();
                    System.exit(2);
                    return null;
                }
                String v1SigBasename;
                if (signer.v1SigFileBasename != null) {
                    v1SigBasename = signer.v1SigFileBasename;
                } else if (signer.keystoreKeyAlias != null) {
                    v1SigBasename = signer.keystoreKeyAlias;
                } else if (signer.keyFile != null) {
                    String keyFileName = new File(signer.keyFile).getName();
                    int delimiterIndex = keyFileName.indexOf('.');
                    if (delimiterIndex == -1) {
                        v1SigBasename = keyFileName;
                    } else {
                        v1SigBasename = keyFileName.substring(0, delimiterIndex);
                    }
                } else {
                    throw new RuntimeException("Neither KeyStore key alias nor private key file available");
                }
                ApkSigner.SignerConfig signerConfig =
                        new ApkSigner.SignerConfig.Builder(
                                v1SigBasename,
                                signer.privateKey
                                , signer.certs
                        ).build();
                signerConfigs.add(signerConfig);
            }
        }
        return signerConfigs;
    }

    private void renameOutputApk() throws IOException {
        FileSystem fs = FileSystems.getDefault();
        Files.move(
                fs.getPath(tmpOutputApk.getPath()),
                fs.getPath(outputApk.getPath()),
                StandardCopyOption.REPLACE_EXISTING);
    }
}
