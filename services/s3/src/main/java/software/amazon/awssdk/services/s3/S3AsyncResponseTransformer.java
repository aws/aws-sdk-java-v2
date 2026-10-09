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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtBorrowedBufferResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Provides S3-specific asynchronous response transformers.
 */
@SdkPublicApi
public final class S3AsyncResponseTransformer {
    private S3AsyncResponseTransformer() {
    }

    /**
     * Creates a transformer that exposes the response body as a blocking stream backed by borrowed direct buffers.
     *
     * <p>This transformer requires an {@link S3AsyncClient} created by {@link S3AsyncClient#crtBuilder()} with a direct
     * buffer pool configured through {@link S3CrtAsyncClientBuilder#directBufferPoolConfiguration}. Unsupported clients
     * fail before transmitting the request.
     *
     * <p>Concurrent downloads that share a pool can stall if the pool is too small. See
     * {@link software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration} for how to size it.
     *
     * @return a transformer that produces a blocking response stream
     */
    public static AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>>
        toBlockingInputStreamWithBorrowedBuffers() {
        return new S3CrtBorrowedBufferResponseTransformer();
    }
}
