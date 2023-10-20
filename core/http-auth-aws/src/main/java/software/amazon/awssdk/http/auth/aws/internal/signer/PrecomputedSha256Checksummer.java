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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * An implementation of a checksummer that simply passes along a computed value as a checksum. Specifically, this is used in the
 * cases where the checksum is a pre-defined value that dictates specific behavior by the signer, and flexible checksums is not
 * enabled for the request (such as aws-chunked payload signing without trailers, unsigned streaming without trailers, etc.).
 */
@SdkInternalApi
public final class PrecomputedSha256Checksummer implements Checksummer {

    private final Callable<String> computation;

    public PrecomputedSha256Checksummer(Callable<String> computation) {
        this.computation = computation;
    }

    @Override
    public void checksum(ContentStreamProvider payload, SdkHttpRequest.Builder request) {
        try {
            String checksum = computation.call();
            request.putHeader(X_AMZ_CONTENT_SHA256, checksum);
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve checksum: ", e);
        }
    }

    @Override
    public CompletableFuture<Publisher<ByteBuffer>> checksum(Publisher<ByteBuffer> payload, SdkHttpRequest.Builder request) {
        try {
            String checksum = computation.call();
            request.putHeader(X_AMZ_CONTENT_SHA256, checksum);
            return CompletableFuture.completedFuture(payload);
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve checksum: ", e);
        }
    }
}
