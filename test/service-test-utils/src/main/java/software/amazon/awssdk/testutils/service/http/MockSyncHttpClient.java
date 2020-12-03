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

package software.amazon.awssdk.testutils.service.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Mockable implementation of {@link SdkHttpClient}.
 */
public final class MockSyncHttpClient implements SdkHttpClient, MockHttpClient {

    private final List<SdkHttpRequest> capturedRequests = new ArrayList<>();
    private final List<HttpExecuteResponse> responses = new LinkedList<>();
    private final AtomicInteger responseIndex = new AtomicInteger(0);

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        capturedRequests.add(request.httpRequest());
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() {
                HttpExecuteResponse response = responses.get(responseIndex.getAndIncrement() % responses.size());
                if (response == null) {
                    throw new IllegalStateException("No responses remain.");
                }
                return response;
            }

            @Override
            public void abort() {
            }
        };
    }


    @Override
    public void close() {
    }

    @Override
    public void reset() {
        this.capturedRequests.clear();
        this.responses.clear();
        this.responseIndex.set(0);
    }

    @Override
    public void stubNextResponse(HttpExecuteResponse nextResponse) {
        this.responses.clear();
        this.responses.add(nextResponse);
        this.responseIndex.set(0);
    }

    @Override
    public void stubResponses(HttpExecuteResponse... responses) {
        this.responses.clear();
        this.responses.addAll(Arrays.asList(responses));
        this.responseIndex.set(0);
    }

    @Override
    public List<SdkHttpRequest> getRequests() {
        return Collections.unmodifiableList(capturedRequests);
    }

    @Override
    public SdkHttpRequest getLastRequest() {
        if (capturedRequests.isEmpty()) {
            throw new IllegalStateException("No requests were captured by the mock");
        }
        return capturedRequests.get(capturedRequests.size() - 1);
    }
}
