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

public class TcpKeepAliveConfigurationTest {

    @Test
    public void builder_allPropertiesSet() {
        TcpKeepAliveConfiguration tcpKeepAliveConfiguration =
            TcpKeepAliveConfiguration.builder()
                                     .keepAliveInterval(Duration.ofMinutes(1))
                                     .keepAliveTimeout(Duration.ofSeconds(1))
                                     .keepAliveMaxFailedProbes(1)
                                     .build();

        assertThat(tcpKeepAliveConfiguration.keepAliveInterval()).isEqualTo(Duration.ofMinutes(1));
        assertThat(tcpKeepAliveConfiguration.keepAliveTimeout()).isEqualTo(Duration.ofSeconds(1));
        assertThat(tcpKeepAliveConfiguration.keepAliveMaxFailedProbes()).isEqualTo(1);
    }
    
    @Test
    public void builder_nullKeepAliveTimeout_shouldThrowException() {
        assertThatThrownBy(() ->
                               TcpKeepAliveConfiguration.builder()
                                                        .keepAliveInterval(Duration.ofMinutes(1))
                                                        .build())
            .hasMessageContaining("keepAliveTimeout");
    }
    
    @Test
    public void builder_nullKeepAliveInterval_shouldThrowException() {
        assertThatThrownBy(() ->
                               TcpKeepAliveConfiguration.builder()
                                                        .keepAliveTimeout(Duration.ofSeconds(1))
                                                        .build())
            .hasMessageContaining("keepAliveInterval");
    }

    @Test
    public void builder_nullKeepAliveMaxFailedProbes_shouldBeAllowed() {
        TcpKeepAliveConfiguration config = TcpKeepAliveConfiguration.builder()
                                                                    .keepAliveInterval(Duration.ofMinutes(1))
                                                                    .keepAliveTimeout(Duration.ofSeconds(1))
                                                                    .build();

        assertThat(config.keepAliveMaxFailedProbes()).isNull();
    }

    @Test
    public void builder_zeroKeepAliveMaxFailedProbes_shouldBeAllowed() {
        TcpKeepAliveConfiguration config = TcpKeepAliveConfiguration.builder()
                                                                    .keepAliveInterval(Duration.ofMinutes(1))
                                                                    .keepAliveTimeout(Duration.ofSeconds(1))
                                                                    .keepAliveMaxFailedProbes(0)
                                                                    .build();

        assertThat(config.keepAliveMaxFailedProbes()).isEqualTo(0);
    }


    @Test
    public void builder_nonPositiveKeepAliveTimeout_shouldThrowException() {
        assertThatThrownBy(() ->
                               TcpKeepAliveConfiguration.builder()
                                                        .keepAliveInterval(Duration.ofMinutes(1))
                                                        .keepAliveTimeout(Duration.ofSeconds(0))
                                                        .build())
            .hasMessageContaining("keepAliveTimeout");
    }
    
    @Test
    public void builder_nonPositiveKeepAliveInterval_shouldThrowException() {
        assertThatThrownBy(() ->
                               TcpKeepAliveConfiguration.builder()
                                                        .keepAliveInterval(Duration.ofMinutes(0))
                                                        .keepAliveTimeout(Duration.ofSeconds(1))
                                                        .build())
            .hasMessageContaining("keepAliveInterval");
    }

    @Test
    public void builder_nonPositiveKeepAliveMaxFailedProbes_shouldThrowException() {
        assertThatThrownBy(() ->
                               TcpKeepAliveConfiguration.builder()
                                                        .keepAliveInterval(Duration.ofMinutes(1))
                                                        .keepAliveTimeout(Duration.ofSeconds(1))
                                                        .keepAliveMaxFailedProbes(-1)
                                                        .build())
            .hasMessageContaining("keepAliveMaxFailedProbes");
    }
}
