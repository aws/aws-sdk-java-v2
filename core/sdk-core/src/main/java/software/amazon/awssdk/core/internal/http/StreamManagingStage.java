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

package software.amazon.awssdk.core.internal.http;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.io.ReleasableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Logger;

/**
 * Adds additional wrapping around the request {@link ContentStreamProvider}.
 * <p>
 * Currently, it ensures that the stream returned by the provider is not closeable.
 *
 * @param <OutputT> Type of unmarshalled response
 */
@SdkInternalApi
public final class StreamManagingStage<OutputT> implements RequestPipeline<SdkHttpFullRequest, Response<OutputT>> {

    private static final Logger log = Logger.loggerFor(StreamManagingStage.class);

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;

    public StreamManagingStage(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        ClosingStreamProvider toBeClosed = null;
        if (request.contentStreamProvider().isPresent()) {
            toBeClosed = createManagedProvider(request.contentStreamProvider().get());
            request = request.toBuilder().contentStreamProvider(toBeClosed).build();
        }
        try {
            InterruptMonitor.checkInterrupted();
            return wrapped.execute(request, context);
        } finally {
            if (toBeClosed != null) {
                toBeClosed.closeCurrentStream();
            }
        }
    }

    private static ClosingStreamProvider createManagedProvider(ContentStreamProvider contentStreamProvider) {
        return new ClosingStreamProvider(contentStreamProvider);
    }

    private static class ClosingStreamProvider implements ContentStreamProvider {
        private final ContentStreamProvider wrapped;
        private InputStream currentStream;

        ClosingStreamProvider(ContentStreamProvider wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public InputStream newStream() {
            currentStream = wrapped.newStream();
            return ReleasableInputStream.wrap(currentStream).disableClose();
        }

        void closeCurrentStream() {
            if (currentStream != null) {
                invokeSafely(currentStream::close);
                currentStream = null;
            }
        }
    }
}
