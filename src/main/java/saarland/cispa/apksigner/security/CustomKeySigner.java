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

import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import saarland.cispa.apksigner.sign.ZipSigner;
import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

/**
 */
public class CustomKeySigner {

    public static final String KEY_NAME_CUSTOM = "custom";
    private static final String TAG = Logg.TAG;

    /** KeyStore-type agnostic.  This method will sign the zip file, automatically handling JKS or BKS keystores. */
    public static void signZip( ZipSigner zipSigner,
                         String keystorePath,
                         char[] keystorePw,
                         String certAlias,
                         char[] certPw,
                         String signatureAlgorithm,
                         String inputZipFilename,
                         String outputZipFilename)
        throws Exception
    {
        Log.d(TAG, String.format("signZip() Cert: %s [%s] In: %s Out: %s",
                certAlias, keystorePath, inputZipFilename, outputZipFilename));

        zipSigner.issueLoadingCertAndKeysProgressEvent();

        KeyStore keystore = KeyStoreFileManager.loadKeyStore( keystorePath, keystorePw);

        Certificate cert = keystore.getCertificate(certAlias);
        X509Certificate publicKey = (X509Certificate)cert;
        Key key = keystore.getKey(certAlias, certPw);
        PrivateKey privateKey = (PrivateKey)key;

        zipSigner.setKeys(KEY_NAME_CUSTOM, publicKey, privateKey, signatureAlgorithm, null);
        zipSigner.signZip( inputZipFilename, outputZipFilename);
        Log.d(TAG, String.format("signZip() Cert: %s [%s] In: %s Out: %s DONE",
                certAlias, keystorePath, inputZipFilename, outputZipFilename));
    }

}
