/*
 * Copyright (C) 2010 Ken Ellinwood
 *
 * Changes Copyright (C) 2017 CISPA (https://cispa.saarland), Saarland University
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
 */
package saarland.cispa.apksigner.security;

import org.spongycastle.crypto.RuntimeCryptoException;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;

import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

/**
 */
public class KeyStoreFileManager {

    private static final String TAG = Logg.TAG;

    public static KeyStore loadKeyStore(String keystorePath, String encodedPassword)
            throws Exception {
        char password[] = null;
        try {
            if (encodedPassword != null) {
                password = PasswordObfuscator.getInstance().decodeKeystorePassword( keystorePath, encodedPassword);
            }
            return loadKeyStore(keystorePath, password);
        } finally {
            if (password != null) PasswordObfuscator.flush(password);
        }
    }

    public static KeyStore createKeyStore(String keystorePath, char[] password)
        throws Exception
    {
        KeyStore ks = null;
        if (keystorePath.toLowerCase().endsWith(".bks")) {
            ks = KeyStore.getInstance("bks", new BouncyCastleProvider());
        } else {
            throw new RuntimeCryptoException("JKS Keystore is not upported.");
        }
        ks.load(null, password);
        return ks;
    }

    public static KeyStore loadKeyStore(String keystorePath, char[] password)
        throws Exception
    {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("JKS", SpongySecurity.getProvider());
            FileInputStream fis = new FileInputStream( keystorePath);
            ks.load( fis, password);
            fis.close();
            return ks;
        } catch (LoadKeystoreException x) {
            // This type of exception is thrown when the keystore is a JKS keystore, but the file is malformed
            // or the validity/password check failed.  In this case don't bother to attempt loading it as a BKS keystore.
            Log.e(TAG, "Could not load Keystore: " + keystorePath, x);
            throw x;
        } catch (Exception x) {
            // logger.warning( x.getMessage(), x);
            try {
                ks = KeyStore.getInstance("BKS", SpongySecurity.getProvider());
                FileInputStream fis = new FileInputStream( keystorePath);
                ks.load( fis, password);
                fis.close();
                return ks;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore: " + e.getMessage(), e);
            }
        }
    }

    public static void writeKeyStore( KeyStore ks, String keystorePath, String encodedPassword)
        throws Exception
    {
        char password[] = null;
        try {
            password = PasswordObfuscator.getInstance().decodeKeystorePassword( keystorePath, encodedPassword);
            writeKeyStore( ks, keystorePath, password);
        } finally {
            if (password != null) PasswordObfuscator.flush(password);
        }
    }

    public static void writeKeyStore( KeyStore ks, String keystorePath, char[] password)
        throws Exception
    {

        File keystoreFile = new File( keystorePath);
        try {
            if (keystoreFile.exists()) {
                // I've had some trouble saving new verisons of the keystore file in which the file becomes empty/corrupt.
                // Saving the new version to a new file and creating a backup of the old version.
                File tmpFile = File.createTempFile( keystoreFile.getName(), null, keystoreFile.getParentFile());
                FileOutputStream fos = new FileOutputStream( tmpFile);
                ks.store(fos, password);
                fos.flush();
                fos.close();
                /* create a backup of the previous version
                int i = 1;
                File backup = new File( keystorePath + "." + i + ".bak");
                while (backup.exists()) {
                    i += 1;
                    backup = new File( keystorePath + "." + i + ".bak");
                }
                renameTo(keystoreFile, backup);
                */
                renameTo(tmpFile, keystoreFile);
            } else {
                FileOutputStream fos = new FileOutputStream( keystorePath);
                ks.store(fos, password);
                fos.close();
            }
        } catch (Exception x) {
            try {
                File logfile = File.createTempFile("zipsigner-error", ".log", keystoreFile.getParentFile());
                PrintWriter pw = new PrintWriter(new FileWriter( logfile));
                x.printStackTrace( pw);
                pw.flush();
                pw.close();
            } catch (Exception y) {}
            throw x;
        }
    }


    static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream input = new FileInputStream(srcFile);
        try {
            FileOutputStream output = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                long count = 0;
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                    count += n;
                }
            } finally {
                try { output.close();  } catch (IOException x) {} // Ignore
            }
        } finally {
            try { input.close(); } catch (IOException x) {}
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" +
                srcFile + "' to '" + destFile + "'");
        }
        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified());
        }
    }


    public static void renameTo(File fromFile, File toFile)
        throws IOException
    {
        copyFile(fromFile, toFile, true);
        if (!fromFile.delete()) throw new IOException("Failed to delete " + fromFile);
    }

    public static void deleteKey(String storePath, String storePass, String keyName)
        throws Exception
    {
        KeyStore ks = loadKeyStore( storePath, storePass);
        ks.deleteEntry( keyName);
        writeKeyStore(ks, storePath, storePass);
    }

    public static String renameKey( String keystorePath, String storePass, String oldKeyName, String newKeyName, String keyPass)
        throws Exception
    {
        char[] keyPw = null;

        try {
            KeyStore ks = loadKeyStore(keystorePath, storePass);
            Log.d(TAG, "renameKey() Before newKeyName: " + newKeyName);
            if (ks instanceof KeyStore) {
                newKeyName = newKeyName.toLowerCase();
            }
            Log.d(TAG, "renameKey() After  newKeyName: " + newKeyName);

            if (ks.containsAlias(newKeyName)) {
                throw new KeyNameConflictException();
            }

            keyPw = PasswordObfuscator.getInstance().decodeAliasPassword( keystorePath, oldKeyName, keyPass);
            Key key = ks.getKey(oldKeyName, keyPw);
            Certificate cert = ks.getCertificate( oldKeyName);

            ks.setKeyEntry(newKeyName, key, keyPw, new Certificate[] { cert});
            ks.deleteEntry( oldKeyName);

            writeKeyStore(ks, keystorePath, storePass);
            return newKeyName;
        }
        finally {
            PasswordObfuscator.flush(keyPw);
        }
    }

    public static KeyStore.Entry getKeyEntry( String keystorePath, String storePass, String keyName, String keyPass)
        throws Exception
    {
        char[] keyPw = null;
        KeyStore.PasswordProtection passwordProtection = null;

        try {
            KeyStore ks = loadKeyStore(keystorePath, storePass);
            keyPw = PasswordObfuscator.getInstance().decodeAliasPassword( keystorePath, keyName, keyPass);
            passwordProtection = new KeyStore.PasswordProtection(keyPw);
            return ks.getEntry( keyName, passwordProtection);
        }
        finally {
            if (keyPw != null) PasswordObfuscator.flush(keyPw);
            if (passwordProtection != null) passwordProtection.destroy();
        }
    }

    public static boolean containsKey( String keystorePath, String storePass, String keyName)
        throws Exception
    {
        KeyStore ks = loadKeyStore(keystorePath, storePass);
        return ks.containsAlias( keyName);
    }


    /**
     *
     * @param keystorePath
     * @param encodedPassword
     * @throws Exception if the password is invalid
     */
    public static void validateKeystorePassword( String keystorePath, String encodedPassword)
        throws Exception
    {
        char[] password = null;
        try {
            KeyStore ks = KeyStoreFileManager.loadKeyStore( keystorePath, encodedPassword);
        } finally {
            if (password != null) PasswordObfuscator.flush(password);
        }

    }

    /**
     *
     * @param keystorePath
     * @param keyName
     * @param encodedPassword
     * @throws UnrecoverableKeyException if the password is invalid
     */
    public static void validateKeyPassword( String keystorePath, String keyName, String encodedPassword)
        throws Exception
    {
        char[] password = null;
        try {
            KeyStore ks = KeyStoreFileManager.loadKeyStore( keystorePath, (char[])null);
            password = PasswordObfuscator.getInstance().decodeAliasPassword(keystorePath,keyName, encodedPassword);
            ks.getKey(keyName, password);
        } finally {
            if (password != null) PasswordObfuscator.flush(password);
        }

    }
}
