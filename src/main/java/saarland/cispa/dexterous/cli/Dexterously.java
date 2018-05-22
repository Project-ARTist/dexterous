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

import comm.android.dex.*;
import comm.android.dex.util.FileUtils;
import comm.android.dx.command.dexer.DxContext;
import comm.android.dx.merge.CollisionPolicy;
import comm.android.dx.merge.DexMerger;
import saarland.cispa.dexterous.Config;
import saarland.cispa.dexterous.DxUtils;
import saarland.cispa.dexterous.MultiDex;
import saarland.cispa.dexterous.stats.ClassDefStats;
import saarland.cispa.dexterous.stats.MethodIdStats;
import trikita.log.Log;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Dexterously {

    private static final String TOOLNAME = "Dexterously";

    private static final String TAG = TOOLNAME;

    public static String CODE_LIB_NAME = "";
    public static String CODE_LIB_DEX_NAME = "";
//    public final static String CODE_LIB_NAME = "codelib.apk";
//    public final static String CODE_LIB_DEX_NAME = "codelib.apk:classes.dex";

    private DxContext context;

    private final Config runConfig;

    Map<String, Dex> dexBuffers = null;

    Set<String> dexSourceFiles = null;

    private String apk_injected_name;

    Map<String, HashSet<String>> javaSourceFiles = null;

    Map<String, HashSet<String>> methodIdsTotal = null;
    Map<String, HashSet<String>> methodIdsFromClasses = null;
    Map<String, HashSet<String>> methodIdsReferencedOnly = null;
    Set<String> methodIdDuplicates = null;
    Map<String, HashSet<MethodIdStats>> methodIdStats = null;

    Map<String, HashSet<String>> classDefsTotal = null;
    Map<String, HashSet<String>> classDefsWithData = null;
    Map<String, HashSet<String>> classDefsWithoutData = null;
    Set<String> classDefDuplicates = null;
    Map<String, HashSet<ClassDefStats>> classDefStats = null;

    public Dexterously(final Config dexterousRunConfig) {
        this.context = new DxContext();
        this.dexBuffers = new LinkedHashMap<>();
        this.dexSourceFiles = new HashSet<>();

        this.runConfig = dexterousRunConfig;
        if (dexterousRunConfig.codelib != null) {
            this.CODE_LIB_NAME = dexterousRunConfig.codelib.getName();
            this.CODE_LIB_DEX_NAME = this.CODE_LIB_NAME + ":classes.dex";
        }

        this.javaSourceFiles = new LinkedHashMap<>();
        // Methods
        this.methodIdsTotal = new LinkedHashMap<>();
        this.methodIdsFromClasses = new LinkedHashMap<>();
        this.methodIdsReferencedOnly = new LinkedHashMap<>();
        this.methodIdDuplicates = new HashSet<>();
        this.methodIdStats = new LinkedHashMap<>();
        // Classes
        this.classDefsTotal = new LinkedHashMap<>();
        this.classDefsWithData = new LinkedHashMap<>();
        this.classDefsWithoutData = new LinkedHashMap<>();
        this.classDefDuplicates = new HashSet<>();
        this.classDefStats = new LinkedHashMap<>();

        this.loadDexFiles();
    }

    public void analyze() {
        dexBuffers.keySet().stream().forEach(
                DEX_NAME -> {
                    Dex dexFile = this.dexBuffers.get(DEX_NAME);

                    Log.i(TAG, "");
                    Log.i(TAG, String.format("# DEXFILE: %s #############################################", DEX_NAME));
                    Log.i(TAG, "");

                    Loggy.printDexHeader(dexFile);

                    logClassDefsWithData(dexFile, DEX_NAME);

                    Log.i(TAG, "");

                    logTypeDefs(dexFile, DEX_NAME);

                    analyzeMethodIds(dexFile, DEX_NAME);

                    Log.i(TAG, "");

                    analyzeProtoIds(dexFile);

                    Log.i(TAG, "");

                    analyzeClassDefs(dexFile, DEX_NAME);

                    Log.i(TAG, "");

                    // analyzeTypeIds(dexFile);

                    logClasses(DEX_NAME);

                    logJavaSourceFileNames(DEX_NAME);

                    Log.i(TAG, "");
                }
        );
        analyzeMethodIdsMultidexDuplicates();
//        analyzeMethodIdsDuplicatesAdvanced();
        analyzeClassDefMultidexDuplicates();
        analyzeClassDefDuplicatesAdvanced();
    }

    private void loadDexFiles() {
        for (final File dexFile : runConfig.dexFiles) {
            final String dexFileName = dexFile.getAbsolutePath();
            this.dexSourceFiles.add(dexFileName);
            //if apk file
            addApkFile(dexFile, dexFileName);
        }
        if (runConfig.codelib != null) {
            addApkFile(runConfig.codelib, runConfig.codelib.getName());
        } else {
            Log.i(TAG, "> No Codelib present");
        }

        initializeStatisticFields(dexBuffers);

        // merge((Vector<Dex>) dexBuffers.values());
        if (hasMultipleDexes()) {
            Log.i(TAG, "# MULTIDEX File. DexFile Count: " + dexBuffers.size());
        } else {
            Log.i(TAG, "# MONODEX: Single Dexfile found");
        }
    }

    private void addApkFile(final File dexFile, final String dexFileName) {
        if (FileUtils.hasArchiveSuffix(dexFileName)) {
            final Map<String, Dex> singleApkDexFiles = MultiDex.loadDexfiles(dexFile);
            dexBuffers.putAll(singleApkDexFiles);
        } else { // if dex file
            try {
                FileInputStream is = new FileInputStream(dexFile);
                dexBuffers.put(dexFileName, new Dex(is));
            } catch (final Exception e) {
                Log.i(TAG, "Opening DexFile failed ", e);
            }
        }
    }

    private void logTypeDefs(final Dex dexFile, final String DEX_NAME) {
        Log.i(TAG, "\n## Typenames ###########");

        dexFile.typeNames().stream().sorted().forEach(
                typeName -> Log.i(TAG, String.format("C: %s", typeName))
        );
    }

    public void summary() {
        dexBuffers.keySet()
                .stream()
                .sorted()
                .forEach(
                        DEX_NAME -> {
                            Log.i(TAG, "");
                            Log.i(TAG, "# DexFile: " + DEX_NAME);
                            Log.i(TAG, String.format("## Classes: %05d | Classes w/ data: %05d | w/o data:              %05d | Combined Calculated: %05d",
                                    classDefsTotal.get(DEX_NAME).size(),
                                    classDefsWithData.get(DEX_NAME).size(),
                                    classDefsWithoutData.get(DEX_NAME).size(),
                                    (classDefsWithData.get(DEX_NAME).size() + classDefsWithoutData.get(DEX_NAME).size())
                            ));
                            Log.i(TAG, String.format("## Methods: %05d | ClassMethods:    %05d | Methods w/o ClassData: %05d | Combined Calculated: %05d",
                                    methodIdsTotal.get(DEX_NAME).size(),
                                    methodIdsFromClasses.get(DEX_NAME).size(),
                                    methodIdsReferencedOnly.get(DEX_NAME).size(),
                                    (methodIdsFromClasses.get(DEX_NAME).size() + methodIdsReferencedOnly.get(DEX_NAME).size())
                            ));

                            Log.d(TAG, "# MethodIds (ReferenceOnly) with ClassDefs");
                            methodIdStats.get(DEX_NAME)
                                    .stream()
                                    .forEach(
                                            methodIdStat -> {
                                                if (methodIdsReferencedOnly.get(DEX_NAME).contains(methodIdStat.NAME)) {
                                                    if (classDefsTotal.get(DEX_NAME).contains(methodIdStat.class_idx)) {
                                                        Log.d(TAG, String.format("Reference Only Method has classdef %s (%s)", methodIdStat.NAME, methodIdStat.class_idx));
                                                    } else {
                                                        Log.d(TAG, String.format("Reference Only Method has NO classdef %s (%s)", methodIdStat.NAME, methodIdStat.class_idx));
                                                    }
                                                }
                                            }
                                    );
                        }
        );

    }

    private boolean hasMultipleDexes() {
        return dexBuffers.size() > 1;
    }

    private void analyzeMethodIdsDuplicatesAdvanced() {
        if (!hasMultipleDexes()) {
            Log.e(TAG, "## Cannot Search Method Duplicates: Only one dexfile present");
            return;
        }
        if (!hasMethodIdDuplicates()) {
            Log.e(TAG, "## Cannot Search Method Duplicates: No Duplicates present");
            return;
        }
        Log.i(TAG, "## analyzeMethodIdsDuplicatesAdvanced");


        Set<String> dexFileNames = methodIdsTotal.keySet();

        Log.i(TAG, "## analyzeMethodIdsDuplicatesAdvanced Methods: " + this.methodIdDuplicates.size());
        this.methodIdDuplicates.stream().forEach(
                methodName -> {
                    Log.i(TAG, "### Method: " + methodName);
                    dexFileNames.stream().forEach(
                            DEX_NAME -> {
                                Set<saarland.cispa.dexterous.stats.MethodIdStats> methodIdStats = this.methodIdStats.get(DEX_NAME);
                                methodIdStats.stream().forEach(
                                        methodStat -> {
                                            if (methodStat.NAME.equals(methodName)) {
                                                Log.i(TAG, methodStat.toString());
                                            }
                                        });
                            }
                    );
                    Log.i(TAG, "");
                }
        );
    }

    private boolean hasMethodIdDuplicates() {
        return this.methodIdDuplicates.size() > 0;
    }

    private boolean hasClassDefDuplicates() {
        return this.classDefDuplicates.size() > 0;
    }

    private void mergeMethodIds(final String DEX_NAME, Dex dexFile) throws DexMerger.MergeException {
        if (!hasMultipleDexes()) {
            Log.e(TAG, String.format("## mergeMethodIds: NO Multiple DexFiles Found: Singular DexFile only."));
            return;
        }

        if (!isCodeLib(DEX_NAME)) {
            Log.i(TAG, String.format("MERGING DEX %s into %s", CODE_LIB_DEX_NAME, DEX_NAME));
            final Dex mergedDex = mergeCodeLibReference(DEX_NAME, dexFile);
            this.dexBuffers.put(DEX_NAME, mergedDex);
            Log.i(TAG, String.format("MERGING DEX %s into %s DONE", CODE_LIB_DEX_NAME, DEX_NAME));
        } else {
            Log.i(TAG, String.format("MERGING DEX %s into %s SKIPPED", CODE_LIB_DEX_NAME, DEX_NAME));
        }
    }

    private boolean isCodeLib(String DEX_NAME) {
        return DEX_NAME.equals(CODE_LIB_DEX_NAME);
    }

    private void analyzeMethodIdsMultidexDuplicates() {
        if (!hasMultipleDexes()) {
            Log.i(TAG, String.format("## MethodIDs: NO MethodIdStats Duplicates Found: Singular DexFile only."));
            return;
        }
        final Set<String> DEX_FILE_NAMES = this.methodIdsTotal.keySet();

        final Set<String> knownMethodIds = new HashSet<>();

        for (final String DEX_NAME : DEX_FILE_NAMES) {
            this.methodIdsTotal.get(DEX_NAME).stream().forEach(
                    methodName -> {
                        if (knownMethodIds.contains(methodName)) {
                            this.methodIdDuplicates.add(methodName);
                        } else {
                            knownMethodIds.add(methodName);
                        }
                    }
            );
        }

        if (hasMethodIdDuplicates()) {
            Log.i(TAG, "");
            Log.i(TAG, String.format("## MethodIDs: %06d Duplicates Found", this.methodIdDuplicates.size()));
            Log.i(TAG, "");
            this.methodIdDuplicates.stream()
                    .sorted()
                    .forEach(methodname -> Log.i(TAG, methodname));
        } else {
            Log.i(TAG, String.format("## MethodIDs: NO MethodID Duplicates Found"));
        }
    }

    private void initializeStatisticFields(Map<String, Dex> dexBuffers) {
        for (Map.Entry<String, Dex> dexfile : dexBuffers.entrySet()) {
            this.javaSourceFiles.put(dexfile.getKey(), new HashSet<>());
            this.methodIdsTotal.put(dexfile.getKey(), new HashSet<>());
            this.classDefsTotal.put(dexfile.getKey(), new HashSet<>());
            this.methodIdsFromClasses.put(dexfile.getKey(), new HashSet<>());
            this.classDefsWithData.put(dexfile.getKey(), new HashSet<>());
            this.classDefsWithoutData.put(dexfile.getKey(), new HashSet<>());
            this.methodIdStats.put(dexfile.getKey(), new HashSet<>());
            this.classDefStats.put(dexfile.getKey(), new HashSet<>());
            this.methodIdsReferencedOnly.put(dexfile.getKey(), new HashSet<>());
        }
    }

    private void logJavaSourceFileNames(final String DEX_NAME) {
        Log.i(TAG, "");
        Log.d(TAG, "## SourceFiles");
        javaSourceFiles.get(DEX_NAME).stream()
                .sorted()
                .forEach(sourceFileName -> Log.d(TAG, sourceFileName));
    }

    private void analyzeProtoIds(final Dex dexi) {
        TableOfContents toc = dexi.getTableOfContents();
        Dex.Section protoIds = dexi.open(toc.protoIds.off);
        for (int i = 0; i < toc.protoIds.size; i++) {
            ProtoId protoId = protoIds.readProtoId();
            Log.d(TAG, String.format("#%06d %s", i, protoId.toString()));
        }
    }

    private void analyzeTypeIds(final Dex dexi) {

        /// Both methods work !
//        Log.i(TAG, "");
//        Log.i(TAG, "# TypeIds Dex");
//        int counter = 0;
//        for(final Integer typeId : dexi.typeIds()) {
//            Log.d(TAG, String.format("#%06d %d (%s)",
//                    counter,
//                    typeId.intValue(),
//                    dexi.strings().get(typeId.intValue())));
//        }

        TableOfContents toc = dexi.getTableOfContents();
        Dex.Section typeIds = dexi.open(toc.typeIds.off);

        /// Both methods work !
        Log.d(TAG, "");
        Log.d(TAG, "# TypeIds Data Offset");

        for (int i = 0; i < toc.typeIds.size; i++) {

            final int typeId = typeIds.readInt();

            Log.d(TAG, String.format("#%06d %d (%s)",
                    i,
                    typeId,
                    dexi.strings().get(typeId)));
        }
    }

    private void analyzeMethodIds(final Dex dexi, final String DEX_NAME) {
        TableOfContents toc = dexi.getTableOfContents();
        Dex.Section methods = dexi.open(toc.methodIds.off);

        Log.d(TAG, String.format("#%6s Class: %6s Proto: %6s Name: %6s", "", "", "", ""));


        for (int i = 0; i < toc.methodIds.size; i++) {
            MethodId methodId = methods.readMethodId();

            if (isMethodIdSpecial(methodId)) {
                Log.i(TAG, String.format("ERROR MethodIdStats: #%06d Class: %06d Proto: %06d Name: %06d %s",
                        i,
                        methodId.getDeclaringClassIndex(),
                        methodId.getProtoIndex(),
                        methodId.getNameIndex(),
                        methodId.toString()
                ));
            }
            Log.d(TAG, String.format("MethodIdStats: #%06d Class: %06d Proto: %06d Name: %06d %s",
                    i,
                    methodId.getDeclaringClassIndex(),
                    methodId.getProtoIndex(),
                    methodId.getNameIndex(),
                    methodId.toString()
            ));
            MethodIdStats methodStat = new MethodIdStats(DEX_NAME, dexi, methodId);

            this.methodIdStats.get(DEX_NAME).add(methodStat);

            this.methodIdsTotal.get(DEX_NAME).add(methodId.toString());
        }
        methodIdsReferencedOnly.get(DEX_NAME).addAll(methodIdsTotal.get(DEX_NAME));
        methodIdsReferencedOnly.get(DEX_NAME).removeAll(methodIdsFromClasses.get(DEX_NAME));
    }

    private boolean isMethodIdSpecial(MethodId methodId) {
        return methodId.getDeclaringClassIndex() == 0
                || methodId.getProtoIndex() == 0
                || methodId.getNameIndex() == 0;
    }

    private void logClassDefsWithData(final Dex dexi, final String DEX_NAME) {

        Log.i(TAG, "# " + DEX_NAME);
        Log.i(TAG, "## ClassDefs: " + DEX_NAME);

        TableOfContents toc = dexi.getTableOfContents();

        Dex.Section classDefs = dexi.open(toc.classDefs.off);
        for (int i = 0; i < toc.classDefs.size; i++) {
            final ClassDef clazzDef = classDefs.readClassDef();
            if (clazzDef.getClassDataOffset() > 0) {
                final String className = DxUtils.getClassName(dexi, clazzDef);
                Log.i(TAG, "- " + className);
            }
        }
    }

    private void analyzeClassDefs(Dex dexi, final String DEX_NAME) {

        Log.i(TAG, "");
        Log.i(TAG, "# ClassDefs: " + DEX_NAME);

        TableOfContents toc = dexi.getTableOfContents();

        Dex.Section classDefs = dexi.open(toc.classDefs.off);
        int allMethodCounter = 0;
        int directMethodCounter = 0;
        int virtualMethodCounter = 0;
        for (int i = 0; i < toc.classDefs.size; i++) {
            ClassDef clazzDef = classDefs.readClassDef();

            final String className = DxUtils.getClassName(dexi, clazzDef);

            Log.d(TAG, className);

            this.classDefsTotal.get(DEX_NAME).add(className);

            ClassDefStats classDefStat = new ClassDefStats(DEX_NAME, dexi, clazzDef);

            this.classDefStats.get(DEX_NAME).add(classDefStat);

            // offset from the start of the file to the associated class data for this item,
            // or 0 if there is no class data for this class.
            // (This may be the case, for example, if this class is a marker interface.)
            if (clazzDef.getClassDataOffset() > 0) {
                Log.d(TAG, String.format("#%06d %s", i, clazzDef.toString()));
                classDefsWithData.get(DEX_NAME).add(clazzDef.toString());
                ClassData clazz = dexi.readClassData(clazzDef);
                ClassData.Method[] clazzMethods = clazz.allMethods();

                int allMethodCount = clazzMethods.length;
                allMethodCounter += allMethodCount;
                int virtualMethodCount = clazz.getVirtualMethods().length;
                virtualMethodCounter += virtualMethodCount;
                int directMethodCount = clazz.getDirectMethods().length;
                directMethodCounter += directMethodCount;
                Log.d(TAG, String.format("  > AllMethods: %04d (Calculated: %04d) Virtual: %04d Direct: %04d",
                        allMethodCount, (directMethodCount + virtualMethodCount), directMethodCount, virtualMethodCount));

                if (clazzDef.getSourceFileIndex() != -1) {
                    final String sourceFileName = dexi.strings().get(clazzDef.getSourceFileIndex());
                    this.javaSourceFiles.get(DEX_NAME).add(sourceFileName);
                }

                for (ClassData.Method method : clazzMethods) {
                    MethodId methodId = dexi.methodIds().get(method.getMethodIndex());
                    Log.d(TAG, String.format("  > %s", methodId.toString()));
                    methodIdsFromClasses.get(DEX_NAME).add(methodId.toString());
                }
            } else {
                Log.d(TAG, String.format("No Class_Data #%06d %s", i, clazzDef.toString()));
                classDefsWithoutData.get(DEX_NAME).add(clazzDef.toString());
            }
        }

        Log.i(TAG, "");
        Log.i(TAG, String.format("# TOTAL Defined Methods:    %05d (Calculated %05d) Virtual: %05d Direct: %05d",
                allMethodCounter, (directMethodCounter + virtualMethodCounter), directMethodCounter, virtualMethodCounter));
        Log.i(TAG, String.format("# TOTAL Referenced Methods: %05d ", toc.methodIds.size));
    }

    private void analyzeClassDefMultidexDuplicates() {
        if (!hasMultipleDexes()) {
            Log.i(TAG, String.format("## ClassDefs: NO ClassDefStats Duplicates Found: Singular DexFile only."));
            return;
        }
        final Set<String> DEX_FILE_NAMES = this.classDefsTotal.keySet();

        final Set<String> knownClasses = new HashSet<>();

        for (final String DEX_NAME : DEX_FILE_NAMES) {
            this.classDefsTotal.get(DEX_NAME).stream().forEach(
                    className -> {
                        if (knownClasses.contains(className)) {
                            this.classDefDuplicates.add(className);
                        } else {
                            knownClasses.add(className);
                        }
                    }
            );
        }

        if (hasClassDefDuplicates()) {
            Log.i(TAG, "");
            Log.i(TAG, String.format("## ClassDefs: %06d Duplicates Found", this.classDefDuplicates.size()));
            Log.i(TAG, "");
            this.classDefDuplicates.stream()
                    .sorted()
                    .forEach(className -> Log.i(TAG, className));
        } else {
            Log.i(TAG, String.format("## ClassDefs: NO ClassDefStats Duplicates Found"));
        }
    }

    private void analyzeClassDefDuplicatesAdvanced() {
        if (!hasMultipleDexes()) {
            Log.e(TAG, "## Cannot Search ClassDefStats Duplicates: Only one dexfile present");
            return;
        }
        if (!hasClassDefDuplicates()) {
            Log.e(TAG, "## Cannot Search ClassDefStats Duplicates: No Duplicates present");
            return;
        }
        Log.i(TAG, "## analyzeClassDefDuplicatesAdvanced");


        Set<String> dexFileNames = classDefsTotal.keySet();

        Log.i(TAG, "## analyzeClassDefDuplicatesAdvanced Classes: " + this.classDefDuplicates.size());
        this.classDefDuplicates.stream().forEach(
                className -> {
                    Log.i(TAG, "### Class: " + className);
                    dexFileNames.stream().forEach(
                            DEX_NAME -> {
                                Set<ClassDefStats> classDefStats = this.classDefStats.get(DEX_NAME);
                                classDefStats.stream().forEach(
                                        classStat -> {
                                            if (classStat.NAME.equals(className)) {
                                                Log.i(TAG, classStat.toString());
                                            }
                                        });
                            }
                    );
                    Log.i(TAG, "");
                }
        );
    }

    private void logClasses(final String DEX_NAME) {
        Log.d(TAG, "");
        Log.d(TAG, "## ClassDefs");
        logClassesWithoutData(DEX_NAME);
        logClassesWithData(DEX_NAME);
    }

    private void logClassesWithoutData(final String DEX_NAME) {
        Log.d(TAG, "");
        Log.d(TAG, "### Classes (NO DATA)");
        Log.d(TAG, "");
        classDefsWithoutData.get(DEX_NAME).stream()
                .sorted()
                .forEach(
                        clazz -> Log.d(TAG, clazz)
                );
    }

    private void logClassesWithData(final String DEX_NAME) {
        Log.d(TAG, "");
        Log.d(TAG, "### Classes (DATA)");
        Log.d(TAG, "");
        classDefsWithData.get(DEX_NAME).stream()
                .sorted()
                .forEach(
                        clazz -> Log.d(TAG, clazz)
                );
    }

    public void mergeCodeLib() throws DexMerger.MergeException {
        dexBuffers.get(CODE_LIB_DEX_NAME).setWhitelistedAnnotation("Lsaarland/cispa/artist/codelib/CodeLib$Inject;");
        for (Map.Entry<String, Dex> dexfile : dexBuffers.entrySet()) {
            final String DEX_NAME = dexfile.getKey();
            Dex dexFile = dexfile.getValue();
            mergeMethodIds(DEX_NAME, dexFile);
        }
    }

    public void buildApk() {
        Log.i(TAG, "");
        Log.i(TAG, "# Building APK");

        for (final String dexSourceName : this.dexSourceFiles) {
            if (dexSourceName.contains(this.CODE_LIB_NAME)) {
                Log.i(TAG, "");
                Log.i(TAG, "Building APK: Skipping " + dexSourceName);
                continue;
            }
            final String apk_injected_name = dexSourceName.replace(".apk", "_injected.apk");
            this.apk_injected_name = apk_injected_name;

            Log.i(TAG, String.format("Building APK: %s (was: %s)", apk_injected_name, dexSourceName));

            InputStream apk_original = null;
            try {
                apk_original = new FileInputStream(new File(dexSourceName));
            } catch (final FileNotFoundException e) {
                Log.e(TAG, e);
            }
            OutputStream apk_injected = null;
            try {
                apk_injected = new FileOutputStream(new File(apk_injected_name));
            } catch (final FileNotFoundException e) {
                Log.e(TAG, e);
            }
            try (ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(apk_original))) {
                try (ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(apk_injected))) {

                    ZipEntry apkContent;

                    int classes_dex_counter = 1;

                    while ((apkContent = zipInput.getNextEntry()) != null) {

                        if (apkContent.getName().contains(".dex") && classes_dex_counter == 1) {

                            for (Dex classes_dex : this.dexBuffers.values()) {

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
                                    zipOutput.write(classes_dex.getBytes(), 0, classes_dex.getLength());
                                    zipOutput.closeEntry();
                                } catch (final IOException e) {
                                    Log.e(TAG, e);
                                }
                                ++classes_dex_counter;
                            }
                        } else {
                            byte[] buffer = new byte[1024];
                            int count;
                            try {
                                zipOutput.putNextEntry(apkContent);
                                while ((count = zipInput.read(buffer)) != -1) {
                                    zipOutput.write(buffer, 0, count);
                                }
                                zipOutput.closeEntry();
                            } catch (final IOException e) {
                                Log.e(TAG, e);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, e);
                }
            } catch (IOException e) {
                Log.e(TAG, e);
            }

        }
    }

    public void signApk() {
        Log.i(TAG, "");
        Log.i(TAG, "# Signing APK");
        if (this.apk_injected_name != null
                && new File(this.apk_injected_name).exists()) {
            signApk(this.apk_injected_name);
        } else {
            Log.e(TAG, "> Could not sign APK: " + this.apk_injected_name);
        }
    }

    private void signApk(final String apkPath) {

        ApkSir apkSir = new ApkSir();

        try {
            apkSir.signApk(apkPath);
        } catch (final Exception e) {
            Log.e(TAG, "> Signing of APK Failed");
            Log.e(TAG, e);
        }
    }
    public Dex mergeCodeLibReference(final String dexName, final Dex dexFile) throws DexMerger.MergeException {
        return mergeCodeLibReference(dexName, dexFile, true);
    }

    public Dex mergeCodeLibReference(final String dexName, final Dex dexFile, final boolean saveDexFile) throws DexMerger.MergeException {
        Dex mergedDexContent = null;
        DexMerger dexMerger = null;
        try {
            dexMerger = new DexMerger(
                    new Dex[]{dexFile, dexBuffers.get(CODE_LIB_DEX_NAME)},
                    CODE_LIB_DEX_NAME,
                    CollisionPolicy.FAIL,
                    this.context
            );
        } catch (IOException e) {
            throw new DexMerger.MergeException(e);
        }
        mergedDexContent = dexMerger.mergeMethodsOnly();

        if (saveDexFile) {
            try {
                final String outputDexName = dexName.replace(":", "_");
                OutputStream os = new FileOutputStream(new File(outputDexName));
                os.write(mergedDexContent.getBytes());
            } catch (final FileNotFoundException e) {
                Log.e(TAG, e);
            } catch (final IOException e) {
                Log.e(TAG, e);
            }
        }
        return mergedDexContent;
    }

    public void info() {
        dexBuffers.keySet().stream().forEach(
                DEX_NAME -> {
                    Dex dexFile = this.dexBuffers.get(DEX_NAME);
                    Log.i(TAG, "");
                    Log.i(TAG, String.format("# DEXFILE: %s #############################################", DEX_NAME));
                    Log.i(TAG, "");
                    Loggy.printDexHeader(dexFile);
                }
        );
    }

    public void mergeDexfiles() {
        mergeDexfiles(true);
    }

    public Dex mergeDexfiles(final boolean saveDexFile) {
        final String outputDexName = "merged.dex";
        Log.i(TAG, String.format("MERGING %d dexfiles into %s", dexBuffers.values().size(), outputDexName));
        Dex mergedDexContent = null;
        try {
            DexMerger dexMerger = new DexMerger(
                    dexBuffers.values().toArray(new Dex[dexBuffers.values().size()]),
                    CODE_LIB_DEX_NAME,
                    CollisionPolicy.FAIL,
                    this.context
            );
            mergedDexContent = dexMerger.merge();
        } catch (final IOException e) {
            Log.e(TAG, e);
        }
        if (saveDexFile) {
            try {
                OutputStream os = new FileOutputStream(new File(outputDexName));
                os.write(mergedDexContent.getBytes());
            } catch (final FileNotFoundException e) {
                Log.e(TAG, e);
            } catch (final IOException e) {
                Log.e(TAG, e);
            }
        }
        return mergedDexContent;
    }
}
