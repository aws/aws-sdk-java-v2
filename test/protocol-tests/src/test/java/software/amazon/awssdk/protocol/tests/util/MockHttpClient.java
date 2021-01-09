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

package software.amazon.awssdk.protocol.tests.util;

import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Mock implementation of {@link SdkHttpClient}.
 */
public final class MockHttpClient implements SdkHttpClient {

    private HttpExecuteResponse nextResponse;
    private boolean isClosed;

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() {
                return nextResponse;
            }

            @Override
            public void abort() {
            }
        };
    }

    @Override
    public void close() {
        isClosed = true;
    }

    /**
     * Sets up the next HTTP response that will be returned by the mock.
     *
     * @param nextResponse Next {@link SdkHttpFullResponse} to return from
     *                     {@link #prepareRequest(HttpExecuteRequest)}
     */
    public void stubNextResponse(HttpExecuteResponse nextResponse) {
        this.nextResponse = nextResponse;
    }

    public boolean isClosed() {
        return isClosed;
    }
}
