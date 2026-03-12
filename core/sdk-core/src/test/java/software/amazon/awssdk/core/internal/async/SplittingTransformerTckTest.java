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

package software.amazon.awssdk.core.internal.async;

import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

public class SplittingTransformerTckTest extends PublisherVerification<AsyncResponseTransformer<Object, Object>> {

    public SplittingTransformerTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<AsyncResponseTransformer<Object, Object>> createPublisher(long l) {
        CompletableFuture<ResponseBytes<Object>> future = new CompletableFuture<>();
        AsyncResponseTransformer<Object, ResponseBytes<Object>> upstreamTransformer = AsyncResponseTransformer.toBytes();
        SplittingTransformer<Object, ResponseBytes<Object>> transformer =
            SplittingTransformer.<Object, ResponseBytes<Object>>builder()
                                .upstreamResponseTransformer(upstreamTransformer)
                                .maximumBufferSizeInBytes(64 * 1024L)
                                .resultFuture(future)
                                .build();
        return SdkPublisher.adapt(transformer).limit(Math.toIntExact(l));
    }

    @Override
    public Publisher<AsyncResponseTransformer<Object, Object>> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return Long.MAX_VALUE;
    }
}
