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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.progress.listener.DefaultProgressUpdater;
import software.amazon.awssdk.core.internal.util.RequestProgressUpdaterInvoker;
import software.amazon.awssdk.core.progress.listener.ProgressListener;

public class BytesSentTrackingPublisherTest {

    @Test
    public void validate_updatesBytesSent_invocation_tracksBytesSentAccurately() {
        int nElements = 8;
        int elementSize = 2;

        DefaultProgressUpdater defaultProgressUpdater = Mockito.mock(DefaultProgressUpdater.class);

        Publisher<ByteBuffer> upstreamPublisher = createUpstreamPublisher(nElements, elementSize);
        BytesReadTrackingPublisher trackingPublisher = new BytesReadTrackingPublisher(upstreamPublisher, new AtomicLong(0),
                                                                                      new RequestProgressUpdaterInvoker(defaultProgressUpdater));
        readFully(trackingPublisher);

        long expectedSent = nElements * elementSize;

        assertThat(trackingPublisher.bytesRead()).isEqualTo(expectedSent);
    }

    @Test
    public void progressUpdater_invokes_incrementBytesSent() {
        int nElements = 8;
        int elementSize = 2;

        ProgressListener progressListener = Mockito.mock(ProgressListener.class);

        SdkRequestOverrideConfiguration config = SdkRequestOverrideConfiguration.builder()
                                                                                .addProgressListener(progressListener)
                                                                                .build();

        SdkRequest request = NoopTestRequest.builder()
                                            .overrideConfiguration(config)
                                            .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(request, null);

        Publisher<ByteBuffer> upstreamPublisher = createUpstreamPublisher(nElements, elementSize);
        BytesReadTrackingPublisher trackingPublisher = new BytesReadTrackingPublisher(upstreamPublisher, new AtomicLong(0L),
                                                                                      new RequestProgressUpdaterInvoker(defaultProgressUpdater));
        readFully(trackingPublisher);

        long expectedSent = nElements * elementSize;

        assertThat(trackingPublisher.bytesRead()).isEqualTo(expectedSent);
        Mockito.verify(progressListener, Mockito.times(nElements)).requestBytesSent(ArgumentMatchers.any());
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

