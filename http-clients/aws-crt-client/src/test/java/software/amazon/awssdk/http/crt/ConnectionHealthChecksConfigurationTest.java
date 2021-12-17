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
import org.junit.Test;

public class ConnectionHealthChecksConfigurationTest {

    @Test
    public void builder_allPropertiesSet() {
        ConnectionHealthChecksConfiguration connectionHealthChecksConfiguration =
            ConnectionHealthChecksConfiguration.builder()
                                               .minThroughputInBytesPerSecond(123l)
                                               .allowableThroughputFailureInterval(Duration.ofSeconds(1))
                                               .build();

        assertThat(connectionHealthChecksConfiguration.minThroughputInBytesPerSecond()).isEqualTo(123);
        assertThat(connectionHealthChecksConfiguration.allowableThroughputFailureInterval()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    public void builder_nullMinThroughputInBytesPerSecond_shouldThrowException() {
        assertThatThrownBy(() ->
            ConnectionHealthChecksConfiguration.builder()
                                               .allowableThroughputFailureInterval(Duration.ofSeconds(1))
                                               .build()).hasMessageContaining("minThroughputInBytesPerSecond");
    }

    @Test
    public void builder_nullAllowableThroughputFailureInterval() {
        assertThatThrownBy(() ->
                               ConnectionHealthChecksConfiguration.builder()
                                                                  .minThroughputInBytesPerSecond(1L)
                                                                  .build()).hasMessageContaining("allowableThroughputFailureIntervalSeconds");
    }

    @Test
    public void builder_negativeAllowableThroughputFailureInterval() {
        assertThatThrownBy(() ->
                               ConnectionHealthChecksConfiguration.builder()
                                                                  .minThroughputInBytesPerSecond(1L)
                                                                  .allowableThroughputFailureInterval(Duration.ofSeconds(-1))
                                                                  .build()).hasMessageContaining("allowableThroughputFailureIntervalSeconds");
    }
}
