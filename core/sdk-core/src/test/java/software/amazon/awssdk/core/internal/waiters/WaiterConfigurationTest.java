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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.Test;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;

public class WaiterConfigurationTest {

    @Test
    public void overrideConfigurationNull_shouldUseDefaultValue() {
        WaiterConfiguration waiterConfiguration = new WaiterConfiguration(null);

        assertThat(waiterConfiguration.backoffStrategy())
            .isEqualTo(FixedDelayBackoffStrategy.create(Duration.ofSeconds(5)));
        assertThat(waiterConfiguration.maxAttempts()).isEqualTo(3);
        assertThat(waiterConfiguration.waitTimeout()).isNull();
    }

    @Test
    public void overrideConfigurationNotAllValuesProvided_shouldUseDefaultValue() {
        WaiterConfiguration waiterConfiguration = new WaiterConfiguration(WaiterOverrideConfiguration.builder()
                                                                                                     .backoffStrategy(BackoffStrategy.none())
                                                                                                     .build());

        assertThat(waiterConfiguration.backoffStrategy())
            .isEqualTo(BackoffStrategy.none());
        assertThat(waiterConfiguration.maxAttempts()).isEqualTo(3);
        assertThat(waiterConfiguration.waitTimeout()).isNull();
    }

    @Test
    public void overrideConfigurationProvided_shouldTakesPrecedence() {
        WaiterConfiguration waiterConfiguration =
            new WaiterConfiguration(WaiterOverrideConfiguration.builder()
                                                               .backoffStrategy(BackoffStrategy.none())
                                                               .maxAttempts(10)
                                                               .waitTimeout(Duration.ofMinutes(3))
                                                               .build());

        assertThat(waiterConfiguration.backoffStrategy())
            .isEqualTo(BackoffStrategy.none());
        assertThat(waiterConfiguration.maxAttempts()).isEqualTo(10);
        assertThat(waiterConfiguration.waitTimeout()).isEqualTo(Duration.ofMinutes(3));
    }


}
