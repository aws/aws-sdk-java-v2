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

package software.amazon.awssdk.benchmark.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Mock implementation of {@link SdkHttpClient} to return mock response.
 */
public final class MockHttpClient implements SdkHttpClient {

    private String successResponseContent;
    private String errorResponseContent;

    public MockHttpClient(String successResponseContent, String errorResponseContent) {
        this.successResponseContent = successResponseContent;
        this.errorResponseContent = errorResponseContent;
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        if (request.httpRequest().firstMatchingHeader("stub-error").isPresent()) {
            return new MockHttpRequest(errorResponse());
        }

        return new MockHttpRequest(successResponse());
    }

    @Override
    public void close() {
    }

    private class MockHttpRequest implements ExecutableHttpRequest {

        private final HttpExecuteResponse response;

        private MockHttpRequest(HttpExecuteResponse response) {
            this.response = response;
        }

        @Override
        public HttpExecuteResponse call() throws IOException {
            return response;
        }

        @Override
        public void abort() {
        }
    }

    private HttpExecuteResponse successResponse() {

        AbortableInputStream inputStream =
            AbortableInputStream.create(new ByteArrayInputStream(successResponseContent.getBytes()));

        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .build())
                                  .responseBody(inputStream)
                                  .build();
    }

    private HttpExecuteResponse errorResponse() {

        AbortableInputStream inputStream =
            AbortableInputStream.create(new ByteArrayInputStream(errorResponseContent.getBytes()));

        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(500)
                                                           .build())
                                  .responseBody(inputStream)
                                  .build();
    }
}