package software.amazon.awssdk.codegen.tracing;

import java.util.HashMap;
import java.util.Map;

import com.squareup.javapoet.AnnotationSpec;
import software.amazon.awssdk.annotations.Traceable;

public class AnnotationProvider {
    /*
     This class provides the Annotations for different entities in the code depending upon their type
     i.e. classes, methods, fields etc.
    */
    public static final String TYPE_SEP = ".";
    public static final String TYPE_STR = "TYPE";
    public static final String METHOD_SEP = "#";
    public static final String METHOD_STR = "METHOD";
    public static final String FIELD_SEP = "$";
    public static final String FIELD_STR = "FIELD";

    private static AnnotationSpec annotate(String name, String parent, String type, String separator) {
        AnnotationSpec annotation = AnnotationSpec.builder(Traceable.class)
                                                    .addMember("name", "$S", parent+separator+name)
                                                    .addMember("type", "$S", type)
                                                    .build();
        return annotation;
    }

    public static AnnotationSpec annotateType(String name, String parent) {
        return annotate(name, parent, TYPE_STR, TYPE_SEP);
    }

    public static AnnotationSpec annotateField(String name, String parent) {
        return annotate(name, parent, FIELD_STR, FIELD_SEP);
    }

    public static AnnotationSpec annotateMethod(String name, String parent) {
        return annotate(name, parent, METHOD_STR, METHOD_SEP);
    }
}
