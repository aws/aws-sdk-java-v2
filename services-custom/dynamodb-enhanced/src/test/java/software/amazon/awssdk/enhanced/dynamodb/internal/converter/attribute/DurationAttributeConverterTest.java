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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute;

import static java.time.temporal.ChronoUnit.MICROS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class DurationAttributeConverterTest {

    private DurationAttributeConverter converter;

    @BeforeEach
    void init() {
        this.converter = DurationAttributeConverter.create();
    }

    @ParameterizedTest
    @MethodSource("durations")
    void testConvertTo(String value, Duration expected) {
        Duration converted = converter.transformTo(AttributeValue.builder()
                                                                 .n(value)
                                                                 .build());
        assertThat(converted).isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @MethodSource("durations")
    void testConvertFrom(String expected, Duration value) {
        AttributeValue converted = converter.transformFrom(value);
        String actual = converted.n();
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("noPadding")
    void testConvertTo_NoPadding(String value, Duration expected) {
        Duration converted = converter.transformTo(AttributeValue.builder()
                                                                 .n(value)
                                                                 .build());
        assertThat(converted).isEqualByComparingTo(expected);
    }

    static Stream<Arguments> noPadding() {
        return Stream.of(
            Arguments.of("0.123456789", Duration.ofNanos(123_456_789)),
            Arguments.of("0.12345678",  Duration.ofNanos(123_456_780)),
            Arguments.of("0.1234567",   Duration.ofNanos(123_456_700)),
            Arguments.of("0.123456",    Duration.of(123_456, MICROS)),
            Arguments.of("0.12345",     Duration.of(123_450, MICROS)),
            Arguments.of("0.1234",      Duration.of(123_400, MICROS)),
            Arguments.of("0.123",       Duration.ofMillis(123)),
            Arguments.of("0.12",        Duration.ofMillis(120)),
            Arguments.of("0.1",         Duration.ofMillis(100)),
            Arguments.of("0.001", Duration.ofMillis(1)),
            Arguments.of("0.000001", Duration.of(1, MICROS)),
            Arguments.of("0.001", Duration.ofMillis(1))
        );
    }

    static Stream<Arguments> durations() {
        return Stream.of(
            Arguments.of("0", Duration.ofSeconds(0)),

            Arguments.of("0.123456789", Duration.ofNanos(123_456_789)),
            Arguments.of("0.123456780", Duration.ofNanos(123_456_780)),
            Arguments.of("0.123456700", Duration.ofNanos(123_456_700)),
            Arguments.of("0.123456000", Duration.of(123_456, MICROS)),
            Arguments.of("0.123450000", Duration.of(123_450, MICROS)),
            Arguments.of("0.123400000", Duration.of(123_400, MICROS)),
            Arguments.of("0.123000000", Duration.ofMillis(123)),
            Arguments.of("0.120000000", Duration.ofMillis(120)),
            Arguments.of("0.100000000", Duration.ofMillis(100)),

            Arguments.of("0.123456789", Duration.ofNanos(123_456_789)),
            Arguments.of("0.012345678", Duration.ofNanos(12_345_678)),
            Arguments.of("0.001234567", Duration.ofNanos(1_234_567)),
            Arguments.of("0.000123456", Duration.ofNanos(123_456)),
            Arguments.of("0.000012345", Duration.ofNanos(12_345)),
            Arguments.of("0.000001234", Duration.ofNanos(1_234)),
            Arguments.of("0.000000123", Duration.ofNanos(123)),
            Arguments.of("0.000000012", Duration.ofNanos(12)),
            Arguments.of("0.000000001", Duration.ofNanos(1)),

            Arguments.of("12345678", Duration.ofSeconds(12345678)),
            Arguments.of("86400", Duration.ofDays(1)),
            Arguments.of("9", Duration.ofSeconds(9)),
            Arguments.of("-9", Duration.ofSeconds(-9)),
            Arguments.of("0.001000000", Duration.ofMillis(1)),
            Arguments.of("0.000000001", Duration.ofNanos(1)));
    }

}