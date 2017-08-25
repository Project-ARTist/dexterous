package saarland.cispa.dexterous.cli;

import comm.android.dex.Dex;
import comm.android.dex.TableOfContents;
import trikita.log.Log;

public class Loggy {

    public static String formatSectionInfo(TableOfContents.Section section) {
        // return String.format("Section[TYPE: %#06x | OFF: % 8d | SIZE: % 8d]", section.type, section.off, section.size);
        return String.format("Section |TYPE: %26s | OFF: % 8d | SIZE: % 8d |", DexSectionType.valueOf(section.type).name(), section.off, section.size);
    }

    public static void printDexHeader(final Dex dex) {
        TableOfContents toc = dex.getTableOfContents();
        Log.i("----------------------------------------------------------------------------");
        for (int i = 0; i < toc.sections.length; i++) {
            TableOfContents.Section section = toc.sections[i];
            Log.i(Loggy.formatSectionInfo(section));
        }
        Log.i("----------------------------------------------------------------------------");
    }
}
