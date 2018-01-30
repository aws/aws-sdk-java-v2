/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.util.function.Supplier;
import org.slf4j.LoggerFactory;

public final class Logger {
    private final org.slf4j.Logger log;

    Logger(org.slf4j.Logger log) {
        this.log = log;
    }

    /**
     * Checks if info is enabled and if so logs the supplied message
     * @param msg - supplier for the log message
     */
    public void info(Supplier<String> msg) {
        if (log.isInfoEnabled()) {
            log.info(msg.get());
        }
    }

    /**
     * Checks if info is enabled and if so logs the supplied message and exception
     * @param msg - supplier for the log message
     * @param throwable - a throwable to log
     */
    public void info(Supplier<String> msg, Throwable throwable) {
        if (log.isInfoEnabled()) {
            log.info(msg.get(), throwable);
        }
    }

    /**
     * Checks if error is enabled and if so logs the supplied message
     * @param msg - supplier for the log message
     */
    public void error(Supplier<String> msg) {
        if (log.isErrorEnabled()) {
            log.error(msg.get());
        }
    }

    /**
     * Checks if error is enabled and if so logs the supplied message and exception
     * @param msg - supplier for the log message
     * @param throwable - a throwable to log
     */
    public void error(Supplier<String> msg, Throwable throwable) {
        if (log.isErrorEnabled()) {
            log.error(msg.get(), throwable);
        }
    }

    /**
     * Checks if debug is enabled and if so logs the supplied message
     * @param msg - supplier for the log message
     */
    public void debug(Supplier<String> msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg.get());
        }
    }

    /**
     * Checks if debug is enabled and if so logs the supplied message and exception
     * @param msg - supplier for the log message
     * @param throwable - a throwable to log
     */
    public void debug(Supplier<String> msg, Throwable throwable) {
        if (log.isDebugEnabled()) {
            log.debug(msg.get(), throwable);
        }
    }

    /**
     * Checks if warn is enabled and if so logs the supplied message
     * @param msg - supplier for the log message
     */
    public void warn(Supplier<String> msg) {
        if (log.isWarnEnabled()) {
            log.warn(msg.get());
        }
    }

    /**
     * Checks if warn is enabled and if so logs the supplied message and exception
     * @param msg - supplier for the log message
     * @param throwable - a throwable to log
     */
    public void warn(Supplier<String> msg, Throwable throwable) {
        if (log.isWarnEnabled()) {
            log.warn(msg.get(), throwable);
        }
    }

    /**
     * Checks if trace is enabled and if so logs the supplied message
     * @param msg - supplier for the log message
     */
    public void trace(Supplier<String> msg) {
        if (log.isTraceEnabled()) {
            log.trace(msg.get());
        }
    }

    /**
     * Checks if trace is enabled and if so logs the supplied message and exception
     * @param msg - supplier for the log message
     * @param throwable - a throwable to log
     */
    public void trace(Supplier<String> msg, Throwable throwable) {
        if (log.isTraceEnabled()) {
            log.trace(msg.get(), throwable);
        }
    }

    /**
     * Determines if the log-level passed is enabled
     * @param logLevel a string representation of the log level, e.g. "debug"
     * @return whether or not that level is enable
     */
    public boolean isLoggingLevelEnabled(String logLevel) {
        String lowerLogLevel = lowerCase(logLevel);
        switch (lowerLogLevel) {
            case "debug":
                return log.isDebugEnabled();
            case "trace":
                return log.isTraceEnabled();
            case "error":
                return log.isErrorEnabled();
            case "info":
                return log.isInfoEnabled();
            case "warn":
                return log.isWarnEnabled();
            default:
                throw new IllegalArgumentException("Unknown log level: " + lowerLogLevel);
        }
    }

    /**
     * Static factory to get a logger instance for a given class
     * @param clz - class to get the logger for
     * @return a Logger instance
     */
    public static Logger loggerFor(Class<?> clz) {
        return new Logger(LoggerFactory.getLogger(clz));
    }

    /**
     * Static factory to get a logger instance with a specific name.
     * @param name - The name of the logger to create
     * @return a Logger instance
     */
    public static Logger loggerFor(String name) {
        return new Logger(LoggerFactory.getLogger(name));
    }
}