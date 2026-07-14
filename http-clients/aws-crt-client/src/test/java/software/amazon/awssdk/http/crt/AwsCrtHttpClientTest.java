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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.utils.AttributeMap;


public class AwsCrtHttpClientTest extends AwsCrtHttpClientTestBase {

    @Test
    public void negativeConnectionAcquisitionTimeout_shouldFail() {
        assertThatThrownBy(() -> {
            SdkHttpClient client = AwsCrtHttpClient.builder()
                    .connectionAcquisitionTimeout(Duration.ofSeconds(-1))
                    .build();
            client.close();
        }).hasMessage("connectionAcquisitionTimeout must be positive");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTlsNegotiationTimeouts")
    void tlsNegotiationTimeout_invalidDuration_shouldThrowException(String description, Duration input,
                                                                    String expectedMessageFragment) {
        assertThatThrownBy(() -> AwsCrtHttpClient.builder().tlsNegotiationTimeout(input).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(expectedMessageFragment);
    }

    @Test
    void syncBuilder_buildWithDefaults_serviceDefaultsLacksTlsNegotiationTimeout_resolvesToCrtDefault10s() {
        try (SdkHttpClient client = AwsCrtHttpClient.builder().buildWithDefaults(AttributeMap.empty())) {
            assertThat(((AwsCrtHttpClient) client).resolvedTlsNegotiationTimeout()).isEqualTo(CRT_DEFAULT);
        }
    }


    @ParameterizedTest(name = "[sync] {0}")
    @MethodSource("resolutionMatrix")
    void syncBuilder_resolvedTlsNegotiationTimeout_matchesPathBPrecedence(String description, Duration customer,
                                                                          Duration serviceDefault, Duration expected) {
        AwsCrtHttpClient.Builder builder = AwsCrtHttpClient.builder();
        if (customer != null) {
            builder.tlsNegotiationTimeout(customer);
        }

        try (SdkHttpClient client = buildSync(builder, serviceDefault)) {
            assertThat(((AwsCrtHttpClient) client).resolvedTlsNegotiationTimeout()).isEqualTo(expected);
        }
    }

    private static SdkHttpClient buildSync(AwsCrtHttpClient.Builder builder, Duration serviceDefault) {
        return serviceDefault == null
               ? builder.build()
               : builder.buildWithDefaults(serviceDefaultsMap(serviceDefault));
    }
}
