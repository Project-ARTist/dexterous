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
package saarland.cispa.artist.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import trikita.log.Log;

public class Logg {

    public static final String TAG = "ArtistGui";

    public static final String HR = "############################################";
    public static final String PREF_KEY_LOGLEVEL = "pref_general_log_level";


    public static void setUserLogLevel(final Context context) {
        setUserLogLevel(context, PREF_KEY_LOGLEVEL);
    }

    public static void setUserLogLevel(final Context context, final String preference_key) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final String logLevel = sharedPref.getString(preference_key, "debug");
        setUserLogLevel(logLevel);
    }

    public static void setUserLogLevel(final String logLevel) {
        switch (logLevel) {
            case "verbose":
                Log.level(Log.V);
                Log.v("setUserLogLevel: " + logLevel);
                break;
            case "debug":
                Log.level(Log.D);
                Log.d("setUserLogLevel: " + logLevel);
                break;
            case "info":
                Log.level(Log.I);
                Log.i("setUserLogLevel: " + logLevel);
                break;
            case "off":
                Log.level(Log.W);
                Log.w("setUserLogLevel: " + logLevel + " (Warnings & Errors)");
                break;
            default:
                Log.level(Log.D);
                Log.v("setUserLogLevel: " + logLevel + " DEFAULT (Debug)");
                break;
        }
    }

    public static void logTest() {
        Log.v(TAG, "Log Verbose");
        Log.d(TAG, "Log Debug");
        Log.i(TAG, "Log Info");
        Log.w(TAG, "Log Warning");
        Log.e(TAG, "Log Error");
    }

    public static String BigDivider() {
        final String bigDivider = "\n"
                + HR + "\n"
                + HR + "\n"
                + HR + "\n"
                +  "\n";

        return bigDivider;
    }

    /**
     * Priority constant for the println method; use Log.v.
     */
    public static final int VERBOSE = 2;

    /**
     * Priority constant for the println method; use Log.d.
     */
    public static final int DEBUG = 3;

    /**
     * Priority constant for the println method; use Log.i.
     */
    public static final int INFO = 4;

    /**
     * Priority constant for the println method; use Log.w.
     */
    public static final int WARN = 5;

    /**
     * Priority constant for the println method; use Log.e.
     */
    public static final int ERROR = 6;

    /**
     * Priority constant for the println method.
     */
    public static final int ASSERT = 7;

    public static final int LOG_LEVEL = DEBUG;

    /**
     * Send a {@link #VERBOSE} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
        logChecked(VERBOSE, tag, msg);
    }

    private static int logChecked(final int loglevel, final String tag, final String msg) {
        if (loglevel >= LOG_LEVEL) {
            Log.d("LOGGERTEST", "loglevel : " + loglevel + " > LOG_LEVEL: " + LOG_LEVEL);
            return android.util.Log.println(loglevel, tag, msg);
        } else {
            return 0;
        }
    }

    /**
     * Send a {@link #DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
        logChecked(DEBUG, tag, msg);
    }

    /**
     * Send an {@link #INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
        logChecked(INFO, tag, msg);
    }

    /**
     * Send a {@link #WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
        logChecked(WARN, tag, msg);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
    }

    /*
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static void w(String tag, Throwable tr) {
        Log.w(tag, "", tr);
    }

    /**
     * Send an {@link #ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
        logChecked(ERROR, tag, msg);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }

    /*
 * Send a {@link #ERROR} log message and log the exception.
 * @param tag Used to identify the source of a log message.  It usually identifies
 *        the class or activity where the log call occurs.
 * @param tr An exception to log
 */
    public static void e(String tag, Throwable tr) {
        Log.e(tag, "", tr);
    }

}
