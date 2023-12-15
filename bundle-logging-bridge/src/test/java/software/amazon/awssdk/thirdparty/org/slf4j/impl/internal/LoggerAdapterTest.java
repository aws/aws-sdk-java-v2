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

package software.amazon.awssdk.thirdparty.org.slf4j.impl.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.Marker;

public class LoggerAdapterTest {
    private Logger mockLogger;
    private Marker mockMarker;

    @BeforeEach
    public void setup() {
        mockLogger = mock(Logger.class);
        mockMarker = mock(Marker.class);
    }

    @Test
    public void getName_delegatesCall() {
        when(mockLogger.getName()).thenReturn("MyLogger");
        LoggerAdapter adapter = new LoggerAdapter(mockLogger);
        assertThat(adapter.getName()).isEqualTo("MyLogger");
        verify(mockLogger).getName();
    }

    @Test
    public void isTraceEnabled_delegatesCall() {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        loggerAdapter.isTraceEnabled();
        verify(mockLogger).isTraceEnabled();

        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        loggerAdapter.isTraceEnabled(markerAdapter);
        verify(mockLogger).isTraceEnabled(markerAdapter.getUnshaded());
    }

    @Test
    public void isDebugEnabled_delegatesCall() {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        loggerAdapter.isDebugEnabled();
        verify(mockLogger).isDebugEnabled();

        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        loggerAdapter.isDebugEnabled(markerAdapter);
        verify(mockLogger).isDebugEnabled(markerAdapter.getUnshaded());
    }


    @Test
    public void isInfoEnabled_delegatesCall() {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        loggerAdapter.isInfoEnabled();
        verify(mockLogger).isInfoEnabled();

        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        loggerAdapter.isInfoEnabled(markerAdapter);
        verify(mockLogger).isInfoEnabled(markerAdapter.getUnshaded());
    }

    @Test
    public void isWarnEnabled_delegatesCall() {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        loggerAdapter.isWarnEnabled();
        verify(mockLogger).isWarnEnabled();

        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        loggerAdapter.isWarnEnabled(markerAdapter);
        verify(mockLogger).isWarnEnabled(markerAdapter.getUnshaded());
    }

    @Test
    public void isErrorEnabled_delegatesCall() {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        loggerAdapter.isErrorEnabled();
        verify(mockLogger).isErrorEnabled();

        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        loggerAdapter.isErrorEnabled(markerAdapter);
        verify(mockLogger).isErrorEnabled(markerAdapter.getUnshaded());
    }

    @ParameterizedTest
    @MethodSource("log_str_tests")
    public void log_str_delegatesCall(TestCaseLogSFn tc) {
        LoggerAdapter adapter = new LoggerAdapter(mockLogger);
        String msg = "log me";
        tc.logFn.apply(adapter).invoke(msg);
        tc.verify.apply(mockLogger).invoke(msg);
    }

    @ParameterizedTest
    @MethodSource("log_marker_str_tests")
    public void log_marker_str_delegatesCall(TestCaseLogMSFn tc) {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        String msg = "log me";
        tc.logFn.apply(loggerAdapter).invoke(markerAdapter, msg);
        tc.verify.apply(mockLogger).invoke(markerAdapter.getUnshaded(), msg);
    }

    @ParameterizedTest
    @MethodSource("log_str_object_tests")
    public void log_str_object_delegatesCall(TestCaseLogSOFn tc) {
        LoggerAdapter adapter = new LoggerAdapter(mockLogger);
        String msg = "log me";
        Integer i = 42;
        tc.logFn.apply(adapter).invoke(msg, i);
        tc.verify.apply(mockLogger).invoke(msg, i);
    }

    @ParameterizedTest
    @MethodSource("log_str_throwable_tests")
    public void log_str_throwable_delegatesCall(TestCaseLogSTFn tc) {
        LoggerAdapter adapter = new LoggerAdapter(mockLogger);
        String msg = "log me";
        Throwable t = new RuntimeException("some error");
        tc.logFn.apply(adapter).invoke(msg, t);
        tc.verify.apply(mockLogger).invoke(msg, t);
    }

    @ParameterizedTest
    @MethodSource("log_marker_str_throwable_tests")
    public void log_marker_str_throwable_delegatesCall(TestCaseLogMSTFn tc) {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        String msg = "log me";
        Throwable t = new RuntimeException("some error");
        tc.logFn.apply(loggerAdapter).invoke(markerAdapter, msg, t);
        tc.verify.apply(mockLogger).invoke(markerAdapter.getUnshaded(), msg, t);
    }

    @ParameterizedTest
    @MethodSource("log_marker_str_object_tests")
    public void log_marker_str_object_delegatesCall(TestCaseLogMSOFn tc) {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        String msg = "log me";
        Integer i = 42;
        tc.logFn.apply(loggerAdapter).invoke(markerAdapter, msg, i);
        tc.verify.apply(mockLogger).invoke(markerAdapter.getUnshaded(), msg, i);
    }

    @ParameterizedTest
    @MethodSource("log_str_object1_object2_tests")
    public void log_str_object1_object2_delegatesCall(TestCaseLogSOOFn tc) {
        LoggerAdapter adapter = new LoggerAdapter(mockLogger);
        String msg = "log me";
        Integer i = 42;
        String s = "me as well";
        tc.logFn.apply(adapter).invoke(msg, i, s);
        tc.verify.apply(mockLogger).invoke(msg, i, s);
    }

    @ParameterizedTest
    @MethodSource("log_marker_str_object1_object2_tests")
    public void log_marker_str_object1_object2_delegatesCall(TestCaseLogMSOOFn tc) {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        String msg = "log me";
        Integer i = 42;
        String s = "me as well";
        tc.logFn.apply(loggerAdapter).invoke(markerAdapter, msg, i, s);
        tc.verify.apply(mockLogger).invoke(markerAdapter.getUnshaded(), msg, i, s);
    }

    @ParameterizedTest
    @MethodSource("log_str_vararg_tests")
    public void log_str_vararg_delegatesCall(TestCaseLogSOVarFn tc) {
        LoggerAdapter adapter = new LoggerAdapter(mockLogger);
        String msg = "log me";
        Integer i = 42;
        String s = "me as well";
        Double d = 1.0;
        tc.logFn.apply(adapter).invoke(msg, i, s, d);
        tc.verify.apply(mockLogger).invoke(msg, i, s, d);
    }

    @ParameterizedTest
    @MethodSource("log_marker_str_vararg_tests")
    public void log_marker_str_vararg_delegatesCall(TestCaseLogMSOVarFn tc) {
        LoggerAdapter loggerAdapter = new LoggerAdapter(mockLogger);
        ShadedMarkerAdapter markerAdapter = new ShadedMarkerAdapter(mockMarker);
        String msg = "log me";
        Integer i = 42;
        String s = "me as well";
        Double d = 1.0;
        tc.logFn.apply(loggerAdapter).invoke(markerAdapter, msg, i, s, d);
        tc.verify.apply(mockLogger).invoke(markerAdapter.getUnshaded(), msg, i, s, d);
    }

    public static Stream<TestCaseLogSFn> log_str_tests() {
        return Stream.of(
            new TestCaseLogSFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogSFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogSFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogSFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogSFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogSTFn> log_str_throwable_tests() {
        return Stream.of(
            new TestCaseLogSTFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogSTFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogSTFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogSTFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogSTFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogMSFn> log_marker_str_tests() {
        return Stream.of(
            new TestCaseLogMSFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogMSFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogMSFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogMSFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogMSFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogMSTFn> log_marker_str_throwable_tests() {
        return Stream.of(
            new TestCaseLogMSTFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogMSTFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogMSTFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogMSTFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogMSTFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogSOFn> log_str_object_tests() {
        return Stream.of(
            new TestCaseLogSOFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogSOFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogSOFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogSOFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogSOFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogMSOFn> log_marker_str_object_tests() {
        return Stream.of(
            new TestCaseLogMSOFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogMSOFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogMSOFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogMSOFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogMSOFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogSOOFn> log_str_object1_object2_tests() {
        return Stream.of(
            new TestCaseLogSOOFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogSOOFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogSOOFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogSOOFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogSOOFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogMSOOFn> log_marker_str_object1_object2_tests() {
        return Stream.of(
            new TestCaseLogMSOOFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogMSOOFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogMSOOFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogMSOOFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogMSOOFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogSOVarFn> log_str_vararg_tests() {
        return Stream.of(
            new TestCaseLogSOVarFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogSOVarFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogSOVarFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogSOVarFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogSOVarFn(l -> l::error, l -> verify(l)::error)
        );
    }

    public static Stream<TestCaseLogMSOVarFn> log_marker_str_vararg_tests() {
        return Stream.of(
            new TestCaseLogMSOVarFn(l -> l::trace, l -> verify(l)::trace),
            new TestCaseLogMSOVarFn(l -> l::debug, l -> verify(l)::debug),
            new TestCaseLogMSOVarFn(l -> l::info, l -> verify(l)::info),
            new TestCaseLogMSOVarFn(l -> l::warn, l -> verify(l)::warn),
            new TestCaseLogMSOVarFn(l -> l::error, l -> verify(l)::error)
        );
    }

    private static class TestCaseLogSFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSFn> logFn;
        private final Function<Logger, LogSFn> verify;

        private TestCaseLogSFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSFn> logFn,
                               Function<Logger, LogSFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogSTFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSTFn> logFn;
        private final Function<Logger, LogSTFn> verify;

        private TestCaseLogSTFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSTFn> logFn,
                               Function<Logger, LogSTFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogMSFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSFn> logFn;
        private final Function<Logger, LogMSFn> verify;

        private TestCaseLogMSFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSFn> logFn,
                               Function<Logger, LogMSFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogMSTFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSTFn> logFn;
        private final Function<Logger, LogMSTFn> verify;

        private TestCaseLogMSTFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSTFn> logFn,
                                Function<Logger, LogMSTFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogSOFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSOFn> logFn;
        private final Function<Logger, LogSOFn> verify;

        private TestCaseLogSOFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSOFn> logFn,
                                Function<Logger, LogSOFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogMSOFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSOFn> logFn;
        private final Function<Logger, LogMSOFn> verify;

        private TestCaseLogMSOFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSOFn> logFn,
                                Function<Logger, LogMSOFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogSOOFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSOOFn> logFn;
        private final Function<Logger, LogSOOFn> verify;

        private TestCaseLogSOOFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSOOFn> logFn,
                                 Function<Logger, LogSOOFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogMSOOFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSOOFn> logFn;
        private final Function<Logger, LogMSOOFn> verify;

        private TestCaseLogMSOOFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSOOFn> logFn,
                                 Function<Logger, LogMSOOFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogSOVarFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSOVarFn> logFn;
        private final Function<Logger, LogSOVarFn> verify;

        private TestCaseLogSOVarFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogSOVarFn> logFn,
                                   Function<Logger, LogSOVarFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private static class TestCaseLogMSOVarFn {
        private final Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSOVarFn> logFn;
        private final Function<Logger, LogMSOVarFn> verify;

        private TestCaseLogMSOVarFn(Function<software.amazon.awssdk.thirdparty.org.slf4j.Logger, LogShadedMSOVarFn> logFn,
                                   Function<Logger, LogMSOVarFn> verify) {
            this.logFn = logFn;
            this.verify = verify;
        }
    }

    private interface LogSFn {
        void invoke(String msg);
    }

    private interface LogSTFn {
        void invoke(String msg, Throwable t);
    }

    private interface LogMSFn {
        void invoke(Marker marker, String msg);
    }

    private interface LogShadedMSFn {
        void invoke(software.amazon.awssdk.thirdparty.org.slf4j.Marker marker, String msg);
    }

    private interface LogMSTFn {
        void invoke(Marker marker, String msg, Throwable t);
    }

    private interface LogShadedMSTFn {
        void invoke(software.amazon.awssdk.thirdparty.org.slf4j.Marker marker, String msg, Throwable t);
    }

    private interface LogSOFn {
        void invoke(String msg, Object o);
    }

    private interface LogMSOFn {
        void invoke(Marker marker, String msg, Object o);
    }

    private interface LogShadedMSOFn {
        void invoke(software.amazon.awssdk.thirdparty.org.slf4j.Marker marker, String msg, Object o);
    }

    private interface LogSOOFn {
        void invoke(String msg, Object o1, Object o2);
    }

    private interface LogMSOOFn {
        void invoke(Marker marker, String msg, Object o1, Object o2);
    }

    private interface LogShadedMSOOFn {
        void invoke(software.amazon.awssdk.thirdparty.org.slf4j.Marker marker, String msg, Object o1, Object o2);
    }

    private interface LogSOVarFn {
        void invoke(String msg, Object... o);
    }

    private interface LogMSOVarFn {
        void invoke(Marker marker, String msg, Object... o);
    }

    private interface LogShadedMSOVarFn {
        void invoke(software.amazon.awssdk.thirdparty.org.slf4j.Marker marker, String msg, Object... o);
    }
}