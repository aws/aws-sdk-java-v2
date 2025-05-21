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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
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
    public static final String V1_EN_PKG = "com.amazonaws.services.s3.event.";

    public static final String V2_S3_CLIENT = "software.amazon.awssdk.services.s3.S3Client";
    public static final String V2_S3_MODEL_PKG = "software.amazon.awssdk.services.s3.model.";
    public static final String V2_S3_PKG = "software.amazon.awssdk.services.s3.";

    public static final String V2_TM_CLIENT = "software.amazon.awssdk.transfer.s3.S3TransferManager";
    public static final String V2_TM_MODEL_PKG = "software.amazon.awssdk.transfer.s3.model.";

    public static final Set<String> SUPPORTED_METADATA_TRANSFORMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "contentLength",
        "contentEncoding",
        "contentType",
        "contentLanguage",
        "cacheControl",
        "contentDisposition",
        "contentMd5",
        "sseAlgorithm",
        "serverSideEncryption",
        "sseCustomerKeyMd5",
        "bucketKeyEnabled",
        "userMetadata",
        "httpExpiresDate"
    )));

    public static final Set<String> UNSUPPORTED_PUT_OBJ_REQUEST_TRANSFORMS = Collections.unmodifiableSet(new HashSet<>(
        Arrays.asList(
            "sseCustomerKey",
            "sseAwsKeyManagementParams",
            "accessControlList"
        )));


    private S3TransformUtils() {

    }

    public static MethodMatcher v1S3MethodMatcher(String methodSignature) {
        return new MethodMatcher(V1_S3_CLIENT + " " + methodSignature, true);
    }

    public static MethodMatcher v1EnMethodMatcher(String methodSignature) {
        return new MethodMatcher(V1_EN_PKG + methodSignature, true);
    }

    public static MethodMatcher v2S3MethodMatcher(String methodSignature) {
        return new MethodMatcher(V2_S3_CLIENT + " " + methodSignature, true);
    }

    public static MethodMatcher v2TmMethodMatcher(String methodSignature) {
        return new MethodMatcher(V2_TM_CLIENT + " " + methodSignature, true);
    }

    public static void addMetadataFields(StringBuilder sb, String metadataName,
                                         Map<String, Map<String, Expression>> metadataMap) {
        Map<String, Expression> map = metadataMap.get(metadataName);
        if (map == null) {
            return;
        }

        Expression contentLen = map.get("contentLength");
        if (contentLen != null) {
            sb.append(".contentLength(").append(contentLen);
            if (contentLen instanceof J.Literal) {
                sb.append("L");
            }
            sb.append(")\n");
        }
        Expression contentEncoding = map.get("contentEncoding");
        if (contentEncoding != null) {
            sb.append(".contentEncoding(\"").append(contentEncoding).append("\")\n");
        }
        Expression contentType = map.get("contentType");
        if (contentType != null) {
            sb.append(".contentType(\"").append(contentType).append("\")\n");
        }
        Expression contentLanguage = map.get("contentLanguage");
        if (contentLanguage != null) {
            sb.append(".contentLanguage(\"").append(contentLanguage).append("\")\n");
        }
        Expression cacheControl = map.get("cacheControl");
        if (cacheControl != null) {
            sb.append(".cacheControl(\"").append(cacheControl).append("\")\n");
        }
        Expression contentDisposition = map.get("contentDisposition");
        if (contentDisposition != null) {
            sb.append(".contentDisposition(\"").append(contentDisposition).append("\")\n");
        }
        Expression contentMd5 = map.get("contentMd5");
        if (contentMd5 != null) {
            sb.append(".contentMD5(\"").append(contentMd5).append("\")\n");
        }
        Expression serverSideEncryption = map.get("serverSideEncryption");
        if (serverSideEncryption != null) {
            sb.append(".serverSideEncryption(\"").append(serverSideEncryption).append("\")\n");
        }
        Expression sseAlgorithm = map.get("sseAlgorithm");
        if (sseAlgorithm != null) {
            sb.append(".serverSideEncryption(\"").append(sseAlgorithm).append("\")\n");
        }
        Expression sseCustomerKeyMd5 = map.get("sseCustomerKeyMd5");
        if (sseCustomerKeyMd5 != null) {
            sb.append(".sseCustomerKeyMD5(\"").append(sseCustomerKeyMd5).append("\")\n");
        }
        Expression bucketKeyEnabled = map.get("bucketKeyEnabled");
        if (bucketKeyEnabled != null) {
            sb.append(".bucketKeyEnabled(").append(bucketKeyEnabled).append(")\n");
        }
        Expression userMetadata = map.get("userMetadata");
        if (userMetadata != null) {
            sb.append(".metadata(").append(userMetadata).append(")\n");
        }
        Expression expiresDate = map.get("httpExpiresDate");
        if (expiresDate != null) {
            sb.append(".expires(").append(expiresDate).append(")\n");
        }
    }

    public static String changeBucketNameToBucket(String methodName) {
        if (methodName.contains("BucketName")) {
            return methodName.replace("BucketName", "Bucket");
        }
        return methodName;
    }

    public static String getArgumentName(J.MethodInvocation method) {
        Expression val = method.getArguments().get(0);
        return ((J.Identifier) val).getSimpleName();
    }

    public static String getSelectName(J.MethodInvocation method) {
        Expression select = method.getSelect();
        return ((J.Identifier) select).getSimpleName();
    }

    private static Comment generateComment(String comment, boolean withNewLine) {
        String suffix = withNewLine ? "\n" : "";
        return new TextComment(true, "AWS SDK for Java v2 migration: " + comment, suffix, Markers.EMPTY);
    }

    public static Comment createComment(String comment) {
        return generateComment(comment, false);
    }

    public static Comment createCommentWithNewline(String comment) {
        return generateComment(comment, true);
    }

    public static List<Comment> createComments(String comment) {
        return Arrays.asList(createComment(comment));
    }

    public static List<Comment> createCommentsWithNewline(String comment) {
        return Arrays.asList(createCommentWithNewline(comment));
    }

    public static boolean isUnsupportedPutObjectRequestSetter(J.MethodInvocation method) {
        return UNSUPPORTED_PUT_OBJ_REQUEST_TRANSFORMS.contains(method.getSimpleName());
    }

    public static boolean isObjectMetadataSetter(J.MethodInvocation method) {
        return isSetterForClassType(method, V2_S3_MODEL_PKG + "HeadObjectResponse");
    }

    /** Field set during POJO instantiation, e.g.,
     * PutObjectRequest request = new PutObjectRequest("bucket" "key", "redirectLocation").withFile(file);
     */
    public static boolean isPutObjectRequestBuilderSetter(J.MethodInvocation method) {
        return isSetterForClassType(method, "software.amazon.awssdk.services.s3.model.PutObjectRequest$Builder");
    }

    /** Field set after POJO instantiation, e.g.,
     * PutObjectRequest request = new PutObjectRequest("bucket" "key", "redirectLocation");
     * request.setFile(file);
     */
    public static boolean isPutObjectRequestSetter(J.MethodInvocation method) {
        return isSetterForClassType(method, "software.amazon.awssdk.services.s3.model.PutObjectRequest");
    }

    public static boolean isS3PutObjectOrObjectMetadata(J.MethodInvocation method) {
        return isObjectMetadataSetter(method)
            || isPutObjectRequestSetter(method)
            || isPutObjectRequestBuilderSetter(method);
    }

    public static boolean isSetterForClassType(J.MethodInvocation method, String fqcn) {
        if (method.getSelect() == null || method.getSelect().getType() == null) {
            return false;
        }
        return hasArguments(method) && TypeUtils.isOfClassType(method.getSelect().getType(), fqcn);
    }

    public static boolean hasArguments(J.MethodInvocation method) {
        return !method.getArguments().isEmpty();
    }

    public static boolean isPayloadSetter(J.MethodInvocation method) {
        return "file".equals(method.getSimpleName()) || "inputStream".equals(method.getSimpleName());
    }

    public static boolean isRequestPayerSetter(J.MethodInvocation method) {
        return "requestPayer".equals(method.getSimpleName());
    }

    public static boolean isRequestMetadataSetter(J.MethodInvocation method) {
        return "metadata".equals(method.getSimpleName());
    }

    public static boolean isCompleteMpuRequestMultipartUploadSetter(J.MethodInvocation method) {
        return "multipartUpload".equals(method.getSimpleName())
               && TypeUtils.isAssignableTo(V2_S3_MODEL_PKG + "CompleteMultipartUploadRequest.Builder",
                                           method.getSelect().getType());
    }

    public static boolean isGetObjectRequestPayerSetter(J.MethodInvocation method) {
        return "requestPayer".equals(method.getSimpleName())
               && TypeUtils.isAssignableTo(V2_S3_MODEL_PKG + "GetObjectRequest.Builder", method.getSelect().getType());
    }

    public static boolean isRestoreObjectRequestDaysSetter(J.MethodInvocation method) {
        return "days".equals(method.getSimpleName())
               && TypeUtils.isAssignableTo(V2_S3_MODEL_PKG + "RestoreObjectRequest.Builder",
                                           method.getSelect().getType());
    }

    public static boolean isGeneratePresignedUrl(J.MethodInvocation method) {
        return "generatePresignedUrl".equals(method.getSimpleName())
               && TypeUtils.isAssignableTo(V2_S3_CLIENT, method.getSelect().getType());
    }

    public static boolean isGetS3AccountOwner(J.MethodInvocation method) {
        return "getS3AccountOwner".equals(method.getSimpleName())
               && TypeUtils.isAssignableTo(V2_S3_CLIENT, method.getSelect().getType());
    }

    public static boolean isUnsupportedHttpMethod(String httpMethod) {
        return Arrays.asList("Head", "Post", "Patch").contains(httpMethod);
    }

    public static List<Comment> inputStreamBufferingWarningComment() {
        String warning = "When using InputStream to upload with S3Client, Content-Length should be specified and used "
                         + "with RequestBody.fromInputStream(). Otherwise, the entire stream will be buffered in memory. If"
                         + " content length must be unknown, we recommend using the CRT-based S3 client - "
                         + "https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/crt-based-s3-client.html";
        return createComments(warning);
    }

    public static List<Comment> assignedVariableHttpMethodNotSupportedComment() {
        String comment = "Transform for S3 generatePresignedUrl() with an assigned variable for HttpMethod is not supported."
                         + " Please manually migrate your code - "
                         + "https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner"
                         + ".html";
        return createComments(comment);
    }

    public static List<Comment> requestPojoTransformNotSupportedComment() {
        String comment = "Transforms are not supported for GeneratePresignedUrlRequest, please manually migrate your code - "
                         + "https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner"
                         + ".html";
        return createComments(comment);
    }

    public static List<Comment> httpMethodNotSupportedComment(String httpMethod) {
        String comment = String.format("S3 generatePresignedUrl() with %s HTTP method is not supported in v2. Only GET, PUT, "
                                       + "and DELETE are supported - "
                                       + "https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3"
                                       + "/presigner/S3Presigner.html",
                                       httpMethod.toUpperCase(Locale.ROOT));
        return createComments(comment);
    }

    public static List<Comment> presignerSingleInstanceSuggestion() {
        String comment = "If generating multiple pre-signed URLs, it is recommended to create a single instance of "
                         + "S3Presigner, since creating a presigner can be expensive. If applicable, please manually "
                         + "refactor the transformed code.";
        return createCommentsWithNewline(comment);
    }

    public static J.MethodInvocation sseAwsKeyManagementParamsNotSupportedComment(J.MethodInvocation method) {
        String comment = "Transform for PutObjectRequest setter sseAwsKeyManagementParam is not supported, please manually "
                         + "migrate your code to use the v2 setters: ssekmsKeyId, serverSideEncryption, sseCustomerAlgorithm - "
                         + "https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model"
                         + "/PutObjectRequest.Builder.html#ssekmsKeyId(java.lang.String)";
        return appendCommentToMethod(method, comment);
    }

    public static J.MethodInvocation sseCustomerKeyNotSupportedComment(J.MethodInvocation method) {
        String comment = "Transform for PutObjectRequest setter sseCustomerKey is not supported, please manually "
                         + "migrate your code to use the v2 setters: sseCustomerKey, sseCustomerKeyMD5 - "
                         + "https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model"
                         + "/PutObjectRequest.Builder.html#sseCustomerKey(java.lang.String)";
        return appendCommentToMethod(method, comment);
    }

    public static J.MethodInvocation accessControlListNotSupportedComment(J.MethodInvocation method) {
        String comment = "Transform for PutObjectRequest setter accessControlList is not supported, please manually "
                         + "migrate your code to use the v2 setters: acl, grantReadACP, grantWriteACP - "
                         + "https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model"
                         + "/PutObjectRequest.Builder.html#acl(java.lang.String)";
        return appendCommentToMethod(method, comment);
    }

    public static J.MethodInvocation addCommentForUnsupportedPutObjectRequestSetter(J.MethodInvocation method) {
        String methodName = method.getSimpleName();
        switch (methodName) {
            case "sseCustomerKey":
                return sseAwsKeyManagementParamsNotSupportedComment(method);
            case "sseAwsKeyManagementParams":
                return sseCustomerKeyNotSupportedComment(method);
            case "accessControlList":
                return accessControlListNotSupportedComment(method);
            default:
                return method;
        }
    }

    public static J.MethodInvocation appendCommentToMethod(J.MethodInvocation method, String comment) {
        if (method.getComments().isEmpty()) {
            return method.withComments(createCommentsWithNewline(comment));
        }

        List<Comment> existingComments = method.getComments();
        existingComments.add(createCommentWithNewline(comment));
        return method.withComments(existingComments);
    }
}
