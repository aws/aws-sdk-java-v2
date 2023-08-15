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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.aws.signer.Checksummer;

@SdkProtectedApi
public final class PrecomputedChecksummer implements Checksummer {

    private final Callable<String> computation;

    public PrecomputedChecksummer(Callable<String> computation) {
        this.computation = computation;
    }

    @Override
    public String checksum(ContentStreamProvider payload) {
        try {
            return computation.call();
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve checksum: ", e);
        }
    }

    @Override
    public CompletableFuture<String> checksum(Publisher<ByteBuffer> payload) {
        try {
            return CompletableFuture.completedFuture(computation.call());
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve checksum: ", e);
        }
    }
}
