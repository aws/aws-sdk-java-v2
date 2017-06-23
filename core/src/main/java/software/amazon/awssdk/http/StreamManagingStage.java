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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.event.SdkProgressPublisher.publishProgress;
import static software.amazon.awssdk.http.AmazonHttpClient.unreliableTestConfig;
import static software.amazon.awssdk.utils.IoUtils.closeQuietly;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressInputStream;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.runtime.io.ReleasableInputStream;
import software.amazon.awssdk.runtime.io.ResettableInputStream;
import software.amazon.awssdk.runtime.io.SdkBufferedInputStream;
import software.amazon.awssdk.util.UnreliableFilterInputStream;

/**
 * Instruments the request input stream for both retry purposes (to allow for mark/reset) and metrics/progress reporting. Handles
 * closing the input stream when the request completes.
 *
 * @param <OutputT> Type of unmarshalled response
 */
public class StreamManagingStage<OutputT> implements RequestPipeline<SdkHttpFullRequest, Response<OutputT>> {

    private static final Log log = LogFactory.getLog(StreamManagingStage.class);

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;

    public StreamManagingStage(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        final InputStream toBeClosed = createManagedStream(request, context.requestConfig());
        try {
            ProgressListener listener = context.requestConfig().getProgressListener();
            publishProgress(listener, ProgressEventType.CLIENT_REQUEST_STARTED_EVENT);
            Response<OutputT> response = wrapped.execute(
                    request.toBuilder()
                           .content(nonCloseableInputStream(toBeClosed))
                           .build(), context);
            publishProgress(listener, ProgressEventType.CLIENT_REQUEST_SUCCESS_EVENT);
            context.awsRequestMetrics().getTimingInfo().endTiming();
            return response;
        } finally {
            // Always close so any progress tracking would get the final events propagated.
            closeQuietly(toBeClosed, log);
        }
    }

    /**
     * Disables close on the input stream so reset will work on retries.
     *
     * @param toBeClosed Input stream to disable close on.
     * @return An input stream with close disabled or null if toBeClosed is null.
     */
    private InputStream nonCloseableInputStream(InputStream toBeClosed) {
        return toBeClosed == null ? null : ReleasableInputStream.wrap(toBeClosed).disableClose();
    }

    /**
     * Wraps the request input stream in several wrappers to handle both mark/retry behavior needed for retries
     * and reporting needs via a {@link ProgressListener}. Also will inject faulty behavior if an {@link UnreliableTestConfig} is
     * provided.
     *
     * @return Modified input stream to use for the remainder of the execution.
     */
    private InputStream createManagedStream(SdkHttpFullRequest request, RequestConfig requestConfig) {
        if (request.getContent() == null) {
            return null;
        }
        final InputStream content = monitorStreamProgress(requestConfig.getProgressListener(),
                                                          bufferIfNeeded(makeResettable(request.getContent())));

        return unreliableTestConfig == null ? content : wrapWithUnreliableStream(content);
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
     * Wrap with a {@link ProgressInputStream} to report request progress to listener.
     *
     * @param listener Listener to report to
     * @param content  Input stream to monitor progress for
     * @return Wrapped input stream with progress monitoring capabilities.
     */
    private InputStream monitorStreamProgress(ProgressListener listener,
                                              InputStream content) {
        return ProgressInputStream.inputStreamForRequest(content, listener);
    }

    /**
     * Used only for internal testing purposes. Makes a stream unreliable in certain ways for
     * fault testing.
     *
     * @param content Input stream to make unreliable.
     * @return UnreliableFilterInputStream
     */
    private InputStream wrapWithUnreliableStream(InputStream content) {
        return new UnreliableFilterInputStream(content, unreliableTestConfig.isFakeIoException())
                .withBytesReadBeforeException(unreliableTestConfig.getBytesReadBeforeException())
                .withMaxNumErrors(unreliableTestConfig.getMaxNumErrors())
                .withResetIntervalBeforeException(unreliableTestConfig.getResetIntervalBeforeException());
    }

}
