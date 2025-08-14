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

package software.amazon.awssdk.http.apache5;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.time.Duration;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientLocalAddressFunctionalTestSuite;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Functional tests for Apache5 HTTP Client's local address binding capabilities.
 * Tests three scenarios:
 * 1. Local address configured via builder
 * 2. Local address configured via custom route planner
 * 3. Both methods used together (route planner takes precedence)
 */
@DisplayName("Apache5 HTTP Client - Local Address Functional Tests")
class Apache5HttpClientLocalAddressFunctionalTest {

    @Nested
    @DisplayName("When local address is configured via builder")
    class LocalAddressViaBuilderTest extends SdkHttpClientLocalAddressFunctionalTestSuite {
        @Override
        protected SdkHttpClient createHttpClient(InetAddress localAddress, Duration connectionTimeout) {
            return Apache5HttpClient.builder()
                                    .localAddress(localAddress)
                                    .connectionTimeout(connectionTimeout)
                                    .build();
        }
    }

    @Nested
    @DisplayName("When local address is configured via custom route planner")
    class LocalAddressViaRoutePlannerTest extends SdkHttpClientLocalAddressFunctionalTestSuite {

        @Override
        protected SdkHttpClient createHttpClient(InetAddress localAddress, Duration connectionTimeout) {
            HttpRoutePlanner routePlanner = createLocalAddressRoutePlanner(localAddress);
            return Apache5HttpClient.builder()
                                    .httpRoutePlanner(routePlanner)
                                    .connectionTimeout(connectionTimeout)
                                    .build();
        }

        private HttpRoutePlanner createLocalAddressRoutePlanner(InetAddress localAddress) {
            return new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {
                @Override
                protected InetAddress determineLocalAddress(HttpHost firstHop, HttpContext context) throws HttpException {
                    return localAddress != null ? localAddress : super.determineLocalAddress(firstHop, context);
                }
            };
        }
    }

    @Nested
    @DisplayName("When both route planner and builder local address are configured (route planner takes precedence)")
    class RoutePlannerPrecedenceTest extends SdkHttpClientLocalAddressFunctionalTestSuite {

        private final InetAddress BUILDER_LOCAL_ADDRESS = InetAddress.getLoopbackAddress();

        @Override
        protected SdkHttpClient createHttpClient(InetAddress localAddress, Duration connectionTimeout) {
            // The localAddress parameter will be used by the route planner
            // The builder's localAddress will be overridden
            HttpRoutePlanner routePlanner = createLocalAddressRoutePlanner(localAddress);
            SdkHttpClient httpClient;

            try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
                 httpClient = Apache5HttpClient.builder()
                                                            .httpRoutePlanner(routePlanner)
                                                            .localAddress(BUILDER_LOCAL_ADDRESS) // This will be overridden by route planner
                                                            .connectionTimeout(connectionTimeout)
                                                            .build();

                assertThat(logCaptor.loggedEvents()).anySatisfy(logEvent -> {
                    assertThat(logEvent.getLevel()).isEqualTo(Level.DEBUG);
                    assertThat(logEvent.getMessage().getFormattedMessage())
                        .contains("localAddress configuration was ignored since Route planner was explicitly provided");
                });
            }
            return httpClient;
        }

        private HttpRoutePlanner createLocalAddressRoutePlanner(InetAddress routePlannerAddress) {
            return new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {
                @Override
                protected InetAddress determineLocalAddress(HttpHost firstHop, HttpContext context) throws HttpException {
                    // Route planner's address takes precedence over builder's address
                    return routePlannerAddress != null ? routePlannerAddress : super.determineLocalAddress(firstHop, context);
                }
            };
        }
    }
}
