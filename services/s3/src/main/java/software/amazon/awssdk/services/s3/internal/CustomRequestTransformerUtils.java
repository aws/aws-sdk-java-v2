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

package software.amazon.awssdk.services.s3.internal;

import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncodeIgnoreSlashes;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.internal.resource.S3ArnUtils;
import software.amazon.awssdk.services.s3.internal.resource.S3ResourceType;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * This class transforms specific request parameter into a custom parameter. The logic needed to
 * construct parameters like {@code copySource} which is non-trivial, so this class facilitates the use of
 * higher-level constructs that more closely match other APIs, such as {@link PutObjectRequest}.
 * <p>
 * For more information, see:
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html#API_CopyObject_RequestParameters">CopyObject Request Parameters</a>
 * <p>
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html#API_UploadPartCopy_RequestParameters">UploadPartCopy Request Parameters</a>
 */
@SdkInternalApi
public final class CustomRequestTransformerUtils {

    private CustomRequestTransformerUtils() {
    }

    public static CopyObjectRequest modifyCopyObjectRequest(CopyObjectRequest request) {
        if (request.copySource() != null) {
            requireNotSet(request.sourceBucket(), "sourceBucket");
            requireNotSet(request.sourceKey(), "sourceKey");
            requireNotSet(request.sourceVersionId(), "sourceVersionId");
            return request;
        }
        String copySource = constructCopySource(
            requireSet(request.sourceBucket(), "sourceBucket"),
            requireSet(request.sourceKey(), "sourceKey"),
            request.sourceVersionId()
        );
        return request.toBuilder()
                      .sourceBucket(null)
                      .sourceKey(null)
                      .sourceVersionId(null)
                      .copySource(copySource)
                      .build();
    }

    public static UploadPartCopyRequest modifyUploadPartCopyRequest(UploadPartCopyRequest request) {
        if (request.copySource() != null) {
            requireNotSet(request.sourceBucket(), "sourceBucket");
            requireNotSet(request.sourceKey(), "sourceKey");
            requireNotSet(request.sourceVersionId(), "sourceVersionId");
            return request;
        }
        String copySource = constructCopySource(
            requireSet(request.sourceBucket(), "sourceBucket"),
            requireSet(request.sourceKey(), "sourceKey"),
            request.sourceVersionId()
        );
        return request.toBuilder()
                      .sourceBucket(null)
                      .sourceKey(null)
                      .sourceVersionId(null)
                      .copySource(copySource)
                      .build();
    }

    private static String constructCopySource(String sourceBucket, String sourceKey, String sourceVersionId) {
        StringBuilder copySource = new StringBuilder();
        copySource.append(urlEncodeIgnoreSlashes(sourceBucket));
        S3ArnUtils.getArnType(sourceBucket).ifPresent(arnType -> {
            if (arnType == S3ResourceType.ACCESS_POINT || arnType == S3ResourceType.OUTPOST) {
                copySource.append("/object");
            }
        });
        copySource.append("/");
        copySource.append(urlEncodeIgnoreSlashes(sourceKey));
        if (sourceVersionId != null) {
            copySource.append("?versionId=");
            copySource.append(urlEncodeIgnoreSlashes(sourceVersionId));
        }
        return copySource.toString();
    }

    private static void requireNotSet(Object value, String paramName) {
        Validate.isTrue(value == null, "Parameter 'copySource' must not be used in conjunction with '%s'",
                        paramName);
    }

    private static <T> T requireSet(T value, String paramName) {
        Validate.isTrue(value != null, "Parameter '%s' must not be null",
                        paramName);
        return value;
    }
}
