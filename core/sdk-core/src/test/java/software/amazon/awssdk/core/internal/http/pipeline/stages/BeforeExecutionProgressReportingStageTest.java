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

import static software.amazon.awssdk.core.internal.util.ProgressListenerTestUtils.createHttpRequestBuilder;
import static software.amazon.awssdk.core.internal.util.ProgressListenerTestUtils.createSdkHttpRequest;
import static software.amazon.awssdk.core.internal.util.ProgressListenerTestUtils.progressListenerContext;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.progress.listener.DefaultProgressUpdater;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.http.SdkHttpFullRequest;

class BeforeExecutionProgressReportingStageTest {
    @Test
    void beforeExecutionProgressListener_calledFrom_ExecutionPipeline() throws Exception {
        ProgressListener progressListener = Mockito.mock(ProgressListener.class);

        SdkRequestOverrideConfiguration config = SdkRequestOverrideConfiguration.builder()
                                                                                .addProgressListener(progressListener)
                                                                                .build();

        SdkHttpFullRequest requestBuilder = createHttpRequestBuilder().build();

        SdkRequest request = createSdkHttpRequest(config).build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(request, null);

        RequestExecutionContext requestExecutionContext = progressListenerContext(false, request,
                                                                                  defaultProgressUpdater);

        BeforeExecutionProgressReportingStage beforeExecutionUpdateProgressStage = new BeforeExecutionProgressReportingStage();
        beforeExecutionUpdateProgressStage.execute(requestBuilder, requestExecutionContext);

        Mockito.verify(progressListener, Mockito.times(1)).requestPrepared(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).requestBytesSent(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).responseHeaderReceived(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).responseBytesReceived(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).executionSuccess(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).executionFailure(Mockito.any());
        Mockito.verify(progressListener, Mockito.times(0)).attemptFailure(Mockito.any());

    }
}
