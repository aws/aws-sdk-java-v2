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
public class S3NonStreamingRequestToV2 extends Recipe {
    private static final MethodMatcher CREATE_BUCKET =
        new MethodMatcher("com.amazonaws.services.s3.AmazonS3 createBucket(java.lang.String)", true);
    private static final MethodMatcher DELETE_BUCKET =
        new MethodMatcher("com.amazonaws.services.s3.AmazonS3 deleteBucket(java.lang.String)", true);
    private static final JavaType.FullyQualified V1_CREATE_BUCKET_REQUEST =
        TypeUtils.asFullyQualified(JavaType.buildType("com.amazonaws.services.s3.model.CreateBucketRequest"));
    private static final JavaType.FullyQualified V1_DELETE_BUCKET_REQUEST =
        TypeUtils.asFullyQualified(JavaType.buildType("com.amazonaws.services.s3.model.DeleteBucketRequest"));

    @Override
    public String getDisplayName() {
        return "V1 S3 non-streaming requests to V2";
    }

    @Override
    public String getDescription() {
        return "Transform usage of V1 S3 non-streaming requests such as CreateBucket and DeleteBucket to V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            if (CREATE_BUCKET.matches(method, false)) {
                method = transformBucketNameArgOverload(method, V1_CREATE_BUCKET_REQUEST);
            } else if (DELETE_BUCKET.matches(method, false)) {
                method = transformBucketNameArgOverload(method, V1_DELETE_BUCKET_REQUEST);
            }
            return super.visitMethodInvocation(method, executionContext);
        }

        private J.MethodInvocation transformBucketNameArgOverload(J.MethodInvocation method, JavaType.FullyQualified fqcn) {
            JavaType.Method methodType = method.getMethodType();
            if (methodType == null) {
                return method;
            }

            Expression bucketExpr = method.getArguments().get(0);
            List<Expression> newArgs = new ArrayList<>();
            Expression getObjectExpr = bucketToPojo(bucketExpr, fqcn);
            newArgs.add(getObjectExpr);

            List<String> paramNames = Collections.singletonList("request");
            List<JavaType> paramTypes = newArgs.stream()
                                               .map(Expression::getType)
                                               .collect(Collectors.toList());

            methodType = methodType.withParameterTypes(paramTypes)
                                   .withParameterNames(paramNames);

            return method.withMethodType(methodType).withArguments(newArgs);
        }

        private Expression bucketToPojo(Expression bucketExpr, JavaType.FullyQualified fqcn) {
            maybeAddImport(fqcn);

            J.Identifier putObjRequestId = IdentifierUtils.makeId(fqcn.getClassName(), fqcn);

            JavaType.Method ctorType = new JavaType.Method(
                null,
                0L,
                fqcn,
                "<init>",
                fqcn,
                Collections.singletonList("bucket"),
                Collections.singletonList(bucketExpr.getType()),
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
                JContainer.build(Collections.singletonList(JRightPadded.build(bucketExpr))),
                null,
                ctorType
            );
        }
    }
}
