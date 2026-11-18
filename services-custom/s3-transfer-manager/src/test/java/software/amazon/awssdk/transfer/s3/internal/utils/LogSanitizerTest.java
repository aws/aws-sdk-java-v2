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

package software.amazon.awssdk.transfer.s3.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LogSanitizerTest {

    static Stream<Arguments> escapedCases() {
        return Stream.of(
            Arguments.of("a\r\nb", "a\\r\\nb"),
            Arguments.of("\n", "\\n"),
            Arguments.of("a\tb", "a\\tb"),
            Arguments.of("\u001b[2J", "\\u001b[2J"),
            Arguments.of("x\u0085y", "x\\u0085y"),
            Arguments.of("x\u2028y", "x\\u2028y"),
            Arguments.of("x\u2029y", "x\\u2029y"),
            Arguments.of("x\u0000y", "x\\u0000y"),
            Arguments.of("x\u007fy", "x\\u007fy"),
            Arguments.of("x\u009fy", "x\\u009fy"),
            Arguments.of("C:\\dir\\caf\u00e9\r.txt", "C:\\dir\\caf\u00e9\\r.txt"));
    }

    @ParameterizedTest
    @MethodSource("escapedCases")
    void escapeControlCharacters_controlCharacters_areEscaped(String input, String expected) {
        String escaped = LogSanitizer.escapeControlCharacters(input);

        assertThat(escaped).isEqualTo(expected);
        assertThat(escaped.chars()).noneMatch(c -> Character.isISOControl(c) || c == '\u2028' || c == '\u2029');
    }

    @ParameterizedTest
    @MethodSource("unchangedCases")
    void escapeControlCharacters_nothingToEscape_returnsSameInstance(String input) {
        assertThat(LogSanitizer.escapeControlCharacters(input)).isSameAs(input);
    }

    static Stream<String> unchangedCases() {
        return Stream.of("", "plain key/path.txt", "C:\\dir\\file.txt", "caf\u00e9 \ud83d\ude00");
    }

    @Test
    void escapeControlCharacters_null_returnsNullString() {
        assertThat(LogSanitizer.escapeControlCharacters(null)).isEqualTo("null");
    }
}
