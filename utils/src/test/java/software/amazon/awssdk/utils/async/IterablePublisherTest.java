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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class IterablePublisherTest {

    @Test
    void nullIterable_throwException() {
        assertThatThrownBy(() -> new IterablePublisher<>(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void emptyIterable_shouldComplete() {
        TestSubscriber testSubscriber = new TestSubscriber();
        IterablePublisher<String> iterablePublisher = new IterablePublisher<>(new ArrayList<>());
        iterablePublisher.subscribe(testSubscriber);
        assertThat(testSubscriber.onCompleteInvoked).isTrue();
        assertThat(testSubscriber.onNextInvoked).isFalse();
        assertThat(testSubscriber.onErrorInvoked).isFalse();
    }

    @Test
    void iterableReturnNull_shouldInvokeOnError() {
        TestSubscriber testSubscriber = new TestSubscriber();
        IterablePublisher<String> iterablePublisher = new IterablePublisher<>(Arrays.asList("foo", null));
        iterablePublisher.subscribe(testSubscriber);
        assertThat(testSubscriber.onCompleteInvoked).isFalse();
        assertThat(testSubscriber.results).contains("foo");
        assertThat(testSubscriber.onErrorInvoked).isTrue();
        assertThat(testSubscriber.throwable).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("returned null");
    }

    @Test
    void happyCase_shouldSendAllEvents() {
        TestSubscriber testSubscriber = new TestSubscriber();
        List<String> strings = IntStream.range(0, 100).mapToObj(i -> RandomStringUtils.random(i)).collect(Collectors.toList());

        IterablePublisher<String> iterablePublisher = new IterablePublisher<>(strings);
        iterablePublisher.subscribe(testSubscriber);
        assertThat(testSubscriber.onCompleteInvoked).isTrue();
        assertThat(testSubscriber.results).hasSameElementsAs(strings);
        assertThat(testSubscriber.onErrorInvoked).isFalse();
    }

    private static class TestSubscriber implements Subscriber<String> {
        private Subscription subscription;
        private List<String> results = new ArrayList<>();
        private boolean onNextInvoked;
        private boolean onErrorInvoked;
        private boolean onCompleteInvoked;
        private Throwable throwable;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(String s) {
            onNextInvoked = true;
            results.add(s);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            onErrorInvoked = true;
            throwable = t;
        }

        @Override
        public void onComplete() {
            onCompleteInvoked = true;

        }
    }
}
