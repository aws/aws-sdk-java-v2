/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.java.internal;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;



@SdkPublicApi
public final class JavaHttpRequestExecutor {

    private HttpRequest javaHttpRequest;
    private HttpClient javaHttpClient;
    private AsyncExecuteRequest asyncExecuteRequest;

    public JavaHttpRequestExecutor(HttpClient javaHttpClient, AsyncExecuteRequest asyncExecuteRequest) {
        this.asyncExecuteRequest = asyncExecuteRequest;
        this.javaHttpRequest = new JavaHttpRequestFactory(asyncExecuteRequest).createJavaHttpRequest();
        this.javaHttpClient = javaHttpClient;
    }

    private void assignBodyPublisher(Publisher<ByteBuffer> stream) {
        asyncExecuteRequest.responseHandler().onStream(stream);
    }

    /**
     * Creates the {@link ListToByteBufferProcessor} and pass the Publisher and Subscriber to
     * SdkAsyncHttpResponseHandler and HttpResponse.BodyHandler respectively to connect these two ends.
     *
     * @return CompletableFuture&lt;Void&gt; The CompletableFuture object that indicates whether the
     *         request has been execute successfully
     */
    public CompletableFuture<Void> execute() {
        ListToByteBufferProcessor listToByteBufferProcessor = new ListToByteBufferProcessor();
        HttpResponse.BodyHandler<Void> javaHttpClientBodyHandler = HttpResponse.BodyHandlers
                .fromSubscriber(FlowAdapters.toFlowSubscriber(listToByteBufferProcessor));

        assignBodyPublisher(listToByteBufferProcessor.getPublisherToSdk());

        javaHttpClient.sendAsync(javaHttpRequest, javaHttpClientBodyHandler);

        CompletableFuture<Void> terminated = listToByteBufferProcessor.getTerminated();
        terminated.whenComplete((r, t) -> {
            if (t != null) {
                terminated.completeExceptionally(t);
            }
        });
        return terminated;
    }

}
