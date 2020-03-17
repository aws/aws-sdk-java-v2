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

package software.amazon.awssdk.modulepath.tests.mocktests;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Mock implementation of {@link SdkAsyncHttpClient}.
 */
public final class MockAyncHttpClient implements SdkAsyncHttpClient {

    private final List<SdkHttpRequest> capturedRequests = new ArrayList<>();
    private HttpExecuteResponse nextResponse;


    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        capturedRequests.add(request.request());

        request.responseHandler().onHeaders(nextResponse.httpResponse());
        request.responseHandler().onStream(new SdkHttpContentPublisher() {
            byte[] content = nextResponse.responseBody().map(p -> invokeSafely(() -> IoUtils.toByteArray(p)))
                                         .orElseGet(() -> new byte[0]);

            @Override
            public Optional<Long> contentLength() {
                return Optional.of((long) content.length);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                s.onSubscribe(new Subscription() {
                    private boolean running = true;

                    @Override
                    public void request(long n) {
                        if (n <= 0) {
                            running = false;
                            s.onError(new IllegalArgumentException("Demand must be positive"));
                        } else if (running) {
                            running = false;
                            s.onNext(ByteBuffer.wrap(content));
                            s.onComplete();
                        }
                    }

                    @Override
                    public void cancel() {
                        running = false;
                    }
                });
            }
        });
        return CompletableFuture.completedFuture(null);
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

    public void stubNextResponse(HttpExecuteResponse nextResponse) {
        this.nextResponse = nextResponse;
    }

}
