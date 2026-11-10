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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import software.amazon.awssdk.crt.http.HttpMonitoringOptions;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.io.TlsContext;

class CrtConnectionUtilsTest {

    @BeforeEach
    @AfterEach
    void clearProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
    }

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
    void resolveProxy_nonProxyHosts_setAsCommaSeperatedString() {
        TlsContext tlsContext = Mockito.mock(TlsContext.class);
        CrtProxyConfiguration configuration = new TestProxy.Builder().host("1.2.3.4")
                                                                     .port(123)
                                                                     .scheme("https")
                                                                     .password("bar")
                                                                     .username("foo")
                                                                     .addNonProxyHost("host1")
                                                                     .addNonProxyHost("host2")
                                                                     .build();
        assertThat(CrtConfigurationUtils.resolveProxy(configuration, tlsContext).get().getNoProxyHosts())
            .isEqualTo("host1,host2");
    }

    @ParameterizedTest(name = "builder nonProxyHost \"{0}\" -> curl \"{1}\"")
    @CsvSource({
        "*.internal.example.com, .internal.example.com",
        "'*',                    '*'",
        "example.com,            example.com",
        "10.0.0.0/8,             10.0.0.0/8",
        "192.168.*,              192.168.*",
        "internal*,              internal*",
        "*foo.com,               *foo.com",
        "::1,                    ::1",
        "2001:db8::/32,          2001:db8::/32",
        "::1/128,                ::1/128"
    })
    void resolveProxy_builderNonProxyHost_translatedToCurlForm(String nonProxyHost, String expectedCurl) {
        TlsContext tlsContext = Mockito.mock(TlsContext.class);
        CrtProxyConfiguration configuration = new TestProxy.Builder().host("1.2.3.4")
                                                                     .port(123)
                                                                     .addNonProxyHost(nonProxyHost)
                                                                     .build();

        assertThat(CrtConfigurationUtils.resolveProxy(configuration, tlsContext).get().getNoProxyHosts())
            .isEqualTo(expectedCurl);
    }

    @ParameterizedTest(name = "http.nonProxyHosts \"{0}\" -> curl \"{1}\"")
    @CsvSource({
        "*.internal.example.com, .internal.example.com",
        "'*',                    '*'"
    })
    void resolveProxy_systemPropertyNonProxyHost_translatedToCurlForm(String nonProxyHost, String expectedCurl) {
        System.setProperty("http.proxyHost", "proxy.example.com");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("http.nonProxyHosts", nonProxyHost);

        TlsContext tlsContext = Mockito.mock(TlsContext.class);
        CrtProxyConfiguration configuration = new TestProxy.Builder().build();

        assertThat(CrtConfigurationUtils.resolveProxy(configuration, tlsContext).get().getNoProxyHosts())
            .isEqualTo(expectedCurl);
    }

    @Test
    void resolveProxy_builderMixedGlobExactCidr_eachTokenTranslatedIndependently() {
        TlsContext tlsContext = Mockito.mock(TlsContext.class);
        CrtProxyConfiguration configuration = new TestProxy.Builder()
            .host("1.2.3.4")
            .port(123)
            .nonProxyHosts(Stream.of("*.internal.example.com", "localhost", "10.0.0.0/8").collect(Collectors.toSet()))
            .build();

        String noProxyHosts = CrtConfigurationUtils.resolveProxy(configuration, tlsContext).get().getNoProxyHosts();
        assertThat(noProxyHosts.split(",")).containsExactlyInAnyOrder(".internal.example.com", "localhost", "10.0.0.0/8");
    }

    @Test
    void resolveProxy_builderAndSystemPropertySuffixWildcard_produceSameCurlForm() {
        TlsContext tlsContext = Mockito.mock(TlsContext.class);
        CrtProxyConfiguration builderConfig = new TestProxy.Builder().host("1.2.3.4")
                                                                     .port(123)
                                                                     .addNonProxyHost("*.internal.example.com")
                                                                     .build();
        String builderNoProxyHosts = CrtConfigurationUtils.resolveProxy(builderConfig, tlsContext).get().getNoProxyHosts();

        System.setProperty("http.proxyHost", "proxy.example.com");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("http.nonProxyHosts", "*.internal.example.com");
        CrtProxyConfiguration systemPropertyConfig = new TestProxy.Builder().build();
        String systemPropertyNoProxyHosts =
            CrtConfigurationUtils.resolveProxy(systemPropertyConfig, tlsContext).get().getNoProxyHosts();

        assertThat(builderNoProxyHosts).isEqualTo(systemPropertyNoProxyHosts).isEqualTo(".internal.example.com");
    }

    @Test
    void resolveProxy_withNullHostAndLiteralNullToken_shouldNotReturnEmpty( ) {
        TlsContext tlsContext = Mockito.mock(TlsContext.class);
        CrtProxyConfiguration configuration = new TestProxy.Builder().host(null)
                                                                     .port(123)
                                                                     .scheme("https")
                                                                     .password("bar")
                                                                     .username("foo")
                                                                     .nonProxyHosts(Stream.of("someRandom", "null")
                                                                                          .collect(Collectors.toSet()))
                                                                     .build();
        assertThat(CrtConfigurationUtils.resolveProxy(configuration, tlsContext)).isNotEmpty();
    }

    @Test
    void resolveProxy_basicAuthorization_WithNonMatchingNoProxy() {
        CrtProxyConfiguration configuration = new TestProxy.Builder().host("1.2.3.4")
                                                                     .port(123)
                                                                     .scheme("https")
                                                                     .password("bar")
                                                                     .addNonProxyHost("someRandom")
                                                                     .addNonProxyHost(null)
                                                                     .username("foo")
                                                                     .build();

        TlsContext tlsContext = Mockito.mock(TlsContext.class);

        Optional<HttpProxyOptions> httpProxyOptions = CrtConfigurationUtils.resolveProxy(configuration, tlsContext);
        assertThat(httpProxyOptions).hasValueSatisfying(proxy -> {
            assertThat(proxy.getNoProxyHosts()).isEqualTo("someRandom");
            assertThat(proxy.getTlsContext()).isEqualTo(tlsContext);
            assertThat(proxy.getAuthorizationPassword()).isEqualTo("bar");
            assertThat(proxy.getAuthorizationUsername()).isEqualTo("foo");
            assertThat(proxy.getAuthorizationType()).isEqualTo(HttpProxyOptions.HttpProxyAuthorizationType.Basic);
        });
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
