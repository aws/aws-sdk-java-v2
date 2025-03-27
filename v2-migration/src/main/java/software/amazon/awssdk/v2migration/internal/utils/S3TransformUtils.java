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

package software.amazon.awssdk.v2migration.internal.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class S3TransformUtils {

    public static final String V1_S3_CLIENT = "com.amazonaws.services.s3.AmazonS3";
    public static final String V1_S3_MODEL_PKG = "com.amazonaws.services.s3.model.";
    public static final String V1_S3_PKG = "com.amazonaws.services.s3.";

    public static final String V2_S3_CLIENT = "software.amazon.awssdk.services.s3.S3Client";
    public static final String V2_S3_MODEL_PKG = "software.amazon.awssdk.services.s3.model.";
    public static final String V2_S3_PKG = "software.amazon.awssdk.services.s3.";

    public static final String V2_TM_CLIENT = "software.amazon.awssdk.transfer.s3.S3TransferManager";
    public static final String V2_TM_MODEL_PKG = "software.amazon.awssdk.transfer.s3.model.";

    private S3TransformUtils() {

    }

    public static MethodMatcher v1S3MethodMatcher(String methodSignature) {
        return new MethodMatcher(V1_S3_CLIENT + " " + methodSignature, true);
    }

    public static MethodMatcher v2S3MethodMatcher(String methodSignature) {
        return new MethodMatcher(V2_S3_CLIENT + " " + methodSignature, true);
    }

    public static MethodMatcher v2TmMethodMatcher(String methodSignature) {
        return new MethodMatcher(V2_TM_CLIENT + " " + methodSignature, true);
    }

    public static List<Comment> createComments(String comment) {
        return Collections.singletonList(
            new TextComment(true, "AWS SDK for Java v2 migration: " + comment, "", Markers.EMPTY));
    }

    public static boolean isCompleteMpuRequestMultipartUploadSetter(J.MethodInvocation method) {
        return "multipartUpload".equals(method.getSimpleName())
               && TypeUtils.isAssignableTo(V2_S3_MODEL_PKG + "CompleteMultipartUploadRequest.Builder",
                                           method.getSelect().getType());
    }

    public static boolean isGeneratePresignedUrl(J.MethodInvocation method) {
        return "generatePresignedUrl".equals(method.getSimpleName())
               && TypeUtils.isAssignableTo(V2_S3_CLIENT, method.getSelect().getType());
    }

    public static boolean isUnsupportedHttpMethod(String httpMethod) {
        return Arrays.asList("Head", "Post", "Patch").contains(httpMethod);
    }

    public static List<Comment> assignedVariableHttpMethodNotSupportedComment() {
        String comment = "Transform for S3 generatePresignedUrl() with an assigned variable for HttpMethod is not supported."
                         + " Please manually migrate your code - https://sdk.amazonaws"
                         + ".com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html";
        return createComments(comment);
    }

    public static List<Comment> requestPojoTransformNotSupportedComment() {
        String comment = "Transforms are not supported for GeneratePresignedUrlRequest, please manually migrate your code "
                         + "- https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner"
                         + "/S3Presigner.html";
        return createComments(comment);
    }

    public static List<Comment> httpMethodNotSupportedComment(String httpMethod) {
        String comment = String.format("S3 generatePresignedUrl() with %s HTTP method is not supported in v2. Only GET, PUT, "
                                       + "and DELETE are supported - https://sdk.amazonaws"
                                       + ".com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html",
                                       httpMethod.toUpperCase(Locale.ROOT));
        return createComments(comment);
    }

    public static List<Comment> presignerSingleInstanceSuggestion() {
        String comment = "If generating multiple pre-signed URLs, it is recommended to create a single instance of "
                         + "S3Presigner, since creating a presigner can be expensive. If applicable, please manually "
                         + "refactor the transformed code.";
        return createComments(comment);
    }
}
