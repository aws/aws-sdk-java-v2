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
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyHttpResponseContent;

import java.io.InputStream;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class GetBucketPolicyInterceptorTest {

    private GetBucketPolicyInterceptor interceptor = new GetBucketPolicyInterceptor();

    @Test
    public void getBucketPolicy_shouldModifyResponseContent() {
        GetBucketPolicyRequest request = GetBucketPolicyRequest.builder().build();
        Context.ModifyHttpResponse context = modifyHttpResponseContent(request, SdkHttpResponse.builder()
                                                                                               .statusCode(200)
                                                                                               .build());
        Optional<InputStream> inputStream = interceptor.modifyHttpResponseContent(context, new ExecutionAttributes());
        assertThat(inputStream).isNotEqualTo(context.responseBody());
    }

    @Test
    public void nonGetBucketPolicyResponse_ShouldNotModifyResponse() {
        GetObjectRequest request = GetObjectRequest.builder().build();
        Context.ModifyHttpResponse context = modifyHttpResponseContent(request, SdkHttpResponse.builder().statusCode(200).build());
        Optional<InputStream> inputStream = interceptor.modifyHttpResponseContent(context, new ExecutionAttributes());
        assertThat(inputStream).isEqualTo(context.responseBody());
    }

    @Test
    public void errorResponseShouldNotModifyResponse() {
        GetBucketPolicyRequest request = GetBucketPolicyRequest.builder().build();
        Context.ModifyHttpResponse context = modifyHttpResponseContent(request, SdkHttpResponse.builder().statusCode(404).build());
        Optional<InputStream> inputStream = interceptor.modifyHttpResponseContent(context, new ExecutionAttributes());
        assertThat(inputStream).isEqualTo(context.responseBody());
    }
}
