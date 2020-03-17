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

package software.amazon.awssdk.services.s3.checksums;

import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.SERVER_SIDE_ENCRYPTION_HEADER;
import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AWS_KMS;

import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.handlers.AsyncChecksumValidationInterceptor;
import software.amazon.awssdk.services.s3.internal.handlers.SyncChecksumValidationInterceptor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.internal.Base16Lower;

/**
 * Class used by {@link SyncChecksumValidationInterceptor} and
 * {@link AsyncChecksumValidationInterceptor} to determine if trailing checksums
 * should be enabled for a given request.
 */
@SdkInternalApi
public final class ChecksumsEnabledValidator {

    public static final ExecutionAttribute<SdkChecksum> CHECKSUM = new ExecutionAttribute<>("checksum");

    private ChecksumsEnabledValidator() {
    }

    /**
     * Checks if trailing checksum is enabled for {@link S3Client#getObject(GetObjectRequest)} per request.
     *
     * @param request the request
     * @param executionAttributes the executionAttributes
     * @return true if trailing checksums is enabled, false otherwise
     */
    public static boolean getObjectChecksumEnabledPerRequest(SdkRequest request,
                                                             ExecutionAttributes executionAttributes) {
        return request instanceof GetObjectRequest && checksumEnabledPerConfig(executionAttributes);
    }

    /**
     * Checks if trailing checksum is enabled for {@link S3Client#getObject(GetObjectRequest)} per response.
     *
     * @param request the request
     * @param responseHeaders the response headers
     * @return true if trailing checksums is enabled, false otherwise
     */
    public static boolean getObjectChecksumEnabledPerResponse(SdkRequest request, SdkHttpHeaders responseHeaders) {
        return request instanceof GetObjectRequest && checksumEnabledPerResponse(responseHeaders);
    }

    /**
     * Validates that checksums should be enabled based on {@link ClientType} and the presence
     * or S3 specific headers.
     *
     * @param expectedClientType - The expected client type for enabling checksums
     * @param executionAttributes - {@link ExecutionAttributes} to determine the actual client type
     * @return If trailing checksums should be enabled for this request.
     */
    public static boolean shouldRecordChecksum(SdkRequest sdkRequest,
                                               ClientType expectedClientType,
                                               ExecutionAttributes executionAttributes,
                                               SdkHttpRequest httpRequest) {
        if (!(sdkRequest instanceof PutObjectRequest)) {
            return false;
        }

        ClientType actualClientType = executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_TYPE);

        if (!expectedClientType.equals(actualClientType)) {
            return false;
        }


        if (hasServerSideEncryptionHeader(httpRequest)) {
            return false;
        }

        return checksumEnabledPerConfig(executionAttributes);
    }

    public static boolean responseChecksumIsValid(SdkHttpResponse httpResponse) {
        return !hasServerSideEncryptionHeader(httpResponse);
    }

    private static boolean hasServerSideEncryptionHeader(SdkHttpHeaders httpRequest) {
        // S3 doesn't support trailing checksums for customer encryption
        if (httpRequest.firstMatchingHeader(SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER).isPresent()) {
            return true;
        }

        // S3 doesn't support trailing checksums for KMS encrypted objects
        if (httpRequest.firstMatchingHeader(SERVER_SIDE_ENCRYPTION_HEADER)
                       .filter(h -> h.contains(AWS_KMS.toString()))
                       .isPresent()) {
            return true;
        }
        return false;
    }

    /**
     * Client side validation for {@link PutObjectRequest}
     *
     * @param response the response
     * @param executionAttributes the execution attributes
     */
    public static void validatePutObjectChecksum(PutObjectResponse response, ExecutionAttributes executionAttributes) {
        SdkChecksum checksum = executionAttributes.getAttribute(CHECKSUM);

        if (response.eTag() != null) {
            String contentMd5 = BinaryUtils.toBase64(checksum.getChecksumBytes());
            byte[] digest = BinaryUtils.fromBase64(contentMd5);
            byte[] ssHash = Base16Lower.decode(response.eTag().replace("\"", ""));

            if (!Arrays.equals(digest, ssHash)) {
                throw SdkClientException.create(
                    String.format("Data read has a different checksum than expected. Was 0x%s, but expected 0x%s",
                                  BinaryUtils.toHex(digest), BinaryUtils.toHex(ssHash)));
            }
        }
    }

    /**
     * Check the response header to see if the trailing checksum is enabled.
     *
     * @param responseHeaders the SdkHttpHeaders
     * @return true if the trailing checksum is present in the header, false otherwise.
     */
    private static boolean checksumEnabledPerResponse(SdkHttpHeaders responseHeaders) {
        return responseHeaders.firstMatchingHeader(CHECKSUM_ENABLED_RESPONSE_HEADER)
                              .filter(b -> b.equals(ENABLE_MD5_CHECKSUM_HEADER_VALUE))
                              .isPresent();
    }

    /**
     * Check the {@link S3Configuration#checksumValidationEnabled()} to see if the checksum is enabled.
     *
     * @param executionAttributes the execution attributes
     * @return true if the trailing checksum is enabled in the config, false otherwise.
     */
    private static boolean checksumEnabledPerConfig(ExecutionAttributes executionAttributes) {
        S3Configuration serviceConfiguration =
            (S3Configuration) executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG);

        return serviceConfiguration == null || serviceConfiguration.checksumValidationEnabled();
    }
}
