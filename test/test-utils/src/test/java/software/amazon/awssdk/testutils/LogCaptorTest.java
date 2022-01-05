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

package software.amazon.awssdk.testutils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.utils.Logger;

class LogCaptorTest {
    private static final Logger log = Logger.loggerFor(LogCaptorTest.class);

    @Test
    @DisplayName("Assert that default level captures all levels")
    void test_defaultLevel_capturesAll() {
        Level originalRootLevel = getRootLevel();
        try (LogCaptor logCaptor = LogCaptor.create()) {
            assertThat(getRootLevel()).isEqualTo(Level.ALL);
            logAtAllLevels();
            allLevels()
                .forEach(l -> assertContains(logCaptor.loggedEvents(), event -> event.getLevel().equals(l)));
        }
        assertThat(getRootLevel()).isEqualTo(originalRootLevel);
    }


    @ParameterizedTest
    @MethodSource("allLevels")
    @DisplayName("Assert that explicit level captures same level and higher")
    void test_explicitLevel_capturesCorrectLevels(Level level) {
        Level originalRootLevel = getRootLevel();
        try (LogCaptor logCaptor = LogCaptor.create(level)) {
            assertThat(getRootLevel()).isEqualTo(level);
            logAtAllLevels();
            allLevels()
                .filter(l -> l.isMoreSpecificThan(level))
                .forEach(l -> assertContains(logCaptor.loggedEvents(), event -> event.getLevel().equals(l)));
        }
        assertThat(getRootLevel()).isEqualTo(originalRootLevel);
    }

    @Test
    @DisplayName("Assert that appender name uses caller's class name")
    void test_getCallerClassName() {
        String callerClassName = LogCaptor.DefaultLogCaptor.getCallerClassName();
        assertThat(callerClassName).isEqualTo(this.getClass().getName());
    }

    private static Stream<Level> allLevels() {
        return Stream.of(
            Level.TRACE,
            Level.DEBUG,
            Level.INFO,
            Level.WARN,
            Level.ERROR
        );
    }

    private static void assertContains(List<LogEvent> events, Predicate<LogEvent> predicate) {
        for (LogEvent event : events) {
            if (predicate.test(event)) {
                return;
            }
        }
        throw new AssertionError("Couldn't find a log event matching the given predicate");
    }

    private static void logAtAllLevels() {
        allLevels()
            .map(Level::toString)
            .map(org.slf4j.event.Level::valueOf)
            .forEach(level -> log.log(level, level::toString));
    }


    private static Level getRootLevel() {
        LoggerContext loggerContext = LoggerContext.getContext(false);
        LoggerConfig loggerConfig = loggerContext.getConfiguration().getRootLogger();
        return loggerConfig.getLevel();
    }
}