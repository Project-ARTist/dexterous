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
 * Default resource adapter.
 */
public class DefaultResourceAdapter implements ResourceAdapter {

    @Override
    public String getString(Item item, Object... args) {

        switch (item) {
            case INPUT_SAME_AS_OUTPUT_ERROR:
                return "Input and output files are the same.  Specify a different name for the output.";
            case AUTO_KEY_SELECTION_ERROR:
                return "Unable to auto-select key for signing " + args[0];
            case LOADING_CERTIFICATE_AND_KEY:
                return "Loading certificate and private key";
            case PARSING_CENTRAL_DIRECTORY:
                return "Parsing the input's central directory";
            case GENERATING_MANIFEST:
                return "Generating manifest";
            case GENERATING_SIGNATURE_FILE:
                return "Generating signature file";
            case GENERATING_SIGNATURE_BLOCK:
                return "Generating signature block file";
            case COPYING_ZIP_ENTRY:
                return String.format("Copying zip entry %d of %d", args[0], args[1]);
            default:
                throw new IllegalArgumentException("Unknown item " + item);
        }

    }
}
