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

package software.amazon.awssdk.http.apache5.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CancelOnInterruptWrapperTest {
    private Future<String> mockDelegate;

    @BeforeEach
    void setup() {
        mockDelegate = mock(Future.class);
    }

    @AfterEach
    void teardown() {
        Thread.interrupted(); // clear the flag if it was set by the last test
    }

    @Test
    void cancel_callsDelegate() {
        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);
        wrapper.cancel(true);
        verify(mockDelegate).cancel(eq(true));
    }

    @Test
    void isCancelled_callsDelegate() {
        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);
        wrapper.isCancelled();
        verify(mockDelegate).isCancelled();
    }

    @Test
    void isDone_callsDelegate() {
        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);
        wrapper.isDone();
        verify(mockDelegate).isDone();
    }

    @Test
    void get_callsDelegate() throws ExecutionException, InterruptedException {
        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);
        wrapper.get();
        verify(mockDelegate).get();
    }

    @Test
    void getTimeout_callsDelegate() throws ExecutionException, InterruptedException, TimeoutException {
        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);
        wrapper.get(42, TimeUnit.DAYS);
        verify(mockDelegate).get(eq(42L), eq(TimeUnit.DAYS));
    }

    @Test
    void getTimeout_interrupted_cancelSuccessful_throws() throws ExecutionException, InterruptedException, TimeoutException {
        when(mockDelegate.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("interrupt"));
        when(mockDelegate.cancel(eq(true))).thenReturn(true);

        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);

        assertThatThrownBy(() -> wrapper.get(1, TimeUnit.SECONDS))
            .isInstanceOf(InterruptedException.class)
            .hasMessage("interrupt");

        verify(mockDelegate).get(eq(1L), eq(TimeUnit.SECONDS));
        verify(mockDelegate).cancel(eq(true));

        verifyNoMoreInteractions(mockDelegate);
    }

    @Test
    void getTimeout_interrupted_cancelUnsuccessful_returnsEntry() throws ExecutionException, InterruptedException,
                                                                         TimeoutException {
        String result = "hello there";

        when(mockDelegate.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("interrupt"));
        when(mockDelegate.cancel(eq(true))).thenReturn(false);
        when(mockDelegate.get()).thenReturn(result);

        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);

        assertThat(wrapper.get(1, TimeUnit.SECONDS)).isEqualTo(result);
    }

    @Test
    void getTimeout_interrupted_cancelUnsuccessful_getUnsuccessful_rethrowsOriginalIe() throws ExecutionException,
                                                                                                  InterruptedException, TimeoutException {
        InterruptedException interrupt = new InterruptedException("interrupt");

        when(mockDelegate.get(anyLong(), any(TimeUnit.class))).thenThrow(interrupt);
        when(mockDelegate.cancel(eq(true))).thenReturn(false);
        when(mockDelegate.get()).thenThrow(new CancellationException("cancelled"));

        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);

        assertThatThrownBy(() -> wrapper.get(1, TimeUnit.SECONDS)).isSameAs(interrupt);
    }

    @Test
    void get_interrupted_cancelSuccessful_throws() throws ExecutionException, InterruptedException, TimeoutException {
        when(mockDelegate.get()).thenThrow(new InterruptedException("interrupt"));
        when(mockDelegate.cancel(eq(true))).thenReturn(true);

        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);

        assertThatThrownBy(wrapper::get)
            .isInstanceOf(InterruptedException.class)
            .hasMessage("interrupt");

        verify(mockDelegate).get();
        verify(mockDelegate).cancel(eq(true));

        verifyNoMoreInteractions(mockDelegate);
    }

    @Test
    void get_interrupted_cancelUnsuccessful_returnsEntry() throws ExecutionException, InterruptedException, TimeoutException {
        String result = "hello there";

        AtomicBoolean first = new AtomicBoolean(true);
        when(mockDelegate.get()).thenAnswer(i -> {
           if (first.compareAndSet(true, false)) {
               throw new InterruptedException("interrupt");
           }
           return result;
        });
        when(mockDelegate.cancel(eq(true))).thenReturn(false);

        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);

        assertThat(wrapper.get()).isEqualTo(result);
    }

    @Test
    void get_interrupted_cancelUnsuccessful_getUnsuccessful_rethrowsOriginalIe() throws ExecutionException, InterruptedException, TimeoutException {
        InterruptedException interrupt = new InterruptedException("interrupt");

        AtomicBoolean first = new AtomicBoolean(true);
        when(mockDelegate.get()).thenAnswer(i -> {
            if (first.compareAndSet(true, false)) {
                throw interrupt;
            }
            throw new CancellationException("cancelled");
        });
        when(mockDelegate.cancel(eq(true))).thenReturn(false);

        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);

        assertThatThrownBy(wrapper::get).isSameAs(interrupt);
    }

    @Test
    void get_interrupted_cancelUnsuccessful_cancelUnsuccessful_preservesInterruptedFlag() throws ExecutionException, InterruptedException {
        String result = "hello there";

        AtomicBoolean first = new AtomicBoolean(true);
        when(mockDelegate.get()).thenAnswer(i -> {
            if (first.compareAndSet(true, false)) {
                throw new InterruptedException("interrupt");
            }
            return result;
        });
        when(mockDelegate.cancel(eq(true))).thenReturn(false);

        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);

        wrapper.get();

        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void getTimeout_interrupted_cancelUnsuccessful_preservesInterruptedFlag() throws ExecutionException, InterruptedException,
                                                                         TimeoutException {
        String result = "hello there";

        when(mockDelegate.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("interrupt"));
        when(mockDelegate.cancel(eq(true))).thenReturn(false);
        when(mockDelegate.get()).thenReturn(result);

        CancelOnInterruptWrapper<String> wrapper = new CancelOnInterruptWrapper<>(mockDelegate);

        wrapper.get(1, TimeUnit.SECONDS);

        assertThat(Thread.interrupted()).isTrue();
    }
}
