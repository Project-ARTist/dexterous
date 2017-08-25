/*
 * Copyright (C) 2010 Ken Ellinwood
 *
 * Changes Copyright (C) 2017 CISPA (https://cispa.saarland), Saarland University
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
 */
package saarland.cispa.apksigner.zipio;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import saarland.cispa.log.LogG;
import trikita.log.Log;

/**
 *
 */
public class ZipListingHelper {

    private static final String TAG = LogG.TAG;
    static DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy HH:mm");

    public static void listHeader() {
        Log.d(TAG, " Length   Method    Size  Ratio   Date   Time   CRC-32    Name");
        Log.d(TAG, "--------  ------  ------- -----   ----   ----   ------    ----");

    }

    public static void listEntry(ZioEntry entry) {
        int ratio = 0;
        if (entry.getSize() > 0) {
            ratio = (100 * (entry.getSize() - entry.getCompressedSize())) / entry.getSize();
        }
//        Log.d(TAG, String.format("%8d  %6s %8d %4d%% %s  %08x  %s",
//                entry.getSize(),
//                entry.getCompression() == 0 ? "Stored" : "Defl:N",
//                entry.getCompressedSize(),
//                ratio,
//                dateFormat.format(new Date(entry.getTime())),
//                entry.getCrc32(),
//                entry.getName()));
    }
}
