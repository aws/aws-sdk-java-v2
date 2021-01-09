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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.FunctionalUtils.safeConsumer;
import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Test;


public class FunctionalUtilsTest {

    private static final boolean DONT_THROW_EXCEPTION = false;
    private static final boolean THROW_EXCEPTION = true;

    @Test
    public void checkedExceptionsAreConvertedToRuntimeExceptions() {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> invokeSafely(this::methodThatThrows))
            .withCauseInstanceOf(Exception.class);
    }

    @Test
    public void ioExceptionsAreConvertedToUncheckedIoExceptions() {
        assertThatExceptionOfType(UncheckedIOException.class)
            .isThrownBy(() -> invokeSafely(this::methodThatThrowsIOException))
            .withCauseInstanceOf(IOException.class);
    }

    @Test
    public void runtimeExceptionsAreNotWrapped() {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> invokeSafely(this::methodWithCheckedSignatureThatThrowsRuntimeException))
            .withNoCause();
    }

    @Test
    public void canUseConsumerThatThrowsCheckedExceptionInLambda() {
        Stream.of(DONT_THROW_EXCEPTION).forEach(safeConsumer(this::consumerMethodWithChecked));
    }

    @Test
    public void exceptionsForConsumersAreConverted() {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> Stream.of(THROW_EXCEPTION).forEach(safeConsumer(this::consumerMethodWithChecked)))
            .withCauseExactlyInstanceOf(Exception.class);
    }

    @Test
    public void canUseFunctionThatThrowsCheckedExceptionInLambda() {
        Optional<String> result = Stream.of(DONT_THROW_EXCEPTION).map(safeFunction(this::functionMethodWithChecked)).findFirst();
        assertThat(result).isPresent().contains("Hello");
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void exceptionsForFunctionsAreConverted() {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> Stream.of(THROW_EXCEPTION).map(safeFunction(this::functionMethodWithChecked)).findFirst())
            .withCauseExactlyInstanceOf(Exception.class);
    }

    @Test
    public void interruptedExceptionShouldSetInterruptedOnTheThread() {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> invokeSafely(this::methodThatThrowsInterruptedException))
            .withCauseInstanceOf(InterruptedException.class);

        assertThat(Thread.currentThread().isInterrupted()).isTrue();

        // Clear interrupt flag
        Thread.interrupted();
    }

    private String methodThatThrows() throws Exception {
        throw new Exception("Ouch");
    }

    private String methodThatThrowsIOException() throws IOException {
        throw new IOException("Boom");
    }

    private String methodWithCheckedSignatureThatThrowsRuntimeException() throws Exception {
        throw new RuntimeException("Uh oh");
    }

    private String methodThatThrowsInterruptedException() throws InterruptedException {
        throw new InterruptedException();
    }

    private void consumerMethodWithChecked(Boolean shouldThrow) throws Exception {
        if (shouldThrow) {
            throw new Exception("Duh, something went wrong");
        }
    }

    private String functionMethodWithChecked(Boolean shouldThrow) throws Exception {
        if (shouldThrow) {
            throw new Exception("Duh, something went wrong");
        }
        return "Hello";
    }
}
