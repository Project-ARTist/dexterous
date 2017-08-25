package saarland.cispa.artist.utils;

import saarland.cispa.log.LogG;
import trikita.log.Log;

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

public class ArtistThread {

    public static void checkThreadCancellation() throws ArtistInterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            Log.d(LogG.TAG, String.format("checkThreadCancellation() interrupted[%b]",
                    Thread.currentThread().isInterrupted()));
            throw new ArtistInterruptedException("Thread is interrupted.");
        }
    }
}
