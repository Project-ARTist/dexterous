package saarland.cispa.dexterous.stats;

import comm.android.dex.Dex;
import saarland.cispa.dexterous.DxUtils;

/**
 * Created by weisgerber on 18.11.16.
 */
public class ClassDefStats {

    public String NAME = null;

    public String class_idx = null;
    public String access_flags = null;
    public String superclass_idx = null;
    public String interfaces_off = null;
    public String source_file_idx = null;
    public String annotations_off = null;
    public String class_data_off = null;
    public String static_values_off = null;


    public ClassDefStats() {}

    public ClassDefStats(final String dex_name, final Dex dexi, final comm.android.dex.ClassDef clazzDef) {
        this.NAME = DxUtils.getClassName(dexi, clazzDef);
    }

    @Override
    public String toString() {
        return "ClassDefStats{" +
                "NAME='" + NAME + '\'' +
                ", class_idx='" + class_idx + '\'' +
                ", access_flags='" + access_flags + '\'' +
                ", superclass_idx='" + superclass_idx + '\'' +
                ", interfaces_off='" + interfaces_off + '\'' +
                ", source_file_idx='" + source_file_idx + '\'' +
                ", annotations_off='" + annotations_off + '\'' +
                ", class_data_off='" + class_data_off + '\'' +
                ", static_values_off='" + static_values_off + '\'' +
                '}';
    }
}
