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
package saarland.cispa.artist.settings;

import android.support.annotation.Nullable;

import java.io.File;

import saarland.cispa.artist.utils.ArtistUtils;

public class ArtistRunConfig {

    public final static String KEYSTORE_NAME = "artist.bks";

    public static final String BASE_APK = "base.apk";
    public static final String BASE_APK_ALTERNATIVE = "base2.apk";
    public static final String BASE_APK_MERGED = "base_merged.apk";
    public static final String BASE_APK_SIGNED = "base_merged-signed.apk";

    public final static String OAT_FILE = "base.odex";
    public boolean REPLACE_BASE_APK = false;

    /** Package Name of the application */
    public String app_name = "";

    public String app_apk_name = "";

    public String app_folder_path = "";
    public String app_apk_file_path = ""; // base.apk
    public String app_apk_merged_file_path = ""; // base_merged.apk
    public String app_apk_merged_signed_file_path = ""; // base_merged-signed.apk

    public String app_apk_file_path_alternative = "";

    public String app_oat_folder_path = "";
    public String app_oat_file_path = "";
    public String app_package_name = "";

    public String app_oat_architecture = "";

    /** Android Version / API Level
     *  => Used the select the appropriate dex2oat compiler version
     */
    public String api_level = "";

    public int COMPILER_THREADS = -1;

    public boolean DEX2OAT_DEBUG_MODE = true;

    public boolean CODELIB_SETTINGS_APPCOMPART = false;
    public boolean ARTIST_LAUNCH_ACTIVITY = false;

    public String DEX2OAT_LOGLEVEL = "info";
    public boolean INJECT_CODELIB = true;

    public boolean BACKUP_APK_ORIGINAL = false;
    public boolean BACKUP_APK_MERGED = false;

    /**
     * Whether compilation should abort for multidex apps.
     */
    public boolean MULTIDEX_ABORT;

    public String asset_path_artist_root = "";
    public String asset_path_dex2oat = "";
    public String asset_path_dex2oat_libs = "";
    public String asset_path_keystore = "artist" + File.separator + KEYSTORE_NAME;

    public String artist_exec_path = "";
    public String artist_exec_path_libs_dir = "";
    public String artist_exec_path_dex2oat = "";

    @Nullable
    public File codeLib = null;
    public String codeLibName = "";

    public File keystore = null;

    public String nativeLibraryDir = "";

    public String nativeLibraryRootDir = "";
    public String secondaryNativeLibraryDir = "";
    public boolean nativeLibraryRootRequiresIsa = false;
    public String primaryCpuAbi = "";
    public String secondaryCpuAbi = "";

    /**
     * e.g. last path of the full path: /data/app/de.infsec.artist.saarland.cispa.artist.artistgui-1/lib/arm/
     */
    public final static String APP_PATH_NATIVE_LIBRARIES = "lib/arm/";

    public final static String RUN_PATH_DEX2OAT = "dex2oat";

    public ArtistRunStats stats = new ArtistRunStats();

    public String oatOwner = "";
    public String oatGroup = "";
    public String oatPermissions = "";

    @Override
    public String toString() {
        return "ArtistRunConfig {" + "\n" +
                "  api_level=                        '" + api_level + '\'' + "\n" +
                ", app_name=                         '" + app_name + '\'' + "\n" +
                ", app_apk_name=                     '" + app_apk_name + '\'' + "\n" +
                ", app_folder_path=                  '" + app_folder_path + '\'' + "\n" +
                ", app_apk_file_path=                '" + app_apk_file_path + '\'' + "\n" +
                ", app_apk_merged_file_path=         '" + app_apk_merged_file_path + '\'' + "\n" +
                ", app_apk_merged_signed_file_path=  '" + app_apk_merged_signed_file_path + '\'' + "\n" +
                ", app_apk_file_path_alternative=    '" + app_apk_file_path_alternative + '\'' + "\n" +
                ", app_oat_folder_path=              '" + app_oat_folder_path + '\'' + "\n" +
                ", app_oat_file_path=                '" + app_oat_file_path + '\'' + "\n" +
                ", app_package_name=                 '" + app_package_name + '\'' + "\n" +
                ", app_oat_architecture=             '" + app_oat_architecture + '\'' + "\n" +
                ", COMPILER_THREADS=                 '" + COMPILER_THREADS + "'\n" +
                ", DEX2OAT_DEBUG_MODE=               '" + DEX2OAT_DEBUG_MODE + "'\n" +
                ", CODELIB_SETTINGS_APPCOMPART=      '" + CODELIB_SETTINGS_APPCOMPART + "'\n" +
                ", ARTIST_LAUNCH_ACTIVITY=           '" + ARTIST_LAUNCH_ACTIVITY + "'\n" +
                ", DEX2OAT_LOGLEVEL=                 '" + DEX2OAT_LOGLEVEL + '\'' + "\n" +
                ", INJECT_CODELIB=                   '" + INJECT_CODELIB + "'\n" +
                ", BACKUP_APK_ORIGINAL=              '" + BACKUP_APK_ORIGINAL + "'\n" +
                ", BACKUP_APK_MERGED=                '" + BACKUP_APK_MERGED + "'\n" +
                ", MULTIDEX_ABORT=                   '" + MULTIDEX_ABORT + "'\n" +
                ", REPLACE_BASE_APK=                 '" + REPLACE_BASE_APK + "'\n" +
                ", asset_path_artist_root=           '" + asset_path_artist_root + '\'' + "\n" +
                ", asset_path_dex2oat=               '" + asset_path_dex2oat + '\'' + "\n" +
                ", asset_path_dex2oat_libs=          '" + asset_path_dex2oat_libs + '\'' + "\n" +
                ", asset_path_keystore=              '" + asset_path_keystore + '\'' + "\n" +
                ", artist_exec_path=                 '" + artist_exec_path + '\'' + "\n" +
                ", artist_exec_path_libs_dir=        '" + artist_exec_path_libs_dir + '\'' + "\n" +
                ", artist_exec_path_dex2oat=         '" + artist_exec_path_dex2oat + '\'' + "\n" +
                ", codeLib=                          '" + codeLib + "'\n" +
                ", codeLibName=                      '" + codeLibName + "'\n" +
                ", keystore=                         '" + keystore + "'\n" +
                ", nativeLibraryDir=                 '" + nativeLibraryDir + '\'' + "\n" +
                ", nativeLibraryRootDir=             '" + nativeLibraryRootDir + '\'' + "\n" +
                ", secondaryNativeLibraryDir=        '" + secondaryNativeLibraryDir + '\'' + "\n" +
                ", nativeLibraryRootRequiresIsa=     '" + nativeLibraryRootRequiresIsa + "'\n" +
                ", primaryCpuAbi=                    '" + primaryCpuAbi + '\'' + "\n" +
                ", secondaryCpuAbi=                  '" + secondaryCpuAbi + '\'' + "\n" +
                ", stats=                            '" + stats + "\n" +
                ", oatOwner=                         '" + oatOwner + '\'' + "\n" +
                ", oatGroup=                         '" + oatGroup + '\'' + "\n" +
                ", oatPermissions=                   '" + oatPermissions + '\'' + "\n" +
                '}';
    }

    public boolean isAssetCodeLib() {
        if (codeLibName != null) {
            return codeLibName.startsWith(ArtistUtils.CODELIB_ASSET);
        } else {
            return false;
        }
    }

    public boolean isImportedCodeLib() {
        if (codeLibName != null) {
            return codeLibName.startsWith(ArtistUtils.CODELIB_IMPORTED);
        } else {
            return false;
        }
    }
}
