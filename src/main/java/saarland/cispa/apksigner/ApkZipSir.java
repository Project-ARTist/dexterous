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
 * @author "Oliver Schranz <oliver.schranz@cispa.saarland>"
 * @author "Sebastian Weisgerber <weisgerber@cispa.saarland>"
 *
 */
package saarland.cispa.apksigner;

import java.io.File;
import java.security.Provider;
import java.security.Security;

import saarland.cispa.apksigner.security.CustomKeySigner;
import saarland.cispa.apksigner.sign.ZipSigner;
import saarland.cispa.artist.log.Logg;
import saarland.cispa.artist.settings.ArtistRunConfig;
import trikita.log.Log;

/**
 * Uses unofficial kellinwood.security.zipsigner.
 * <p>
 * kellinwood.security.zipsigner are modified and cleaned for android
 * usage. Original implementation is outdated and not available anymore.
 * <p>
 * Original implementation used apksig from the android SDK,
 * but due to nio.Files API usage and more it's not easily portable to Android.
 */
public class ApkZipSir implements ApkSigner {

    public static final String SIGNATURE_BLOCK_GENERATOR =
            "saarland.cispa.apksigner.security.SignatureBlockGenerator";
    public static final String SIGNATURE_BLOCK_GENERATOR_METHOD = "generate";
    // Algorithms
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final String DIGEST_ALGORITHM = "SHA1";
    // Keystore
    private static final String PASSWORD_KEYSTORE = "android";
    // Key
    private static final String KEY_ALIAS = "artist";
    private static final String PASSWORD_KEY = "android";
    private static final String TAG = Logg.TAG;

    private String signedApkPath;

    public ApkZipSir(final ArtistRunConfig config) {
        for (Provider secProvider : Security.getProviders()) {
            Log.d(TAG, "> Provider: " + secProvider.getName());
        }
        this.signedApkPath = config.app_apk_merged_signed_file_path;
    }

    @Override
    public String signApk(final String keyStorePath, final String unsignedApkPath) {
        if (unsignedApkPath == null || unsignedApkPath.isEmpty()) {
            throw new IllegalArgumentException("Please specify a valid path to an APK: " + unsignedApkPath);
        }
        final File signedApk = new File(this.signedApkPath);

        if (signedApk.exists()) {
            signedApk.delete();
        }
        try {
            // Sign with Custom ZipSigner
            ZipSigner apkSigner = new ZipSigner();
            CustomKeySigner.signZip(
                    apkSigner,
                    keyStorePath,
                    PASSWORD_KEYSTORE.toCharArray(),
                    KEY_ALIAS,
                    PASSWORD_KEY.toCharArray(),
                    SIGNATURE_ALGORITHM,
                    unsignedApkPath,
                    signedApkPath
            );
        } catch (final ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            Log.e(TAG, "ZipSigner() failed", e);
        } catch (final Exception e) {
            Log.e(TAG, "ZipSigner() failed Completely", e);
        }
        Log.i(TAG, "ApkZipSir - Signing APK: " + unsignedApkPath);
        Log.i(TAG, "> Saving Signed APK to: " + signedApkPath);

        if (!signedApk.exists()) {
            signedApkPath = "";
        }
        Log.i(TAG, String.format("> ApkZipSir DONE (Path: %s)", signedApk.getAbsolutePath()));
        return signedApkPath;
    }

    static {
        final int insertPosition = Security.insertProviderAt(
                new org.spongycastle.jce.provider.BouncyCastleProvider(),
                1
        );
        Log.i(TAG, "Injected SpongyCastle @ position: " + insertPosition);
    }
}
