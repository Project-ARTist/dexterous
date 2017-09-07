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
package saarland.cispa.dexterous.cli;

import java.util.HashMap;
import java.util.Map;

public enum DexSectionType {
    HEADER(0x0000),
    STRING_IDS(0x0001),
    TYPE_IDS(0x0002),
    PROTO_IDS(0x0003),
    FIELD_IDS(0x0004),
    METHOD_IDS(0x0005),
    CLASS_DEFS(0x0006),
    MAP_LIST(0x1000),
    TYPE_LISTS(0x1001),
    ANNOTATION_SET_REF_LISTS(0x1002),
    ANNOTATION_SETS(0x1003),
    CLASS_DATA_ITEMS(0x2000),
    CODE_ITEMS(0x2001),
    STRING_DATA_ITEMS(0x2002),
    DEBUG_INFO_ITEM(0x2003),
    ANNOTATION_ITEM(0x2004),
    ENCODED_ARRAY_ITEMS(0x2005),
    ANNOTATION_DIRECTORY_ITEMS(0x2006);

    private final int id;
    private static Map<Integer, DexSectionType> map = new HashMap<>();

    DexSectionType(int id) {
        this.id = id;
    }

    public static DexSectionType valueOf(int typeId) {
        return map.get(typeId);
    }

    public static DexSectionType valueOf(short typeId) {
        return map.get(Short.toUnsignedInt(typeId));
    }

    public int getValue() {
        return id;
    }

    static {
        for (DexSectionType dexSectionType : DexSectionType.values()) {
            map.put(dexSectionType.getValue(), dexSectionType);
        }
    }
}
