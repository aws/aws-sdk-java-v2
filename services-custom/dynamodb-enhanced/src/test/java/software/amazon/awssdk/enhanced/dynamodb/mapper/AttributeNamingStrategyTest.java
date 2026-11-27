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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks the Appendix A names for both public strategy types.
 * A custom strategy receives the source name once and does not use the built-in split.
 */
class AttributeNamingStrategyTest {

    /**
     * Returns the Appendix A rows.
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> appendixRows() {
        return AttributeNamingCases.appendixRows();
    }

    /**
     * Returns source names that contain no word.
     */
    static Stream<String> rejectedSources() {
        return AttributeNamingCases.rejectedSources();
    }

    @ParameterizedTest
    @MethodSource("appendixRows")
    void transform_whenAppendixSource_matchesBothPublicTypes(String source,
                                                             String lowerSnake,
                                                             String upperSnake,
                                                             String lowerCamel,
                                                             String upperCamel) {
        assertThat(AttributeNamingStrategies.LOWER_SNAKE_CASE.transform(source)).isEqualTo(lowerSnake);
        assertThat(BuiltInAttributeNamingStrategy.LOWER_SNAKE_CASE.transform(source)).isEqualTo(lowerSnake);
        assertThat(AttributeNamingStrategies.UPPER_SNAKE_CASE.transform(source)).isEqualTo(upperSnake);
        assertThat(BuiltInAttributeNamingStrategy.UPPER_SNAKE_CASE.transform(source)).isEqualTo(upperSnake);
        assertThat(AttributeNamingStrategies.LOWER_CAMEL_CASE.transform(source)).isEqualTo(lowerCamel);
        assertThat(BuiltInAttributeNamingStrategy.LOWER_CAMEL_CASE.transform(source)).isEqualTo(lowerCamel);
        assertThat(AttributeNamingStrategies.UPPER_CAMEL_CASE.transform(source)).isEqualTo(upperCamel);
        assertThat(BuiltInAttributeNamingStrategy.UPPER_CAMEL_CASE.transform(source)).isEqualTo(upperCamel);
    }

    @Test
    void transform_whenPublicTypes_areDistinctInstances() {
        assertThat(AttributeNamingStrategies.LOWER_SNAKE_CASE)
            .isNotSameAs(BuiltInAttributeNamingStrategy.LOWER_SNAKE_CASE);
        assertThat(AttributeNamingStrategies.UPPER_SNAKE_CASE)
            .isNotSameAs(BuiltInAttributeNamingStrategy.UPPER_SNAKE_CASE);
        assertThat(AttributeNamingStrategies.LOWER_CAMEL_CASE)
            .isNotSameAs(BuiltInAttributeNamingStrategy.LOWER_CAMEL_CASE);
        assertThat(AttributeNamingStrategies.UPPER_CAMEL_CASE)
            .isNotSameAs(BuiltInAttributeNamingStrategy.UPPER_CAMEL_CASE);
    }

    @ParameterizedTest
    @MethodSource("rejectedSources")
    void transform_whenSourceHasNoWord_throwsIllegalArgumentException(String source) {
        for (AttributeNamingStrategy strategy : AttributeNamingCases.bothPublicTypes()) {
            assertThatThrownBy(() -> strategy.transform(source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(messageFragment(source));
        }
    }

    @Test
    void transform_whenSourceIsNull_failsForEveryBuiltInStrategy() {
        for (AttributeNamingStrategy strategy : AttributeNamingCases.bothPublicTypes()) {
            assertThatThrownBy(() -> strategy.transform(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void transform_whenCustomLowerCase_returnsSourceOnceAndSkipsBuiltInSplit() {
        AtomicInteger calls = new AtomicInteger();
        AttributeNamingStrategy strategy = source -> {
            calls.incrementAndGet();
            return source.toLowerCase(Locale.ROOT);
        };

        assertThat(strategy.transform("HTTPRequest")).isEqualTo("httprequest");
        assertThat(calls).hasValue(1);
        assertThat(strategy.transform("HTTPRequest")).isNotEqualTo(
            AttributeNamingStrategies.LOWER_SNAKE_CASE.transform("HTTPRequest"));
    }

    @Test
    void transform_whenUpperCamelAppliedTwice_changesAb() {
        assertThat(AttributeNamingStrategies.UPPER_CAMEL_CASE.transform("A_B")).isEqualTo("AB");
        assertThat(AttributeNamingStrategies.UPPER_CAMEL_CASE.transform("AB")).isEqualTo("Ab");
        assertThat(BuiltInAttributeNamingStrategy.UPPER_CAMEL_CASE.transform("A_B")).isEqualTo("AB");
    }

    /**
     * Returns the source text that the failure message must contain.
     * An empty source is quoted in the message, so the assertion looks for that quote.
     */
    private static String messageFragment(String source) {
        if (source.isEmpty()) {
            return "source name ''";
        }
        return source;
    }
}
