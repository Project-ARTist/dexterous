package saarland.cispa.dexterous;

import comm.android.dex.ClassDef;
import comm.android.dex.Dex;

import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by weisgerber on 18.11.16.
 */
public class DxUtils {

    private static void closeIoStream(final ZipInputStream zipInput) {
        if (zipInput != null) try {
            zipInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void closeIoStream(final ZipOutputStream zipOutput) {
        if (zipOutput != null) try {
            zipOutput.flush();
            zipOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getClassName(final Dex dexi, final ClassDef clazzDef) {
        return dexi.typeNames().get(clazzDef.getTypeIndex());
    }

}
