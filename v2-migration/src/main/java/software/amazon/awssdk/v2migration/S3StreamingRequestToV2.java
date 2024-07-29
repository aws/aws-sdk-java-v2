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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.v2migration.internal.utils.IdentifierUtils;

@SdkInternalApi
public class S3StreamingRequestToV2 extends Recipe {
    private static final MethodMatcher PUT_OBJECT_FILE =
        new MethodMatcher("com.amazonaws.services.s3.AmazonS3 "
                          + "putObject(java.lang.String, java.lang.String, java.io.File)", true);
    private static final MethodMatcher PUT_OBJECT_STRING =
        new MethodMatcher("com.amazonaws.services.s3.AmazonS3 "
                          + "putObject(java.lang.String, java.lang.String, java.lang.String)", true);

    private static final JavaType.FullyQualified V1_PUT_OBJECT_REQUEST =
        TypeUtils.asFullyQualified(JavaType.buildType("com.amazonaws.services.s3.model.PutObjectRequest"));
    private static final JavaType.FullyQualified REQUEST_BODY =
        TypeUtils.asFullyQualified(JavaType.buildType("software.amazon.awssdk.core.sync.RequestBody"));

    @Override
    public String getDisplayName() {
        return "S3StreamingRequestToV2";
    }

    @Override
    public String getDescription() {
        return "S3StreamingRequestToV2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            if (PUT_OBJECT_FILE.matches(method, false)) {
                method = transformPutFileOverload(method);
            }
            if (PUT_OBJECT_STRING.matches(method, false)) {
                method = transformPutStringOverload(method);
            }
            return super.visitMethodInvocation(method, executionContext);
        }

        private J.MethodInvocation transformPutStringOverload(J.MethodInvocation method) {
            JavaType.Method methodType = method.getMethodType();
            if (methodType == null) {
                return method;
            }

            List<Expression> originalArgs = method.getArguments();

            Expression bucketExpr = originalArgs.get(0);
            Expression keyExpr = originalArgs.get(1);
            Expression stringExpr = originalArgs.get(2);

            List<Expression> newArgs = new ArrayList<>();
            Expression getObjectExpr = bucketAndKeyToPutObject(bucketExpr, keyExpr);
            newArgs.add(getObjectExpr);

            // This is to maintain the formatting/spacing of original code, getPrefix() retrieves the leading whitespace
            Space stringArgPrefix = stringExpr.getPrefix();
            stringExpr = stringToRequestBody(stringExpr.withPrefix(Space.EMPTY)).withPrefix(stringArgPrefix);
            newArgs.add(stringExpr);

            List<String> paramNames = Arrays.asList("request", "stringContent");
            List<JavaType> paramTypes = newArgs.stream()
                                               .map(Expression::getType)
                                               .collect(Collectors.toList());


            methodType = methodType.withParameterTypes(paramTypes)
                                   .withParameterNames(paramNames);

            return method.withMethodType(methodType).withArguments(newArgs);
        }

        private J.MethodInvocation stringToRequestBody(Expression fileExpr) {
            maybeAddImport(REQUEST_BODY);

            J.Identifier requestBodyId = IdentifierUtils.makeId(REQUEST_BODY.getClassName(), REQUEST_BODY);

            JavaType.Method fromStringType = new JavaType.Method(
                null,
                0L,
                REQUEST_BODY,
                "fromString",
                REQUEST_BODY,
                Collections.singletonList("stringContent"),
                Collections.singletonList(JavaType.buildType("java.lang.String")),
                null,
                null
            );

            J.Identifier fromFileId = IdentifierUtils.makeId("fromString", fromStringType);

            return new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded.build(requestBodyId),
                null,
                fromFileId,
                JContainer.build(Collections.singletonList(JRightPadded.build(fileExpr))),
                fromStringType
            );
        }

        private J.MethodInvocation transformPutFileOverload(J.MethodInvocation method) {
            JavaType.Method methodType = method.getMethodType();
            if (methodType == null) {
                return method;
            }

            List<Expression> originalArgs = method.getArguments();

            Expression bucketExpr = originalArgs.get(0);
            Expression keyExpr = originalArgs.get(1);
            Expression fileExpr = originalArgs.get(2);

            List<Expression> newArgs = new ArrayList<>();
            Expression getObjectExpr = bucketAndKeyToPutObject(bucketExpr, keyExpr);
            newArgs.add(getObjectExpr);

            Space fileArgPrefix = fileExpr.getPrefix();
            fileExpr = fileToRequestBody(fileExpr.withPrefix(Space.EMPTY)).withPrefix(fileArgPrefix);
            newArgs.add(fileExpr);

            List<String> paramNames = Arrays.asList("request", "file");
            List<JavaType> paramTypes = newArgs.stream()
                                               .map(Expression::getType)
                                               .collect(Collectors.toList());


            methodType = methodType.withParameterTypes(paramTypes)
                                   .withParameterNames(paramNames);

            return method.withMethodType(methodType).withArguments(newArgs);
        }

        private J.MethodInvocation fileToRequestBody(Expression fileExpr) {
            maybeAddImport(REQUEST_BODY);

            J.Identifier requestBodyId = IdentifierUtils.makeId(REQUEST_BODY.getClassName(), REQUEST_BODY);

            JavaType.Method fromFileType = new JavaType.Method(
                null,
                0L,
                REQUEST_BODY,
                "fromFile",
                REQUEST_BODY,
                Collections.singletonList("file"),
                Collections.singletonList(JavaType.buildType("java.io.File")),
                null,
                null
            );

            J.Identifier fromFileId = IdentifierUtils.makeId("fromFile", fromFileType);

            return new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded.build(requestBodyId),
                null,
                fromFileId,
                JContainer.build(Collections.singletonList(JRightPadded.build(fileExpr))),
                fromFileType
            );
        }

        private Expression bucketAndKeyToPutObject(Expression bucketExpr, Expression keyExpr) {
            maybeAddImport(V1_PUT_OBJECT_REQUEST);

            J.Identifier putObjRequestId = IdentifierUtils.makeId(V1_PUT_OBJECT_REQUEST.getClassName(), V1_PUT_OBJECT_REQUEST);

            JavaType.Method ctorType = new JavaType.Method(
                null,
                0L,
                V1_PUT_OBJECT_REQUEST,
                "<init>",
                V1_PUT_OBJECT_REQUEST,
                Arrays.asList("bucket", "key"),
                Arrays.asList(bucketExpr.getType(), keyExpr.getType()),
                null,
                null
            );

            return new J.NewClass(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                null,
                Space.EMPTY,
                putObjRequestId.withPrefix(Space.SINGLE_SPACE),
                JContainer.build(
                    Arrays.asList(
                        JRightPadded.build(bucketExpr),
                        JRightPadded.build(keyExpr)
                    )
                ),
                null,
                ctorType
            );
        }
    }
}
