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

package software.amazon.awssdk.awscore.presigner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PresignExpirationUtilsTest {

    private static final Instant SIGNING_INSTANT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Duration REQUESTED = Duration.ofHours(2);

    @ParameterizedTest(name = "{0}")
    @MethodSource("credentialExpirations")
    void effectiveExpirationDuration_givenCredentialExpiration_returnsExpectedDuration(String scenario,
                                                                                    Instant credentialExpiration,
                                                                                    Duration expected) {
        assertThat(PresignExpirationUtils.effectiveExpirationDuration(REQUESTED, SIGNING_INSTANT, credentialExpiration))
            .isEqualTo(expected);
    }

    private static Stream<Arguments> credentialExpirations() {
        return Stream.of(
            Arguments.of("expires before requested end, capped", SIGNING_INSTANT.plus(Duration.ofHours(1)), Duration.ofHours(1)),
            Arguments.of("fractional remaining, truncated to whole seconds",
                         SIGNING_INSTANT.plus(Duration.ofMinutes(90)).plusMillis(700), Duration.ofMinutes(90)),
            Arguments.of("expires at requested end, not capped", SIGNING_INSTANT.plus(REQUESTED), REQUESTED),
            Arguments.of("expires after requested end, not capped", SIGNING_INSTANT.plus(Duration.ofHours(5)), REQUESTED),
            Arguments.of("no expiration, not capped", null, REQUESTED),
            Arguments.of("Instant.MAX expiration, not capped", Instant.MAX, REQUESTED),
            Arguments.of("under one second remaining, not capped", SIGNING_INSTANT.plusMillis(500), REQUESTED),
            Arguments.of("expires at signing instant, not capped", SIGNING_INSTANT, REQUESTED),
            Arguments.of("already expired, not capped", SIGNING_INSTANT.minus(Duration.ofMinutes(30)), REQUESTED));
    }
}
