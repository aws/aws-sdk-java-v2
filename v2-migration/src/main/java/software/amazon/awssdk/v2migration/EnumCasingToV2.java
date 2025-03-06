/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.v2migration;

import static software.amazon.awssdk.utils.internal.CodegenNamingUtils.splitOnWordBoundaries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils;

@SdkInternalApi
public class EnumCasingToV2 extends Recipe {

    private static Set<String> ENUMS = new HashSet<>();

    private static final Map<String, String> SPECIAL_CASES = new HashMap<>();

    static {
        SPECIAL_CASES.put("StandardInfrequentAccess", "STANDARD_IA");
        SPECIAL_CASES.put("OneZoneInfrequentAccess", "ONEZONE_IA");
        SPECIAL_CASES.put("GlacierInstantRetrieval", "GLACIER_IR");
        SPECIAL_CASES.put("textORcsv", "TEXT_OR_CSV");
    }

    @Override
    public String getDisplayName() {
        return "V1 Enum Casing to V2";
    }

    @Override
    public String getDescription() {
        return "Transforms V1 enum constants from pascal case to screaming snake case for v2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static String v2Casing(String enumValue) {
        if (SPECIAL_CASES.containsKey(enumValue)) {
            return SPECIAL_CASES.get(enumValue);
        }

        String result = enumValue;
        result = String.join("_", splitOnWordBoundaries(result));
        return StringUtils.upperCase(result);
    }

    private static class Visitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ctx);

            if (isV2EnumValue(fa)) {
                String v2Casing = v2Casing(fa.getSimpleName());
                if (isS3EventsEnum(fa)) {
                    v2Casing = "S3_" + v2Casing;
                }
                ENUMS.add(v2Casing);
                return fa.withName(fa.getName().withSimpleName(v2Casing));
            }

            return fa;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            J.Identifier id = super.visitIdentifier(identifier, ctx);

            if (ENUMS.contains(id.getSimpleName())) {
                JavaType.Variable fieldType = id.getFieldType();
                if (fieldType == null) {
                    return id;
                }
                JavaType.Variable variable = fieldType.withName(id.getSimpleName());
                return id.withFieldType(variable);
            }

            return id;
        }

        public boolean isV2EnumValue(J.FieldAccess fa) {
            JavaType javaType = fa.getTarget().getType();
            JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(javaType);
            if (fullyQualified != null) {
                return SdkTypeUtils.isV2ModelClass(javaType)
                       && fullyQualified.getKind() == JavaType.FullyQualified.Kind.Enum;
            }
            return false;
        }

        private boolean isS3EventsEnum(J.FieldAccess fa) {
            JavaType javaType = fa.getTarget().getType();
            JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(javaType);
            return fullyQualified != null && fullyQualified.getClassName().equals("Event")
                && fullyQualified.getPackageName().contains("s3.model") && !fa.getSimpleName().startsWith("S3_");
        }

    }
}
