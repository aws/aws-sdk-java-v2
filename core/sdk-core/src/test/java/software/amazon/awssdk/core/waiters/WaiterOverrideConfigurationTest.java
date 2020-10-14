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

package software.amazon.awssdk.core.waiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.Test;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;

public class WaiterOverrideConfigurationTest {

    @Test
    public void valuesProvided_shouldReturnOptionalValues() {
        WaiterOverrideConfiguration configuration = WaiterOverrideConfiguration.builder()
                                                                               .maxAttempts(10)
                                                                               .backoffStrategy(BackoffStrategy.none())
                                                                               .waitTimeout(Duration.ofSeconds(1))
                                                                               .build();
        assertThat(configuration.backoffStrategy()).contains(BackoffStrategy.none());
        assertThat(configuration.maxAttempts()).contains(10);
        assertThat(configuration.waitTimeout()).contains(Duration.ofSeconds(1));
    }

    @Test
    public void valuesNotProvided_shouldReturnEmptyOptionalValues() {
        WaiterOverrideConfiguration configuration = WaiterOverrideConfiguration.builder().build();
        assertThat(configuration.backoffStrategy()).isEmpty();
        assertThat(configuration.maxAttempts()).isEmpty();
        assertThat(configuration.waitTimeout()).isEmpty();
    }

    @Test
    public void nonPositiveMaxWaitTime_shouldThrowException() {
        assertThatThrownBy(() -> WaiterOverrideConfiguration.builder()
                                                            .waitTimeout(Duration.ZERO)
                                                            .build()).hasMessageContaining("must be positive");
    }

    @Test
    public void nonPositiveMaxAttempts_shouldThrowException() {
        assertThatThrownBy(() -> WaiterOverrideConfiguration.builder()
                                                            .maxAttempts(-10)
                                                            .build()).hasMessageContaining("must be positive");
    }

    @Test
    public void toBuilder_shouldGenerateSameBuilder() {
        WaiterOverrideConfiguration overrideConfiguration =
            WaiterOverrideConfiguration.builder()
                                       .waitTimeout(Duration.ofSeconds(2))
                                       .maxAttempts(10)
                                       .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1)))
                                       .build();

        WaiterOverrideConfiguration config = overrideConfiguration.toBuilder().build();
        assertThat(overrideConfiguration).isEqualTo(config);
        assertThat(overrideConfiguration.hashCode()).isEqualTo(config.hashCode());
    }
}
