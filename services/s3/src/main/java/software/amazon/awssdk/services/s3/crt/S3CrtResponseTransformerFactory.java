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

package software.amazon.awssdk.services.s3.crt;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtDirectBufferPoolStreamMarker;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Provides factory methods for CRT-specific S3 asynchronous response transformers.
 */
@SdkPublicApi
public final class S3CrtResponseTransformerFactory {
    private S3CrtResponseTransformerFactory() {
    }

    /**
     * Creates a transformer that returns the response body as a blocking {@link ResponseInputStream} that reads directly
     * from the client's direct buffer pool. Unlike {@link AsyncResponseTransformer#toBlockingInputStream()}, the client
     * does not copy each downloaded part into a new heap array. Reading into the caller's array still copies.
     *
     * <p>The buffers come from the client's direct buffer pool. For {@code GetObject} requests with {@code partNumber}, CRT
     * delivers the body from a separately allocated buffer outside the pool.
     *
     * <p>This transformer requires an {@link S3AsyncClient} created by {@link S3AsyncClient#crtBuilder()} with a direct
     * buffer pool configured through {@link S3CrtAsyncClientBuilder#directBufferPoolConfiguration}. Pass it directly to
     * {@code getObject}. Clients without that configuration fail before request transmission, and transformers that wrap
     * this one are not supported.
     *
     * <p>The future returned by {@code getObject} completes when the response stream is ready. The transformer can be
     * reused across {@code getObject} calls.
     *
     * <p>Always close the response stream to release unread buffers and cancel remaining download work. For downloads using
     * a direct buffer pool, unread pool memory remains unavailable to other requests on the client until the stream is
     * closed.
     *
     * <p>Concurrent downloads that share a direct buffer pool can stall if the pool is too small. See
     * {@link S3CrtDirectBufferPoolConfiguration} for how to size it.
     *
     * <p>{@code modifyAsyncHttpResponseContent} is not applied to the object body.
     *
     * @return a transformer that produces a blocking response stream
     */
    public static AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>>
        toBlockingInputStreamFromDirectBufferPool() {
        return new S3CrtDirectBufferPoolStreamMarker();
    }
}
