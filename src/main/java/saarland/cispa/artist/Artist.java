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

import android.app.Activity;
import android.content.Context;

import saarland.cispa.artist.gui.artist.ArtistGuiProgress;
import saarland.cispa.artist.settings.ArtistRunConfig;

public interface Artist {

    String CODE_LIB_ASSET = "assetcodelib.apk";

    void init(final Context context);
    void addGuiProgressListener(final ArtistGuiProgress callback);

    boolean mergeCodeLib(final Context context, final ArtistRunConfig config);

    boolean Run(final Context context);
}
