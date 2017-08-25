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
package saarland.cispa.apksigner.sign;

/**
 * Interface to obtain internationalized strings for the progress events.
 */
public interface ResourceAdapter {

    public enum Item {
        INPUT_SAME_AS_OUTPUT_ERROR,
        AUTO_KEY_SELECTION_ERROR,
        LOADING_CERTIFICATE_AND_KEY,
        PARSING_CENTRAL_DIRECTORY,
        GENERATING_MANIFEST,
        GENERATING_SIGNATURE_FILE,
        GENERATING_SIGNATURE_BLOCK,
        COPYING_ZIP_ENTRY
    }

    ;

    public String getString(Item item, Object... args);
}
