package saarland.cispa.dexterous.stats;

import comm.android.dex.Dex;

/**
 * Created by weisgerber on 17.11.16.
 */
public class MethodIdStats {

    public String DEX_NAME = null;

    public String NAME = null;

    public String class_idx = null;
    public String proto_idx = null;
    public String name_idx  = null;

    public MethodIdStats() {}

    public MethodIdStats(final String DEX_NAME, final Dex dexi, comm.android.dex.MethodId methodId) {
        this.DEX_NAME = DEX_NAME;
        this.NAME = methodId.toString();

        this.class_idx = dexi.strings().get(dexi.typeIds().get(methodId.getDeclaringClassIndex()));;
        this.proto_idx = dexi.strings().get(dexi.protoIds().get(methodId.getProtoIndex()).getShortyIndex());
        this.name_idx  = dexi.strings().get(methodId.getNameIndex());
    }

    @Override
    public String toString() {
        return String.format("> %s\n" +
                             "> DexFile    %s\n" +
                             "  > class_idx  %s\n" +
                             "  > proto_idx  %s\n" +
                             "  > name_idx   %s\n"
                , NAME
                , DEX_NAME
                , class_idx
                , proto_idx
                , name_idx);
    }
}
