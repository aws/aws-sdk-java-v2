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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.checksums.SdkChecksum;

public class UnbufferedChecksumSubscriberTest {
    @Test
    void subscribe_updatesEachChecksumWithIdenticalData() {
        List<ByteBuffer> buffers = Arrays.asList(ByteBuffer.wrap("foo".getBytes()),
                                                 ByteBuffer.wrap("bar".getBytes()),
                                                 ByteBuffer.wrap("baz".getBytes()));

        Publisher<ByteBuffer> publisher = Flowable.fromIterable(buffers);

        SdkChecksum checksum1 = Mockito.mock(SdkChecksum.class);
        SdkChecksum checksum2 = Mockito.mock(SdkChecksum.class);

        List<SdkChecksum> checksums = Arrays.asList(checksum1, checksum2);

        UnbufferedChecksumSubscriber subscriber = new UnbufferedChecksumSubscriber(checksums, new TestSubscriber<>());

        publisher.subscribe(subscriber);

        for (SdkChecksum checksum : checksums) {
            ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
            Mockito.verify(checksum, Mockito.times(3)).update(captor.capture());
            assertThat(captor.getAllValues()).containsExactlyElementsOf(buffers);
        }
    }

    @Test
    public void subscribe_onNextDelegatedToWrappedSubscriber() {
        List<ByteBuffer> buffers = Arrays.asList(ByteBuffer.wrap("foo".getBytes()),
                                                 ByteBuffer.wrap("bar".getBytes()),
                                                 ByteBuffer.wrap("baz".getBytes()));

        Publisher<ByteBuffer> publisher = Flowable.fromIterable(buffers);

        SdkChecksum checksum = Mockito.mock(SdkChecksum.class);

        Subscriber<ByteBuffer> wrappedSubscriber = Mockito.mock(Subscriber.class);
        doAnswer(i -> {
            ((Subscription) i.getArguments()[0]).request(Long.MAX_VALUE);
            return null;
        }).when(wrappedSubscriber).onSubscribe(any(Subscription.class));

        UnbufferedChecksumSubscriber subscriber = new UnbufferedChecksumSubscriber(Collections.singletonList(checksum),
                                                                                   wrappedSubscriber);

        publisher.subscribe(subscriber);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);

        Mockito.verify(wrappedSubscriber, Mockito.times(3)).onNext(captor.capture());

        assertThat(captor.getAllValues()).containsExactlyElementsOf(buffers);
    }
}
