package comm.android.dx.merge;


import java.util.HashMap;
import java.util.LinkedHashMap;

import comm.android.dex.Annotation;
import comm.android.dex.Dex;
import comm.android.dex.EncodedValueReader;
import comm.android.dex.FieldId;
import comm.android.dex.MethodId;
import comm.android.dex.ProtoId;
import comm.android.dex.TableOfContents;
import trikita.log.Log;


public class MethodFilter {

    private final String annotationType;
    protected final Dex dex;

    private final boolean skip;

    public enum Usage {
        BLACKLISTED,
        STRIP,
        WHITELISTED;
    }

    protected HashMap<Short, Usage> methodIdWhitelist;
    protected HashMap<Short, Usage> fieldIdWhitelist;
    protected HashMap<Short, Usage> protoIdWhitelist;
    protected HashMap<Short, Usage> typeIdWhitelist;
    protected HashMap<Integer, Usage> stringIdWhitelist;
    private static final String TAG = "MethodFilter";


    private Usage check(HashMap<Short, Usage> whitelist, short id) {
        Usage usage = whitelist.get(id);
        return usage!=null?usage:Usage.BLACKLISTED;
    }

    private Usage check(HashMap<Integer, Usage> whitelist, int id) {
        Usage usage = whitelist.get(id);
        return usage!=null?usage:Usage.BLACKLISTED;
    }

    Usage checkMethodId(short methodId){
        return skip?Usage.WHITELISTED:check(methodIdWhitelist, methodId);
    }

    Usage checkFieldId(short fieldId){
        return skip?Usage.WHITELISTED:check(fieldIdWhitelist, fieldId);
    }

    Usage checkProtoId(short protoId){
        return skip?Usage.WHITELISTED:check(protoIdWhitelist, protoId);
    }

    Usage checkTypeId(short typeId){
        return skip?Usage.WHITELISTED:check(typeIdWhitelist, typeId);
    }

    Usage checkStringId(int stringId){
        return skip?Usage.WHITELISTED:check(stringIdWhitelist, stringId);
    }

    void reset(){
        methodIdWhitelist = new LinkedHashMap<>();
        fieldIdWhitelist = new LinkedHashMap<>();
        protoIdWhitelist = new LinkedHashMap<>();
        typeIdWhitelist = new LinkedHashMap<>();
        stringIdWhitelist = new LinkedHashMap<>();
    }
    
    private void processMethodIds(){
        TableOfContents.Section section = dex.getTableOfContents().methodIds;
        Dex.Section in = dex.open(section.off);
        for (short j = 0; j < section.size; j++) {
            MethodId m = in.readMethodId();
            if (checkMethodId(j) == Usage.WHITELISTED) {
                methodIdWhitelist.put((short) j, Usage.WHITELISTED);
                typeIdWhitelist.put((short) m.getDeclaringClassIndex(), Usage.WHITELISTED);
                stringIdWhitelist.put(m.getNameIndex(), Usage.WHITELISTED);
                protoIdWhitelist.put((short) m.getProtoIndex(), Usage.WHITELISTED);
            }
        }
    }

    private void processFieldIds(){
        TableOfContents.Section fieldIds = dex.getTableOfContents().fieldIds;
        Dex.Section section = dex.open(fieldIds.off);
        for (short j = 0; j < fieldIds.size; j++) {
            FieldId m = section.readFieldId();
            if (checkFieldId(j) == Usage.WHITELISTED) {
                typeIdWhitelist.put((short) m.getDeclaringClassIndex(), Usage.WHITELISTED);
                stringIdWhitelist.put(m.getNameIndex(), Usage.WHITELISTED);
                protoIdWhitelist.put((short) m.getTypeIndex(), Usage.WHITELISTED);
            }
        }
    }

    private void processProtoIds(){
        TableOfContents.Section protoIds = dex.getTableOfContents().protoIds;
        Dex.Section section = dex.open(protoIds.off);
        for (int j = 0; j < protoIds.size; j++) {
            ProtoId m = section.readProtoId();
            if (checkProtoId((short) j) == Usage.WHITELISTED) {
                stringIdWhitelist.put(m.getShortyIndex(), Usage.WHITELISTED);
                typeIdWhitelist.put((short) m.getReturnTypeIndex(), Usage.WHITELISTED);
                int parametersOffset = m.getParametersOffset();
                if (parametersOffset != 0) {
                    Dex.Section in2 = dex.open(m.getParametersOffset());
                    int size = in2.readInt();
                    for (short k = 0; k < size; k++){
                        typeIdWhitelist.put(in2.readShort(), Usage.WHITELISTED);
                    }
                }
            }
        }
    }
    
    private void processTypeIds(){
        TableOfContents.Section typeIds = dex.getTableOfContents().typeIds;
        Dex.Section section = dex.open(typeIds.off);
        for (int j = 0; j < typeIds.size; j++) {
            int m = section.readInt();
            if (checkTypeId((short) j) == Usage.WHITELISTED) {
                stringIdWhitelist.put(m, Usage.WHITELISTED);
            }
        }
    }


    protected String getString(int stringid){
        Dex.Section stringsSection = dex.open(dex.getTableOfContents().stringIds.off+4*stringid);
        return stringsSection.readString();
    }


    protected String getTypeString(int typeid){
        Dex.Section typesSection = dex.open(dex.getTableOfContents().typeIds.off+4*typeid);
        int stringid = typesSection.readInt();
        return getString(stringid);
    }

    protected String getMethodString(int methodid){
        Dex.Section typesSection = dex.open(dex.getTableOfContents().typeIds.off+8*methodid+4);
        int stringid = typesSection.readInt();
        return getString(stringid);
    }

    protected void initializeWhitelist() throws DexMerger.MergeException {
        if (!skip) {

            int annotationtypeid = -1;

            TableOfContents.Section typeIds = dex.getTableOfContents().typeIds;
            Dex.Section section = dex.open(typeIds.off);
            for (int j = 0; j < typeIds.size; j++) {
                int m = section.readInt();
                if (getString(m).equals(annotationType)) {
                    annotationtypeid = j;
                }
            }

            Log.i(TAG, "Annotationtype used for whitelisting:" + annotationType);

            if (annotationtypeid == -1) {

                throw new DexMerger.MergeException(new IllegalArgumentException("Annotation type " + annotationType + " is not defined in the codelib dex file"));

            } else {

                typeIdWhitelist.put((short)annotationtypeid, Usage.STRIP);

            }

            int classdefs = dex.getTableOfContents().classDefs.size;
            Dex.Section classDefsSection = dex.open(dex.getTableOfContents().classDefs.off);
            for (int classdefsi = 0; classdefsi < classdefs; classdefsi ++) {
                int class_idx = classDefsSection.readInt();
                classDefsSection.readInt(); // access_flags
                classDefsSection.readInt(); // superclass_idx
                classDefsSection.readInt(); // interfaces_off
                classDefsSection.readInt(); // source_file_idx
                int annotations_directory_off = classDefsSection.readInt();
                classDefsSection.readInt(); // class_data_off
                classDefsSection.readInt(); // static_values_off
                if (annotations_directory_off != 0) {
                    Dex.Section annotations_directory = dex.open(annotations_directory_off);
                    /*
                    class_annotations_off:      uint
                    fields_size:                uint
                    methods_dize:               uint
                    parameters_size:            uint
                    field_annotations:          field_annotation[fields_size]
                    method_annotations:         method_annotation[methods_size]
                    parameter_annotations:      parameter_annotation[parameters_size]
                     */
                    int class_annotations_off = annotations_directory.readInt();
                    int fields_size = annotations_directory.readInt();
                    int methods_size = annotations_directory.readInt();
                    int parameters_size = annotations_directory.readInt();

                    for (int k = 0; k < fields_size; k++) {
                        int fieldid = annotations_directory.readInt();
                        int annotations_off = annotations_directory.readInt();
                        Dex.Section annotation_set_item = dex.open(annotations_off);
                        int size = annotation_set_item.readInt();
                        for (int l = 0; l < size; l++) {
                            Dex.Section annotation_off_item = dex.open(annotation_set_item.readInt());
                            Annotation annotation = annotation_off_item.readAnnotation();
                            if (annotation.getTypeIndex() == annotationtypeid) {
                                Log.d(TAG, "Field whitelisted:" + fieldid);
                                typeIdWhitelist.put((short) class_idx, Usage.WHITELISTED);
                                fieldIdWhitelist.put((short) fieldid, Usage.WHITELISTED);
                            }
                        }
                    }
                    for (int k = 0; k < methods_size; k++) {
                        int methodid = annotations_directory.readInt();
                        int annotations_off = annotations_directory.readInt();
                        Dex.Section annotation_set_item = dex.open(annotations_off);
                        int size = annotation_set_item.readInt();
                        for (int l = 0; l < size; l++) {
                            Dex.Section annotation_off_item = dex.open(annotation_set_item.readInt());
                            Annotation annotation = annotation_off_item.readAnnotation();
                            if (annotation.getTypeIndex() == annotationtypeid) {
                                Log.d("ColdelibWhitelisting", "Method annotated:" + getMethodString(methodid));
                                typeIdWhitelist.put((short) class_idx, Usage.WHITELISTED);
                                methodIdWhitelist.put((short) methodid, Usage.WHITELISTED);
                            }
                        }
                    }
                    for (int k = 0; k < parameters_size; k++) {
                        annotations_directory.readInt();    // methodid
                        annotations_directory.readInt();    // offset
                    }
                    if (class_annotations_off != 0) {
                        Dex.Section class_annotation = dex.open(class_annotations_off);
                        int class_annotation_size = class_annotation.readInt();
                        for (int class_annotation_index = 0; class_annotation_index < class_annotation_size; class_annotation_index++) {
                            Dex.Section class_annotation_item = dex.open(class_annotation.readInt());
                            Annotation annotation = class_annotation_item.readAnnotation();
                            if (annotation.getTypeIndex() == annotationtypeid) {
                                typeIdWhitelist.put((short) class_idx, Usage.WHITELISTED);
                            }
                        }
                    }
                    if (typeIdWhitelist.get((short) class_idx) == Usage.WHITELISTED)
                        Log.i("CodelibWhitelisting", "Class whitelisted: " + String.valueOf(class_idx) + " (" + getTypeString(class_idx) +
                                ")\nclass_annotations_off: " + class_annotations_off + "\nf/m/p:" + fields_size + "/" + methods_size + "/" + parameters_size);
                }
            }
        }
    }




    private void processAnnotations(){
        Dex.Section classDefsSection = dex.open(dex.getTableOfContents().classDefs.off);
        int classdefs = dex.getTableOfContents().classDefs.size;
        for (int classdefsi = 0; classdefsi < classdefs; classdefsi ++) {
            int class_idx = classDefsSection.readInt();
            classDefsSection.readInt(); // access_flags
            classDefsSection.readInt(); // superclass_idx
            classDefsSection.readInt(); // interfaces_off
            classDefsSection.readInt(); // source_file_idx
            int annotations_directory_off = classDefsSection.readInt();
            classDefsSection.readInt(); // class_data_off
            classDefsSection.readInt(); // static_values_off
            if (annotations_directory_off != 0 && checkTypeId((short) class_idx) == Usage.WHITELISTED) {
                Dex.Section annotations_directory = dex.open(annotations_directory_off);
                /*
                class_annotations_off:      uint
                fields_size:                uint
                methods_dize:               uint
                parameters_size:            uint
                field_annotations:          field_annotation[fields_size]
                method_annotations:         method_annotation[methods_size]
                parameter_annotations:      parameter_annotation[parameters_size]
                 */
                int class_annotations_off = annotations_directory.readInt();
                if (class_annotations_off != 0) {
                    Dex.Section class_annotation = dex.open(class_annotations_off);
                    int class_annotation_size = class_annotation.readInt();
                    for (int class_annotation_index = 0; class_annotation_index < class_annotation_size; class_annotation_index++) {
                        Dex.Section class_annotation_item = dex.open(class_annotation.readInt());
                        Annotation annotation = class_annotation_item.readAnnotation();
                        if (checkTypeId((short) annotation.getTypeIndex()) != Usage.STRIP) {
                            typeIdWhitelist.put((short) annotation.getTypeIndex(), Usage.WHITELISTED);
                            EncodedValueReader encodedValueReader = annotation.getReader();
                            int annotation_size = encodedValueReader.readAnnotation();
                            for (int m = 0; m < annotation_size; m++) {
                                stringIdWhitelist.put(encodedValueReader.readAnnotationName(), Usage.WHITELISTED);
                                encodedValueReader.skipValue();
                            }
                        }
                    }
                }
                int fields_size = annotations_directory.readInt();
                int methods_size = annotations_directory.readInt();
                int parameters_size = annotations_directory.readInt();
                for (int k = 0; k < fields_size; k++) {
                    int fieldid = annotations_directory.readInt();
                    int annotations_off = annotations_directory.readInt();
                    if (checkFieldId((short) fieldid) == Usage.WHITELISTED) {
                        Dex.Section in2 = dex.open(annotations_off);
                        int size = in2.readInt();
                        for (int l = 0; l < size; l++) {
                            Dex.Section in3 = dex.open(in2.readInt());
                            Annotation annotation = in3.readAnnotation();
                            if (checkTypeId((short) annotation.getTypeIndex()) != Usage.STRIP) {
                                typeIdWhitelist.put((short) annotation.getTypeIndex(), Usage.WHITELISTED);
                                EncodedValueReader r = annotation.getReader();
                                int annotation_size = r.readAnnotation();
                                for (int m = 0; m < annotation_size; m++) {
                                    stringIdWhitelist.put(r.readAnnotationName(), Usage.WHITELISTED);
                                    r.skipValue();
                                }
                            }
                        }
                    }
                }
                for (int k = 0; k < methods_size; k++) {
                    int methodid = annotations_directory.readInt();
                    int annotations_off = annotations_directory.readInt();
                    if (checkMethodId((short) methodid) == Usage.WHITELISTED) {
                        Dex.Section in2 = dex.open(annotations_off);
                        int size = in2.readInt();
                        for (int l = 0; l < size; l++) {
                            Dex.Section in3 = dex.open(in2.readInt());
                            Annotation annotation = in3.readAnnotation();
                            if (checkTypeId((short) annotation.getTypeIndex()) != Usage.STRIP) {
                                typeIdWhitelist.put((short) annotation.getTypeIndex(), Usage.WHITELISTED);
                                EncodedValueReader r = annotation.getReader();
                                int annotation_size = r.readAnnotation();
                                for (int m = 0; m < annotation_size; m++) {
                                    stringIdWhitelist.put(r.readAnnotationName(), Usage.WHITELISTED);
                                    r.skipValue();
                                }
                            }
                        }
                    }
                }
                for (int k = 0; k < parameters_size; k++) {
                    int methodid = annotations_directory.readInt();
                    int annotations_off = annotations_directory.readInt();
                    if (checkMethodId((short) methodid) == Usage.WHITELISTED) {
                        Dex.Section in2 = dex.open(annotations_off);
                        int size = in2.readInt();
                        for (int l = 0; l < size; l++) {
                            int aoff = in2.readInt();
                            if (aoff != 0) {
                                Dex.Section in3 = dex.open(aoff);
                                int size2 = in3.readInt();
                                for (int m = 0; m < size2; m++) {
                                    Dex.Section in4 = dex.open(in3.readInt());
                                    Annotation annotation = in4.readAnnotation();
                                    EncodedValueReader reader = annotation.getReader();
                                    int annotation_size = reader.readAnnotation();
                                    for (int o = 0; o < annotation_size; o++) {
                                        stringIdWhitelist.put(reader.readAnnotationName(), Usage.WHITELISTED);
                                        reader.skipValue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    public MethodFilter(Dex dex, String annotationType) throws DexMerger.MergeException {
        this.dex = dex;
        this.annotationType = annotationType;
        reset();

        skip = annotationType == null;

        if (!skip) {

            initializeWhitelist();

            for (int i = 0; i < 2; i++) {

                processMethodIds();

                processFieldIds();

                processProtoIds();

                processTypeIds();

                processAnnotations();
            }

        }

    }

}


