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

package software.amazon.awssdk.core.async;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncRequestBody;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;
import software.amazon.awssdk.core.internal.async.InputStreamWithExecutorAsyncRequestBody;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Interface to allow non-blocking streaming of request content. This follows the reactive streams pattern where
 * this interface is the {@link Publisher} of data (specifically {@link ByteBuffer} chunks) and the HTTP client is the Subscriber
 * of the data (i.e. to write that data on the wire).
 *
 * <p>
 * {@link #subscribe(Subscriber)} should be implemented to tie this publisher to a subscriber. Ideally each call to subscribe
 * should reproduce the content (i.e if you are reading from a file each subscribe call should produce a {@link
 * org.reactivestreams.Subscription} that reads the file fully). This allows for automatic retries to be performed in the SDK. If
 * the content is not reproducible,  an exception may be thrown from any subsequent {@link #subscribe(Subscriber)} calls.
 * </p>
 *
 * <p>
 * It is important to only send the number of chunks that the subscriber requests to avoid out of memory situations.
 * The subscriber does it's own buffering so it's usually not needed to buffer in the publisher. Additional permits
 * for chunks will be notified via the {@link org.reactivestreams.Subscription#request(long)} method.
 * </p>
 *
 * @see FileAsyncRequestBody
 * @see ByteArrayAsyncRequestBody
 */
@SdkPublicApi
public interface AsyncRequestBody extends SdkPublisher<ByteBuffer> {

    /**
     * @return The content length of the data being produced.
     */
    Optional<Long> contentLength();

    /**
     * @return The content type of the data being produced.
     */
    default String contentType() {
        return Mimetype.MIMETYPE_OCTET_STREAM;
    }

    /**
     * Creates an {@link AsyncRequestBody} the produces data from the input ByteBuffer publisher.
     * The data is delivered when the publisher publishes the data.
     *
     * @param publisher Publisher of source data
     * @return Implementation of {@link AsyncRequestBody} that produces data send by the publisher
     */
    static AsyncRequestBody fromPublisher(Publisher<ByteBuffer> publisher) {
        return new AsyncRequestBody() {

            /**
             * Returns empty optional as size of the each bytebuffer sent is unknown
             */
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                publisher.subscribe(s);
            }
        };
    }

    /**
     * Creates an {@link AsyncRequestBody} that produces data from the contents of a file. See
     * {@link FileAsyncRequestBody#builder} to create a customized body implementation.
     *
     * @param path Path to file to read from.
     * @return Implementation of {@link AsyncRequestBody} that reads data from the specified file.
     * @see FileAsyncRequestBody
     */
    static AsyncRequestBody fromFile(Path path) {
        return FileAsyncRequestBody.builder().path(path).build();
    }

    /**
     * Creates an {@link AsyncRequestBody} that produces data from the contents of a file. See
     * {@link FileAsyncRequestBody#builder} to create a customized body implementation.
     *
     * @param file The file to read from.
     * @return Implementation of {@link AsyncRequestBody} that reads data from the specified file.
     * @see FileAsyncRequestBody
     */
    static AsyncRequestBody fromFile(File file) {
        return FileAsyncRequestBody.builder().path(file.toPath()).build();
    }

    /**
     * Creates an {@link AsyncRequestBody} that uses a single string as data.
     *
     * @param string The string to provide.
     * @param cs The {@link Charset} to use.
     * @return Implementation of {@link AsyncRequestBody} that uses the specified string.
     * @see ByteArrayAsyncRequestBody
     */
    static AsyncRequestBody fromString(String string, Charset cs) {
        return new ByteArrayAsyncRequestBody(string.getBytes(cs),
                                             Mimetype.MIMETYPE_TEXT_PLAIN + "; charset=" + cs.name());
    }

    /**
     * Creates an {@link AsyncRequestBody} that uses a single string as data with UTF_8 encoding.
     *
     * @param string The string to send.
     * @return Implementation of {@link AsyncRequestBody} that uses the specified string.
     * @see #fromString(String, Charset)
     */
    static AsyncRequestBody fromString(String string) {
        return fromString(string, StandardCharsets.UTF_8);
    }

    /**
     * Creates a {@link AsyncRequestBody} from a byte array. The contents of the byte array are copied so modifications to the
     * original byte array are not reflected in the {@link AsyncRequestBody}.
     *
     * @param bytes The bytes to send to the service.
     * @return AsyncRequestBody instance.
     */
    static AsyncRequestBody fromBytes(byte[] bytes) {
        return new ByteArrayAsyncRequestBody(bytes, Mimetype.MIMETYPE_OCTET_STREAM);
    }

    /**
     * Creates a {@link AsyncRequestBody} from a {@link ByteBuffer}. Buffer contents are copied so any modifications
     * made to the original {@link ByteBuffer} are not reflected in the {@link AsyncRequestBody}.
     *
     * @param byteBuffer ByteBuffer to send to the service.
     * @return AsyncRequestBody instance.
     */
    static AsyncRequestBody fromByteBuffer(ByteBuffer byteBuffer) {
        return fromBytes(BinaryUtils.copyAllBytesFrom(byteBuffer));
    }

    /**
     * Creates a {@link AsyncRequestBody} from a {@link InputStream}.
     *
     * <p>An {@link ExecutorService} is required in order to perform the blocking data reads, to prevent blocking the
     * non-blocking event loop threads owned by the SDK.
     */
    static AsyncRequestBody fromInputStream(InputStream inputStream, Long contentLength, ExecutorService executor) {
        return new InputStreamWithExecutorAsyncRequestBody(inputStream, contentLength, executor);
    }

    /**
     * Creates a {@link BlockingInputStreamAsyncRequestBody} to use for writing an input stream to the downstream service.
     *
     * <p><b>Example Usage</b>
     *
     * <p>
     * {@snippet :
     *     S3AsyncClient s3 = S3AsyncClient.create(); // Use one client for your whole application!
     *
     *     byte[] dataToSend = "Hello".getBytes(StandardCharsets.UTF_8);
     *     InputStream streamToSend = new ByteArrayInputStream();
     *     long streamToSendLength = dataToSend.length();
     *
     *     // Start the operation
     *     BlockingInputStreamAsyncRequestBody body =
     *         AsyncRequestBody.forBlockingInputStream(streamToSendLength);
     *     CompletableFuture<PutObjectResponse> responseFuture =
     *         s3.putObject(r -> r.bucket("bucketName").key("key"), body);
     *
     *     // Write the input stream to the running operation
     *     body.writeInputStream(streamToSend);
     *
     *     // Wait for the service to respond.
     *     PutObjectResponse response = responseFuture.join();
     * }
     */
    static BlockingInputStreamAsyncRequestBody forBlockingInputStream(Long contentLength) {
        return new BlockingInputStreamAsyncRequestBody(contentLength);
    }

    /**
     * Creates a {@link BlockingOutputStreamAsyncRequestBody} to use for writing to the downstream service as if it's an output
     * stream. Retries are not supported for this request body.
     *
     * <p>The caller is responsible for calling {@link OutputStream#close()} on the {@link #outputStream()} when writing is
     * complete.
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     S3AsyncClient s3 = S3AsyncClient.create(); // Use one client for your whole application!
     *
     *     byte[] dataToSend = "Hello".getBytes(StandardCharsets.UTF_8);
     *     long lengthOfDataToSend = dataToSend.length();
     *
     *     // Start the operation
     *     BlockingInputStreamAsyncRequestBody body =
     *         AsyncRequestBody.forBlockingOutputStream(lengthOfDataToSend);
     *     CompletableFuture<PutObjectResponse> responseFuture =
     *         s3.putObject(r -> r.bucket("bucketName").key("key"), body);
     *
     *     // Write the input stream to the running operation
     *     try (CancellableOutputStream outputStream = body.outputStream()) {
     *         outputStream.write(dataToSend);
     *     }
     *
     *     // Wait for the service to respond.
     *     PutObjectResponse response = responseFuture.join();
     * }
     */
    static BlockingOutputStreamAsyncRequestBody forBlockingOutputStream(Long contentLength) {
        return new BlockingOutputStreamAsyncRequestBody(contentLength);
    }

    /**
     * Creates a {@link AsyncRequestBody} with no content.
     *
     * @return AsyncRequestBody instance.
     */
    static AsyncRequestBody empty() {
        return fromBytes(new byte[0]);
    }
}
