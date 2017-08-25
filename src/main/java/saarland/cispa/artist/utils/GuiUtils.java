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

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;

import saarland.cispa.artist.StringUtils;
import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

public class GuiUtils {

    private static final String TAG = Logg.TAG;

    public static final String LIST_VIEW_SEPARATOR = "";

    public static void displayToast(final Activity activity, final String toastMessage) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(activity.getApplicationContext(), toastMessage, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
    }

    public static void displaySnackForever(final View view, final String snackMessage) {
        if (view != null) {
            Snackbar.make(view, snackMessage, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Action", null).show();
        }
    }

    public static void displaySnackLong(final View view, final String snackMessage) {
        if (view != null) {
            Snackbar.make(view, snackMessage, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    public static List<String> probeInstalledApps() {

        final String cmd_ls_data_apps = "ls -a /data/app/";

        final StringBuffer outputBuffer = new StringBuffer();

        boolean success = ProcessExecutor.execute(cmd_ls_data_apps, true, outputBuffer);

        Log.d(TAG, "OUTPUTBUFFER " + outputBuffer.toString());

        final List<String> installedApps = StringUtils.convertToList(outputBuffer);

        installedApps.remove(".");
        installedApps.remove("..");

        deleteBlacklistedApps(installedApps);

        return installedApps;
    }


    public static String getArchitectureFolderName() {

        final String os_arch_property = System.getProperty("os.arch");
        final String ro_product_cpu_abi_property = System.getProperty("ro.product.cpu.abi");

        Log.e(TAG, "SystemProperty os.arch:            " + os_arch_property);
        Log.e(TAG, "SystemProperty ro.product.cpu.abi: " + ro_product_cpu_abi_property);

        final String architecture;

        if (ro_product_cpu_abi_property != null
                && ro_product_cpu_abi_property.contains("arm64-v8a")) {
            architecture = "arm64";
        } else if (os_arch_property.compareTo("arch64") == 0) {
            architecture = "arm64";
        } else if (os_arch_property.compareTo("aarch64") == 0) {
            architecture = "arm64";
        } else if (os_arch_property.contains("armv8l")) {
            architecture = "arm64";
        } else if (os_arch_property.contains("arm")) {
            architecture = "arm";
        } else if (os_arch_property.contains("x86")) {
            // TODO intel and mips architectures are untested
            if (os_arch_property.contains("64")) {
                architecture = "x86_64";
            } else {
                architecture = "x86";
            }
        } else if (os_arch_property.compareTo("mips") == 0
                || os_arch_property.compareTo("mips") == 0) {
            architecture = os_arch_property;
        } else {
            Log.e(TAG, "Unrecognized architecture: " + os_arch_property);
            throw new RuntimeException("Unrecognized architecture: " + os_arch_property);
        }
        return architecture;
    }

    public static void deleteBlacklistedApps(List<String> installedApps) {
        for (Iterator<String> iter = installedApps.listIterator(); iter.hasNext(); ) {
            final String appName = iter.next();
            if ( appName.startsWith("com.google.")
                    || appName.startsWith("com.android.")
                    || appName.startsWith("com.teslacoilsw.")
                    || appName.startsWith("eu.chainfire.")
                    || appName.startsWith("jackpal.androidterm")
                    || appName.startsWith("stericson.busybox.")
                    || appName.startsWith("com.joeykrim.rootcheck")
                    || appName.startsWith("org.connectbot")
                    || appName.startsWith("com.termux")
            ) {
                iter.remove();
            }
        }
    }

}
