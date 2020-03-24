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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LazyTest {
    private Lazy<String> lazy;
    private Supplier<String> mockDelegate;

    @Before
    public void setup() {
        mockDelegate = Mockito.mock(Supplier.class);
        lazy = new Lazy<>(mockDelegate);
    }

    @Test
    public void delegateNotCalledOnCreation() {
        Mockito.verifyZeroInteractions(mockDelegate);
    }

    @Test
    public void nullIsNotCached() {
        Mockito.when(mockDelegate.get()).thenReturn(null);
        lazy.getValue();
        lazy.getValue();
        Mockito.verify(mockDelegate, times(2)).get();
    }

    @Test
    public void exceptionsAreNotCached() {
        IllegalStateException exception = new IllegalStateException();
        Mockito.when(mockDelegate.get()).thenThrow(exception);
        assertThatThrownBy(lazy::getValue).isEqualTo(exception);
        assertThatThrownBy(lazy::getValue).isEqualTo(exception);
        Mockito.verify(mockDelegate, times(2)).get();
    }

    @Test(timeout = 10_000)
    public void delegateCalledOnlyOnce() throws Exception {
        final int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < 1000; ++i) {
                mockDelegate = Mockito.mock(Supplier.class);
                Mockito.when(mockDelegate.get()).thenReturn("");
                lazy = new Lazy<>(mockDelegate);

                CountDownLatch everyoneIsWaitingLatch = new CountDownLatch(threads);
                CountDownLatch everyoneIsDoneLatch = new CountDownLatch(threads);
                CountDownLatch callGetValueLatch = new CountDownLatch(1);

                for (int j = 0; j < threads; ++j) {
                    executor.submit(() -> {
                        everyoneIsWaitingLatch.countDown();
                        callGetValueLatch.await();
                        lazy.getValue();
                        everyoneIsDoneLatch.countDown();
                        return null;
                    });
                }

                everyoneIsWaitingLatch.await();
                callGetValueLatch.countDown();
                everyoneIsDoneLatch.await();

                Mockito.verify(mockDelegate, times(1)).get();
            }
        } finally {
            executor.shutdownNow();
        }
    }
}