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

package software.amazon.awssdk.core.internal.http;

import static software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient.unreliableTestConfig;
import static software.amazon.awssdk.utils.IoUtils.closeQuietly;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.io.ReleasableInputStream;
import software.amazon.awssdk.core.io.ResettableInputStream;
import software.amazon.awssdk.core.io.SdkBufferedInputStream;
import software.amazon.awssdk.core.util.UnreliableFilterInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Instruments the request input stream for both retry purposes (to allow for mark/reset) and progress reporting. Handles
 * closing the input stream when the request completes.
 *
 * @param <OutputT> Type of unmarshalled response
 */
@SdkInternalApi
public final class StreamManagingStage<OutputT> implements RequestPipeline<SdkHttpFullRequest, Response<OutputT>> {

    private static final Logger log = LoggerFactory.getLogger(StreamManagingStage.class);

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;

    public StreamManagingStage(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        Optional<InputStream> toBeClosed = createManagedStream(request);
        try {
            return wrapped.execute(request.toBuilder().content(nonCloseableInputStream(toBeClosed).orElse(null)).build(),
                                   context);
        } finally {
            // Always close so any progress tracking would get the final events propagated.
            toBeClosed.ifPresent(i -> closeQuietly(i, log));
        }
    }

    /**
     * Disables close on the input stream so reset will work on retries.
     *
     * @param toBeClosed Input stream to disable close on.
     * @return An input stream with close disabled or null if toBeClosed is null.
     */
    private Optional<InputStream> nonCloseableInputStream(Optional<InputStream> toBeClosed) {
        return toBeClosed.map(is -> ReleasableInputStream.wrap(is).disableClose());
    }

    /**
     * Wraps the request input stream in several wrappers to handle both mark/retry behavior needed for retries.
     * Also will inject faulty behavior if an {@link UnreliableTestConfig} is provided.
     *
     * @return Modified input stream to use for the remainder of the execution.
     */
    private Optional<InputStream> createManagedStream(SdkHttpFullRequest request) {
        return request.content()
                      .map(this::makeResettable)
                      .map(this::bufferIfNeeded)
                      .map(content -> unreliableTestConfig == null ? content : makeUnreliable(content));
    }

    /**
     * Make input stream resettable if possible.
     *
     * @param content Input stream to make resettable
     * @return ResettableInputStream if possible otherwise original input stream.
     */
    private InputStream makeResettable(InputStream content) {
        if (!content.markSupported() && content instanceof FileInputStream) {
            try {
                // ResettableInputStream supports mark-and-reset without memory buffering
                return new ResettableInputStream((FileInputStream) content);
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug("For the record; ignore otherwise", e);
                }
            }
        }
        return content;
    }

    /**
     * Buffer input stream if needed. We buffer to be able to reset a non-resettable stream.
     *
     * @param content Input stream to buffer
     * @return SdkBufferedInputStream if needed, otherwise original input stream.
     */
    private InputStream bufferIfNeeded(InputStream content) {
        return content.markSupported() ? content : new SdkBufferedInputStream(content);
    }

    /**
     * Used only for internal testing purposes. Makes a stream unreliable in certain ways for
     * fault testing.
     *
     * @param content Input stream to make unreliable.
     * @return UnreliableFilterInputStream
     */
    private InputStream makeUnreliable(InputStream content) {
        return new UnreliableFilterInputStream(content, unreliableTestConfig.isFakeIoException())
                .withBytesReadBeforeException(unreliableTestConfig.getBytesReadBeforeException())
                .withMaxNumErrors(unreliableTestConfig.getMaxNumErrors())
                .withResetIntervalBeforeException(unreliableTestConfig.getResetIntervalBeforeException());
    }

}
