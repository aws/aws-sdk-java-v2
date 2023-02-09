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

package software.amazon.awssdk.http.crt;



import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConnectionHealthConfigurationTest {

    @Test
    void builder_allPropertiesSet() {
        ConnectionHealthConfiguration connectionHealthConfiguration =
            ConnectionHealthConfiguration.builder()
                                         .minimumThroughputInBps(123l)
                                         .minimumThroughputTimeout(Duration.ofSeconds(1))
                                         .build();

        assertThat(connectionHealthConfiguration.minimumThroughputInBps()).isEqualTo(123);
        assertThat(connectionHealthConfiguration.minimumThroughputTimeout()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void builder_nullMinimumThroughputInBps_shouldThrowException() {
        assertThatThrownBy(() ->
            ConnectionHealthConfiguration.builder()
                                         .minimumThroughputTimeout(Duration.ofSeconds(1))
                                         .build()).hasMessageContaining("minimumThroughputInBps");
    }

    @Test
    void builder_nullMinimumThroughputTimeout() {
        assertThatThrownBy(() ->
                               ConnectionHealthConfiguration.builder()
                                                            .minimumThroughputInBps(1L)
                                                            .build()).hasMessageContaining("minimumThroughputTimeout");
    }

    @Test
    void builder_negativeMinimumThroughputTimeout() {
        assertThatThrownBy(() ->
                               ConnectionHealthConfiguration.builder()
                                                            .minimumThroughputInBps(1L)
                                                            .minimumThroughputTimeout(Duration.ofSeconds(-1))
                                                            .build()).hasMessageContaining("minimumThroughputTimeout");
    }
}
