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

package software.amazon.awssdk.core.internal.waiters;

import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.Test;
import org.testng.Assert;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;

class WaiterExecutorTest {
    @Test
    void largeMaxAttempts() {

        int expectedAttempts = 10_000;

        WaiterOverrideConfiguration conf =
            WaiterOverrideConfiguration.builder()
                                       .maxAttempts(expectedAttempts)
                                       .backoffStrategy(BackoffStrategy.none())
                                       .build();

        WaiterExecutor<Integer> sut =
            new WaiterExecutor<>(new WaiterConfiguration(conf),
                                 Arrays.asList(
                                     WaiterAcceptor.retryOnResponseAcceptor(c -> c < expectedAttempts),
                                     WaiterAcceptor.successOnResponseAcceptor(c -> c == expectedAttempts)
                                 ));

        LongAdder attemptCounter = new LongAdder();
        sut.execute(() -> {
            attemptCounter.increment();
            return attemptCounter.intValue();
        });

        Assert.assertEquals(attemptCounter.intValue(), expectedAttempts);
    }
}