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

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.internal.progress.listener.NoOpProgressUpdater;
import software.amazon.awssdk.core.internal.util.RequestProgressUpdaterInvoker;

/**
 * TCK verification class for {@link BytesReadTrackingPublisher}.
 */
public class BytesReadTrackingPublisherTckTest extends PublisherVerification<ByteBuffer> {
    public BytesReadTrackingPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long l) {
        return new BytesReadTrackingPublisher(createUpstreamPublisher(l), new AtomicLong(0), new RequestProgressUpdaterInvoker(new NoOpProgressUpdater()));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    private Publisher<ByteBuffer> createUpstreamPublisher(long elements) {
        return Flowable.fromIterable(Stream.generate(() -> ByteBuffer.wrap(new byte[1]))
                                           .limit(elements)
                                           .collect(Collectors.toList()));
    }
}
