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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.aws.signer.V4Context;
import software.amazon.awssdk.http.auth.aws.signer.V4PayloadSigner;

/**
 * A default implementation of a payload signer that is a no-op, since payloads are most commonly unsigned.
 */
@SdkProtectedApi
public class DefaultV4PayloadSigner implements V4PayloadSigner {
    @Override
    public ContentStreamProvider sign(ContentStreamProvider payload, V4Context v4Context) {
        return payload;
    }

    @Override
    public Publisher<ByteBuffer> sign(Publisher<ByteBuffer> payload, CompletableFuture<V4Context> futureV4Context) {
        return payload;
    }
}
