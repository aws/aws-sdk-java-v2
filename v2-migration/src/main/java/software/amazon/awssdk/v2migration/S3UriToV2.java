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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V1_S3_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.createComments;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isStringType;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isUriType;

import java.util.List;
import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3UriToV2 extends Recipe {

    private static final Pattern V1_AMAZON_S3_URI = Pattern.compile(V1_S3_PKG + "AmazonS3URI");
    private static final String V2_S3_URI = V2_S3_PKG + "S3Uri";

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Convert v1 AmazonS3URI to v2 S3Uri";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Convert v1 AmazonS3URI to v2 S3Uri";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDecls, ExecutionContext ctx) {
            J.VariableDeclarations varDec = (J.VariableDeclarations) super.visitVariableDeclarations(variableDecls, ctx);

            JavaType type = varDec.getType();
            if (type == null) {
                return varDec;
            }

            if (!type.isAssignableFrom(V1_AMAZON_S3_URI)) {
                return varDec;
            }

            addV2S3UriImport();
            removeV1S3UriImport();
            JavaType v2Type = JavaType.buildType(V2_S3_URI);
            TypeTree v2TypeTree = TypeTree.build("S3Uri");
            return varDec.withType(v2Type).withTypeExpression(v2TypeTree);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            method = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
            JavaType.Method methodType = method.getMethodType();

            if (methodType == null) {
                return method;
            }

            Expression select = method.getSelect();
            if (select == null) {
                return method;
            }
            JavaType selectType = select.getType();
            if (selectType == null || !selectType.isAssignableFrom(V1_AMAZON_S3_URI)) {
                return method;
            }

            String methodName = method.getSimpleName();
            String v2Method;

            switch (methodName) {
                case "getVersionId":
                    v2Method = "#{any()}.firstMatchingRawQueryParameter(\"versionId\").orElse(null)";
                    break;
                case "getBucket":
                    v2Method = "#{any()}.bucket().orElse(null)";
                    break;
                case "getKey":
                    v2Method = "#{any()}.key().orElse(null)";
                    break;
                case "getRegion":
                    v2Method = "#{any()}.region().map(Region::id).orElse(null)";
                    addV2RegionImport();
                    break;
                default:
                    return method;
            }


            return JavaTemplate.builder(v2Method).build().apply(getCursor(), method.getCoordinates().replace(),
                                                                method.getSelect());
        }

        @Override
        public J visitNewClass(J.NewClass previousNewClass, ExecutionContext ctx) {
            J.NewClass newClass = super.visitNewClass(previousNewClass, ctx).cast();

            JavaType type = newClass.getType();
            if (!(type instanceof JavaType.FullyQualified) || !type.isAssignableFrom(V1_AMAZON_S3_URI)) {
                return newClass;
            }

            List<Expression> args = newClass.getArguments();
            Expression uriArg = args.get(0);
            JavaType uriType = uriArg.getType();
            if (uriType == null) {
                return newClass;
            }

            StringBuilder sb = new StringBuilder("S3Utilities.builder().build().parseUri(");

            if (isUriType(uriType)) {
                sb.append("#{any()})");
            } else if (isStringType(uriType)) {
                sb.append("URI.create(#{any()})");
                addJavaUriImport();
            }

            J.MethodInvocation newMethod = JavaTemplate.builder(sb.toString()).build()
                              .apply(getCursor(), newClass.getCoordinates().replace(), uriArg);

            if (shouldAddWarning(uriType, args)) {
                newMethod = newMethod.withComments(v2DoesNotEncodeWarning());
            }

            removeV1S3UriImport();
            addS3UtilitiesImport();
            return newMethod;
        }

        private boolean shouldAddWarning(JavaType uriType, List<Expression> args) {
            if (isUriType(uriType)) {
                return false;
            }
            if (args.size() == 1) {
                return true;
            }
            return !urlEncodeArgIsLiteralFalse(args.get(1));
        }

        private boolean urlEncodeArgIsLiteralFalse(Expression arg) {
            if (arg instanceof J.Literal) {
                J.Literal literal = (J.Literal) arg;
                return literal.getValue().equals(Boolean.FALSE);
            }
            return false;
        }


        private List<Comment> v2DoesNotEncodeWarning() {
            String warning = "v2 S3Uri does not URL-encode a String URI. If you relied on this functionality in v1 you must "
                             + "update your code to manually encode the String.";
            return createComments(warning);
        }

        private void removeV1S3UriImport() {
            doAfterVisit(new RemoveImport<>(V1_AMAZON_S3_URI.toString(), true));
        }

        private void addV2S3UriImport() {
            doAfterVisit(new AddImport<>(V2_S3_URI, null, false));
        }
        
        private void addS3UtilitiesImport() {
            String fqcn = V2_S3_PKG + "S3Utilities";
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addV2RegionImport() {
            String fqcn = "software.amazon.awssdk.regions.Region";
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addJavaUriImport() {
            String fqcn = "java.net.URI";
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }
    }
}
