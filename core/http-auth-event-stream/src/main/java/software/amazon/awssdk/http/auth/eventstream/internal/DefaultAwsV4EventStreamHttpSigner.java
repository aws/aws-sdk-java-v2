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

package software.amazon.awssdk.http.auth.eventstream.internal;

import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.eventstream.AwsV4EventStreamHttpSigner;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;

/**
 * A default implementation of {@link AwsV4EventStreamHttpSigner}.
 */
@SdkInternalApi
public final class DefaultAwsV4EventStreamHttpSigner extends DefaultAwsV4HttpSigner implements AwsV4EventStreamHttpSigner {

    private static final String HTTP_CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-EVENTS";

    @Override
    protected void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                    ContentChecksum contentChecksum) {
        requestBuilder.putHeader(X_AMZ_CONTENT_SHA256, HTTP_CONTENT_SHA_256);
        super.addPrerequisites(requestBuilder, contentChecksum);
    }

    @Override
    protected Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload) {
        if (payload == null) {
            return null;
        }

        return new SigV4DataFramePublisher(payload, credentials, credentialScope, signature, signingClock);
    }

    @Override
    protected CompletableFuture<String> createContentHash(Publisher<ByteBuffer> payload) {
        return CompletableFuture.completedFuture(HTTP_CONTENT_SHA_256);
    }
}
