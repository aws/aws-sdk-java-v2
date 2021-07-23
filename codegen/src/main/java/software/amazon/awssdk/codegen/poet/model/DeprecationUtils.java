package software.amazon.awssdk.codegen.poet.model;

import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.codegen.internal.Constant.LF;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import java.util.List;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.utils.StringUtils;

public final class DeprecationUtils {

    private static final AnnotationSpec DEPRECATED = AnnotationSpec.builder(Deprecated.class).build();

    /**
     * If a given member is modeled as deprecated, add the {@link Deprecated} annotation to the method and, if the method
     * already has existing Javadoc, append a section with the {@code @deprecated} tag.
     */
    public static MethodSpec checkDeprecated(MemberModel member, MethodSpec method) {
        if (!member.isDeprecated() || method.annotations.contains(DEPRECATED)) {
            return method;
        }
        MethodSpec.Builder builder = method.toBuilder().addAnnotation(DEPRECATED);
        if (!method.javadoc.isEmpty()) {
            builder.addJavadoc(LF + "@deprecated");
            if (StringUtils.isNotBlank(member.getDeprecatedMessage())) {
                builder.addJavadoc("$L", " " + member.getDeprecatedMessage());
            }
        }
        return builder.build();
    }

    public static List<MethodSpec> checkDeprecated(MemberModel member, List<MethodSpec> methods) {
        return methods.stream().map(methodSpec -> checkDeprecated(member, methodSpec)).collect(toList());
    }
}
