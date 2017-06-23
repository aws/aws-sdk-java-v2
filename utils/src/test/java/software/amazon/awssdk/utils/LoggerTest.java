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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.apache.commons.logging.Log;
import org.junit.Test;


public class LoggerTest {

    private final Log log = mock(Log.class);
    private final Logger sut = new Logger(log);
    private final Supplier<String> supplierThatThrows = () -> { throw new RuntimeException("Should not get called"); };
    private final String message = "Log message";
    private final Throwable throwable = new RuntimeException("Boom");

    @Test
    public void messageSupplierCalledWhenInfoEnabled() {
        when(log.isInfoEnabled()).thenReturn(true);

        sut.info(() -> message);

        verify(log).info(message);
    }

    @Test
    public void throwablePassedWhenInfoEnabled() {
        when(log.isInfoEnabled()).thenReturn(true);

        sut.info(() -> message, throwable);

        verify(log).info(message, throwable);
    }

    @Test
    public void messageSupplierNotCalledWhenInfoDisabled() {
        when(log.isInfoEnabled()).thenReturn(false);
        sut.info(supplierThatThrows);
    }

    @Test
    public void throwableSupplierNotCalledWhenInfoDisabled() {
        when(log.isInfoEnabled()).thenReturn(false);

        sut.info(supplierThatThrows, throwable);
    }

    @Test
    public void messageSupplierCalledWhenWarnEnabled() {
        when(log.isWarnEnabled()).thenReturn(true);

        sut.warn(() -> message);

        verify(log).warn(message);
    }

    @Test
    public void throwablePassedCalledWhenWarnEnabled() {
        when(log.isWarnEnabled()).thenReturn(true);

        sut.warn(() -> message, throwable);

        verify(log).warn(message, throwable);
    }

    @Test
    public void messageSupplierNotCalledWhenWarnDisabled() {
        when(log.isWarnEnabled()).thenReturn(false);

        sut.warn(supplierThatThrows);
    }

    @Test
    public void throwableSupplierNotCalledWhenWarnDisabled() {
        when(log.isWarnEnabled()).thenReturn(false);

        sut.warn(supplierThatThrows, throwable);
    }

    @Test
    public void messageSupplierCalledWhenDebugEnabled() {
        when(log.isDebugEnabled()).thenReturn(true);

        sut.debug(() -> message);

        verify(log).debug(message);
    }

    @Test
    public void throwablePassedCalledWhenDebugEnabled() {
        when(log.isDebugEnabled()).thenReturn(true);

        sut.debug(() -> message, throwable);

        verify(log).debug(message, throwable);
    }

    @Test
    public void messageSupplierNotCalledWhenDebugDisabled() {
        when(log.isDebugEnabled()).thenReturn(false);

        sut.debug(supplierThatThrows);
    }

    @Test
    public void throwableSupplierNotCalledWhenDebugDisabled() {
        when(log.isDebugEnabled()).thenReturn(false);

        sut.debug(supplierThatThrows, throwable);
    }

    @Test
    public void messageSupplierCalledWhenTraceEnabled() {
        when(log.isTraceEnabled()).thenReturn(true);

        sut.trace(() -> message);

        verify(log).trace(message);
    }

    @Test
    public void throwablePassedCalledWhenTraceEnabled() {
        when(log.isTraceEnabled()).thenReturn(true);

        sut.trace(() -> message, throwable);

        verify(log).trace(message, throwable);
    }

    @Test
    public void messageSupplierNotCalledWhenTraceDisabled() {
        when(log.isTraceEnabled()).thenReturn(false);

        sut.trace(supplierThatThrows);
    }

    @Test
    public void throwableSupplierNotCalledWhenTraceDisabled() {
        when(log.isTraceEnabled()).thenReturn(false);

        sut.trace(supplierThatThrows, throwable);
    }

    @Test
    public void messageSupplierCalledWhenErrorEnabled() {
        when(log.isErrorEnabled()).thenReturn(true);

        sut.error(() -> message);

        verify(log).error(message);
    }

    @Test
    public void throwablePassedCalledWhenErrorEnabled() {
        when(log.isErrorEnabled()).thenReturn(true);

        sut.error(() -> message, throwable);

        verify(log).error(message, throwable);
    }

    @Test
    public void messageSupplierNotCalledWhenErrorDisabled() {
        when(log.isErrorEnabled()).thenReturn(false);

        sut.error(supplierThatThrows);
    }

    @Test
    public void throwableSupplierNotCalledWhenErrorDisabled() {
        when(log.isErrorEnabled()).thenReturn(false);

        sut.error(supplierThatThrows, throwable);
    }

    @Test
    public void logLevelCanBeChecked() {
        when(log.isErrorEnabled()).thenReturn(true);
        assertThat(sut.isLoggingLevelEnabled("error")).isTrue();
    }
}