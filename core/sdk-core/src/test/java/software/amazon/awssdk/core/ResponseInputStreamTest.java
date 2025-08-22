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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;

class ResponseInputStreamTest {

    InputStream stream;
    Abortable abortable;
    AbortableInputStream abortableInputStream;

    @BeforeEach
    public void setUp() throws Exception {
        stream = Mockito.mock(InputStream.class);
        abortable = Mockito.mock(Abortable.class);
        abortableInputStream = AbortableInputStream.create(stream, abortable);
    }

    @Test
    void abort_withAbortable_closesUnderlyingStream() throws IOException {
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        responseInputStream.abort();

        verify(abortable).abort();
        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void failedClose_withinAbort_isIgnored() throws IOException {
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        Mockito.doThrow(new IOException()).when(stream).close();
        assertThatCode(responseInputStream::abort).doesNotThrowAnyException();

        verify(abortable).abort();
        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void abort_withoutAbortable_closesUnderlyingStream() throws IOException {
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), stream);

        responseInputStream.abort();

        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void close_withAbortable_closesUnderlyingStream() throws IOException {
        ResponseInputStream<Object> responseInputStream = new ResponseInputStream<>(new Object(), abortableInputStream);

        responseInputStream.close();

        verify(abortable, never()).abort();
        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isFalse();
    }

    @Test
    void customTimeout_noRead_abortsAfterTimeout() throws Exception {
        ResponseInputStream<Object> responseInputStream = responseInputStream(Duration.ofSeconds(1));
        Thread.sleep(2000);

        verify(abortable).abort();
        verify(stream).close();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void customTimouet_readBeforeTimeout_cancelsTimeout() throws Exception {
        ResponseInputStream<Object> responseInputStream = responseInputStream(Duration.ofSeconds(1));
        responseInputStream.read();
        Thread.sleep(2000);

        verify(abortable, never()).abort();
        verify(stream).read();
        assertThat(responseInputStream.hasTimeoutTask()).isTrue();
        assertThat(responseInputStream.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void zeroTimeout_disablesTimeout() throws Exception {
        ResponseInputStream<Object> responseInputStream = responseInputStream(Duration.ZERO);
        Thread.sleep(2000);

        verify(abortable, never()).abort();
        verify(stream, never()).close();
        assertThat(responseInputStream.hasTimeoutTask()).isFalse();
    }

    private ResponseInputStream<Object> responseInputStream(Duration timeout) {
        return new ResponseInputStream<>(new Object(), abortableInputStream, timeout);
    }
}
