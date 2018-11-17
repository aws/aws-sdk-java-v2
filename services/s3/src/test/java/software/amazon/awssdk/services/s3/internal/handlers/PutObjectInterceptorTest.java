/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class PutObjectInterceptorTest {
    private final PutObjectInterceptor interceptor = new PutObjectInterceptor();

    @Test
    public void modifyHttpRequest_setsExpect100Continue_whenSdkRequestIsPutObject() {

        final SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(context(PutObjectRequest.builder().build()),
                                                                             new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
    }

    @Test
    public void modifyHttpRequest_doesNotSetExpect_whenSdkRequestIsNotPutObject() {

        final SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(context(GetObjectRequest.builder().build()),
                                                                             new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).isNotPresent();
    }

    private SdkHttpFullRequest sdkHttpFullRequest() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://test.com:80"))
                                 .method(SdkHttpMethod.GET)
                                 .build();
    }

    private Context.ModifyHttpRequest context(SdkRequest request) {
        return new Context.ModifyHttpRequest() {
            @Override
            public SdkHttpRequest httpRequest() {
                return sdkHttpFullRequest();
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return null;
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return null;
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }
}
