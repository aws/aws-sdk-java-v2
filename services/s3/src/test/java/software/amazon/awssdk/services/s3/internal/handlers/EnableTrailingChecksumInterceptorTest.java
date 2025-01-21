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

package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.ENABLE_CHECKSUM_REQUEST_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.S3_MD5_CHECKSUM_LENGTH;
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyHttpRequestContext;
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyResponseContext;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.endpoints.internal.KnownS3ExpressEndpointProperty;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class EnableTrailingChecksumInterceptorTest {

    private EnableTrailingChecksumInterceptor interceptor = new EnableTrailingChecksumInterceptor();

    /**
     * Returns all possible Boolean combinations
     */
    private static Stream<Arguments> getObjectRequestParams() {
        return IntStream.range(0, 1 << 3).mapToObj(i -> Arguments.of((i & 1) == 1, (i & 2) == 2, (i & 4) == 4));
    }

    @ParameterizedTest
    @MethodSource("getObjectRequestParams")
    public void testGetObjectModifyRequest(boolean useS3Express, boolean disableChecksumValidation, boolean checksumModeEnabled) {
        GetObjectRequest request = createGetObjectRequest(checksumModeEnabled);
        ExecutionAttributes executionAttributes = createExecutionAttributes(disableChecksumValidation, useS3Express);
        Context.ModifyRequest modifyRequestContext = () -> request;
        SdkRequest modifiedRequest = interceptor.modifyRequest(modifyRequestContext, executionAttributes);

        Context.ModifyHttpRequest modifyHttpRequestContext = modifyHttpRequestContext(modifiedRequest);
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, executionAttributes);
        GetObjectRequest getObjectRequest = (GetObjectRequest) modifiedRequest;

        validateGetObjectValues(sdkHttpRequest, getObjectRequest, useS3Express, disableChecksumValidation, checksumModeEnabled);
    }

    private void validateGetObjectValues(SdkHttpRequest sdkHttpRequest, GetObjectRequest getObjectRequest,
                                         boolean useS3Express, boolean disableChecksumValidation, boolean checksumModeEnabled) {
        if (disableChecksumValidation) {
            // S3Express requests should follow general flow and respect checksum validation disabled flag
            if (checksumModeEnabled) {
                assertThat(getObjectRequest.checksumMode()).isNotNull();
                assertThat(getObjectRequest.checksumMode()).isEqualTo(ChecksumMode.ENABLED);
                assertThat(sdkHttpRequest.headers().get(ENABLE_CHECKSUM_REQUEST_HEADER)).isNull();
            } else {
                assertThat(getObjectRequest.checksumMode()).isNull();
                assertThat(sdkHttpRequest.headers().get(ENABLE_CHECKSUM_REQUEST_HEADER)).isNull();
            }
        } else {
            if (useS3Express) {
                // Interceptor modifyRequest() enables ChecksumMode
                assertThat(getObjectRequest.checksumMode()).isNotNull();
                assertThat(getObjectRequest.checksumMode()).isEqualTo(ChecksumMode.ENABLED);
                assertThat(sdkHttpRequest.headers().get(ENABLE_CHECKSUM_REQUEST_HEADER)).isNull();
            } else {
                if (checksumModeEnabled) {
                    assertThat(getObjectRequest.checksumMode()).isNotNull();
                    assertThat(getObjectRequest.checksumMode()).isEqualTo(ChecksumMode.ENABLED);
                    assertThat(sdkHttpRequest.headers().get(ENABLE_CHECKSUM_REQUEST_HEADER)).isNull();
                } else {
                    // Default SDK behavior
                    assertThat(getObjectRequest.checksumMode()).isNull();
                    assertThat(sdkHttpRequest.headers().get(ENABLE_CHECKSUM_REQUEST_HEADER)).containsOnly(ENABLE_MD5_CHECKSUM_HEADER_VALUE);
                }
            }
        }
    }

    private GetObjectRequest createGetObjectRequest(boolean checksumModeEnabled) {
        GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder();
        if (checksumModeEnabled) {
            requestBuilder.checksumMode(ChecksumMode.ENABLED);
        }
        return requestBuilder.build();
    }

    @Test
    public void modifyRequest_nonGetObjectRequest_shouldNotModify() {
        PutObjectRequest request = PutObjectRequest.builder().build();
        boolean useS3Express = false;
        boolean disableChecksumValidation = false;

        ExecutionAttributes executionAttributes = createExecutionAttributes(disableChecksumValidation, useS3Express);
        Context.ModifyRequest modifyRequestContext = () -> request;
        SdkRequest modifiedRequest = interceptor.modifyRequest(modifyRequestContext, executionAttributes);

        Context.ModifyHttpRequest modifyHttpRequestContext = modifyHttpRequestContext(modifiedRequest);
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, executionAttributes);

        assertThat(sdkHttpRequest.headers().get(ENABLE_CHECKSUM_REQUEST_HEADER)).isNull();
    }

    private ExecutionAttributes createExecutionAttributes(boolean disableChecksumValidation, boolean useS3Express) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        if (disableChecksumValidation) {
            executionAttributes.putAttribute(REQUEST_CHECKSUM_CALCULATION, RequestChecksumCalculation.WHEN_REQUIRED);
            executionAttributes.putAttribute(RESPONSE_CHECKSUM_VALIDATION, ResponseChecksumValidation.WHEN_REQUIRED);
        } else {
            executionAttributes.putAttribute(REQUEST_CHECKSUM_CALCULATION, RequestChecksumCalculation.WHEN_SUPPORTED);
            executionAttributes.putAttribute(RESPONSE_CHECKSUM_VALIDATION, ResponseChecksumValidation.WHEN_SUPPORTED);
        }
        if (useS3Express) {
            Endpoint s3ExpressEndpoint = Endpoint.builder().putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express").build();
            executionAttributes.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, s3ExpressEndpoint);
        }
        return executionAttributes;
    }

    @Test
    public void modifyResponse_getObjectResponseContainsChecksumHeader_shouldModifyResponse() {
        long contentLength = 50;
        GetObjectResponse response = GetObjectResponse.builder().contentLength(contentLength).build();
        Context.ModifyResponse modifyResponseContext = modifyResponseContext(
            GetObjectRequest.builder().build(),
            response,
            SdkHttpFullResponse.builder()
                               .putHeader(CHECKSUM_ENABLED_RESPONSE_HEADER, ENABLE_MD5_CHECKSUM_HEADER_VALUE).build());

        GetObjectResponse actualResponse = (GetObjectResponse) interceptor.modifyResponse(modifyResponseContext,
                                                                                          new ExecutionAttributes());

        assertThat(actualResponse).isNotEqualTo(response);
        assertThat(actualResponse.contentLength()).isEqualTo(contentLength - S3_MD5_CHECKSUM_LENGTH);
    }

    @Test
    public void modifyResponse_getObjectResponseNoChecksumHeader_shouldNotModifyResponse() {
        long contentLength = 50;
        GetObjectResponse response = GetObjectResponse.builder().contentLength(contentLength).build();
        Context.ModifyResponse modifyResponseContext = modifyResponseContext(
            GetObjectRequest.builder().build(),
            response,
            SdkHttpFullResponse.builder().build());

        GetObjectResponse actualResponse = (GetObjectResponse) interceptor.modifyResponse(modifyResponseContext,
                                                                                          new ExecutionAttributes());
        assertThat(actualResponse).isEqualTo(response);
    }

    @Test
    public void modifyResponse_nonGetObjectResponse_shouldNotModifyResponse() {
        GetObjectAclResponse getObjectAclResponse = GetObjectAclResponse.builder().build();
        Context.ModifyResponse modifyResponseContext = modifyResponseContext(
            GetObjectAclRequest.builder().build(),
            getObjectAclResponse,
            SdkHttpFullResponse.builder().build());

        GetObjectAclResponse actualResponse = (GetObjectAclResponse) interceptor.modifyResponse(modifyResponseContext,
                                                                                                new ExecutionAttributes());
        assertThat(actualResponse).isEqualTo(getObjectAclResponse);
    }
}
