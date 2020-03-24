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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * Common implementations of {@link SdkPublisher} that are provided for convenience when building asynchronous
 * interceptors to be used with specific clients.
 */
@SdkInternalApi
public final class SdkPublishers {
    private SdkPublishers() {
    }

    /**
     * Constructs an {@link SdkPublisher} that wraps a {@link ByteBuffer} publisher and inserts additional content
     * that wraps the published content like an envelope. This can be used when you want to transform the content of
     * an asynchronous SDK response by putting it in an envelope. This publisher implementation does not comply with
     * the complete flow spec (as it inserts data into the middle of a flow between a third-party publisher and
     * subscriber rather than acting as a fully featured independent publisher) and therefore should only be used in a
     * limited fashion when we have complete control over how data is being published to the publisher it wraps.
     * @param publisher The underlying publisher to wrap the content of.
     * @param envelopePrefix A string representing the content to be inserted as the head of the containing envelope.
     * @param envelopeSuffix A string representing the content to be inserted as the tail of containing envelope.
     * @return An {@link SdkPublisher} that wraps the provided publisher.
     */
    public static SdkPublisher<ByteBuffer> envelopeWrappedPublisher(Publisher<ByteBuffer> publisher,
                                                                    String envelopePrefix,
                                                                    String envelopeSuffix) {
        return EnvelopeWrappedSdkPublisher.of(publisher,
                                              wrapUtf8(envelopePrefix),
                                              wrapUtf8(envelopeSuffix),
                                              SdkPublishers::concat);
    }

    private static ByteBuffer wrapUtf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private static ByteBuffer concat(ByteBuffer b1, ByteBuffer b2) {
        ByteBuffer result = ByteBuffer.allocate(b1.remaining() + b2.remaining());
        result.put(b1);
        result.put(b2);
        result.rewind();
        return result;
    }
}
