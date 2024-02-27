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

package software.amazon.awssdk.crtcore;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.crt.http.HttpMonitoringOptions;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.io.TlsContext;

class CrtConnectionUtilsTest {

    @Test
    void resolveProxy_basicAuthorization() {
        CrtProxyConfiguration configuration = new TestProxy.Builder().host("1.2.3.4")
                                                                     .port(123)
                                                                     .scheme("https")
                                                                     .password("bar")
                                                                     .username("foo")
                                                                     .build();

        TlsContext tlsContext = Mockito.mock(TlsContext.class);

        Optional<HttpProxyOptions> httpProxyOptions = CrtConfigurationUtils.resolveProxy(configuration, tlsContext);
        assertThat(httpProxyOptions).hasValueSatisfying(proxy -> {
            assertThat(proxy.getTlsContext()).isEqualTo(tlsContext);
            assertThat(proxy.getAuthorizationPassword()).isEqualTo("bar");
            assertThat(proxy.getAuthorizationUsername()).isEqualTo("foo");
            assertThat(proxy.getAuthorizationType()).isEqualTo(HttpProxyOptions.HttpProxyAuthorizationType.Basic);
        });
    }

    @Test
    void resolveProxy_emptyProxy_shouldReturnEmpty() {
        TlsContext tlsContext = Mockito.mock(TlsContext.class);
        assertThat(CrtConfigurationUtils.resolveProxy(null, tlsContext)).isEmpty();
    }

    @Test
    void resolveProxy_noneAuthorization() {
        CrtProxyConfiguration configuration = new TestProxy.Builder().host("1.2.3.4")
                                                                     .port(123)
                                                                     .build();
        TlsContext tlsContext = Mockito.mock(TlsContext.class);

        Optional<HttpProxyOptions> httpProxyOptions = CrtConfigurationUtils.resolveProxy(configuration, tlsContext);
        assertThat(httpProxyOptions).hasValueSatisfying(proxy -> {
            assertThat(proxy.getTlsContext()).isNull();
            assertThat(proxy.getAuthorizationPassword()).isNull();
            assertThat(proxy.getAuthorizationUsername()).isNull();
            assertThat(proxy.getAuthorizationType()).isEqualTo(HttpProxyOptions.HttpProxyAuthorizationType.None);
        });
    }

    @Test
    void resolveHttpMonitoringOptions_shouldMap() {
        CrtConnectionHealthConfiguration configuration = new TestConnectionHealthConfiguration.Builder()
            .minimumThroughputInBps(123L)
            .minimumThroughputTimeout(Duration.ofSeconds(5))
            .build();

        Optional<HttpMonitoringOptions> options = CrtConfigurationUtils.resolveHttpMonitoringOptions(configuration);
        assertThat(options).hasValueSatisfying(proxy -> {
            assertThat(proxy.getAllowableThroughputFailureIntervalSeconds()).isEqualTo(5);
            assertThat(proxy.getMinThroughputBytesPerSecond()).isEqualTo(123L);
        });
    }

    @Test
    void resolveHttpMonitoringOptions_nullConfig_shouldReturnEmpty() {
        assertThat(CrtConfigurationUtils.resolveHttpMonitoringOptions(null)).isEmpty();
    }

    private static final class TestProxy extends CrtProxyConfiguration {
        private TestProxy(DefaultBuilder<?> builder) {
            super(builder);
        }

        private static final class Builder extends CrtProxyConfiguration.DefaultBuilder<Builder> {

            @Override
            public TestProxy build() {
                return new TestProxy(this);
            }
        }
    }

    private static final class TestConnectionHealthConfiguration extends CrtConnectionHealthConfiguration {
        private TestConnectionHealthConfiguration(DefaultBuilder<?> builder) {
            super(builder);
        }

        private static final class Builder extends CrtConnectionHealthConfiguration.DefaultBuilder<Builder> {

            @Override
            public TestConnectionHealthConfiguration build() {
                return new TestConnectionHealthConfiguration(this);
            }
        }
    }

}
