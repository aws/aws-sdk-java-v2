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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V1_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.createComments;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v1S3MethodMatcher;

import java.util.List;
import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3AddImportsAndComments extends Recipe {

    private static final MethodMatcher LIST_NEXT_BATCH_OBJECTS = v1S3MethodMatcher("listNextBatchOfObjects(..)");
    private static final MethodMatcher LIST_NEXT_BATCH_VERSIONS = v1S3MethodMatcher("listNextBatchOfVersions(..)");
    private static final MethodMatcher SET_BUCKET_ACL = v1S3MethodMatcher("setBucketAcl(..)");
    private static final MethodMatcher SET_OBJECT_ACL = v1S3MethodMatcher("setObjectAcl(..)");

    private static final Pattern CANNED_ACL = Pattern.compile(V1_S3_MODEL_PKG + "CannedAccessControlList");
    private static final Pattern GET_OBJECT_REQUEST = Pattern.compile(V1_S3_MODEL_PKG + "GetObjectRequest");
    private static final Pattern INITIATE_MPU = Pattern.compile(V1_S3_MODEL_PKG + "GetObjectRequest");
    private static final Pattern MULTI_FACTOR_AUTH = Pattern.compile(V1_S3_MODEL_PKG + "MultiFactorAuthentication");

    /*
    TODO
    Methods:
    selectObjectContent(..)
    setS3ClientOptions(S3ClientOptions clientOptions)
    createBucket(String bucketName, Region region)

    setRegion(Region region)
    setEndpoint(String endpoint)
    getCachedResponseMetadata(AmazonWebServiceRequest request)
     */


    @Override
    public String getDisplayName() {
        return "Add imports and comments to unsupported S3 transforms.";
    }

    @Override
    public String getDescription() {
        return "Add imports and comments to unsupported S3 transforms.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            boolean isSetObjectAcl = SET_OBJECT_ACL.matches(method);
            boolean isSetBucketAcl = SET_BUCKET_ACL.matches(method);

            if (isSetObjectAcl || isSetBucketAcl) {
                removeV1S3ModelImport("AccessControlList");
                removeV1S3ModelImport("CannedAccessControlList");
                maybeAddV2CannedAclImport(method.getArguments(), isSetObjectAcl, isSetBucketAcl);

                // TODO - AccessControlList and CannedAccessControlList v2 differences
                String comment = "";
                return method.withComments(createComments(comment));
            }
            if (LIST_NEXT_BATCH_OBJECTS.matches(method)) {
                // TODO
                String comment = "";
                return method.withComments(createComments(comment));
            }
            if (LIST_NEXT_BATCH_VERSIONS.matches(method)) {
                // TODO
                String comment = "";
                return method.withComments(createComments(comment));
            }

            // TODO

            return method;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            JavaType type = newClass.getType();
            if (!(type instanceof JavaType.FullyQualified)) {
                return newClass;
            }

            if (type.isAssignableFrom(MULTI_FACTOR_AUTH)) {
                removeV1S3ModelImport("MultiFactorAuthentication");
                String comment = "v2 does not have a MultiFactorAuthentication POJO. Please manually set the String value on "
                                 + "the request POJO.";
                return newClass.withComments(createComments(comment));
            }

            if (type.isAssignableFrom(GET_OBJECT_REQUEST) && newClass.getArguments().size() == 1) {
                removeV1S3ModelImport("S3ObjectId");
                // TODO - S3ObjectId not supported
                String comment = "";
                return newClass.withComments(createComments(comment));
            }

            if (type.isAssignableFrom(INITIATE_MPU) && newClass.getArguments().size() == 3) {
                // TODO - ObjectMetadata transform for InitiateMultipartUpload not supported
                String comment = "";
                return newClass.withComments(createComments(comment));
            }

            // TODO

            return newClass;
        }

        private void maybeAddV2CannedAclImport(List<Expression> args, boolean isSetObjectAcl, boolean isSetBucketAcl) {
            for (Expression expr : args) {
                JavaType type = expr.getType();
                if (type == null || !type.isAssignableFrom(CANNED_ACL)) {
                    continue;
                }
                removeV1S3ModelImport("CannedAccessControlList");
                if (isSetBucketAcl) {
                    addV2S3ModelImport("BucketCannedACL");
                }
                if (isSetObjectAcl) {
                    addV2S3ModelImport("ObjectCannedACL");
                }
            }
        }

        private void removeV1S3ModelImport(String className) {
            doAfterVisit(new RemoveImport<>(V1_S3_MODEL_PKG + className, true));
        }

        private void addV2S3ModelImport(String className) {
            doAfterVisit(new AddImport<>(V2_S3_MODEL_PKG + className, null, false));
        }
    }
}
