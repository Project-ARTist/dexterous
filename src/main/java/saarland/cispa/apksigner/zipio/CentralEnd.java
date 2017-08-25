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

import java.io.IOException;

import saarland.cispa.artist.log.Logg;
import trikita.log.Log;

public class CentralEnd {
    private static final String TAG = Logg.TAG;

    public int signature = 0x06054b50; // end of central dir signature    4 bytes
    public short numberThisDisk = 0;   // number of this disk             2 bytes     
    public short centralStartDisk = 0; // number of the disk with the start of the central directory  2 bytes
    public short numCentralEntries;    // total number of entries in the central directory on this disk  2 bytes
    public short totalCentralEntries;  // total number of entries in the central directory           2 bytes

    public int centralDirectorySize;   // size of the central directory   4 bytes
    public int centralStartOffset;     // offset of start of central directory with respect to the starting disk number        4 bytes
    public String fileComment;         // .ZIP file comment       (variable size)

    public static CentralEnd read(ZipInput input) throws IOException {

        int signature = input.readInt();
        if (signature != 0x06054b50) {
            // back up to the signature
            input.seek(input.getFilePointer() - 4);
            return null;
        }

        CentralEnd entry = new CentralEnd();

        entry.doRead(input);
        return entry;
    }

    private void doRead(ZipInput input) throws IOException {

        numberThisDisk = input.readShort();
        Log.d(TAG, String.format("This disk number: 0x%04x", numberThisDisk));

        centralStartDisk = input.readShort();
        Log.d(TAG, String.format("Central dir start disk number: 0x%04x", centralStartDisk));

        numCentralEntries = input.readShort();
        Log.d(TAG, String.format("Central entries on this disk: 0x%04x", numCentralEntries));

        totalCentralEntries = input.readShort();
        Log.d(TAG, String.format("Total number of central entries: 0x%04x", totalCentralEntries));

        centralDirectorySize = input.readInt();
        Log.d(TAG, String.format("Central directory size: 0x%08x", centralDirectorySize));

        centralStartOffset = input.readInt();
        Log.d(TAG, String.format("Central directory offset: 0x%08x", centralStartOffset));

        short zipFileCommentLen = input.readShort();
        fileComment = input.readString(zipFileCommentLen);
        Log.d(TAG, ".ZIP file comment: " + fileComment);
    }


    public void write(ZipOutput output) throws IOException {
        output.writeInt(signature);
        output.writeShort(numberThisDisk);
        output.writeShort(centralStartDisk);
        output.writeShort(numCentralEntries);
        output.writeShort(totalCentralEntries);
        output.writeInt(centralDirectorySize);
        output.writeInt(centralStartOffset);
        output.writeShort((short) fileComment.length());
        output.writeString(fileComment);
    }

}
