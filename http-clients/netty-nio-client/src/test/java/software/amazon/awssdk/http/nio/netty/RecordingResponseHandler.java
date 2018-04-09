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

package software.amazon.awssdk.http.nio.netty;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.async.SimpleSubscriber;

public final class RecordingResponseHandler implements SdkHttpResponseHandler<Void> {

    List<SdkHttpResponse> responses = new ArrayList<>();
    private StringBuilder bodyParts = new StringBuilder();
    CompletableFuture<Void> completeFuture = new CompletableFuture<>();

    @Override
    public void headersReceived(SdkHttpResponse response) {
        responses.add(response);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        publisher.subscribe(new SimpleSubscriber(byteBuffer -> {
            byte[] b = new byte[byteBuffer.remaining()];
            byteBuffer.duplicate().get(b);
            bodyParts.append(new String(b, StandardCharsets.UTF_8));
        }));
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        completeFuture.completeExceptionally(throwable);
    }

    @Override
    public Void complete() {
        completeFuture.complete(null);
        return null;
    }

    String fullResponseAsString() {
        return bodyParts.toString();
    }

}
