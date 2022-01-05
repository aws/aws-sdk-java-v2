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
import java.util.NoSuchElementException;
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

    static LogCaptor create() {
        return new DefaultLogCaptor();
    }

    static LogCaptor create(Level level) {
        return new DefaultLogCaptor(level);
    }

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
        public void startCapturing() {
            super.startCapturing();
        }

        @Override
        @AfterEach
        public void stopCapturing() {
            super.stopCapturing();
        }
    }

    class DefaultLogCaptor extends AbstractAppender implements LogCaptor {

        private final List<LogEvent> loggedEvents = new ArrayList<>();
        private final Level originalLoggingLevel = rootLogger().getLevel();
        private final Level levelToCapture;

        private DefaultLogCaptor() {
            this(Level.ALL);
        }

        private DefaultLogCaptor(Level level) {
            super(/* name */ getCallerClassName(),
                /* filter */ null,
                /* layout */ null,
                /* ignoreExceptions */ false,
                /* properties */ Property.EMPTY_ARRAY);
            this.levelToCapture = level;
            startCapturing();
        }

        @Override
        public List<LogEvent> loggedEvents() {
            return new ArrayList<>(loggedEvents);
        }

        @Override
        public void clear() {
            loggedEvents.clear();
        }

        protected void startCapturing() {
            loggedEvents.clear();
            rootLogger().addAppender(this);
            this.start();
            setRootLevel(levelToCapture);
        }

        protected void stopCapturing() {
            rootLogger().removeAppender(this);
            this.stop();
            setRootLevel(originalLoggingLevel);
        }

        @Override
        public void append(LogEvent event) {
            loggedEvents.add(event.toImmutable());
        }

        @Override
        public void close() {
            stopCapturing();
        }

        private static org.apache.logging.log4j.core.Logger rootLogger() {
            return (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        }

        static String getCallerClassName() {
            return Arrays.stream(Thread.currentThread().getStackTrace())
                         .map(StackTraceElement::getClassName)
                         .filter(className -> !className.equals(Thread.class.getName()))
                         .filter(className -> !className.equals(DefaultLogCaptor.class.getName()))
                         .filter(className -> !className.equals(LogCaptor.class.getName()))
                         .findFirst()
                         .orElseThrow(NoSuchElementException::new);
        }
    }
}
