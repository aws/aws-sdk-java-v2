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

package software.amazon.awssdk.services.s3.internal.crt;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.DrainingSubscriber;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Marks a response transformer that requires direct-buffer-pool delivery from the CRT S3 client.
 */
@SdkInternalApi
public final class S3CrtDirectBufferPoolStreamMarker
    implements AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> {

    @Override
    public CompletableFuture<ResponseInputStream<GetObjectResponse>> prepare() {
        throw new UnsupportedOperationException(
            "This transformer requires a CRT-based S3 client configured with a direct buffer pool, and cannot be "
            + "wrapped by another transformer.");
    }

    @Override
    public void onResponse(GetObjectResponse response) {
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        publisher.subscribe(new DrainingSubscriber<>());
    }

    @Override
    public void exceptionOccurred(Throwable error) {
    }

    @Override
    public String name() {
        return TransformerType.DIRECT_BUFFER_POOL_STREAM.getName();
    }
}
