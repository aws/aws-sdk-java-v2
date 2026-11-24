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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
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

    @Test
    void sanitize_throwableGraph_escapesDescriptionsAndStackFrames() {
        IllegalStateException cause = new IllegalStateException("cause\rvalue");
        cause.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("cause\nclass", "cause\tmethod", "cause\u2028file", 12)
        });
        IOException suppressed = new IOException("suppressed\u0085value");
        RuntimeException failure = new RuntimeException("failure\nvalue", cause);
        failure.addSuppressed(suppressed);
        StackTraceElement safeFrame = new StackTraceElement("SafeClass", "safeMethod", "SafeFile.java", 34);
        failure.setStackTrace(new StackTraceElement[] {safeFrame});

        Throwable sanitized = LogSanitizer.sanitize(failure);

        assertThat(sanitized).isNotSameAs(failure);
        assertThat(sanitized.getMessage()).isEqualTo("java.lang.RuntimeException: failure\\nvalue");
        assertThat(sanitized.toString()).isEqualTo("java.lang.RuntimeException: failure\\nvalue");
        assertThat(sanitized.getStackTrace()).containsExactly(safeFrame);
        assertThat(sanitized.getCause().toString()).isEqualTo("java.lang.IllegalStateException: cause\\rvalue");
        assertThat(sanitized.getCause().getStackTrace()).containsExactly(
            new StackTraceElement("cause\\nclass", "cause\\tmethod", "cause\\u2028file", 12));
        assertThat(sanitized.getSuppressed()).hasSize(1);
        assertThat(sanitized.getSuppressed()[0].toString())
            .isEqualTo("java.io.IOException: suppressed\\u0085value");
        assertThat(failure.getMessage()).isEqualTo("failure\nvalue");
        assertThat(cause.getStackTrace()[0].getClassName()).isEqualTo("cause\nclass");

        StringWriter rendered = new StringWriter();
        sanitized.printStackTrace(new PrintWriter(rendered));
        assertThat(rendered.toString()).contains("failure\\nvalue", "cause\\rvalue", "suppressed\\u0085value",
                                                 "cause\\nclass.cause\\tmethod(cause\\u2028file:12)")
                                       .doesNotContain("failure\nvalue", "cause\rvalue", "suppressed\u0085value");
    }

    @Test
    void sanitize_sharedAndCyclicThrowableGraph_preservesStructure() {
        RuntimeException first = new RuntimeException("first\nvalue");
        RuntimeException second = new RuntimeException("second\rvalue");
        first.initCause(second);
        second.initCause(first);
        first.addSuppressed(second);

        Throwable sanitized = LogSanitizer.sanitize(first);

        assertThat(sanitized.getCause().getCause()).isSameAs(sanitized);
        assertThat(sanitized.getSuppressed()).containsExactly(sanitized.getCause());
    }

    @Test
    void sanitize_nothingToEscape_returnsSameInstance() {
        IOException failure = new IOException("failure", new IllegalStateException("cause"));
        failure.addSuppressed(new RuntimeException("suppressed"));
        failure.getCause().initCause(failure);

        assertThat(LogSanitizer.sanitize(failure)).isSameAs(failure);
    }

    @Test
    void sanitize_onlyCauseNeedsEscaping_returnsSanitizedGraph() {
        IOException failure = new IOException("failure", new IllegalStateException("cause\nvalue"));

        Throwable sanitized = LogSanitizer.sanitize(failure);

        assertThat(sanitized).isNotSameAs(failure);
        assertThat(sanitized.toString()).isEqualTo("java.io.IOException: failure");
        assertThat(sanitized.getCause().toString()).isEqualTo("java.lang.IllegalStateException: cause\\nvalue");
    }

    @Test
    void sanitize_onlyStackFrameNeedsEscaping_returnsSanitizedGraph() {
        IOException failure = new IOException("failure");
        failure.setStackTrace(new StackTraceElement[] {new StackTraceElement("forged\nclass", "method", null, 1)});

        Throwable sanitized = LogSanitizer.sanitize(failure);

        assertThat(sanitized).isNotSameAs(failure);
        assertThat(sanitized.getStackTrace()[0].getClassName()).isEqualTo("forged\\nclass");
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    void sanitize_onlyModuleFieldOfStackFrameNeedsEscaping_returnsSanitizedGraph() throws Exception {
        Constructor<StackTraceElement> java9Constructor = StackTraceElement.class.getConstructor(
            String.class, String.class, String.class, String.class, String.class, String.class, int.class);
        StackTraceElement frame = java9Constructor.newInstance("loader\nname", "module", "1", "Example", "method",
                                                               "Example.java", 1);
        IOException failure = new IOException("failure");
        failure.setStackTrace(new StackTraceElement[] {frame});

        Throwable sanitized = LogSanitizer.sanitize(failure);

        assertThat(sanitized).isNotSameAs(failure);
        assertThat(sanitized.getStackTrace()[0].toString()).doesNotContain("\n");
    }

    @Test
    void sanitize_overriddenToStringHidesMessage_returnsSanitizedGraph() {
        RuntimeException failure = new RuntimeException("failure\nvalue") {
            @Override
            public String toString() {
                return "custom";
            }
        };

        Throwable sanitized = LogSanitizer.sanitize(failure);

        assertThat(sanitized).isNotSameAs(failure);
        assertThat(sanitized.getMessage()).isEqualTo(failure.getClass().getName() + ": custom");
    }

    @Test
    void sanitize_accessorThrows_returnsStandInWithClassName() {
        IllegalStateException failure = new IllegalStateException("failure") {
            @Override
            public String getMessage() {
                throw new UnsupportedOperationException("broken");
            }
        };

        Throwable sanitized = LogSanitizer.sanitize(failure);

        assertThat(sanitized).isNotSameAs(failure);
        assertThat(sanitized.getMessage())
            .isEqualTo(failure.getClass().getName() + ": <exception details unavailable>");
        assertThat(sanitized.getCause()).isNull();
    }

    @Test
    void sanitize_null_returnsNull() {
        assertThat(LogSanitizer.sanitize(null)).isNull();
    }
}
