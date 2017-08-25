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
package saarland.cispa.artist;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import saarland.cispa.apksigner.ApkSigner;
import saarland.cispa.apksigner.ApkZipSir;
import saarland.cispa.artist.gui.artist.ArtistGuiProgress;
import saarland.cispa.artist.log.Logg;
import saarland.cispa.artist.settings.ArtistRunConfig;
import saarland.cispa.artist.utils.AndroidUtils;
import saarland.cispa.artist.utils.ArtistInterruptedException;
import saarland.cispa.artist.utils.ArtistUtils;
import saarland.cispa.artist.utils.CompilationException;
import saarland.cispa.artist.utils.ProcessExecutor;
import saarland.cispa.dexterous.Dexterous;
import trikita.log.Log;

/**
 * @author Sebastian Weisgerber (weisgerber@cispa.saarland)
 * @author Oliver Schranz (oliver.schranz@cispa.saarland)
 */
public class ArtistImpl implements Artist {

    private static final String TAG = "ArtistImpl";

    public static final String INTENT_EXTRA_APP_NAME = "AppPackageName";
    public static final String INTENT_EXTRA_FAIL_REASON = "FailReason";

    public final static int COMPILATION_SUCCESS = 1;
    public final static int COMPILATION_CANCELED = 0;
    public final static int COMPILATION_ERROR = -1;

    private ArtistRunConfig config;

    private ArtistGuiProgress guiProgress = null;

    public ArtistImpl(final ArtistRunConfig artistRunConfig) {
        this.config = artistRunConfig;
    }

    @Override
    public void addGuiProgressListener(@Nullable final ArtistGuiProgress callback) {
        this.guiProgress = callback;
    }

    public void progressUpdate(final int progress, final String message) {
        if (guiProgress != null) {
            guiProgress.updateProgress(progress, message);
        }
    }

    public void progressUpdateVerbose(final int progress, final String message) {
        if (guiProgress != null) {
            guiProgress.updateProgressVerbose(progress, message);
        }
    }

    public void progressFailed(final String message) {
        if (guiProgress != null) {
            guiProgress.doneFailed(message);
        }
    }

    public void progressSucess(final String message) {
        if (guiProgress != null) {
            guiProgress.doneSuccess(message);
        }
    }

    @Override
    public void init(final Context context) {
        AndroidUtils.createFoldersInFilesDir(context, config.artist_exec_path);
        AndroidUtils.createFoldersInFilesDir(context, config.artist_exec_path_libs_dir);
    }

    /**
     * @param context
     * @param config
     * @return
     * @throws IOException
     */
    @Override
    public boolean mergeCodeLib(final Context context,
                                final ArtistRunConfig config) {
        Log.d(TAG, "MergeCodeLib into: " + config.app_apk_file_path);

        String pathToApkSigned;
        // deactivate injection upon user wish or if no code lib is provided
        if (config.INJECT_CODELIB && config.codeLib != null) {
            progressUpdateVerbose(-1, "Injecting CodeLib");
            final File appApk = new File(config.app_apk_file_path);
            final File codeLibApk = config.codeLib;

            setupCodeLib(context);

            Dexterous dexterous = new Dexterous(config);
            dexterous.init(appApk, codeLibApk);
            dexterous.mergeCodeLib();
            final String pathToApk = dexterous.buildApk();
            progressUpdateVerbose(-1, "Resigning APK");
            Log.d(TAG, String.format("MergeCodeLib DONE (%s)", pathToApk));

            pathToApkSigned = resignApk(pathToApk);
            Log.d(TAG, String.format("MergeCodeLib Signing DONE (%s)", pathToApkSigned));
            return !pathToApkSigned.isEmpty();
        } else {
            progressUpdateVerbose(-1, "Not Injecting CodeLib");
            Log.i(TAG, String.format("Skip CodeLib Injection " +
                    "(Inject CodeLib: %b)",
                    config.INJECT_CODELIB));
            Log.d(TAG, "MergeCodeLib SKIPPED");
            return true;
        }
    }

    private String resignApk(final String unsignedApkPath) {
        Log.d(TAG, "resignApk() " + unsignedApkPath);

        String signedApkPath;
        final ApkSigner apkSir = new ApkZipSir(config);
        try {
            signedApkPath = apkSir.signApk(config.keystore.getAbsolutePath(), unsignedApkPath);
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "> Signing of APK Failed", e);
            signedApkPath = "";
        }
        return signedApkPath;
    }

    @Override
    public boolean Run(final Context context) {
        Log.i(TAG, "Run() compiling and starting " + config.app_name);
        Log.i(TAG, "> app_name:    " + config.app_name);
        Log.i(TAG, "> apkPath:     " + config.app_apk_file_path);
        Log.i(TAG, "> packageName: " + config.app_package_name);
        Log.i(TAG, "> codeLibName: " + config.codeLibName);
        Log.i(TAG, "> Keystore:    " + config.keystore);

        boolean success = false;
        try {
            progressUpdate(10, "Cleaning Build Files");

            ArtistCompilationTask.checkThreadCancellation();

            cleanBuildFiles();

            progressUpdate(20, "Setup Keystore");

            ArtistCompilationTask.checkThreadCancellation();

            setupKeystore(context);

            progressUpdate(30, "Setup dex2artist");

            ArtistCompilationTask.checkThreadCancellation();

            final String pathDex2oat = copyAssetToFilesDir(context,
                    config.asset_path_dex2oat,
                    config.artist_exec_path_dex2oat);
            Log.i(TAG, "> pathDex2oat: " + pathDex2oat);
            Log.i(TAG, "  > config:    " + config.artist_exec_path_dex2oat);

            if (pathDex2oat.isEmpty()) {
                throw new CompilationException("Artist: Dex2oat Setup failed");
            }

            ArtistCompilationTask.checkThreadCancellation();

            setupArtistLibraries(context, config);

            ArtistCompilationTask.checkThreadCancellation();

            if (ArtistUtils.isMultiDex(config.app_apk_file_path) && config.MULTIDEX_ABORT) {
                progressUpdateVerbose(-1, "Aborting Compilation: MultiDex APK found");
                throw new CompilationException(String.format("Run() Multidex ABORT (APK: %s)", config.app_apk_file_path));
            }

            ArtistCompilationTask.checkThreadCancellation();

            probeOatFilePermissions();

            ArtistCompilationTask.checkThreadCancellation();

            deleteOatFile();

            progressUpdate(40, "Merging CodeLib");

            ArtistCompilationTask.checkThreadCancellation();

            success = mergeCodeLib(context, config);
            if (!success) {
                throw new CompilationException("Codelib Merge Failed");
            }

            ArtistCompilationTask.checkThreadCancellation();

            backupMergedApk(this.config);

            ArtistCompilationTask.checkThreadCancellation();

            backupOriginalApk(this.config);

            if (this.config.REPLACE_BASE_APK) {
                Log.i(TAG, "Replacing the original base.apk");
                replaceAppApkWithMergedApk(config);
            } else {
                Log.i(TAG, "Leaving the original base.apk untouched");
            }
            progressUpdate(50, "Compiling: " + config.app_name);

            ArtistCompilationTask.checkThreadCancellation();

            success = recompileApp(context, pathDex2oat);
            if (!success) {
                throw new CompilationException("Artist Injection Failed");
            }
            progressUpdate(90, "Compilation done, Cleaning");

            ArtistCompilationTask.checkThreadCancellation();

            success = fixOatFilePermissions(config);

            ArtistCompilationTask.checkThreadCancellation();

            cleanBuildFiles();

        } catch (final CompilationException|ArtistInterruptedException e) {
            Log.e(TAG, "Artist Run() FAILED " + e.getMessage());
            progressFailed(String.format("Injection: %s Failed (%s)", config.app_name, e.getMessage()));
            cleanBuildFiles();
            success = false;
            return success;
        } catch (final Exception e) {
            Log.e(TAG, "Artist Run() FAILED: ", e);
            progressFailed(String.format("Injection: %s Failed", config.app_name));
            success = false;
            return success;
        }
        final String userMessage = String.format("Injection: %s OK: %b", config.app_name, success);
        Log.d(TAG, userMessage);
        progressSucess(userMessage);
        return success;
    }

    private void deleteOatFile() {
        progressUpdateVerbose(-1, "Deleting existing oat file: " + config.app_oat_file_path);
        boolean success = deleteRootFile(config.app_oat_file_path);
        if (!success) {
            Log.d(TAG, String.format("Failed to delete old base oat: %s - Continue", config.app_oat_file_path));
        }
    }

    private void cleanBuildFiles() {
        progressUpdateVerbose(-1, "Deleting Build Relicts.");
        progressUpdateVerbose(-1, "Deleting: " + config.app_apk_merged_file_path);
        AndroidUtils.deleteExistingFile(config.app_apk_merged_file_path);
        progressUpdateVerbose(-1, "Deleting: " + config.app_apk_merged_signed_file_path);
        AndroidUtils.deleteExistingFile(config.app_apk_merged_signed_file_path);
        if (config.isAssetCodeLib()) {
            progressUpdateVerbose(-1, "Deleting: " + config.codeLib);
            AndroidUtils.deleteExistingFile(config.codeLib);
        }
    }

    private void setupArtistLibraries(final Context context, final ArtistRunConfig config) {
        Log.d(TAG, "setupArtistLibraries()");
        progressUpdateVerbose(-1, "Copying Libraries to: " + config.artist_exec_path_libs_dir);
        // Example Permissions: /data/app/com.deepinc.arte360-1/lib/arm/
        // -rwxr-xr-x 1 system system  511044 2016-05-05 17:30 libTBAudioEngine.so
        // -rwxr-xr-x 1 system system  771432 2016-05-05 17:30 libmetadataparser.so
        // -rwxr-xr-x 1 system system 1117296 2016-05-05 17:30 libvideorenderer.so
        // -rwxr-xr-x 1 system system  677784 2016-05-05 17:30 libvrtoolkit.so
        AndroidUtils.copyAssetFolderContent(context,
                config.asset_path_dex2oat_libs,
                config.artist_exec_path_libs_dir);
    }

    public boolean recompileApp(Context context, String pathDex2oat) {
        boolean success;
        final String cmd_dex2oat_compile = setupDex2oatCommand(context, pathDex2oat);

        Log.d(TAG, "dex2oat command:");
        Log.d(TAG, cmd_dex2oat_compile);
        Log.d(TAG, "Starting the compilation process!");
        Log.d(TAG, "> Result will get placed at: " + config.app_oat_file_path);

        Log.d(TAG, Logg.BigDivider());

        success = ProcessExecutor.execute(cmd_dex2oat_compile, true,
                ProcessExecutor.processName(config.app_name, "dex2artist"));

        Log.d(TAG, Logg.BigDivider());

        if (success) {
            Log.d(TAG, "Compilation was successfull");
        } else {
            Log.d(TAG, "Compilation failed...");
        }
        return success;
    }

    public void probeOatFilePermissions() {
        progressUpdateVerbose(-1, "Probing oat file permissions: " + config.app_oat_file_path);
        config.oatOwner = AndroidUtils.getFileOwnerUid(config.app_oat_file_path);
        config.oatGroup = AndroidUtils.getFileGroupId(config.app_oat_file_path);
        config.oatPermissions = AndroidUtils.getFilePermissions(config.app_oat_file_path);
        config.stats.oatFileSizeOriginal = new File(config.app_oat_file_path).length();
        Log.d(TAG, String.format("base.odex UID: %s GID: %s Permissions: %s Size: %s",
                config.oatOwner,
                config.oatGroup,
                config.oatPermissions,
                config.stats.oatFileSizeOriginal));
    }

    public boolean fixOatFilePermissions(final ArtistRunConfig config) {
        boolean success = false;
        final File oatFile = new File(this.config.app_oat_file_path);
        if (oatFile.exists() && !oatFile.isDirectory()) {
            Log.d(TAG, "Success! Oat file created.");
            progressUpdateVerbose(-1, "Fixing oat file permissions");

            config.stats.oatFileSizeRecompiled = oatFile.length();

            progressUpdateVerbose(-1, "odex OLD size: " + config.stats.oatFileSizeOriginal);
            Log.d(TAG, "odex OLD size: " + config.stats.oatFileSizeOriginal);
            progressUpdateVerbose(-1, "odex NEW size: " + config.stats.oatFileSizeRecompiled);
            Log.d(TAG, "odex NEW size: " + config.stats.oatFileSizeRecompiled);

            Log.d(TAG, "Changing the owner of the oat file to " + config.oatOwner);

            final String cmd_chown_oat = "chown " + config.oatOwner + " " + this.config.app_oat_file_path;
            success = ProcessExecutor.execute(cmd_chown_oat, true, ProcessExecutor.processName(config.app_name, "chown_oatfile"));

            if (!success) {
                Log.d(TAG, "Could not change oat owner to " + config.oatOwner + "... ");
                Log.d(TAG, "... for path " + this.config.app_oat_file_path);
                return success;
            }
            Log.d(TAG, "Changing the group of the oat file to " + config.oatGroup);

            final String cmd_chgrp_oat = "chgrp " + config.oatGroup + " " + this.config.app_oat_file_path;
            success = ProcessExecutor.execute(cmd_chgrp_oat, true, ProcessExecutor.processName(config.app_name, "chgrp_oatfile"));

            if (!success) {
                Log.d(TAG, "Could not change oat group to " + config.oatGroup + "... ");
                Log.d(TAG, "... for path " + this.config.app_oat_file_path);
                return success;
            }

            success = AndroidUtils.chmodExecutable(this.config.app_oat_file_path);

            if (!success) {
                Log.d(TAG, "Could not change oat permissions to 777");
                Log.d(TAG, "... for path " + this.config.app_oat_file_path);
                return success;
            }
            Log.d(TAG, "Everything worked out as expected!!!");
        } else {
            Log.d(TAG, "Fail! Oat file not created.");
        }
        return success;
    }

    public String setupDex2oatCommand(final Context context, final String pathDex2oat) {
        String cmd_dex2oat_compile =
                "export LD_LIBRARY_PATH=" + context.getApplicationInfo().nativeLibraryDir + ":"
                        + AndroidUtils.getFilesDirLocation(context, config.artist_exec_path_libs_dir) + ";"
                        + pathDex2oat
                        + " --oat-file=" + config.app_oat_file_path
                        + " --compiler-backend=Optimizing"
                        + " --compiler-filter=everything"
                        + " --generate-debug-info"
                        + " --compile-pic";
        if (this.config.REPLACE_BASE_APK) {
            cmd_dex2oat_compile += " --dex-file="  + config.app_apk_file_path;
            cmd_dex2oat_compile += " --dex-location="  + config.app_apk_file_path;
        } else {
            cmd_dex2oat_compile += " --dex-file="  + config.app_apk_merged_signed_file_path;
            cmd_dex2oat_compile += " --dex-location="  + config.app_apk_file_path;
            cmd_dex2oat_compile += " --checksum-rewriting";
        }

        if (config.COMPILER_THREADS != -1) {
            cmd_dex2oat_compile += " -j" + config.COMPILER_THREADS;
        }

        Log.d(TAG, "Dex2oat: Compiler Threads: " + config.COMPILER_THREADS);

        if (config.CODELIB_SETTINGS_APPCOMPART) {
            String launchActivity = getAppEntrance(context, config.app_name);
            Log.d(TAG, "Dex2oat: CodeLib Special: --launch-activity: " + launchActivity);
            cmd_dex2oat_compile += " --launch-activity=" + launchActivity;
        }

        if (config.app_oat_architecture.contains("arm64")) {
            // ARM64 Special Flags
            cmd_dex2oat_compile += " --instruction-set=arm64";
            cmd_dex2oat_compile += " --instruction-set-features=smp,a53";
            cmd_dex2oat_compile += " --instruction-set-variant=denver64";
            cmd_dex2oat_compile += " --instruction-set-features=default";
            // ARM64 Special Flags END
            Log.d(TAG, "Compiling for 64bit Architecture!");
        }
        return cmd_dex2oat_compile;
    }

    public boolean deleteRootFile(final String filePath) {
        boolean success = false;
        final String cmd_rm_root_file = "rm " + filePath;
        success = ProcessExecutor.execute(cmd_rm_root_file, true, ProcessExecutor.processName(config.app_name, "rm_rootfile"));
        return success;
    }

    private String copyAssetToFilesDir(final Context context,
                                       final String assetPathRelative,
                                       final String filePathRelative) {
        final String filesDirPath = AndroidUtils.getFilesDirLocation(context, filePathRelative);
        // AndroidUtils.deleteExistingFile(filesDirPath);
        AndroidUtils.copyAsset(context, assetPathRelative, filesDirPath);
        boolean success = AndroidUtils.chmodExecutable(filesDirPath);
        if (success) {
            return filesDirPath;
        } else {
            return "";
        }
    }

    private void setupKeystore(final Context context) {
        Log.d(TAG, "setupKeystore()");
        progressUpdateVerbose(-1, "KeyStore: " + config.keystore);
        AndroidUtils.copyAsset(context, config.asset_path_keystore, config.keystore.getAbsolutePath());
    }

    private String replaceAppApkWithMergedApk(final ArtistRunConfig config) {
        Log.d(TAG, "replaceAppApkWithMergedApk()");
        progressUpdateVerbose(-1, "Replacing original APk with merged APK");
        final String packageName = config.app_package_name;
        final String apkPath = config.app_apk_file_path;
        final String mergedApkPath = config.app_apk_merged_signed_file_path;

        final String baseApkUid = AndroidUtils.getFileOwnerUid(apkPath);
        final String baseApkGid = AndroidUtils.getFileGroupId(apkPath);
        final String baseApkPerms = AndroidUtils.getFilePermissions(apkPath);

        Log.d(TAG, "APK: " + apkPath);
        Log.d(TAG, String.format("> UID: %s GID: %s (Permissions: %s)",
                baseApkUid, baseApkGid, baseApkPerms));

        boolean success = false;
        final String cmd_backup_base_apk = "rm " + apkPath;
        success = ProcessExecutor.execute(cmd_backup_base_apk, true, ProcessExecutor.processName(config.app_name, "rm_backup"));
        if (!success) {
            return "";
        }
        final String cmd_copy_merged_apk = "cp " + mergedApkPath + " " + apkPath;
        success = ProcessExecutor.execute(cmd_copy_merged_apk, true, ProcessExecutor.processName(config.app_name, "cp_backup_merged"));

        if (!success) {
            return "";
        }
        AndroidUtils.setFileUid(apkPath, baseApkUid);
        AndroidUtils.setFileGid(apkPath, baseApkGid);
        AndroidUtils.setFilePermissions(apkPath, baseApkPerms);

        return packageName;
    }

    public String getAppEntrance(final Context context, final String packageName) {
        Log.d(TAG, "getAppEntrance packageName: " + packageName);
        String launcherActivity = null;
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> packageInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        String applicationname = null;
        try {
            applicationname = context.getPackageManager().getApplicationInfo(packageName, 0).className;
            Log.i(TAG,  "ClassName: " + applicationname);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if(applicationname != null) {
            launcherActivity = applicationname + ".onCreate";
            Log.i(TAG, "AppEntrance : " + launcherActivity);
            return launcherActivity;
        }
        for (ResolveInfo packageInfo : packageInfos) {
            String packagename = packageInfo.activityInfo.packageName;
            if(packageName.equals(packagename)) {
                launcherActivity = packageInfo.activityInfo.name + ".onCreate";
                Log.i(TAG, packagename + " : " + launcherActivity);
            }
        }
        return launcherActivity;
    }

    private void backupMergedApk(final ArtistRunConfig config) {
        if (!config.BACKUP_APK_MERGED) {
            Log.v(TAG, "Skip: backupMergedApk()");
            return;
        }
        Log.v(TAG, "backupMergedApk()");

        final File sdcard = Environment.getExternalStorageDirectory();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date date = new Date();

        final String mergedApkBackupPath = sdcard.getAbsolutePath() + File.separator
                + config.app_package_name + "_merged_signed_" + dateFormat.format(date) + ".apk";

        progressUpdateVerbose(-1, "Backing up Merged APK: " + mergedApkBackupPath);

        final String cmd_backup_merged_apk = "cp " + config.app_apk_merged_signed_file_path + " " + mergedApkBackupPath;

        boolean success = ProcessExecutor.execute(cmd_backup_merged_apk, true, ProcessExecutor.processName(config.app_name, "cp_backup_merged"));

        if (success) {
            Log.d(TAG, "backupMergedApk() Success: " + mergedApkBackupPath);
        } else {
            Log.e(TAG, "backupMergedApk() Failed:  " + mergedApkBackupPath);
        }
    }

    private void backupOriginalApk(final ArtistRunConfig config) {
        if (!config.BACKUP_APK_ORIGINAL) {
            Log.v(TAG, "Skip: backupOriginalApk()");
            return;
        }
        Log.v(TAG, "backupOriginalApk()");
        final File sdcard = Environment.getExternalStorageDirectory();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date date = new Date();

        final String originalApkBackupPath = sdcard.getAbsolutePath() + File.separator
                + config.app_package_name + "_original_" + dateFormat.format(date) + ".apk";
        final String cmd_backup_merged_apk = "cp " + config.app_apk_file_path + " " + originalApkBackupPath;

        progressUpdateVerbose(-1, "Backing up Merged APK: " + originalApkBackupPath);

        boolean success = ProcessExecutor.execute(cmd_backup_merged_apk, true, ProcessExecutor.processName(config.app_name, "cp_backup_merged"));

        if (success) {
            Log.d(TAG, "backupOriginalApk() Success: " + originalApkBackupPath);
        } else {
            Log.e(TAG, "backupOriginalApk() Failed:  " + originalApkBackupPath);
        }
    }

    public void setupCodeLib(final Context context) {
        if (config.codeLibName.startsWith(ArtistUtils.CODELIB_ASSET)) {
            Log.d(TAG, "setupCodeLib() " + config.codeLibName);
            final String assetName = config.codeLibName.replaceFirst(ArtistUtils.CODELIB_ASSET, "");
            AndroidUtils.copyAsset(context, "codelib" + File.separator + assetName,
                    config.codeLib.getAbsolutePath());
            if (!config.codeLib.exists()) {
                Log.e(TAG, " setupCodeLib: " + config.codeLib + " FAILED");
            } else {
                Log.d(TAG, " setupCodeLib: " + config.codeLib + " READY");
            }
        }
    }
}
