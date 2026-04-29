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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public final class LogCaptor implements AutoCloseable {
    private final LoggerContext loggerContext;
    private final Configuration config;

    private final String loggerName;
    private final String appenderName;

    private final LoggerConfig initialLoggerConfig;
    private final Level initialLoggerLevel;
    private final LoggerConfig dedicatedLoggerConfig;

    private final TestAppender testAppender;

    public LogCaptor(Class<?> loggerClass, org.slf4j.event.Level level) {
        this(loggerClass.getName(), level);
    }

    public LogCaptor(String loggerName, org.slf4j.event.Level level) {
        this.loggerName = loggerName;
        this.appenderName = "TestAppender#" + loggerName;
        Level levelToCapture = Level.valueOf(level.name());

        this.loggerContext = (LoggerContext) LogManager.getContext(false);
        this.config = loggerContext.getConfiguration();

        this.testAppender = new TestAppender(appenderName);
        this.testAppender.start();

        this.config.addAppender(this.testAppender);

        LoggerConfig existingLoggerConfig = config.getLoggerConfig(loggerName);

        if (!existingLoggerConfig.getName().equals(loggerName)) {
            LoggerConfig dedicatedLoggerConfig = new LoggerConfig(loggerName, levelToCapture, false);
            dedicatedLoggerConfig.addAppender(this.testAppender, levelToCapture, null);
            this.config.addLogger(loggerName, dedicatedLoggerConfig);
            this.initialLoggerLevel = null;
            this.dedicatedLoggerConfig = dedicatedLoggerConfig;
            this.initialLoggerConfig = dedicatedLoggerConfig;
        } else {
            existingLoggerConfig.addAppender(this.testAppender, levelToCapture, null);
            this.initialLoggerLevel = existingLoggerConfig.getLevel();
            existingLoggerConfig.setLevel(levelToCapture);
            this.dedicatedLoggerConfig = null;
            this.initialLoggerConfig = existingLoggerConfig;
        }

        this.loggerContext.updateLoggers();
    }

    public List<LogEvent> loggedEvents() {
        return this.testAppender.getEvents();
    }

    @Override
    public void close() {
        this.initialLoggerConfig.removeAppender(appenderName);

        if (this.dedicatedLoggerConfig != null) {
            this.config.removeLogger(loggerName);
        } else if (this.initialLoggerLevel != null) {
            this.initialLoggerConfig.setLevel(this.initialLoggerLevel);
        }

        this.config.getAppenders().remove(appenderName);
        this.testAppender.stop();

        this.loggerContext.updateLoggers();
    }

    private static final class TestAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private TestAppender(String appenderName) {
            super(appenderName, null, null, true, null);
        }

        @Override
        public void append(LogEvent event) {
            this.events.add(event.toImmutable());
        }

        public List<LogEvent> getEvents() {
            return Collections.unmodifiableList(this.events);
        }
    }
}
