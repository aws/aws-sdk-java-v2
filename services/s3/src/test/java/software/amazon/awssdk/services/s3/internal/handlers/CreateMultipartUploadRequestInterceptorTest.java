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
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.sdkHttpFullRequest;

import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.utils.InterceptorTestUtils;

public class CreateMultipartUploadRequestInterceptorTest {

    private final CreateMultipartUploadRequestInterceptor interceptor = new CreateMultipartUploadRequestInterceptor();

    @Test
    public void createMultipartRequest_shouldModifyHttpContent() {
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(CreateMultipartUploadRequest.builder().build());
        Optional<RequestBody> requestBody =
            interceptor.modifyHttpContent(modifyHttpRequest,
                                          new ExecutionAttributes());
        assertThat(modifyHttpRequest.requestBody().get()).isNotEqualTo(requestBody.get());
    }

    @Test
    public void createMultipartRequest_shouldModifyHttpRequest() {
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(CreateMultipartUploadRequest.builder().build());
        SdkHttpRequest httpRequest = interceptor.modifyHttpRequest(modifyHttpRequest, new ExecutionAttributes());
        assertThat(httpRequest).isNotEqualTo(modifyHttpRequest.httpRequest());
        assertThat(httpRequest.headers()).containsEntry(CONTENT_LENGTH, Collections.singletonList("0"));
        assertThat(httpRequest.headers()).containsEntry(CONTENT_TYPE, Collections.singletonList("binary/octet-stream"));
    }

    @Test
    public void createMultipartRequest_contentTypePresent_shouldNotModifyContentType() {
        String overrideContentType = "application/json";
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(CreateMultipartUploadRequest.builder().build(),
                                                          sdkHttpFullRequest().toBuilder()
                                                                              .putHeader(CONTENT_TYPE, overrideContentType).build());

        SdkHttpRequest httpRequest = interceptor.modifyHttpRequest(modifyHttpRequest, new ExecutionAttributes());
        assertThat(httpRequest).isNotEqualTo(modifyHttpRequest.httpRequest());
        assertThat(httpRequest.headers()).containsEntry(CONTENT_LENGTH, Collections.singletonList("0"));
        assertThat(httpRequest.headers()).containsEntry(CONTENT_TYPE, Collections.singletonList(overrideContentType));
    }

    @Test
    public void nonCreateMultipartRequest_shouldNotModifyHttpContent() {
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(PutObjectRequest.builder().build());
        Optional<RequestBody> requestBody =
            interceptor.modifyHttpContent(modifyHttpRequest,
                                          new ExecutionAttributes());
        assertThat(modifyHttpRequest.requestBody().get()).isEqualTo(requestBody.get());
    }

    @Test
    public void nonCreateMultipartRequest_shouldNotModifyHttpRequest() {
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(GetObjectAclRequest.builder().build());
        SdkHttpRequest httpRequest = interceptor.modifyHttpRequest(modifyHttpRequest, new ExecutionAttributes());
        assertThat(httpRequest).isEqualTo(modifyHttpRequest.httpRequest());
    }
}
