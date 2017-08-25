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
package saarland.cispa.artist.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

public class ArtistUtils {

    private static final String TAG = Logg.TAG;

    public static final String CODELIB_IMPORTED = "I::";
    public static final String CODELIB_ASSET    = "A::";

    public static boolean isMultiDex(final String apkPath) {

        boolean isMultidexApk = false;
        int dexFileCount = 0;

        try {
            InputStream is = new FileInputStream(new File(apkPath));
            ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(is));

            try {
                ZipEntry apkContent;
                while ((apkContent = zipInput.getNextEntry()) != null) {
                    if (apkContent.getName().endsWith(".dex")) {
                        ++dexFileCount;
                        Log.d(TAG, apkPath + " DexFile: " + dexFileCount);
                    }
                }
            } finally {
                zipInput.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Copying Could not find APK: " + apkPath);
        }
        if (dexFileCount > 1) {
            isMultidexApk = true;
        }
        return isMultidexApk;
    }

}
