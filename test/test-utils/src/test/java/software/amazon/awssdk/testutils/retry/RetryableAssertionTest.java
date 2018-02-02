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
package software.amazon.awssdk.testutils.retry;

import static org.junit.Assert.fail;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RetryableAssertionTest {

    @Mock
    private AssertCallable callable;

    @Test
    public void assertAlwaysFails_RetriesUpToMaxAttempts() throws Exception {
        Mockito.when(callable.call()).thenThrow(new AssertionError("Assertion failed"));
        try {
            doRetryableAssert();
            fail("Expected AssertionError");
        } catch (AssertionError expected) {
        }
        Mockito.verify(callable, Mockito.times(3)).call();
    }

    @Test
    public void assertFailsOnce_RetriesOnceThenReturns() throws Exception {
        // Throw on the first and return on the second
        Mockito.doThrow(new AssertionError("Assertion failed"))
                .doNothing()
                .when(callable).call();
        doRetryableAssert();
        Mockito.verify(callable, Mockito.times(2)).call();
    }

    @Test
    public void assertCallableThrowsException_DoesNotContinueRetrying() throws Exception {
        Mockito.when(callable.call())
                .thenThrow(new IOException("Unexpected exception in assert logic"));
        try {
            doRetryableAssert();
            fail("Expected IOException");
        } catch (IOException expected) {
        }
        Mockito.verify(callable, Mockito.times(1)).call();
    }

    private void doRetryableAssert() throws Exception {
        RetryableAssertion.doRetryableAssert(callable, new RetryableParams()
                .withMaxAttempts(3)
                .withDelayInMs(10));
    }
}
