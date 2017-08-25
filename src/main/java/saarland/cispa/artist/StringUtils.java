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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

/** Stringmanipulation utilities
 *
 */
public class StringUtils {

    private static final String TAG = Logg.TAG;

    public static List<String> convertToList(final StringBuffer outputBuffer) {

        final List<String> outputLines = new ArrayList<>();

        if (outputBuffer != null) {
            final String[] bufferLines = outputBuffer.toString().split("\\n");
            Collections.addAll(outputLines, bufferLines);
        }
        return outputLines;
    }

    public static String readIntoString(final InputStream inputStream) {

        String returnString = "";

        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            returnString = result.toString("UTF-8");
        } catch (final IOException e) {
            Log.e(TAG, "Could not read supplied InputStream: " + inputStream, e);
        }
        return returnString;
    }
}
