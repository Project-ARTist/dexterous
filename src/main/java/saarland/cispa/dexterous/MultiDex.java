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
package saarland.cispa.dexterous;

import comm.android.dex.Dex;
import comm.android.dex.DexFormat;
import comm.android.dex.util.FileUtils;
import saarland.cispa.utils.LogUtils;
import trikita.log.Log;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MultiDex {

    private static final String TAG = LogUtils.TAG;

    /** This value is a guess / should suffice for the next 1-15 years */
    public static final int MAXIMUM_DEX_FILES = 128;
    public static final int DEX_MAXIMUM_METHODS = 0xFFFF;

    public static Map<String, Dex> loadDexfiles(final File fileContainingDex) {
        Map<String, Dex> dexBuffers = new LinkedHashMap<>();

        if (FileUtils.hasArchiveSuffix(fileContainingDex.getName())) {

            try (ZipFile zipFile = new ZipFile(fileContainingDex)) {

                for (int i = 1; i < MAXIMUM_DEX_FILES; i++) {

                    final String CLASSES_DEX_FILENAME;
                    if (i == 1) {
                        CLASSES_DEX_FILENAME = DexFormat.DEX_IN_JAR_NAME;
                    } else {
                        CLASSES_DEX_FILENAME = String.format("classes%s.dex", i);
                    }
                    final String FULL_DEX_PATH = fileContainingDex.getName() + ":" + CLASSES_DEX_FILENAME;

                    Log.i(TAG, "Loading DexFile: " + FULL_DEX_PATH);

                    ZipEntry entry = zipFile.getEntry(CLASSES_DEX_FILENAME);
                    if (entry == null) {
                        if (i == 1)
                            Log.w(TAG, String.format("ERROR Loading DexFile: %s: Not present in file: %s",
                                    CLASSES_DEX_FILENAME,
                                    fileContainingDex.getName()));
                        break;
                    }
                    try {
                        final Dex localDex = new Dex(zipFile.getInputStream(entry));
                        localDex.setName(FULL_DEX_PATH);
                        dexBuffers.put(FULL_DEX_PATH, localDex);
                    } catch (final IOException e) {
                        Log.e(TAG, e);
                        Log.e(TAG, "Could not open DexFile: " + FULL_DEX_PATH);
                    }
                }
            } catch (final IOException e) {
                Log.e(TAG, "Could not open APK: " + fileContainingDex.toString());
                Log.e(TAG, e);
            }
        } else if (FileUtils.isDexFile(fileContainingDex.getName())) {
            final String FULL_DEX_PATH = fileContainingDex.getName();
            try {
                final Dex localDex = new Dex(fileContainingDex);
                localDex.setName(FULL_DEX_PATH);
                dexBuffers.put(FULL_DEX_PATH, localDex);
            } catch (final IOException e) {
                Log.e(TAG, e);
                Log.e(TAG, "Could not open DexFile: " + FULL_DEX_PATH);
            }
        } else {
            Log.e(TAG, "Could not open File: " + fileContainingDex.getAbsolutePath());
        }
        Log.i(TAG, String.format("DONE Loading %02d Dexfiles (%s)", dexBuffers.size(), fileContainingDex.getName()));
        return dexBuffers;
    }

    public static boolean isMultiDexApk(final File apkPath) {

        boolean isMultidexApk = false;
        int dexFileCount = 0;

        try {
            InputStream is = new FileInputStream(apkPath);
            ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(is));

            try {
                ZipEntry apkContent;
                while ((apkContent = zipInput.getNextEntry()) != null) {
                    if (apkContent.getName().endsWith(".dex")) {
                        ++dexFileCount;
                        Log.i(TAG, apkPath + " DexFile: " + dexFileCount);
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
