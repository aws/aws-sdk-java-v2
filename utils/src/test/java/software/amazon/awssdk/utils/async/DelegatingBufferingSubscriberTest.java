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

package software.amazon.awssdk.utils.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class DelegatingBufferingSubscriberTest {

    @Test
    void givenMultipleBufferTotalToBufferSize_ExpectSubscriberGetThemAll() {
        TestSubscriber testSubscriber = new TestSubscriber(32);
        DelegatingBufferingSubscriber subscriber = new DelegatingBufferingSubscriber(32, testSubscriber);
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        publisher.subscribe(subscriber);

        for (int i = 0; i < 3; i++) {
            ByteBuffer buff = ByteBuffer.wrap(byteArrayWithValue((byte)i, 8));
            publisher.send(buff);
            assertThat(testSubscriber.onNextCallAmount).isZero();
            assertThat(testSubscriber.totalReceived).isZero();
        }

        ByteBuffer buff = ByteBuffer.wrap(byteArrayWithValue((byte)3, 8));
        publisher.send(buff);
        assertThat(testSubscriber.onNextCallAmount).isEqualTo(4);
        assertThat(testSubscriber.totalReceived).isEqualTo(32);

        publisher.complete();
        assertThat(testSubscriber.onNextCallAmount).isEqualTo(4);
        assertThat(testSubscriber.totalReceived).isEqualTo(32);

        testSubscriber.assertAllReceivedInChunk(8);
    }

    @Test
    void givenMultipleBufferLessThenBufferSize_ExpectSubscriberGetThemAll() {
        TestSubscriber testSubscriber = new TestSubscriber(32);
        DelegatingBufferingSubscriber subscriber = new DelegatingBufferingSubscriber(64, testSubscriber);
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        publisher.subscribe(subscriber);

        for (int i = 0; i < 4; i++) {
            ByteBuffer buff = ByteBuffer.wrap(byteArrayWithValue((byte)i, 8));
            publisher.send(buff);
        }
        testSubscriber.assertNothingReceived();

        publisher.complete();
        testSubscriber.assertBytesReceived(4, 32);
        testSubscriber.assertAllReceivedInChunk(8);
    }

    @Test
    void exceedsBufferInMultipleChunk_BytesReceivedInMultipleBatches() {
        TestSubscriber testSubscriber = new TestSubscriber(64);
        DelegatingBufferingSubscriber subscriber = new DelegatingBufferingSubscriber(32, testSubscriber);
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        publisher.subscribe(subscriber);

        for (int i = 0; i < 3; i++) {
            ByteBuffer buff = ByteBuffer.wrap(byteArrayWithValue((byte)i, 8));
            publisher.send(buff);
        }
        testSubscriber.assertNothingReceived();

        for (int i = 3; i < 8; i++) {
            ByteBuffer buff = ByteBuffer.wrap(byteArrayWithValue((byte)i, 8));
            publisher.send(buff);
        }
        testSubscriber.assertBytesReceived(8, 64);

        publisher.complete();

        // make sure nothing more is received
        testSubscriber.assertBytesReceived(8, 64);
        testSubscriber.assertAllReceivedInChunk(8);
    }

    @Test
    void exceedsBufferSingle_BytesReceivedInMultipleBatches() {
        TestSubscriber testSubscriber = new TestSubscriber(256);
        DelegatingBufferingSubscriber subscriber = new DelegatingBufferingSubscriber(32, testSubscriber);
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        publisher.subscribe(subscriber);

        publisher.send(ByteBuffer.wrap(byteArrayWithValue((byte)0, 256)));
        testSubscriber.assertBytesReceived(8, 256);

        publisher.complete();
        testSubscriber.assertBytesReceived(8, 256);

        testSubscriber.assertAllReceivedInChunk(256);
    }

    @Test
    void multipleBuffer_unevenSizes() {
        TestSubscriber testSubscriber = new TestSubscriber(59);
        DelegatingBufferingSubscriber subscriber = new DelegatingBufferingSubscriber(32, testSubscriber);
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        publisher.subscribe(subscriber);

        publisher.send(ByteBuffer.wrap(byteArrayWithValue((byte)0, 9)));
        testSubscriber.assertNothingReceived();

        publisher.send(ByteBuffer.wrap(byteArrayWithValue((byte)1, 20)));
        testSubscriber.assertNothingReceived();

        publisher.send(ByteBuffer.wrap(byteArrayWithValue((byte)2, 30)));
        testSubscriber.assertBytesReceived(2, 29);

        publisher.complete();
        testSubscriber.assertBytesReceived(3, 59);

        ByteBuffer received = testSubscriber.received;
        received.position(0);
        for (int i = 0; i < 9; i++) {
            assertThat(received.get()).isEqualTo((byte)0);
        }
        for (int i = 0; i < 20; i++) {
            assertThat(received.get()).isEqualTo((byte)1);
        }
        for (int i = 0; i < 30; i++) {
            assertThat(received.get()).isEqualTo((byte)2);
        }

    }

    private class TestSubscriber implements Subscriber<ByteBuffer> {
        int onNextCallAmount = 0;
        int totalReceived = 0;
        int totalSizeExpected;
        ByteBuffer received;

        public TestSubscriber(int totalSizeExpected) {
            this.totalSizeExpected = totalSizeExpected;
            this.received = ByteBuffer.allocate(totalSizeExpected);
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            System.out.println("received in delegate " + byteBuffer.remaining());
            onNextCallAmount++;
            totalReceived += byteBuffer.remaining();
            received.put(byteBuffer);
        }

        @Override
        public void onError(Throwable t) {
            // do nothing, test purpose only
        }

        @Override
        public void onComplete() {
            // do nothing, test purpose only
        }

        void assertNothingReceived() {
            assertThat(onNextCallAmount).isZero();
            assertThat(totalReceived).isZero();
        }

        void assertBytesReceived(int timesOnNextWasCalled, int totalBytesReceived) {
            assertThat(onNextCallAmount).isEqualTo(timesOnNextWasCalled);
            assertThat(totalReceived).isEqualTo(totalBytesReceived);

        }

        void assertAllReceivedInChunk(int chunkSize) {
            received.position(0);
            for (int i = 0; i < totalReceived/chunkSize; i++) {
                for (int j = 0; j < chunkSize; j++) {
                    assertThat(received.get(i * chunkSize + j)).isEqualTo((byte)i);
                }
            }
        }
    }

    private static byte[] byteArrayWithValue(byte value, int size) {
        byte[] arr = new byte[size];
        for (int i = 0; i < size; i++) {
            arr[i] = value;
        }
        return arr;
    }
}