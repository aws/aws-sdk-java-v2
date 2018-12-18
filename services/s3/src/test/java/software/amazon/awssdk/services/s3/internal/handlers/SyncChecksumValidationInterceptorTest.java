/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static software.amazon.awssdk.core.ClientType.SYNC;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.CLIENT_TYPE;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_CONFIG;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CONTENT_LENGTH_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.CHECKSUM;

import java.io.InputStream;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.checksums.ChecksumValidatingInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.utils.InterceptorTestUtils;

public class SyncChecksumValidationInterceptorTest {

    private SyncChecksumValidationInterceptor interceptor = new SyncChecksumValidationInterceptor();

    @Test
    public void modifyHttpContent_putObjectRequestChecksumEnabled_shouldWrapChecksumRequestBody() {
        ExecutionAttributes executionAttributes = getExecutionAttributes();
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(PutObjectRequest.builder().build());
        Optional<RequestBody> requestBody = interceptor.modifyHttpContent(modifyHttpRequest,
                                                                          executionAttributes);

        assertThat(requestBody.isPresent()).isTrue();
        assertThat(executionAttributes.getAttribute(CHECKSUM)).isNotNull();
        assertThat(requestBody.get().contentStreamProvider()).isNotEqualTo(modifyHttpRequest.requestBody().get());
    }

    @Test
    public void modifyHttpContent_nonPutObjectRequest_shouldNotModify() {
        ExecutionAttributes executionAttributes = getExecutionAttributes();
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(GetObjectRequest.builder().build());
        Optional<RequestBody> requestBody = interceptor.modifyHttpContent(modifyHttpRequest,
                                                                          executionAttributes);

        assertThat(requestBody).isEqualTo(modifyHttpRequest.requestBody());
    }

    @Test
    public void modifyHttpContent_putObjectRequest_checksumDisabled_shouldNotModify() {
        ExecutionAttributes executionAttributes = getExecutionAttributesWithChecksumDisabled();
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(GetObjectRequest.builder().build());

        Optional<RequestBody> requestBody = interceptor.modifyHttpContent(modifyHttpRequest,
                                                                          executionAttributes);
        assertThat(requestBody).isEqualTo(modifyHttpRequest.requestBody());
    }

    @Test
    public void modifyHttpResponseContent_getObjectRequest_checksumEnabled_shouldWrapChecksumValidatingPublisher() {
        SdkHttpResponse sdkHttpResponse = getSdkHttpResponseWithChecksumHeader();
        Context.ModifyHttpResponse modifyHttpResponse =
            InterceptorTestUtils.modifyHttpResponse(GetObjectRequest.builder().build(), sdkHttpResponse);
        Optional<InputStream> publisher = interceptor.modifyHttpResponseContent(modifyHttpResponse,
                                                                                getExecutionAttributes());
        assertThat(publisher.get()).isExactlyInstanceOf(ChecksumValidatingInputStream.class);
    }

    @Test
    public void modifyHttpResponseContent_getObjectRequest_responseDoesNotContainChecksum_shouldNotModify() {
        ExecutionAttributes executionAttributes = getExecutionAttributesWithChecksumDisabled();
        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                                                         .putHeader(CONTENT_LENGTH_HEADER, "100")
                                                         .build();
        Context.ModifyHttpResponse modifyHttpResponse =
            InterceptorTestUtils.modifyHttpResponse(GetObjectRequest.builder().build(), sdkHttpResponse);
        Optional<InputStream> publisher = interceptor.modifyHttpResponseContent(modifyHttpResponse,
                                                                                executionAttributes);
        assertThat(publisher).isEqualTo(modifyHttpResponse.responseBody());
    }

    @Test
    public void modifyHttpResponseContent_nonGetObjectRequest_shouldNotModify() {
        ExecutionAttributes executionAttributes = getExecutionAttributes();
        SdkHttpResponse sdkHttpResponse = getSdkHttpResponseWithChecksumHeader();
        sdkHttpResponse.toBuilder().clearHeaders();
        Context.ModifyHttpResponse modifyHttpResponse =
            InterceptorTestUtils.modifyHttpResponse(PutObjectRequest.builder().build(), sdkHttpResponse);
        Optional<InputStream> publisher = interceptor.modifyHttpResponseContent(modifyHttpResponse,
                                                                                executionAttributes);
        assertThat(publisher).isEqualTo(modifyHttpResponse.responseBody());

    }

    private ExecutionAttributes getExecutionAttributes() {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(CLIENT_TYPE, SYNC);
        return executionAttributes;
    }

    private SdkHttpResponse getSdkHttpResponseWithChecksumHeader() {
        return SdkHttpResponse.builder()
                              .putHeader(CONTENT_LENGTH_HEADER, "100")
                              .putHeader(CHECKSUM_ENABLED_RESPONSE_HEADER, ENABLE_MD5_CHECKSUM_HEADER_VALUE)
                              .build();
    }

    private ExecutionAttributes getExecutionAttributesWithChecksumDisabled() {
        ExecutionAttributes executionAttributes = getExecutionAttributes();
        executionAttributes.putAttribute(SERVICE_CONFIG, S3Configuration.builder().checksumValidationEnabled(false).build());
        return executionAttributes;
    }
}
