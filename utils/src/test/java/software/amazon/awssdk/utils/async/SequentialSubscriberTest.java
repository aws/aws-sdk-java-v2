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

import io.reactivex.Flowable;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class SequentialSubscriberTest {

    @Test
    void consumerThrowsException_shouldCompleteFutureExceptionally() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AssertionError error = new AssertionError("boom");
        SequentialSubscriber<Integer> subscriber =
            new SequentialSubscriber<>(i -> {
                throw error;
            }, future);

        Flowable.fromArray(1, 2).subscribe(subscriber);
        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(() -> future.join()).hasRootCause(error);
    }
}
