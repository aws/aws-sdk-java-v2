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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.assignedVariableHttpMethodNotSupportedComment;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.httpMethodNotSupportedComment;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isCompleteMpuRequestMultipartUploadSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isGeneratePresignedUrl;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isUnsupportedHttpMethod;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.presignerSingleInstanceSuggestion;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.requestPojoTransformNotSupportedComment;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v2S3MethodMatcher;

import java.util.List;
import java.util.Locale;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * These transforms are more complex (e.g., method chaining) and use JavaTemplate which replaces text directly instead of building
 * up the AST tree as in {@link S3NonStreamingRequestToV2}. Because of that, we need manually add imports as well as run this
 * recipe after the client transforms are completed, e.g. new AmazonS3() -> S3Client.builder().build()
 *
 */
@SdkInternalApi
public class S3NonStreamingRequestToV2Complex extends Recipe {

    private static final MethodMatcher DISABLE_REQUESTER_PAYS = v2S3MethodMatcher("disableRequesterPays(String)");
    private static final MethodMatcher ENABLE_REQUESTER_PAYS = v2S3MethodMatcher("enableRequesterPays(String)");
    private static final MethodMatcher IS_REQUESTER_PAYS_ENABLED = v2S3MethodMatcher("isRequesterPaysEnabled(String)");
    private static final MethodMatcher GET_OBJECT_AS_STRING = v2S3MethodMatcher("getObjectAsString(String, String)");
    private static final MethodMatcher GET_URL = v2S3MethodMatcher("getUrl(String, String)");
    private static final MethodMatcher LIST_BUCKETS = v2S3MethodMatcher("listBuckets()");
    private static final MethodMatcher RESTORE_OBJECT = v2S3MethodMatcher("restoreObject(String, String, int)");
    private static final MethodMatcher SET_OBJECT_REDIRECT_LOCATION =
        v2S3MethodMatcher("objectRedirectLocation(String, String, String)");
    private static final MethodMatcher CHANGE_OBJECT_STORAGE_CLASS = v2S3MethodMatcher(
        String.format("changeObjectStorageClass(String, String, %sStorageClass)", V2_S3_MODEL_PKG));
    private static final MethodMatcher CREATE_BUCKET = v2S3MethodMatcher("createBucket(String, String)");

    @Override
    public String getDisplayName() {
        return "V1 S3 non-streaming requests to V2";
    }

    @Override
    public String getDescription() {
        return "Transform usage of V1 S3 non-streaming requests to V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {

            if (isCompleteMpuRequestMultipartUploadSetter(method)) {
                return transformCompleteMpuRequestCompletedPartsArg(method);
            }
            if (isGeneratePresignedUrl(method)) {
                return maybeAutoFormat(method, transformGeneratePresignedUrl(method), executionContext);
            }
            if (DISABLE_REQUESTER_PAYS.matches(method, false)) {
                return transformSetRequesterPays(method, false);
            }
            if (ENABLE_REQUESTER_PAYS.matches(method, false)) {
                return transformSetRequesterPays(method, true);
            }
            if (IS_REQUESTER_PAYS_ENABLED.matches(method, false)) {
                return transformIsRequesterPays(method);
            }
            if (GET_OBJECT_AS_STRING.matches(method, false)) {
                return transformGetObjectAsString(method);
            }
            if (GET_URL.matches(method, false)) {
                return transformGetUrl(method);
            }
            if (LIST_BUCKETS.matches(method, false)) {
                return transformListBuckets(method);
            }
            if (RESTORE_OBJECT.matches(method, false)) {
                return transformRestoreObject(method);
            }
            if (SET_OBJECT_REDIRECT_LOCATION.matches(method, false)) {
                return transformSetObjectRedirectLocation(method);
            }
            if (CHANGE_OBJECT_STORAGE_CLASS.matches(method, false)) {
                return transformChangeObjectStorageClass(method);
            }
            if (CREATE_BUCKET.matches(method, false)) {
                return transformCreateBucket(method);
            }
            return super.visitMethodInvocation(method, executionContext);
        }

        private J.MethodInvocation transformGeneratePresignedUrl(J.MethodInvocation method) {
            List<Expression> args = method.getArguments();
            if (args.size() == 1) {
                return method.withComments(requestPojoTransformNotSupportedComment());
            }

            String httpMethod = determineHttpMethod(args);

            if (isUnsupportedHttpMethod(httpMethod)) {
                return method.withComments(httpMethodNotSupportedComment(httpMethod));
            }
            if (httpMethod == null) {
                return method.withComments(assignedVariableHttpMethodNotSupportedComment());
            }

            String v2Method = String.format("S3Presigner.builder().s3Client(#{any()}).build()%n"
                                            + ".presign%sObject(p -> p.%sObjectRequest(r -> r.bucket(#{any()}).key(#{any()}))%n"
                                            + ".signatureDuration(Duration.between(Instant.now(), #{any()}.toInstant())))%n"
                                            + ".url()",
                                            httpMethod, httpMethod.toLowerCase(Locale.ROOT));

            removeV1HttpMethodImport();
            addInstantImport();
            addDurationImport();
            addS3PresignerImport();

            return JavaTemplate.builder(v2Method).build()
                               .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                      args.get(0), args.get(1), args.get(2))
                               .withComments(presignerSingleInstanceSuggestion());
        }

        private String determineHttpMethod(List<Expression> args) {
            if (args.size() == 3) {
                return "Get";
            }
            Expression argVal = args.get(3);
            String httpMethod = argVal.printTrimmed(getCursor());

            switch (httpMethod) {
                case "HttpMethod.GET":
                    return "Get";
                case "HttpMethod.PUT":
                    return "Put";
                case "HttpMethod.DELETE":
                    return "Delete";
                case "HttpMethod.HEAD":
                    return "Head";
                case "HttpMethod.POST":
                    return "Post";
                case "HttpMethod.PATCH":
                    return "Patch";
                default:
                    // enum value assigned to variable
                    return null;
            }
        }

        private J.MethodInvocation transformCompleteMpuRequestCompletedPartsArg(J.MethodInvocation method) {
            addImport("CompletedMultipartUpload");
            String v2Method = "CompletedMultipartUpload.builder().parts(#{any()}).build()";

            return JavaTemplate.builder(v2Method).build()
                               .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                      method.getArguments().get(0));
        }

        private J.MethodInvocation transformCreateBucket(J.MethodInvocation method) {
            String v2Method = "#{any()}.createBucket(CreateBucketRequest.builder().bucket(#{any()})"
                              + ".createBucketConfiguration(CreateBucketConfiguration.builder().locationConstraint(#{any()})"
                              + ".build()).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1));

            addImport("CreateBucketRequest");
            addImport("CreateBucketConfiguration");
            return method;
        }

        private J.MethodInvocation transformChangeObjectStorageClass(J.MethodInvocation method) {
            String v2Method = "#{any()}.copyObject(CopyObjectRequest.builder().sourceBucket(#{any()}).sourceKey(#{any()})"
                              + ".destinationBucket(#{any()}).destinationKey(#{any()})"
                              + ".storageClass(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(2));

            addImport("CopyObjectRequest");
            return method;
        }

        private J.MethodInvocation transformSetObjectRedirectLocation(J.MethodInvocation method) {
            String v2Method = "#{any()}.copyObject(CopyObjectRequest.builder().sourceBucket(#{any()}).sourceKey(#{any()})"
                              + ".destinationBucket(#{any()}).destinationKey(#{any()})"
                              + ".websiteRedirectLocation(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(2));

            addImport("CopyObjectRequest");
            return method;
        }

        private J.MethodInvocation transformRestoreObject(J.MethodInvocation method) {
            String v2Method = "#{any()}.restoreObject(RestoreObjectRequest.builder().bucket(#{any()}).key(#{any()})"
                              + ".restoreRequest(RestoreRequest.builder().days(#{any()}).build()).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1), method.getArguments().get(2));

            addImport("RestoreObjectRequest");
            addImport("RestoreRequest");
            return method;
        }

        private J.MethodInvocation transformListBuckets(J.MethodInvocation method) {
            String v2Method = "#{any()}.listBuckets().buckets()";
            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect());

            return method;
        }

        private J.MethodInvocation transformGetObjectAsString(J.MethodInvocation method) {
            String v2Method = "#{any()}.getObjectAsBytes(GetObjectRequest.builder().bucket(#{any()}).key(#{any()})"
                              + ".build()).asUtf8String()";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1));

            addImport("GetObjectRequest");
            return method;
        }

        private J.MethodInvocation transformGetUrl(J.MethodInvocation method) {
            String v2Method = "#{any()}.utilities().getUrl(GetUrlRequest.builder().bucket(#{any()}).key(#{any()}).build())";
            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1));

            addImport("GetUrlRequest");
            return method;
        }

        private J.MethodInvocation transformIsRequesterPays(J.MethodInvocation method) {
            String v2Method = "#{any()}.getBucketRequestPayment(GetBucketRequestPaymentRequest.builder().bucket(#{any()})"
                              + ".build()).payer().toString().equals(\"Requester\")";
            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0));

            addImport("GetBucketRequestPaymentRequest");
            return method;
        }

        private J.MethodInvocation transformSetRequesterPays(J.MethodInvocation method, boolean enable) {
            String payer = enable ? "REQUESTER" : "BUCKET_OWNER";
            String v2Method = String.format("#{any()}.putBucketRequestPayment(PutBucketRequestPaymentRequest.builder()"
                                            + ".bucket(#{any()}).requestPaymentConfiguration("
                                            + "RequestPaymentConfiguration.builder().payer(Payer.%s).build())"
                                            + ".build())", payer);
            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0));

            addImport("PutBucketRequestPaymentRequest");
            addImport("RequestPaymentConfiguration");
            addImport("Payer");
            return method;
        }

        private void removeV1HttpMethodImport() {
            doAfterVisit(new RemoveImport<>("com.amazonaws.HttpMethod", true));
        }

        private void addInstantImport() {
            String fqcn = "java.time.Instant";
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addDurationImport() {
            String fqcn = "java.time.Duration";
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addS3PresignerImport() {
            String fqcn = V2_S3_PKG + "presigner.S3Presigner";
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addImport(String pojoName) {
            String fqcn = V2_S3_MODEL_PKG + pojoName;
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }
    }
}
