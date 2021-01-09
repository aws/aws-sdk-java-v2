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
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyHttpRequestContext;

import org.junit.Test;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class PutObjectInterceptorTest {
    private final PutObjectInterceptor interceptor = new PutObjectInterceptor();

    @Test
    public void modifyHttpRequest_setsExpect100Continue_whenSdkRequestIsPutObject() {

        final SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext(PutObjectRequest.builder().build()),
                                                                             new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
    }

    @Test
    public void modifyHttpRequest_doesNotSetExpect_whenSdkRequestIsNotPutObject() {

        final SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext(GetObjectRequest.builder().build()),
                                                                             new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).isNotPresent();
    }
}
