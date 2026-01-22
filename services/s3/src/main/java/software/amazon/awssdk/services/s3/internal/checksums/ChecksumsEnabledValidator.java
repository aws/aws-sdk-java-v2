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

package software.amazon.awssdk.services.s3.internal.checksums;

import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AWS_KMS;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpHeaders;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.internal.handlers.AsyncChecksumValidationInterceptor;
import software.amazon.awssdk.services.s3.internal.handlers.SyncChecksumValidationInterceptor;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Class used by {@link SyncChecksumValidationInterceptor} and
 * {@link AsyncChecksumValidationInterceptor} to determine if trailing checksums
 * should be enabled for a given request.
 */
@SdkInternalApi
public final class ChecksumsEnabledValidator {

    public static final ExecutionAttribute<SdkChecksum> CHECKSUM = new ExecutionAttribute<>("checksum");
    public static final ExecutionAttribute<Boolean> SKIP_MD5_TRAILING_CHECKSUM = new ExecutionAttribute<>(
        "skipMd5TrailingChecksum");

    private ChecksumsEnabledValidator() {
    }

    /**
     * Checks if trailing checksum is enabled and {@link ChecksumMode} is disabled for
     * {@link S3Client#getObject(GetObjectRequest)} per request.
     *
     * @param request the request
     * @param executionAttributes the executionAttributes
     * @return true if trailing checksums is enabled and ChecksumMode is disabled, false otherwise
     */
    public static boolean getObjectChecksumEnabledPerRequest(SdkRequest request,
                                                             ExecutionAttributes executionAttributes) {
        return request instanceof GetObjectRequest
               && ((GetObjectRequest) request).checksumMode() != ChecksumMode.ENABLED
               && checksumEnabledPerConfigForGet(executionAttributes);
    }

    /**
     * Checks if trailing checksum is enabled for {@link S3Client#getObject(GetObjectRequest)} per response.
     *
     * @param request the request
     * @param responseHeaders the response headers
     * @param executionAttributes the executionAttributes
     * @return true if trailing checksums is enabled, false otherwise
     */
    public static boolean getObjectChecksumEnabledPerResponse(SdkRequest request, SdkHttpHeaders responseHeaders,
                                                              ExecutionAttributes executionAttributes) {
        if (!(request instanceof GetObjectRequest)) {
            return false;
        }

        ResponseChecksumValidation responseChecksumValidation =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION);

        if (responseChecksumValidation != null && responseChecksumValidation == ResponseChecksumValidation.WHEN_REQUIRED) {
            return false;
        }

        return checksumEnabledPerResponse(responseHeaders);
    }

    public static boolean responseChecksumIsValid(SdkHttpResponse httpResponse) {
        return !hasServerSideEncryptionHeader(httpResponse);
    }

    private static boolean hasServerSideEncryptionHeader(SdkHttpHeaders httpRequest) {
        // S3 doesn't support trailing checksums for customer encryption
        if (httpRequest.firstMatchingHeader(ChecksumConstant.SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER).isPresent()) {
            return true;
        }

        // S3 doesn't support trailing checksums for KMS encrypted objects
        if (httpRequest.firstMatchingHeader(ChecksumConstant.SERVER_SIDE_ENCRYPTION_HEADER)
                       .filter(h -> h.contains(AWS_KMS.toString()))
                       .isPresent()) {
            return true;
        }
        return false;
    }

    /**
     * Check the response header to see if the trailing checksum is enabled.
     *
     * @param responseHeaders the SdkHttpHeaders
     * @return true if the trailing checksum is present in the header, false otherwise.
     */
    private static boolean checksumEnabledPerResponse(SdkHttpHeaders responseHeaders) {
        return responseHeaders.firstMatchingHeader(ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER)
                              .filter(b -> b.equals(ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE))
                              .isPresent();
    }

    /**
     * Check the {@code SdkExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION} to see if the checksum is enabled for GET.
     *
     * @param executionAttributes the execution attributes
     * @return true if the checksum is enabled in the config, false otherwise.
     */
    private static boolean checksumEnabledPerConfigForGet(ExecutionAttributes executionAttributes) {
        Boolean skipTrailingChecksum = executionAttributes.getAttribute(SKIP_MD5_TRAILING_CHECKSUM);
        if (skipTrailingChecksum != null && skipTrailingChecksum) {
            return false;
        }

        ResponseChecksumValidation responseChecksumValidation =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION);

        return responseChecksumValidation == ResponseChecksumValidation.WHEN_SUPPORTED;
    }
}
