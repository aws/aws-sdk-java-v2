/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.log;

/**
 * An SDK internal logging API, not intended for general use. This logging API
 * allows a minimal set of signer related classes to make use of logging without
 * direct dependency on any third party library.
 *
 * @see InternalLogFactory
 */
public interface InternalLogApi {

    /**
     * Logs a message with debug log level.
     *
     * @param message
     *            log this message
     */
    void debug(Object message);

    /**
     * Logs an error with debug log level.
     *
     * @param message
     *            log this message
     * @param t
     *            log this cause
     */
    void debug(Object message, Throwable t);

    /**
     * Logs a message with error log level.
     *
     * @param message
     *            log this message
     */
    void error(Object message);

    /**
     * Logs an error with error log level.
     *
     * @param message
     *            log this message
     * @param t
     *            log this cause
     */
    void error(Object message, Throwable t);

    /**
     * Logs a message with fatal log level.
     *
     * @param message
     *            log this message
     */
    void fatal(Object message);

    /**
     * Logs an error with fatal log level.
     *
     * @param message
     *            log this message
     * @param t
     *            log this cause
     */
    void fatal(Object message, Throwable t);

    /**
     * Logs a message with info log level.
     *
     * @param message
     *            log this message
     */
    void info(Object message);

    /**
     * Logs an error with info log level.
     *
     * @param message
     *            log this message
     * @param t
     *            log this cause
     */
    void info(Object message, Throwable t);

    /**
     * Is debug logging currently enabled?
     * <p>
     * Call this method to prevent having to perform expensive operations (for
     * example, <code>String</code> concatenation) when the log level is more
     * than debug.
     *
     * @return true if debug is enabled in the underlying logger.
     */
    boolean isDebugEnabled();

    /**
     * Is error logging currently enabled?
     * <p>
     * Call this method to prevent having to perform expensive operations (for
     * example, <code>String</code> concatenation) when the log level is more
     * than error.
     *
     * @return true if error is enabled in the underlying logger.
     */
    boolean isErrorEnabled();

    /**
     * Is fatal logging currently enabled?
     * <p>
     * Call this method to prevent having to perform expensive operations (for
     * example, <code>String</code> concatenation) when the log level is more
     * than fatal.
     *
     * @return true if fatal is enabled in the underlying logger.
     */
    boolean isFatalEnabled();

    /**
     * Is info logging currently enabled?
     * <p>
     * Call this method to prevent having to perform expensive operations (for
     * example, <code>String</code> concatenation) when the log level is more
     * than info.
     *
     * @return true if info is enabled in the underlying logger.
     */
    boolean isInfoEnabled();

    /**
     * Is trace logging currently enabled?
     * <p>
     * Call this method to prevent having to perform expensive operations (for
     * example, <code>String</code> concatenation) when the log level is more
     * than trace.
     *
     * @return true if trace is enabled in the underlying logger.
     */
    boolean isTraceEnabled();

    /**
     * Is warn logging currently enabled?
     * <p>
     * Call this method to prevent having to perform expensive operations (for
     * example, <code>String</code> concatenation) when the log level is more
     * than warn.
     *
     * @return true if warn is enabled in the underlying logger.
     */
    boolean isWarnEnabled();

    /**
     * Logs a message with trace log level.
     *
     * @param message
     *            log this message
     */
    void trace(Object message);

    /**
     * Logs an error with trace log level.
     *
     * @param message
     *            log this message
     * @param t
     *            log this cause
     */
    void trace(Object message, Throwable t);

    /**
     * Logs a message with warn log level.
     *
     * @param message
     *            log this message
     */
    void warn(Object message);

    /**
     * Logs an error with warn log level.
     *
     * @param message
     *            log this message
     * @param t
     *            log this cause
     */
    void warn(Object message, Throwable t);
}
