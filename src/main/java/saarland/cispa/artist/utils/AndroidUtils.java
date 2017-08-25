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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

public class AndroidUtils {

    private static final String TAG = Logg.TAG;

    public static final int FILE_BUFFER_DEFAULT_SIZE = 4096;

    public static String getFilesDirLocation(final Context context, final String fileName) {
        return getFilesDirLocation(context) + fileName;
    }

    public static String getFilesDirLocation(final Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator;
    }

    public static void copyAssetFolderContent(final Context context,
                                              final String assetFolderPath,
                                              final String toFolderPath) {
        final String cleanedAssetFolderPath = prepareAssetFolderPath(assetFolderPath);

        Log.d(TAG, "copyAssetFolderContent " + cleanedAssetFolderPath + " -> " + toFolderPath);
        final String filesDirFolder = createFoldersInFilesDir(context, toFolderPath);
        try {
            final String[] assetFiles = context.getAssets().list(cleanedAssetFolderPath);
            for (final String assetFile : assetFiles) {
                final String assetFilePath = cleanedAssetFolderPath + File.separator + assetFile;
                copyAsset(context, assetFilePath,  filesDirFolder + File.separator + assetFile);
            }
        } catch (final IOException e) {
            Log.e(TAG, "copyAssetFolderContent " + cleanedAssetFolderPath + " -> " + toFolderPath + " FAILED");
        }
    }

    public static String createFoldersInFilesDir(final Context context, final String toFolderPath) {
        final String filesDirPath = AndroidUtils.getFilesDirLocation(context, toFolderPath);
        return createFolders(filesDirPath);
    }

    public static String createFolders(final String toFolderPath) {
        Log.d(TAG, "Create Folder(s): " + toFolderPath);
        final File toFolderFile = new File(toFolderPath);
        if (!toFolderFile.exists()) {
            toFolderFile.mkdirs();
        }
        return toFolderFile.getAbsolutePath();
    }

    /** Trims and Removes trailing '/' because AssetManager.list()
     *  lists nothing with a trailing '/'
     *
     * @param assetFolderPath
     * @return cleaned assetFolderPath without trailing '/'
     */
    public static String prepareAssetFolderPath(final String assetFolderPath) {
        String cleanedAssetFolderPath = assetFolderPath.trim();
        if (cleanedAssetFolderPath.endsWith(File.separator)) {
            cleanedAssetFolderPath = assetFolderPath.substring(0, assetFolderPath.length() - 1);
        }
        return cleanedAssetFolderPath;
    }

    public static void copyAssetRoot(final Context context,
                                 final String assetPath,
                                 final String toPath) {
        copyAsset(context, assetPath, toPath, true);
    }

    public static void copyAsset(final Context context,
                                 final String assetPath,
                                 final String toPath) {
        copyAsset(context, assetPath, toPath, false);
    }

    public static void copyAsset(final Context context,
                                 final String assetPath,
                                 final String toPath,
                                 final boolean rootCopy) {
        Log.d(TAG, "copyAsset() " + assetPath + " -> " + toPath);

        deleteExistingFile(toPath);

        try (
                InputStream in = context.getAssets().open(assetPath);
                FileOutputStream out = new FileOutputStream(toPath);
        ) {
            int read;
            byte[] buffer = new byte[FILE_BUFFER_DEFAULT_SIZE];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            Log.d(TAG, "copyAsset() " + assetPath + " -> " + toPath + " Done");
        } catch (final IOException e) {
            Log.e(TAG, "copyAsset() " + assetPath + " -> " + toPath + " ERROR", e);
        }
    }

    public static String getFileOwnerUid(final String path) {
        final String cmd_stat_uid = "stat -c '%u' " + path;
        final StringBuffer returnValue = new StringBuffer();

        final boolean success = ProcessExecutor.execute(cmd_stat_uid, true, returnValue);

        if (!success) {
            Log.e(TAG, "ERROR with command: " + cmd_stat_uid);
            return "";
        }
        return removeAllWhitespaces(returnValue);
    }

    public static String removeAllWhitespaces(final String string) {
        return string.replaceAll("\\s+", "");
    }

    public static String removeAllWhitespaces(final StringBuffer string) {
        return removeAllWhitespaces(string.toString());
    }

    public static String getFileGroupId(final String path) {
        final String cmd_stat_gid = "stat -c '%g' " + path;
        final StringBuffer returnValue = new StringBuffer();

        final boolean success = ProcessExecutor.execute(cmd_stat_gid, true, returnValue);

        if (!success) {
            Log.e(TAG, "ERROR with command: " + cmd_stat_gid);
            return "";
        }
        return removeAllWhitespaces(returnValue);
    }

    public static String getFilePermissions(final String path) {
        final String cmd_stat_perms = "stat -c '%a' " + path;
        final StringBuffer returnValue = new StringBuffer();

        final boolean success = ProcessExecutor.execute(cmd_stat_perms, true, returnValue);

        if (!success) {
            Log.e(TAG, "ERROR with command: " + cmd_stat_perms);
            return "";
        }
        return removeAllWhitespaces(returnValue);
    }

    public static String probeArchitetureFolderName(final String oat_folder_path) {

        final String ls_oat_folder = "ls " + oat_folder_path + File.separator;
        final StringBuffer returnValue = new StringBuffer();

        ProcessExecutor.execute(ls_oat_folder, true, returnValue);
        try {
            return returnValue.toString().trim();
        } catch (final NullPointerException e) {
            return GuiUtils.getArchitectureFolderName();
        }
    }

    public static void setFilePermissions(final String path, final String octalFormatPerms) {

        final String cmd_chmod_octal = "chmod " + octalFormatPerms + " " + path;

        final boolean success = ProcessExecutor.execute(cmd_chmod_octal, true);

        if (!success) {
            Log.e(TAG, "Command FAILED " + cmd_chmod_octal);
        } else {
            Log.d(TAG, "PERM Set " + path + " to: " + octalFormatPerms);
        }
    }

    public static void setFileGid(final String path, final String baseApkGid) {

        final String cmd_chown_gid = "chgrp " + baseApkGid + " " + path;

        final boolean success = ProcessExecutor.execute(cmd_chown_gid, true);

        if (!success) {
            Log.e(TAG, "Command FAILED " + cmd_chown_gid);
        } else {
            Log.d(TAG, "GID Set " + path + " to: " + baseApkGid);
        }
    }

    public static void setFileUid(final String path, final String baseApkUid) {

        final String cmd_chown_uid = "chown " + baseApkUid + " " + path;

        final boolean success = ProcessExecutor.execute(cmd_chown_uid, true);

        if (!success) {
            Log.e(TAG, "Command FAILED " + cmd_chown_uid);
        } else {
            Log.d(TAG, "UID Set " + path + " to: " + baseApkUid);
        }
    }

    public static List<String> getInstalledPackages(final Context context) {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> pkgAppsList = context.getPackageManager().queryIntentActivities( mainIntent, 0);
        List<String> packages = new ArrayList<>(pkgAppsList.size());
        for (final ResolveInfo info : pkgAppsList) {
            packages.add(info.activityInfo.packageName);
        }
        return packages;
    }

    public static boolean chmodExecutable(final String pathToFile) {
        final String cmd_chmod777 = "chmod 777 " + pathToFile;

        boolean success = ProcessExecutor.execute(cmd_chmod777, true);

        if (!success) {
            Log.e(TAG, "Command Failed: " + cmd_chmod777);
        }
        return success;
    }

    public static void deleteExistingFile(final File absoluteFile) {
        if (absoluteFile.exists()) {
            Log.d(TAG, "delete() -> " + absoluteFile.getAbsolutePath());
            absoluteFile.delete();
        }
    }
    public static void deleteExistingFile(final String filePath) {
        final File filePathFile = new File(filePath);
        deleteExistingFile(filePathFile);
    }

    public static void listAllAssets(final Context context) {
        Log.d(TAG, "listAllAssets()");
        final AssetManager assetMan = context.getAssets();
        try {
            String[] assets = assetMan.list("");
            for (final String asset : assets) {
                Log.d(TAG, asset);
                listAssets(assetMan, asset);
            }
        } catch (final IOException e) {
            Log.e(TAG, "Could not open File/Folder(s)", e);
        }
        Log.d(TAG, "listAllAssets() Done");
    }

    private static void listAssets(final AssetManager assetMan, final String assetFolder) {
        try {
            String[] assets = assetMan.list(assetFolder);
            for (final String asset : assets) {
                final String ASSET_PATH = assetFolder + File.separator + asset;
                Log.d(TAG, "> " + ASSET_PATH);
                listAssets(assetMan,  ASSET_PATH);
            }
        } catch (final IOException e) {
            Log.e(TAG, "Could not open File/Folder", e);
        }
    }

    public static String copyUriToFilesystem(final Context context, final Uri uri, final String toPathAbsolute) {
        Log.d(TAG, "copyUriToFilesystem() " + uri + " -> " + toPathAbsolute);
        try (
                InputStream in = context.getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(toPathAbsolute);
        ) {
            int read;
            byte[] buffer = new byte[FILE_BUFFER_DEFAULT_SIZE];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            Log.d(TAG, "copyUriToFilesystem() " + uri + " -> " + toPathAbsolute + " Done");
            return toPathAbsolute;
        } catch (final IOException e) {
            Log.e(TAG, "copyUriToFilesystem() " + uri + " -> " + toPathAbsolute + " ERROR", e);
            //Convert your stream to data here
            return "";
        }

    }

    public static void suKill(final Process process) {
        // ["Process[pid=18546, hasExited=false]"]
        //
        // USes PID from SU process -.-
        //
        // 7481 root     20   0  25% R     2 506728K 122772K  fg /data/user/0/de.infsec.artist.saarland.cispa.artist.artistgui/files/artist/dex2oat
        final String processInfo = process.toString();
        Log.d(TAG, String.format("sukill(%s)", processInfo));
        try {
            final String[] parts = processInfo.split("pid=");
            final String pid = parts[1].split(", hasExited")[0];
            Log.d(TAG, String.format("sukill(PID %s)", pid));
            final String cmd_kill = "kill -9 " + pid;
            Log.d(TAG, String.format("sukill() Command: <%s>", cmd_kill));
            ProcessExecutor.execute(cmd_kill, true, "kill_process");
        } catch (final NullPointerException|ArrayIndexOutOfBoundsException e) {
            Log.d(TAG, String.format("sukill(%s) FAILED", processInfo));
        }
        Log.d(TAG, String.format("sukill(%s) DONE", processInfo));
    }
}

