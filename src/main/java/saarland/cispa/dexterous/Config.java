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
package saarland.cispa.dexterous;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {

    public static boolean analyze_apk = true;

    public static boolean build_apk = false;
    public static boolean sign_apk = false;

    public static File codelib = null;

    public static List<File> dexFiles = new ArrayList<>();

    final public static List<String> NO_COMPRESS_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif",
            ".wav", ".mp2", ".mp3", ".ogg", ".aac",
            ".mpg", ".mpeg", ".mid", ".midi", ".smf", ".jet",
            ".rtttl", ".imy", ".xmf", ".mp4", ".m4a",
            ".m4v", ".3gp", ".3gpp", ".3g2", ".3gpp2",
            ".amr", ".awb", ".wma", ".wmv", ".webm", ".mkv"
    );

}
