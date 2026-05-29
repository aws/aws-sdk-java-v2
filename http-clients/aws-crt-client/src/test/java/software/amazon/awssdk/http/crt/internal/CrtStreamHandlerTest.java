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

package software.amazon.awssdk.http.crt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@ExtendWith(MockitoExtension.class)
class CrtStreamHandlerTest {

    @Mock
    private HttpStreamBase stream;

    private CrtStreamHandler streamHandler;

    @BeforeEach
    void setUp() {
        streamHandler = new CrtStreamHandler(CompletableFuture.completedFuture(stream));
    }

    @Test
    void releaseConnection_shouldCallClose() {
        streamHandler.releaseConnection();

        verify(stream, never()).cancel();
        verify(stream).close();
    }

    @Test
    void closeConnection_shouldCallCancelAndClose() {
        streamHandler.closeConnection();

        verify(stream).cancel();
        verify(stream, Mockito.atLeastOnce()).close();
    }

    @Test
    void incrementWindow_afterReleaseConnection_shouldBeNoOp() {
        streamHandler.releaseConnection();
        streamHandler.incrementWindow(1024);

        verify(stream, never()).incrementWindow(1024);
    }

    @Test
    void incrementWindow_afterCloseConnection_shouldBeNoOp() {
        streamHandler.closeConnection();
        streamHandler.incrementWindow(1024);

        verify(stream, never()).incrementWindow(1024);
    }

    @Test
    void incrementWindow_beforeClose_shouldWork() {
        streamHandler.incrementWindow(1024);

        verify(stream).incrementWindow(1024);
    }

    @Test
    void incrementWindow_beforeStreamReady_shouldBeNoOp() {
        CrtStreamHandler handler = new CrtStreamHandler(new CompletableFuture<>());

        handler.incrementWindow(1024);

        verify(stream, never()).incrementWindow(1024);
    }

    @Test
    void waitForStream_afterStreamFutureFailed_throwsCompletionExceptionWrappingCause() {
        IOException cause = new IOException("acquire failed");
        CrtStreamHandler handler = new CrtStreamHandler(CompletableFutureUtils.failedFuture(cause));

        assertThatThrownBy(handler::waitForStream)
            .isInstanceOf(CompletionException.class)
            .hasCause(cause);
    }

    @Test
    void writeData_underlyingWriteFails_propagatesOriginalCauseUnwrapped() {
        RuntimeException writeError = new RuntimeException("write failure");
        Mockito.when(stream.writeData(Mockito.any(byte[].class), Mockito.eq(false)))
               .thenReturn(CompletableFutureUtils.failedFuture(writeError));

        CompletableFuture<Void> writeFuture = streamHandler.writeData(new byte[] {1, 2, 3}, false);

        assertThatThrownBy(writeFuture::get)
            .isInstanceOf(ExecutionException.class)
            .hasCauseReference(writeError);
    }

    @Test
    void writeData_streamAcquisitionFailed_propagatesOriginalCauseUnwrapped() {
        IOException acquireFailure = new IOException("acquire failed");
        CrtStreamHandler handler = new CrtStreamHandler(CompletableFutureUtils.failedFuture(acquireFailure));

        CompletableFuture<Void> writeFuture = handler.writeData(new byte[] {1, 2, 3}, false);

        assertThatThrownBy(writeFuture::get)
            .isInstanceOf(ExecutionException.class)
            .hasCauseReference(acquireFailure);
    }

    @Test
    void writeData_afterCloseConnection_failsWithIoException() {
        streamHandler.closeConnection();

        CompletableFuture<Void> writeFuture = streamHandler.writeData(new byte[] {1, 2, 3}, false);

        assertThat(writeFuture).isCompletedExceptionally();
        assertThatThrownBy(writeFuture::get)
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void writeData_underlyingWriteThrowsSynchronously_failsFutureAndClosesConnection() {
        CrtRuntimeException syncError = new CrtRuntimeException("AWS_ERROR_HTTP_STREAM_NOT_ACTIVATED");
        Mockito.when(stream.writeData(Mockito.any(byte[].class), Mockito.eq(false)))
               .thenThrow(syncError);

        CompletableFuture<Void> writeFuture = streamHandler.writeData(new byte[] {1, 2, 3}, false);

        assertThat(writeFuture).isCompletedExceptionally();
        assertThatThrownBy(writeFuture::get)
            .isInstanceOf(ExecutionException.class)
            .hasCauseReference(syncError);
        verify(stream).cancel();
        verify(stream).close();
    }
}
