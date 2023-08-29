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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;

public class AddingTrailingDataSubscriberTest {

    @Test
    void trailingDataSupplierNull_shouldThrowException() {
        SequentialSubscriber<Integer> downstreamSubscriber = new SequentialSubscriber<Integer>(i -> {}, new CompletableFuture());
        assertThatThrownBy(() -> new AddingTrailingDataSubscriber<>(downstreamSubscriber, null))
            .hasMessageContaining("must not be null");
    }

    @Test
    void subscriberNull_shouldThrowException() {
        assertThatThrownBy(() -> new AddingTrailingDataSubscriber<>(null, () -> 1))
            .hasMessageContaining("must not be null");
    }

    @Test
    void trailingDataNotNull_shouldNotSendAdditionalData() {
        List<Integer> result = new ArrayList<>();
        CompletableFuture future = new CompletableFuture();
        SequentialSubscriber<Integer> downstreamSubscriber = new SequentialSubscriber<Integer>(i -> result.add(i), future);

        Subscriber<Integer> subscriber = new AddingTrailingDataSubscriber<>(downstreamSubscriber, () -> Integer.MAX_VALUE);

        publishData(subscriber);

        future.join();

        assertThat(result).containsExactly(0, 1, 2, Integer.MAX_VALUE);
    }

    @Test
    void trailingDataNull_shouldNotSendAdditionalData() {
        List<Integer> result = new ArrayList<>();
        CompletableFuture future = new CompletableFuture();
        SequentialSubscriber<Integer> downstreamSubscriber = new SequentialSubscriber<Integer>(i -> result.add(i), future);

        Subscriber<Integer> subscriber = new AddingTrailingDataSubscriber<>(downstreamSubscriber, () -> null);

        publishData(subscriber);

        future.join();

        assertThat(result).containsExactly(0, 1, 2);
    }

    private void publishData(Subscriber<Integer> subscriber) {
        SimplePublisher<Integer> simplePublisher = new SimplePublisher<>();
        simplePublisher.subscribe(subscriber);
        for (int i = 0; i < 3; i++) {
            simplePublisher.send(i);
        }
        simplePublisher.complete();
    }
}
