/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.test.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RetryableActionTest {

    @Mock
    private Callable<String> callable;

    @Test
    public void actionFailsWithRetryableError_RetriesUpToMaxAttempts() throws Exception {
        when(callable.call()).thenThrow(new RetryableError(new SomeError()));
        try {
            doRetryableAction();
            fail("Expected SomeError");
        } catch (SomeError expected) {
        }
        Mockito.verify(callable, Mockito.times(3)).call();
    }

    @Test
    public void actionFailsWithNonRetryableException_DoesNotRetry() throws Exception {
        when(callable.call()).thenThrow(new NonRetryableException(new SomeException()));
        try {
            doRetryableAction();
            fail("Expected SomeException");
        } catch (SomeException expected) {
        }
        Mockito.verify(callable, Mockito.times(1)).call();
    }

    @Test
    public void actionFailsWithException_RetriesUpToMaxAttempts() throws Exception {
        when(callable.call()).thenThrow(new SomeException());
        try {
            doRetryableAction();
            fail("Expected SomeException");
        } catch (SomeException expected) {
        }
        Mockito.verify(callable, Mockito.times(3)).call();
    }

    @Test
    public void actionFailsOnceWithExceptionThenSucceeds_RetriesOnceThenReturns() throws Exception {
        // Throw on the first and return on the second
        when(callable.call())
                .thenThrow(new SomeException())
                .thenReturn("foo");
        assertEquals("foo", doRetryableAction());
        Mockito.verify(callable, Mockito.times(2)).call();
    }

    @Test
    public void actionSucceedsOnFirstAttempt_DoesNotRetry() throws Exception {
        when(callable.call()).thenReturn("foo");
        assertEquals("foo", doRetryableAction());
        Mockito.verify(callable, Mockito.times(1)).call();
    }

    private String doRetryableAction() throws Exception {
        return RetryableAction.doRetryableAction(callable, new RetryableParams()
                .withMaxAttempts(3)
                .withDelayInMs(10));
    }

    private static class SomeError extends Error {
    }

    private static class SomeException extends Exception {
    }
}
