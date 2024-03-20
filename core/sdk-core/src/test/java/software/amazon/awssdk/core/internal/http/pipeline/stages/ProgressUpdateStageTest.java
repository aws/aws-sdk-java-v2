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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.progress.listener.ProgressUpdater;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

@RunWith(MockitoJUnitRunner.class)
public class ProgressUpdateStageTest {

    private static final AsyncRequestBody ASYNC_REQUEST_BODY = AsyncRequestBody.fromString("TestBody");
    private static final RequestBody REQUEST_BODY = RequestBody.fromString("TestBody");

    @Test
    public void sync_preExecutionProgressListener_calledFrom_ExecutionPipeline() throws Exception {
        ProgressListener progressListener = Mockito.mock(ProgressListener.class);

        SdkRequestOverrideConfiguration config = SdkRequestOverrideConfiguration.builder()
                                                                                .addProgressListener(progressListener)
                                                                                .build();

        SdkHttpFullRequest requestBuilder = createHttpRequestBuilder().build();

        SdkRequest request = createSdkHttpRequest(config).build();

        ProgressUpdater progressUpdater = new ProgressUpdater(request, null);
        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .progressUpdater(progressUpdater)
                                                            .build();

        RequestExecutionContext requestExecutionContext = progressListenerContext(false, request,
                                                                                  executionContext).build();

        PreExecutionUpdateProgressStage preExecutionUpdateProgressStage = new PreExecutionUpdateProgressStage();
        preExecutionUpdateProgressStage.execute(requestBuilder, requestExecutionContext);

        Mockito.verify(progressListener, Mockito.times(1)).requestPrepared(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).requestBytesSent(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).responseHeaderReceived(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).responseBytesReceived(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).executionSuccess(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).executionFailure(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).attemptFailure(Mockito.any());

    }

    @Test
    public void sync_postExecutionProgressListener_calledFrom_ExecutionPipeline() throws Exception {
        ProgressListener progressListener = Mockito.mock(ProgressListener.class);

        SdkRequestOverrideConfiguration config = SdkRequestOverrideConfiguration.builder()
                                                                                .addProgressListener(progressListener)
                                                                                .build();

        SdkRequest request = createSdkHttpRequest(config).build();

        ProgressUpdater progressUpdater = new ProgressUpdater(request, null);
        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .progressUpdater(progressUpdater)
                                                            .build();

        RequestExecutionContext requestExecutionContext = progressListenerContext(false, request,
                                                                                  executionContext).build();

        SdkResponse response = createSdkResponseBuilder().build();

        PostExecutionUpdateProgressStage postExecutionUpdateProgressStage = new PostExecutionUpdateProgressStage();
        postExecutionUpdateProgressStage.execute(response, requestExecutionContext);

        Mockito.verify(progressListener, Mockito.times(0)).requestPrepared(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).requestBytesSent(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).responseHeaderReceived(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).responseBytesReceived(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(1)).executionSuccess(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).executionFailure(Mockito.any());
    }


    private RequestExecutionContext.Builder progressListenerContext(boolean isAsyncStreaming, SdkRequest sdkRequest,
                                                                    ExecutionContext executionContext) {

        RequestExecutionContext.Builder builder =
            RequestExecutionContext.builder().executionContext(executionContext).
                                   originalRequest(sdkRequest);
        if (isAsyncStreaming) {
            builder.requestProvider(ASYNC_REQUEST_BODY);
        }

        return builder;
    }

    private SdkHttpFullRequest.Builder createHttpRequestBuilder() {
        return SdkHttpFullRequest.builder().uri(URI.create("https://endpoint.host"))
                                 .method(SdkHttpMethod.GET)
                                 .contentStreamProvider(REQUEST_BODY.contentStreamProvider());
    }

    private SdkResponse.Builder createSdkResponseBuilder() {
        return VoidSdkResponse.builder();
    }

    private SdkRequest.Builder createSdkHttpRequest(SdkRequestOverrideConfiguration config) {
        return NoopTestRequest.builder()
                       .overrideConfiguration(config);
    }
}
