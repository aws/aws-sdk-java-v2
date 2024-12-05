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

package software.amazon.awssdk.regions.internal.util;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URI;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

class ConnectionUtilsComponentTest {

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @RegisterExtension
    static WireMockExtension mockProxyServer = WireMockExtension.newInstance()
                                                                .options(WireMockConfiguration.wireMockConfig().port(0))
                                                                .failOnUnmatchedRequests(false)
                                                                .build();

    @RegisterExtension static WireMockExtension mockServer = WireMockExtension.newInstance()
                                                                              .options(WireMockConfiguration.wireMockConfig().port(0))
                                                                              .failOnUnmatchedRequests(false)
                                                                              .build();

    private final ConnectionUtils sut = ConnectionUtils.create();

    @AfterEach
    void cleanup() {
        System.getProperties().remove("http.proxyHost");
        System.getProperties().remove("http.proxyPort");
        System.clearProperty("aws.ec2MetadataServiceTimeout");
        environmentVariableHelper.reset();
    }

    @Test
    void proxiesAreNotUsedEvenIfPropertyIsSet() throws IOException {
        assumeTrue(Inet4Address.getLocalHost().isReachable(100));
        System.getProperties().put("http.proxyHost", "localhost");
        System.getProperties().put("http.proxyPort", String.valueOf(mockProxyServer.getPort()));
        HttpURLConnection connection = sut.connectToEndpoint(URI.create("http://" + Inet4Address.getLocalHost().getHostAddress() + ":" + mockServer.getPort()), emptyMap());

        assertThat(connection.usingProxy()).isFalse();
    }

    @Test
    void headersArePassedAsPartOfRequest() throws IOException {
        HttpURLConnection connection = sut.connectToEndpoint(URI.create("http://localhost:" + mockServer.getPort()), Collections.singletonMap("HeaderA", "ValueA"));
        connection.getResponseCode();
        mockServer.verify(WireMock.getRequestedFor(WireMock.urlMatching("/")).withHeader("HeaderA", WireMock.equalTo("ValueA")));
    }

    @Test
    void shouldNotFollowRedirects() throws IOException {
        mockServer.stubFor(WireMock.get(WireMock.urlMatching("/")).willReturn(WireMock.aResponse().withStatus(301).withHeader("Location", "http://localhost:" + mockServer.getPort() + "/hello")));
        HttpURLConnection connection = sut.connectToEndpoint(URI.create("http://localhost:" + mockServer.getPort()), Collections.emptyMap());
        assertThat(connection.getResponseCode()).isEqualTo(301);
    }

    @ParameterizedTest
    @CsvSource({
        "environment,5,5000",
        "environment,1.9,1900",
        "system,5,5000",
        "system,1.9,1900"
    })
    void shouldSetTimeoutsBasedOnVariable(String variableType, String variableValue, int expectedTimeoutMillis) throws IOException {
        if (variableType.equals("environment")) {
            environmentVariableHelper.set(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.environmentVariable(), variableValue);
        } else if (variableType.equals("system")) {
            System.setProperty("aws.ec2MetadataServiceTimeout", variableValue);
        }

        ConnectionUtils connectionUtils = ConnectionUtils.create();

        HttpURLConnection connection = connectionUtils.connectToEndpoint(
            URI.create("http://localhost:" + mockServer.getPort()), emptyMap()
        );

        assertThat(connection.getConnectTimeout()).isEqualTo(expectedTimeoutMillis);
        assertThat(connection.getReadTimeout()).isEqualTo(expectedTimeoutMillis);
    }

    @ParameterizedTest
    @CsvSource({
        "environment,invalid",
        "system,invalid"
    })
    void shouldThrowExceptionForInvalidValue(String variableType, String variableValue) {
        if (variableType.equals("environment")) {
            environmentVariableHelper.set(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.environmentVariable(), variableValue);
        } else if (variableType.equals("system")) {
            System.setProperty("aws.ec2MetadataServiceTimeout", variableValue);
        }

        ConnectionUtils connectionUtils = ConnectionUtils.create();

        assertThrows(IllegalStateException.class, () -> {
            connectionUtils.connectToEndpoint(
                URI.create("http://localhost:" + mockServer.getPort()), emptyMap()
            );
        });
    }

    @ParameterizedTest
    @CsvSource({
        "environment",
        "system"
    })
    void shouldUseDefaultTimeoutWhenVariableNotSet(String variableType) throws IOException {
        // No setup for environment or system property

        ConnectionUtils connectionUtils = ConnectionUtils.create();

        HttpURLConnection connection = connectionUtils.connectToEndpoint(
            URI.create("http://localhost:" + mockServer.getPort()), emptyMap()
        );

        int expectedTimeoutMillis = 1000; // Default 1 second
        assertThat(connection.getConnectTimeout()).isEqualTo(expectedTimeoutMillis);
        assertThat(connection.getReadTimeout()).isEqualTo(expectedTimeoutMillis);
    }

}
