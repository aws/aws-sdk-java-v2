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

package software.amazon.awssdk.core.sync;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Interface for processing a streaming response from a service in a synchronous fashion. This interfaces gives
 * access to the unmarshalled response POJO which may contain metadata about the streamed contents. It also provides access
 * to the content via an {@link AbortableInputStream}. Callers do not need to worry about calling {@link InputStream#close()}
 * on the content, but if they wish to stop reading data from the stream {@link AbortableInputStream#abort()} may be called
 * to kill the underlying HTTP connection. This is generally not recommended and should only be done when the cost of reading
 * the rest of the data exceeds the cost of establishing a new connection. If callers do not call abort and do not read all
 * of the data in the stream, then the content will be drained by the SDK and the underlying HTTP connection will be returned to
 * the connection pool (if applicable).
 *
 * <p>
 * <h3>Retries</h3>
 * Exceptions thrown from the transformer's {@link #apply(Object, AbortableInputStream)} method are not automatically retried by
 * the RetryPolicy of the client. Since we can't know if a transformer implementation is idempotent or safe to retry, if you wish
 * to retry on the event of a failure you must throw a {@link SdkException} with retryable set to true from the transformer. This
 * exception can wrap the original exception that was thrown. Note that throwing a {@link
 * SdkException} that is marked retryable from the transformer does not guarantee the request will be retried,
 * retries are still limited by the max retry attempts and retry throttling
 * feature of the {@link RetryPolicy}.
 * </p>
 *
 * <p>
 * <h3>Thread Interrupts</h3>
 * Implementations should have proper handling of Thread interrupts. For long running, non-interruptible tasks, it is recommended
 * to check the thread interrupt status periodically and throw an {@link InterruptedException} if set. When an {@link
 * InterruptedException} is thrown from a interruptible task, you should either re-interrupt the current thread or throw that
 * {@link InterruptedException} from the {@link #apply(Object, AbortableInputStream)} method. Failure to do these things will
 * prevent the total execution timeout from working (see {@link ClientOverrideConfiguration#totalExecutionTimeout()} and may
 * prevent the SDK from stopping the request in a timely manner in the event the thread is interrupted externally.
 * </p>
 *
 * @param <ResponseT> Type of unmarshalled POJO response.
 * @param <ReturnT>   Return type of the {@link #apply(Object, AbortableInputStream)} method. Implementations are free to perform
 *                    whatever transformations are appropriate.
 */
@FunctionalInterface
public interface ResponseTransformer<ResponseT, ReturnT> {
    /**
     * Process the response contents.
     *
     * @param response    Unmarshalled POJO response
     * @param inputStream Input stream of streamed data.
     * @return Transformed type.
     * @throws Exception if any error occurs during processing of the response. This will be re-thrown by the SDK, possibly
     *                   wrapped in an {@link SdkClientException}.
     */
    ReturnT apply(ResponseT response, AbortableInputStream inputStream) throws Exception;

    /**
     * Hook to allow connection to be left open after the SDK returns a response. Useful for returning the InputStream to
     * the response content from the transformer.
     *
     * @return True if connection (and InputStream) should be left open after the SDK returns a response, false otherwise.
     */
    default boolean needsConnectionLeftOpen() {
        return false;
    }

    /**
     * Creates a response transformer that writes all response content to the specified file. If the file already exists
     * then a {@link java.nio.file.FileAlreadyExistsException} will be thrown.
     *
     * @param path        Path to file to write to.
     * @param <ResponseT> Type of unmarshalled response POJO.
     * @return ResponseTransformer instance.
     */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseT> toFile(Path path) {
        return (resp, in) -> {
            try {
                Files.copy(in, path);
                return resp;
            } catch (IOException copyException) {
                String copyError = "Failed to read response into file: " + path;

                // If the write failed because of the state of the file, don't retry the request.
                if (copyException instanceof FileAlreadyExistsException || copyException instanceof DirectoryNotEmptyException) {
                    throw new IOException(copyError, copyException);
                }

                // Try to clean up the file so that we can retry the request. If we can't delete it, don't retry the request.
                try {
                    Files.deleteIfExists(path);
                } catch (IOException deletionException) {
                    Logger.loggerFor(ResponseTransformer.class)
                          .error(() -> "Failed to delete destination file '" + path +
                                       "' after reading the service response " +
                                       "failed.", deletionException);

                    throw new IOException(copyError + ". Additionally, the file could not be cleaned up (" +
                                          deletionException.getMessage() + "), so the request will not be retried.",
                                          copyException);
                }

                // Retry the request
                throw new RetryableException(copyError, copyException);
            }
        };
    }

    /**
     * Creates a response transformer that writes all response content to the specified file. If the file already exists
     * then a {@link java.nio.file.FileAlreadyExistsException} will be thrown.
     *
     * @param file        File to write to.
     * @param <ResponseT> Type of unmarshalled response POJO.
     * @return ResponseTransformer instance.
     */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseT> toFile(File file) {
        return toFile(file.toPath());
    }

    /**
     * Creates a response transformer that writes all response content to the given {@link OutputStream}. Note that
     * the {@link OutputStream} is not closed or flushed after writing.
     *
     * @param outputStream Output stream to write data to.
     * @param <ResponseT>  Type of unmarshalled response POJO.
     * @return ResponseTransformer instance.
     */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseT> toOutputStream(OutputStream outputStream) {
        return (resp, in) -> {
            IoUtils.copy(in, outputStream);
            return resp;
        };
    }

    /**
     * Creates a response transformer that loads all response content into memory, exposed as {@link ResponseBytes}. This allows
     * for conversion into a {@link String}, {@link ByteBuffer}, etc.
     *
     * @param <ResponseT> Type of unmarshalled response POJO.
     * @return The streaming response transformer that can be used on the client streaming method.
     */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseBytes<ResponseT>> toBytes() {
        return (response, inputStream) -> {
            try {
                return new ResponseBytes<>(response, IoUtils.toByteArray(inputStream));
            } catch (IOException e) {
                throw new RetryableException("Failed to read response.", e);
            }
        };
    }

    /**
     * Creates a response transformer that returns an unmanaged input stream with the response content. This input stream must
     * be explicitly closed to release the connection. The unmarshalled response object can be obtained via the {@link
     * ResponseInputStream#response} method.
     *
     * <p>
     * Note that the returned stream is not subject to the retry policy or timeout settings (except for socket timeout)
     * of the client. No retries will be performed in the event of a socket read failure or connection reset. Similarly,
     * the total execution timeout (see
     * {@link software.amazon.awssdk.core.config.ClientOverrideConfiguration#totalExecutionTimeout})
     * will stop once the input stream has been returned by the SDK.
     * </p>
     *
     * @param <ResponseT> Type of unmarshalled response POJO.
     * @return ResponseTransformer instance.
     */
    static <ResponseT> ResponseTransformer<ResponseT, ResponseInputStream<ResponseT>> toInputStream() {
        return unmanaged(ResponseInputStream::new);
    }

    /**
     * Static helper method to create a response transformer that allows the connection to be left open. Useful for creating a
     * {@link ResponseTransformer} with a lambda or method reference rather than an anonymous inner class.
     *
     * @param transformer     Transformer to wrap.
     * @param <ResponseT> Type of unmarshalled response POJO.
     * @param <ReturnT>   Return type of transformer.
     * @return New {@link ResponseTransformer} which does not close the connection afterwards.
     */
    static <ResponseT, ReturnT> ResponseTransformer<ResponseT, ReturnT> unmanaged(
            ResponseTransformer<ResponseT, ReturnT> transformer) {
        return new ResponseTransformer<ResponseT, ReturnT>() {
            @Override
            public ReturnT apply(ResponseT response, AbortableInputStream inputStream) throws Exception {
                return transformer.apply(response, inputStream);
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return true;
            }
        };

    }
}
