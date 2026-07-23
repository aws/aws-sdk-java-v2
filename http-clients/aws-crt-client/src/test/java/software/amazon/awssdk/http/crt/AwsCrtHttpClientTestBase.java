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

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;


public class AwsCrtHttpClientTestBase {
    static final Duration CRT_DEFAULT = Duration.ofSeconds(10);
    static final Duration CUSTOMER = Duration.ofSeconds(3);
    static final Duration SERVICE_DEFAULT = Duration.ofSeconds(7);

    static Stream<Arguments> invalidTlsNegotiationTimeouts() {
        return Stream.of(
            Arguments.of("null duration -> rejected by paramNotNull", null, "tlsNegotiationTimeout"),
            Arguments.of("zero duration -> rejected by isPositive", Duration.ZERO, "must be positive"),
            Arguments.of("negative duration -> rejected by isPositive", Duration.ofSeconds(-1), "must be positive")
        );
    }


    static Stream<Arguments> resolutionMatrix() {
        return Stream.of(
            Arguments.of("customer unset, no service default -> CRT default (10s) beats GLOBAL (5s)",
                         null, null, CRT_DEFAULT),
            Arguments.of("customer unset, service default 7s -> service default beats CRT default",
                         null, SERVICE_DEFAULT, SERVICE_DEFAULT),
            Arguments.of("customer set 3s, no service default -> customer wins",
                         CUSTOMER, null, CUSTOMER),
            Arguments.of("customer set 3s, service default 7s -> customer beats service default",
                         CUSTOMER, SERVICE_DEFAULT, CUSTOMER)
        );
    }


    static AttributeMap serviceDefaultsMap(Duration tlsNegotiationTimeout) {
        return AttributeMap.builder()
                           .put(SdkHttpConfigurationOption.TLS_NEGOTIATION_TIMEOUT, tlsNegotiationTimeout)
                           .build();
    }
}
