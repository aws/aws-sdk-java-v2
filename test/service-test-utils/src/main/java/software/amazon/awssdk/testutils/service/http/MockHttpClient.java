/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkRequestContext;

/**
 * Mockable implementation of {@link SdkHttpClient}.
 */
public final class MockHttpClient implements SdkHttpClient {

    private final List<SdkHttpFullRequest> capturedRequests = new ArrayList<>();
    private SdkHttpFullResponse nextResponse;

    @Override
    public AbortableCallable<SdkHttpFullResponse> prepareRequest(SdkHttpFullRequest request,
                                                                 SdkRequestContext requestContext) {
        capturedRequests.add(request);
        return new AbortableCallable<SdkHttpFullResponse>() {
            @Override
            public SdkHttpFullResponse call() throws Exception {
                return nextResponse;
            }

            @Override
            public void abort() {
            }
        };
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return Optional.empty();
    }

    @Override
    public void close() {
    }

    /**
     * Resets this mock by clearing any captured requests and wiping any stubbed responses.
     */
    public void reset() {
        this.capturedRequests.clear();
        this.nextResponse = null;
    }

    /**
     * Sets up the next HTTP response that will be returned by the mock.
     *
     * @param nextResponse Next {@link SdkHttpFullResponse} to return from
     *                     {@link #prepareRequest(SdkHttpFullRequest, SdkRequestContext)}
     */
    public void stubNextResponse(SdkHttpFullResponse nextResponse) {
        this.nextResponse = nextResponse;
    }

    /**
     * @return The last executed request that went through this mock client.
     * @throws IllegalStateException If no requests have been captured.
     */
    public SdkHttpFullRequest getLastRequest() {
        if (capturedRequests.isEmpty()) {
            throw new IllegalStateException("No requests were captured by the mock");
        }
        return capturedRequests.get(capturedRequests.size() - 1);
    }

}