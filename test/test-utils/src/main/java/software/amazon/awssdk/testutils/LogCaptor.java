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

import static org.apache.logging.log4j.core.config.Configurator.setRootLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * A test utility that allows inspection of log statements
 * during testing.
 * <p>
 * Can either be used stand-alone for example
 * <pre><code>
 *     try (LogCaptor logCaptor = new LogCaptor.DefaultLogCaptor(Level.INFO)) {
 *         // Do stuff that you expect to log things
 *         assertThat(logCaptor.loggedEvents(), is(not(empty())));
 *     }
 * </code></pre>
 * <p>
 * Or can extend it to make use of @Before / @After test annotations
 * <p>
 * <pre><code>
 *     class MyTestClass extends LogCaptor.LogCaptorTestBase {
 *         {@literal @}Test
 *         public void someTestThatWeExpectToLog() {
 *             // Do stuff that you expect to log things
 *             assertThat(loggedEvents(), is(not(empty())));
 *         }
 *     }
 * </code></pre>
 */

public interface LogCaptor extends SdkAutoCloseable {

    List<LogEvent> loggedEvents();

    void clear();

    class LogCaptorTestBase extends DefaultLogCaptor {
        public LogCaptorTestBase() {
        }

        public LogCaptorTestBase(Level level) {
            super(level);
        }

        @Override
        @BeforeEach
        public void setupLogging() {
            super.setupLogging();
        }

        @Override
        @AfterEach
        public void stopLogging() {
            super.stopLogging();
        }
    }

    class DefaultLogCaptor extends AbstractAppender implements LogCaptor {

        private final List<LogEvent> loggedEvents = new ArrayList<>();
        private final Level originalLoggingLevel = rootLogger().getLevel();
        private final Level levelToCapture;

        public DefaultLogCaptor() {
            this(Level.ALL);
        }

        public DefaultLogCaptor(Level level) {
            super(/* name */ getCallerClassName(),
                /* filter */ null,
                /* layout */ null,
                /* ignoreExceptions */ false,
                /* properties */ Property.EMPTY_ARRAY);
            this.levelToCapture = level;
            setupLogging();
        }

        @Override
        public List<LogEvent> loggedEvents() {
            return new ArrayList<>(loggedEvents);
        }

        @Override
        public void clear() {
            loggedEvents.clear();
        }

        protected void setupLogging() {
            loggedEvents.clear();
            rootLogger().addAppender(this);
            this.start();
            setRootLevel(levelToCapture);
        }

        protected void stopLogging() {
            rootLogger().removeAppender(this);
            this.stop();
            setRootLevel(originalLoggingLevel);
        }

        @Override
        public void append(LogEvent event) {
            loggedEvents.add(event);
        }

        @Override
        public void close() {
            stopLogging();
        }

        private static org.apache.logging.log4j.core.Logger rootLogger() {
            return (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        }

        private static String getCallerClassName() {
            return Arrays.stream(Thread.currentThread().getStackTrace())
                         .filter(ste -> !ste.getClassName().equals(Thread.class.getName()))
                         .filter(ste -> !ste.getClassName().equals(DefaultLogCaptor.class.getName()))
                         .findFirst()
                         .map(StackTraceElement::getClassName)
                         .orElse(null);
        }
    }
}
