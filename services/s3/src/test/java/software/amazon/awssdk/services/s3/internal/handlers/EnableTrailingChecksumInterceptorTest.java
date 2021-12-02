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
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_CHECKSUM_REQUEST_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.S3_MD5_CHECKSUM_LENGTH;
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyHttpRequestContext;
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyResponseContext;

import org.junit.Test;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class EnableTrailingChecksumInterceptorTest {

    private EnableTrailingChecksumInterceptor interceptor = new EnableTrailingChecksumInterceptor();

    @Test
    public void modifyHttpRequest_getObjectTrailingChecksumEnabled_shouldAddTrailingChecksumHeader() {
        Context.ModifyHttpRequest modifyHttpRequestContext =
            modifyHttpRequestContext(GetObjectRequest.builder().build());

        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, new ExecutionAttributes());

        assertThat(sdkHttpRequest.headers().get(ENABLE_CHECKSUM_REQUEST_HEADER)).containsOnly(ENABLE_MD5_CHECKSUM_HEADER_VALUE);
    }

    @Test
    public void modifyHttpRequest_getObjectTrailingChecksumDisabled_shouldNotModifyHttpRequest() {
        Context.ModifyHttpRequest modifyHttpRequestContext =
            modifyHttpRequestContext(GetObjectRequest.builder().build());

        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext,
                                                                      new ExecutionAttributes().putAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG,
                                                                                                             S3Configuration.builder().checksumValidationEnabled(false).build()));

        assertThat(sdkHttpRequest).isEqualToComparingFieldByField(modifyHttpRequestContext.httpRequest());
    }

    @Test
    public void modifyHttpRequest_nonGetObjectRequest_shouldNotModifyHttpRequest() {
        Context.ModifyHttpRequest modifyHttpRequestContext =
            modifyHttpRequestContext(PutObjectRequest.builder().build());

        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext, new ExecutionAttributes());
        assertThat(sdkHttpRequest).isEqualToComparingFieldByField(modifyHttpRequestContext.httpRequest());
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
