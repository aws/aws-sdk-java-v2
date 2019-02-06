/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.Response;
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
        if (request.contentStreamProvider().isPresent()) {
            request = request.toBuilder()
                             .contentStreamProvider(createManagedStream(request.contentStreamProvider().get())).build();
        }
        InterruptMonitor.checkInterrupted();
        return wrapped.execute(request, context);
    }

    private static ContentStreamProvider createManagedStream(ContentStreamProvider contentStreamProvider) {
        return () -> ReleasableInputStream.wrap(contentStreamProvider.newStream()).disableClose();
    }

}
