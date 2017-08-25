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

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import comm.android.dex.Dex;
import comm.android.dx.merge.CollisionPolicy;
import comm.android.dx.merge.DexMerger;
import saarland.cispa.artist.ArtistCompilationTask;
import saarland.cispa.artist.log.Logg;
import saarland.cispa.artist.settings.ArtistRunConfig;
import trikita.log.Log;

import static saarland.cispa.dexterous.MultiDex.openMultiDexApk;

public class Dexterous {

    private static final String TAG = Logg.TAG;

    private String CODE_LIB_NAME;  // e.g. = "codelib.apk"
    private String CODE_LIB_DEX_NAME; // e.g. = "codelib.apk:classes.dex"

    private final ArtistRunConfig config;

    private Map<String, Dex> dexBuffers = null;

    private Set<String> dexSourceFiles = null;

    public Dexterous(final ArtistRunConfig config) {
        this.config = config;
        this.CODE_LIB_NAME = config.codeLib.getName();
        this.CODE_LIB_DEX_NAME = this.CODE_LIB_NAME + ":classes.dex";

        this.dexBuffers = new LinkedHashMap<>();
        this.dexSourceFiles = new HashSet<>();
    }

    public void init(final File appApk, final File codeLibApk) {
        this.dexSourceFiles.add(appApk.getAbsolutePath());
        this.dexSourceFiles.add(codeLibApk.getAbsolutePath());

        final Map<String, Dex> appApkDexes = openMultiDexApk(appApk);
        dexBuffers.putAll(appApkDexes);
        final Map<String, Dex> codeLibApkDexes = openMultiDexApk(codeLibApk);
        dexBuffers.putAll(codeLibApkDexes);
        Log.d(TAG, "Dexterous() Init:");
        Log.d(TAG, "> App:            " + appApk);
        Log.d(TAG, "> CodeLib:        " + codeLibApk);
        Log.d(TAG, "> CodeLibName:    " + CODE_LIB_NAME);
        Log.d(TAG, "> CodeLibDexName: " + CODE_LIB_DEX_NAME);
        for (final String dexName : dexBuffers.keySet()) {
            Log.d(TAG, "  > DexFile:        " + dexName);
        }
    }

    private boolean hasMultipleDexes() {
        return dexBuffers.size() > 1;
    }

    private void mergeMethodIds(final String DEX_NAME, Dex dexFile) {
        if (!hasMultipleDexes()) {
            Log.e(TAG, String.format("mergeMethodIds: NO Multiple DexFiles Found: Singular DexFile only."));
            return;
        }

        if (!DEX_NAME.equals(CODE_LIB_DEX_NAME)) {
            Log.i(TAG, String.format("MERGING DEX %s into %s", CODE_LIB_DEX_NAME, DEX_NAME));
            final Dex mergedDex = mergeCodeLibReference(DEX_NAME, dexFile);
            this.dexBuffers.put(DEX_NAME, mergedDex);
            Log.i(TAG, String.format("MERGING DEX %s into %s DONE", CODE_LIB_DEX_NAME, DEX_NAME));
        } else {
            Log.i(TAG, String.format("MERGING DEX %s into %s SKIPPED", CODE_LIB_DEX_NAME, DEX_NAME));
        }
    }

    public void mergeCodeLib() {
        for (Map.Entry<String, Dex> dexfile : dexBuffers.entrySet()) {
            final String DEX_NAME = dexfile.getKey();
            Dex dexFile = dexfile.getValue();
            mergeMethodIds(DEX_NAME, dexFile);
        }
    }

    public String buildApk() {
        Log.i(TAG, "");
        Log.i(TAG, "# Building APK");

        Log.i(TAG, String.format("Building APK: %s (was: %s)", config.app_apk_merged_file_path,
                config.app_apk_file_path));

        InputStream apk_original = null;
        OutputStream apk_injected = null;
        try {
            apk_original = new FileInputStream(new File(config.app_apk_file_path));
            apk_injected = new FileOutputStream(new File(config.app_apk_merged_file_path));
        } catch (final FileNotFoundException e) {
            Log.e(TAG, "", e);
        }
        try (
                ZipInputStream zipInput =
                        new ZipInputStream(new BufferedInputStream(apk_original));
                ZipOutputStream zipOutput =
                        new ZipOutputStream(new BufferedOutputStream(apk_injected));
        ) {
            ZipEntry apkContent;
            int classes_dex_counter = 1;

            while ((apkContent = zipInput.getNextEntry()) != null) {
                ArtistCompilationTask.checkThreadCancellation();
//                Log.d(TAG, "> zipInput: " + apkContent.getName());
                if (apkContent.getName().contains(".dex") && classes_dex_counter == 1) {
                    for (final Dex classesDex : this.dexBuffers.values()) {
                        final String classes_dex_name;

                        if (classes_dex_counter == 1) {
                            classes_dex_name = "classes.dex";
                        } else {
                            classes_dex_name = String.format("classes%d.dex", classes_dex_counter);
                        }
                        Log.i(TAG, "> APK - Writing: " + classes_dex_name);

                        ZipEntry newClassesEntry = new ZipEntry(classes_dex_name);

                        try {
                            zipOutput.putNextEntry(newClassesEntry);
                            zipOutput.write(classesDex.getBytes(), 0, classesDex.getLength());
                            zipOutput.closeEntry();
                        } catch (final IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                        ++classes_dex_counter;
                    }
                } else {
                    byte[] buffer = new byte[1024];
                    int count;
                    try {
                        // Reusing zipInput ZipEntry can leads to error:
                        // java.util.zip.ZipException: invalid entry compressed size
                        //   (expected 5088 but got 5171 bytes)
                        final String fileName = apkContent.getName();
                        final String fileExtension = ("." + FilenameUtils.getExtension(fileName));
                        final ZipEntry zipEntry = new ZipEntry(fileName);
                        if (Config.NO_COMPRESS_EXTENSIONS.contains(fileExtension)) {
                            Log.d(TAG, String.format( "> No Compression: %s " +
                                    "[Method: %d] Size: %d Compressed: %d",
                                    fileName,
                                    apkContent.getMethod(),
                                    apkContent.getSize(),
                                    apkContent.getCompressedSize()));
                            zipEntry.setMethod(ZipEntry.STORED);
                            zipEntry.setSize(apkContent.getSize());
                            // zipEntry.setCompressedSize(apkContent.getCompressedSize());
                            zipEntry.setCrc(apkContent.getCrc());
                        }
                        zipOutput.putNextEntry(zipEntry);
                        while ((count = zipInput.read(buffer)) != -1) {
                            zipOutput.write(buffer, 0, count);
                        }
                        zipOutput.closeEntry();
                    } catch (final IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }
            return config.app_apk_merged_file_path;
        } catch (final IOException e) {
            Log.e(TAG, "", e);
            return "";
        }
    }

    private Dex mergeCodeLibReference(final String dexName, final Dex dexFile) {
        return mergeCodeLibReference(dexName, dexFile, false);
    }

    private Dex mergeCodeLibReference(final String dexName, final Dex dexFile, final boolean saveDexFile) {

        Dex mergedDexContent = null;
        try {
            DexMerger dexMerger = new DexMerger(new Dex[]{dexFile, dexBuffers.get(CODE_LIB_DEX_NAME)}, CollisionPolicy.FAIL);
            mergedDexContent = dexMerger.mergeMethodsOnly();

        } catch (final IOException e) {
            Log.e(TAG, "", e);
        }

        if (saveDexFile) {
            final String outputDexName = dexName.replace(":", "_");
            try (
                    final OutputStream os = new FileOutputStream(new File(outputDexName))
            ) {
                os.write(mergedDexContent.getBytes());
            } catch (final IOException e) {
                Log.e(TAG, "> Could not save DexFile: " + outputDexName, e);
            }
        }
        return mergedDexContent;
    }
}
