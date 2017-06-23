/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.sync;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.utils.IoUtils;

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
 * @param <ResponseT> Type of unmarshalled POJO response.
 * @param <ReturnT>   Return type of the {@link #apply(Object, AbortableInputStream)} method. Implementations are free to perform
 *                    whatever transformations are appropriate.
 */
@FunctionalInterface
public interface StreamingResponseHandler<ResponseT, ReturnT> {

    /**
     * Process the response contents.
     *
     * @param response    Unmarshalled POJO response
     * @param inputStream Input stream of streamed data.
     * @return Transformed type.
     * @throws Exception if any error occurs during processing of the response. This will be re-thrown by the SDK, possibly
     *                   wrapped in an {@link software.amazon.awssdk.SdkClientException}.
     */
    ReturnT apply(ResponseT response, AbortableInputStream inputStream) throws Exception;

    /**
     * Creates a response handler that writes all response content to the specified file. If the file already exists
     * then a {@link java.nio.file.FileAlreadyExistsException} will be thrown.
     *
     * @param path        Path to file to write to.
     * @param <ResponseT> Type of unmarshalled response POJO. Ignored by handler.
     * @return Null.
     */
    static <ResponseT> StreamingResponseHandler<ResponseT, Void> toFile(Path path) {
        return (resp, in) -> {
            Files.copy(in, path);
            return null;
        };
    }

    /**
     * Creates a response handler that writes all response content to the given {@link OutputStream}. Note that
     * the {@link OutputStream} is not closed or flushed after writing.
     *
     * @param outputStream Output stream to write data to.
     * @param <ResponseT>  Type of unmarshalled response POJO. Ignored by handler.
     * @return Null.
     */
    static <ResponseT> StreamingResponseHandler<ResponseT, Void> toOutputStream(OutputStream outputStream) {
        return (resp, in) -> {
            IoUtils.copy(in, outputStream);
            return null;
        };
    }
}
