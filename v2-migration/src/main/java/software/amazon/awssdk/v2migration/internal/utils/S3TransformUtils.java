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
import java.util.Map;
import java.util.Set;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class S3TransformUtils {

    public static final String V1_S3_CLIENT = "com.amazonaws.services.s3.AmazonS3";
    public static final String V1_S3_MODEL_PKG = "com.amazonaws.services.s3.model.";

    public static final String V2_S3_CLIENT = "software.amazon.awssdk.services.s3.S3Client";
    public static final String V2_S3_MODEL_PKG = "software.amazon.awssdk.services.s3.model.";

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
        "userMetadata"
    )));


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

    public static void addMetadataFields(StringBuilder sb, String metadataName,
                                         Map<String, Map<String, Expression>> metadataMap) {
        Map<String, Expression> map = metadataMap.get(metadataName);

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
    }

    public static String getArgumentName(J.MethodInvocation method) {
        Expression val = method.getArguments().get(0);
        return ((J.Identifier) val).getSimpleName();
    }

    public static String getSelectName(J.MethodInvocation method) {
        Expression select = method.getSelect();
        return ((J.Identifier) select).getSimpleName();
    }

    public static List<Comment> createComments(String comment) {
        return Collections.singletonList(
            new TextComment(true, "AWS SDK for Java v2 migration: " + comment, "", Markers.EMPTY));
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
}
