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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.internal.async.SdkPublishers;
import utils.FakePublisher;
import utils.FakeSdkPublisher;

public class SdkPublishersTest {
    @Test
    public void envelopeWrappedPublisher() {
        FakePublisher<ByteBuffer> fakePublisher = new FakePublisher<>();
        Publisher<ByteBuffer> wrappedPublisher =
            SdkPublishers.envelopeWrappedPublisher(fakePublisher, "prefix:", ":suffix");

        FakeByteBufferSubscriber fakeSubscriber = new FakeByteBufferSubscriber();
        wrappedPublisher.subscribe(fakeSubscriber);
        fakePublisher.publish(ByteBuffer.wrap("content".getBytes(StandardCharsets.UTF_8)));
        fakePublisher.complete();

        assertThat(fakeSubscriber.recordedEvents()).containsExactly("prefix:content", ":suffix");
    }

    @Test
    public void mapTransformsCorrectly() {
        FakeSdkPublisher<String> fakePublisher = new FakeSdkPublisher<>();
        FakeStringSubscriber fakeSubscriber = new FakeStringSubscriber();
        fakePublisher.map(String::toUpperCase).subscribe(fakeSubscriber);

        fakePublisher.publish("one");
        fakePublisher.publish("two");
        fakePublisher.complete();

        assertThat(fakeSubscriber.recordedEvents()).containsExactly("ONE", "TWO");
        assertThat(fakeSubscriber.isComplete()).isTrue();
        assertThat(fakeSubscriber.isError()).isFalse();
    }

    @Test
    public void mapHandlesError() {
        FakeSdkPublisher<String> fakePublisher = new FakeSdkPublisher<>();
        FakeStringSubscriber fakeSubscriber = new FakeStringSubscriber();
        RuntimeException exception = new IllegalArgumentException("Twos are not supported");

        fakePublisher.map(s -> {
            if ("two".equals(s)) {
                throw exception;
            }

            return s.toUpperCase();
        }).subscribe(fakeSubscriber);

        fakePublisher.publish("one");
        fakePublisher.publish("two");
        fakePublisher.publish("three");

        assertThat(fakeSubscriber.recordedEvents()).containsExactly("ONE");
        assertThat(fakeSubscriber.isComplete()).isFalse();
        assertThat(fakeSubscriber.isError()).isTrue();
        assertThat(fakeSubscriber.recordedErrors()).containsExactly(exception);
    }

    private final static class FakeByteBufferSubscriber implements Subscriber<ByteBuffer> {
        private final List<String> recordedEvents = new ArrayList<>();

        @Override
        public void onSubscribe(Subscription s) {

        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            String s = StandardCharsets.UTF_8.decode(byteBuffer).toString();
            recordedEvents.add(s);
        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onComplete() {

        }

        public List<String> recordedEvents() {
            return this.recordedEvents;
        }
    }

    private final static class FakeStringSubscriber implements Subscriber<String> {
        private final List<String> recordedEvents = new ArrayList<>();
        private final List<Throwable> recordedErrors = new ArrayList<>();
        private boolean isComplete = false;
        private boolean isError = false;

        @Override
        public void onSubscribe(Subscription s) {
            s.request(1000);
        }

        @Override
        public void onNext(String s) {
            recordedEvents.add(s);
        }

        @Override
        public void onError(Throwable t) {
            recordedErrors.add(t);
            this.isError = true;
        }

        @Override
        public void onComplete() {
            this.isComplete = true;
        }

        public List<String> recordedEvents() {
            return this.recordedEvents;
        }

        public List<Throwable> recordedErrors() {
            return this.recordedErrors;
        }

        public boolean isComplete() {
            return isComplete;
        }

        public boolean isError() {
            return isError;
        }
    }
}