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

package software.amazon.awssdk.http.apache.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

class RepeatableInputStreamRequestEntityTest {

    private static final String TRANSFER_ENCODING = "Transfer-Encoding";
    private static final String CHUNKED = "chunked";

    private RepeatableInputStreamRequestEntity entity;
    private SdkHttpRequest.Builder httpRequestBuilder;

    @BeforeEach
    void setUp() {
        httpRequestBuilder = SdkHttpRequest.builder()
                                           .uri(URI.create("https://example.com"))
                                           .method(SdkHttpMethod.POST);
    }

    @Test
    @DisplayName("Constructor should initialize with chunked transfer encoding")
    void constructor_WithChunkedTransferEncoding_SetsChunkedTrue() {
        // Given
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader(TRANSFER_ENCODING, CHUNKED)
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();
        entity = new RepeatableInputStreamRequestEntity(request);
        assertTrue(entity.isChunked());
    }

    @Test
    @DisplayName("Constructor should handle content length header correctly")
    void constructor_WithContentLength_SetsContentLengthCorrectly() {
        long expectedLength = 1024L;
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader("Content-Length", String.valueOf(expectedLength))
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();
        entity = new RepeatableInputStreamRequestEntity(request);
        assertEquals(expectedLength, entity.getContentLength());
    }

    @Test
    @DisplayName("Constructor should handle invalid content length gracefully")
    void constructor_WithInvalidContentLength_DefaultsToMinusOne() {
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader("Content-Length", "not-a-number")
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();
        entity = new RepeatableInputStreamRequestEntity(request);
        assertEquals(-1L, entity.getContentLength());
    }

    @Test
    @DisplayName("Constructor should set content type when provided")
    void constructor_WithContentType_SetsContentTypeCorrectly() {
        String contentType = "application/json";
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader("Content-Type", contentType)
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();
        entity = new RepeatableInputStreamRequestEntity(request);
        assertEquals(contentType, entity.getContentType().getValue());
    }

    @Test
    @DisplayName("Constructor should use provided content stream")
    void constructor_WithContentStreamProvider_UsesProvidedStream() {
        String content = "test content";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        ContentStreamProvider provider = () -> inputStream;

        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();
        entity = new RepeatableInputStreamRequestEntity(request);
        assertSame(inputStream, entity.getContent());
    }

    @Test
    @DisplayName("Constructor should create empty stream when no content provider")
    void constructor_WithoutContentStreamProvider_CreatesEmptyStream() throws IOException {
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();
        entity = new RepeatableInputStreamRequestEntity(request);
        InputStream content = entity.getContent();
        assertNotNull(content);
        assertEquals(0, content.available());
    }

    @Test
    @DisplayName("isRepeatable should return true for mark-supported streams")
    void isRepeatable_WithMarkSupportedStream_ReturnsTrue() {
        ByteArrayInputStream markableStream = new ByteArrayInputStream("content".getBytes());
        ContentStreamProvider provider = () -> markableStream;

        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();
        entity = new RepeatableInputStreamRequestEntity(request);
        assertTrue(entity.isRepeatable());
        assertTrue(markableStream.markSupported());
    }

    @Test
    @DisplayName("isRepeatable should return false for non-mark-supported streams")
    void isRepeatable_WithNonMarkSupportedStream_ReturnsFalse() {
        // Given
        InputStream nonMarkableStream = new InputStream() {
            @Override
            public int read() {
                return -1;
            }
            @Override
            public boolean markSupported() {
                return false;
            }
        };
        ContentStreamProvider provider = () -> nonMarkableStream;

        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);
        assertFalse(entity.isRepeatable());
    }

    @Test
    @DisplayName("writeTo should not reset stream on first attempt")
    void writeTo_FirstAttempt_DoesNotResetStream() throws IOException {
        // Given
        String content = "test content";

        // Create a custom stream that tracks reset calls
        AtomicInteger resetCallCount = new AtomicInteger(0);
        ByteArrayInputStream trackingStream = new ByteArrayInputStream(content.getBytes()) {
            @Override
            public synchronized void reset()  {
                resetCallCount.incrementAndGet();
                super.reset();
            }
        };

        ContentStreamProvider provider = () -> trackingStream;

        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        entity.writeTo(output);
        assertEquals(content, output.toString());
        assertEquals(0, resetCallCount.get(), "Reset should not be called on first attempt");
    }

    @Test
    @DisplayName("writeTo should reset stream on subsequent attempts if repeatable")
    void writeTo_SubsequentAttemptWithRepeatableStream_ResetsStream() throws IOException {
        // Given
        String content = "test content";

        // Create a custom stream that tracks reset calls
        AtomicInteger resetCallCount = new AtomicInteger(0);
        ByteArrayInputStream trackingStream = new ByteArrayInputStream(content.getBytes()) {
            @Override
            public synchronized void reset()  {
                resetCallCount.incrementAndGet();
                super.reset();
            }
        };

        ContentStreamProvider provider = () -> trackingStream;

        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        // First write
        ByteArrayOutputStream firstOutput = new ByteArrayOutputStream();
        entity.writeTo(firstOutput);

        //Second write
        ByteArrayOutputStream secondOutput = new ByteArrayOutputStream();
        entity.writeTo(secondOutput);

        // Then
        assertEquals(content, firstOutput.toString());
        assertEquals(content, secondOutput.toString());
        assertEquals(1, resetCallCount.get(), "Reset should be called exactly once for second attempt");
    }

    @Test
    @DisplayName("writeTo should preserve original exception on first failure")
    void writeTo_FirstAttemptThrowsException_PreservesOriginalException() throws IOException {
        // Given
        IOException originalException = new IOException("Original error");
        InputStream faultyStream = mock(InputStream.class);
        when(faultyStream.read(any(byte[].class))).thenThrow(originalException);
        when(faultyStream.markSupported()).thenReturn(true);

        ContentStreamProvider provider = () -> faultyStream;
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        IOException thrown = assertThrows(IOException.class,
                                          () -> entity.writeTo(new ByteArrayOutputStream()));
        assertSame(originalException, thrown);
    }

    @Test
    @DisplayName("writeTo should throw original exception on subsequent failures")
    void writeTo_SubsequentFailures_ThrowsOriginalException() throws IOException {
        // Given
        IOException originalException = new IOException("Original error");
        IOException secondException = new IOException("Second error");

        InputStream faultyStream = mock(InputStream.class);
        when(faultyStream.read(any(byte[].class)))
            .thenThrow(originalException)
            .thenThrow(secondException);
        when(faultyStream.markSupported()).thenReturn(true);

        ContentStreamProvider provider = () -> faultyStream;
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);
        // First attempt
        IOException firstThrown = assertThrows(IOException.class,
                                               () -> entity.writeTo(new ByteArrayOutputStream()));
        assertEquals("Original error", firstThrown.getMessage());
        assertSame(originalException, firstThrown);
        // Second attempt
        IOException secondThrown = assertThrows(IOException.class,
                                                () -> entity.writeTo(new ByteArrayOutputStream()));

        // Should still throw original exception (not the second one)
        assertSame(originalException, secondThrown);
        assertEquals("Original error", secondThrown.getMessage());
        assertNotSame(secondException, secondThrown);
    }

    @Test
    @DisplayName("writeTo should handle reset failures gracefully")
    void writeTo_ResetThrowsException_PropagatesResetException() throws IOException {
        // Given
        String content = "test content";

        // Create a custom stream that throws on reset after first successful read
        InputStream problematicStream = new InputStream() {
            private final byte[] data = content.getBytes();
            private int position = 0;
            private boolean hasBeenRead = false;

            @Override
            public int read() throws IOException {
                if (position >= data.length) {
                    hasBeenRead = true;
                    return -1;
                }
                hasBeenRead = true;
                return data[position++] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (position >= data.length) {
                    hasBeenRead = true;
                    return -1;
                }
                int bytesToRead = Math.min(len, data.length - position);
                System.arraycopy(data, position, b, off, bytesToRead);
                position += bytesToRead;
                hasBeenRead = true;
                return bytesToRead;
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public synchronized void mark(int readlimit) {
                // Mark at current position
            }

            @Override
            public synchronized void reset() throws IOException {
                if (hasBeenRead) {
                    throw new IOException("Reset failed");
                }
                position = 0;
            }
        };

        ContentStreamProvider provider = () -> problematicStream;
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        // First successful write
        ByteArrayOutputStream firstOutput = new ByteArrayOutputStream();
        entity.writeTo(firstOutput);
        assertEquals(content, firstOutput.toString());

        // Second write where reset should fail
        IOException thrown = assertThrows(IOException.class,
                                          () -> entity.writeTo(new ByteArrayOutputStream()));


        assertEquals("Reset failed", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "0", "9223372036854775807"}) // Long.MAX_VALUE
    @DisplayName("parseContentLength should handle valid numeric values")
    void parseContentLength_ValidNumbers_ParsesCorrectly(String contentLength) {
        // Given
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader("Content-Length", contentLength)
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);
        assertEquals(Long.parseLong(contentLength), entity.getContentLength());
    }

    @Test
    @DisplayName("Multiple writes should work correctly with repeatable stream")
    void writeTo_MultipleWrites_AllSucceed() throws IOException {
        // Given
        String content = "repeatable content";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        ContentStreamProvider provider = () -> inputStream;

        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        // Multiple writes
        ByteArrayOutputStream output1 = new ByteArrayOutputStream();
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();
        ByteArrayOutputStream output3 = new ByteArrayOutputStream();

        entity.writeTo(output1);
        entity.writeTo(output2);
        entity.writeTo(output3);

        // All outputs should contain the same content
        assertEquals(content, output1.toString());
        assertEquals(content, output2.toString());
        assertEquals(content, output3.toString());
    }

    @Test
    @DisplayName("Entity should handle mixed headers correctly")
    void constructor_WithMixedHeaders_HandlesAllCorrectly() {
        // Given
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader("Content-Length", "2048")
            .putHeader("Content-Type", "application/xml")
            .putHeader(TRANSFER_ENCODING, CHUNKED)
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();
        entity = new RepeatableInputStreamRequestEntity(request);
        assertEquals(2048L, entity.getContentLength());
        assertEquals("application/xml", entity.getContentType().getValue());
        assertTrue(entity.isChunked());
    }

    @Test
    @DisplayName("Entity should handle empty content correctly")
    void writeTo_EmptyContent_WritesNothing() throws IOException {
        // Given
        ContentStreamProvider provider = () -> new ByteArrayInputStream(new byte[0]);
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        entity.writeTo(output);
        assertEquals(0, output.size());
    }

    @Test
    @DisplayName("Entity should handle large content streams")
    void writeTo_LargeContent_HandlesCorrectly() throws IOException {
        // Given - 10MB of data
        int size = 10 * 1024 * 1024;
        byte[] largeContent = new byte[size];
        new Random().nextBytes(largeContent);

        ContentStreamProvider provider = () -> new ByteArrayInputStream(largeContent);
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader("Content-Length", String.valueOf(size))
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        entity.writeTo(output);
        assertArrayEquals(largeContent, output.toByteArray());
        assertEquals(size, entity.getContentLength());
    }

    @Test
    @DisplayName("Entity should handle non-repeatable stream on multiple writes")
    void writeTo_NonRepeatableStreamMultipleWrites_FailsGracefully() throws IOException {
        InputStream nonRepeatableStream = new InputStream() {
            private boolean hasBeenRead = false;

            @Override
            public int read() throws IOException {
                if (hasBeenRead) {
                    throw new IOException("Stream already consumed");
                }
                hasBeenRead = true;
                return -1;
            }

            @Override
            public boolean markSupported() {
                return false;
            }
        };

        ContentStreamProvider provider = () -> nonRepeatableStream;
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        // First write should succeed
        entity.writeTo(new ByteArrayOutputStream());

        // Second write should fail
        assertThrows(IOException.class, () -> entity.writeTo(new ByteArrayOutputStream()));
    }

    @Test
    @DisplayName("Entity should handle partial stream reads")
    void writeTo_PartialReads_CompletesSuccessfully() throws IOException {
        // Given - Stream that returns data in small chunks
        String content = "This is a test content that will be read in chunks";
        InputStream chunkingStream = new InputStream() {
            private final byte[] data = content.getBytes();
            private int position = 0;

            @Override
            public int read() {
                if (position >= data.length) {
                    return -1;
                }
                int i = data[position] & 0xFF;
                position++;
                return i;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (position >= data.length) {
                    return -1;
                }
                // Return only 5 bytes at a time to simulate chunked reading
                int bytesToRead = Math.min(5, Math.min(len, data.length - position));
                System.arraycopy(data, position, b, off, bytesToRead);
                position += bytesToRead;
                return bytesToRead;
            }

            @Override
            public boolean markSupported() {
                return false;
            }
        };

        ContentStreamProvider provider = () -> chunkingStream;
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        entity.writeTo(output);


        assertEquals(content, output.toString());
    }

    @Test
    @DisplayName("Entity should handle mark/reset with limited buffer")
    void writeTo_MarkResetWithLimitedBuffer_HandlesCorrectly() throws IOException {
        // Given - Stream with limited mark buffer
        String content = "Short content";
        InputStream limitedMarkStream = new InputStream() {
            private final ByteArrayInputStream delegate = new ByteArrayInputStream(content.getBytes());

            @Override
            public int read() throws IOException {
                return delegate.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return delegate.read(b, off, len);
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public synchronized void mark(int readlimit) {
                delegate.mark(5); // Very small buffer
            }

            @Override
            public synchronized void reset() throws IOException {
                delegate.reset();
            }
        };

        ContentStreamProvider provider = () -> limitedMarkStream;
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        //Multiple writes
        ByteArrayOutputStream output1 = new ByteArrayOutputStream();
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        entity.writeTo(output1);
        entity.writeTo(output2);


        assertEquals(content, output1.toString());
        assertEquals(content, output2.toString());
    }

    @Test
    @DisplayName("Entity should handle null content type gracefully")
    void constructor_WithoutContentType_HandlesGracefully() {
        // Given
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader("Content-Length", "100")
            // No Content-Type header
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();


        entity = new RepeatableInputStreamRequestEntity(request);


        assertNull(entity.getContentType());
        assertEquals(100L, entity.getContentLength());
    }

    @Test
    @DisplayName("Entity should handle concurrent write attempts")
    void writeTo_ConcurrentWrites_HandlesCorrectly() throws Exception {
        // Given
        String content = "Concurrent test content";
        ContentStreamProvider provider = () -> new ByteArrayInputStream(content.getBytes());
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        // Simulate concurrent writes
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<ByteArrayOutputStream> outputs = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    entity.writeTo(output);
                    outputs.add(output);
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);

        // At least one should succeed, others may fail due to stream state
        assertFalse(outputs.isEmpty(), "At least one write should succeed");
        for (ByteArrayOutputStream output : outputs) {
            if (output.size() > 0) {
                assertEquals(content, output.toString());
            }
        }
    }

    @Test
    @DisplayName("Entity should handle interrupted IO operations")
    void writeTo_InterruptedStream_ThrowsIOException() throws IOException {
        // Given
        InputStream interruptibleStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new InterruptedIOException("Stream interrupted");
            }

            @Override
            public boolean markSupported() {
                return true;
            }
        };

        ContentStreamProvider provider = () -> interruptibleStream;
        SdkHttpRequest httpRequest = httpRequestBuilder.build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        IOException thrown = assertThrows(IOException.class,
                                          () -> entity.writeTo(new ByteArrayOutputStream()));
        assertInstanceOf(InterruptedIOException.class, thrown);
        assertEquals("Stream interrupted", thrown.getMessage());
    }

    @Test
    @DisplayName("Entity should preserve state across multiple operations")
    void multipleOperations_StatePreservation_WorksCorrectly() throws IOException {
        // Given
        String content = "State preservation test";
        ContentStreamProvider provider = () -> new ByteArrayInputStream(content.getBytes());
        SdkHttpRequest httpRequest = httpRequestBuilder
            .putHeader("Content-Length", String.valueOf(content.length()))
            .putHeader("Content-Type", "text/plain")
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .contentStreamProvider(provider)
                                                       .build();

        entity = new RepeatableInputStreamRequestEntity(request);

        // Perform multiple operations
        boolean isRepeatable1 = entity.isRepeatable();
        boolean isChunked1 = entity.isChunked();
        long contentLength1 = entity.getContentLength();

        // Write once
        entity.writeTo(new ByteArrayOutputStream());

        boolean isRepeatable2 = entity.isRepeatable();
        boolean isChunked2 = entity.isChunked();
        long contentLength2 = entity.getContentLength();

        // Write again
        entity.writeTo(new ByteArrayOutputStream());

        boolean isRepeatable3 = entity.isRepeatable();
        boolean isChunked3 = entity.isChunked();
        long contentLength3 = entity.getContentLength();

        //State should remain consistent
        assertEquals(isRepeatable1, isRepeatable2);
        assertEquals(isRepeatable2, isRepeatable3);
        assertEquals(isChunked1, isChunked2);
        assertEquals(isChunked2, isChunked3);
        assertEquals(contentLength1, contentLength2);
        assertEquals(contentLength2, contentLength3);
    }
}

