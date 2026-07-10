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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

class AwsCrtAsyncHttpClientTest extends AwsCrtHttpClientTestBase {

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTlsNegotiationTimeouts")
    void tlsNegotiationTimeout_invalidDuration_shouldThrowException(String description, Duration input,
                                                                    String expectedMessageFragment) {
        assertThatThrownBy(() -> AwsCrtAsyncHttpClient.builder().tlsNegotiationTimeout(input).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(expectedMessageFragment);
    }

    @ParameterizedTest(name = "[async] {0}")
    @MethodSource("resolutionMatrix")
    void asyncBuilder_resolvedTlsNegotiationTimeout_matchesPathBPrecedence(String description, Duration customer,
                                                                           Duration serviceDefault, Duration expected) {
        AwsCrtAsyncHttpClient.Builder builder = AwsCrtAsyncHttpClient.builder();
        if (customer != null) {
            builder.tlsNegotiationTimeout(customer);
        }

        try (SdkAsyncHttpClient client = buildAsync(builder, serviceDefault)) {
            assertThat(((AwsCrtAsyncHttpClient) client).resolvedTlsNegotiationTimeout()).isEqualTo(expected);
        }
    }

    @Test
    void asyncBuilder_buildWithDefaults_serviceDefaultsLacksTlsNegotiationTimeout_resolvesToCrtDefault10s() {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().buildWithDefaults(AttributeMap.empty())) {
            assertThat(((AwsCrtAsyncHttpClient) client).resolvedTlsNegotiationTimeout()).isEqualTo(CRT_DEFAULT);
        }
    }

    private static SdkAsyncHttpClient buildAsync(AwsCrtAsyncHttpClient.Builder builder, Duration serviceDefault) {
        return serviceDefault == null
               ? builder.build()
               : builder.buildWithDefaults(serviceDefaultsMap(serviceDefault));
    }

}
