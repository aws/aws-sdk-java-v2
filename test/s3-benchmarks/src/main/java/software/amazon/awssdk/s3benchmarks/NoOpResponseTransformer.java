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

package software.amazon.awssdk.s3benchmarks;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * A no-op {@link AsyncResponseTransformer}
 */
public class NoOpResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, Void> {
    private CompletableFuture<Void> future;

    @Override
    public CompletableFuture<Void> prepare() {
        future = new CompletableFuture<>();
        return future;
    }

    @Override
    public void onResponse(GetObjectResponse response) {
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        publisher.subscribe(new SimpleSubscriber(Buffer::clear) {
            @Override
            public void onComplete() {
                super.onComplete();
                future.complete(null);
            }
        });
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        future.completeExceptionally(error);
    }
}
