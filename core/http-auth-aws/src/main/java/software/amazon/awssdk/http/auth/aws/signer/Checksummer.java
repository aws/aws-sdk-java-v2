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

package software.amazon.awssdk.http.auth.aws.signer;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.DefaultChecksummer;

/**
 * An interface for defining how a checksum is formed from a payload synchronously and asynchronously.
 */
@SdkProtectedApi
public interface Checksummer {
    /**
     * Get a default implementation of a checksummer.
     */
    static Checksummer create() {
        return new DefaultChecksummer();
    }

    /**
     * Given a payload, calculate a checksum and return it as a string.
     */
    String checksum(ContentStreamProvider payload);

    /**
     * Given a payload, asynchronously calculate a checksum and return a future containing it as a string.
     */
    CompletableFuture<String> checksum(Publisher<ByteBuffer> payload);
}
