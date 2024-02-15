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

package software.amazon.awssdk.core.internal.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * Functional tests for {@link BytesReadTrackingPublisher}.
 */
public class BytesReadTrackingPublisherTest {

    @Test
    public void test_requestAll_calculatesCorrectTotal() {
        long nElements = 1024;
        int elementSize = 4;
        Publisher<ByteBuffer> upstreamPublisher = createUpstreamPublisher(nElements, elementSize);
        BytesReadTrackingPublisher trackingPublisher = new BytesReadTrackingPublisher(upstreamPublisher, new AtomicLong(0), Optional.empty());
        readFully(trackingPublisher);

        assertThat(trackingPublisher.bytesRead()).isEqualTo(nElements * elementSize);
    }

    @Test
    public void test_requestAll_updatesInputCount() {
        long nElements = 8;
        int elementSize = 2;

        long baseBytesRead = 1024;
        AtomicLong bytesRead = new AtomicLong(baseBytesRead);

        Publisher<ByteBuffer> upstreamPublisher = createUpstreamPublisher(nElements, elementSize);
        BytesReadTrackingPublisher trackingPublisher = new BytesReadTrackingPublisher(upstreamPublisher, bytesRead, Optional.empty());
        readFully(trackingPublisher);

        long expectedRead = baseBytesRead + nElements * elementSize;
        assertThat(bytesRead.get()).isEqualTo(expectedRead);
        assertThat(trackingPublisher.bytesRead()).isEqualTo(expectedRead);
    }

    private Publisher<ByteBuffer> createUpstreamPublisher(long elements, int elementSize) {
        return Flowable.fromIterable(Stream.generate(() -> ByteBuffer.wrap(new byte[elementSize]))
                                           .limit(elements)
                                           .collect(Collectors.toList()));
    }

    private void readFully(Publisher<ByteBuffer> publisher) {
        Flowable.fromPublisher(publisher).toList().blockingGet();
    }
}
