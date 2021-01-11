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

package software.amazon.awssdk.http.nio.netty.internal.utils;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExceptionHandlingUtilsTest {

    @Mock
    private Consumer<Throwable> errorNotifier;

    @Mock
    private Runnable cleanupExecutor;

    @Test
    public void tryCatch() {
        ExceptionHandlingUtils.tryCatch(() -> {
                                        },
                                        errorNotifier);
        verify(errorNotifier, times(0)).accept(any(Throwable.class));
    }

    @Test
    public void tryCatchExceptionThrows() {
        ExceptionHandlingUtils.tryCatch(() -> {
                                            throw new RuntimeException("helloworld");
                                        },
                                        errorNotifier);
        verify(errorNotifier).accept(any(Throwable.class));
    }

    @Test
    public void tryCatchFinallyException() {
        ExceptionHandlingUtils.tryCatchFinally(() -> "blah",
                                               errorNotifier,
                                               cleanupExecutor
        );
        verify(errorNotifier, times(0)).accept(any(Throwable.class));
        verify(cleanupExecutor).run();
    }

    @Test
    public void tryCatchFinallyExceptionThrows() {
        ExceptionHandlingUtils.tryCatchFinally(() -> {
                                                   throw new RuntimeException("helloworld");
                                               },
                                               errorNotifier,
                                               cleanupExecutor
        );
        verify(errorNotifier).accept(any(Throwable.class));
        verify(cleanupExecutor).run();
    }
}
