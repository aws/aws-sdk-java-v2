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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;
import static software.amazon.awssdk.core.internal.util.ProgressListenerTestUtils.createSdkHttpRequest;
import static software.amazon.awssdk.core.internal.util.ProgressListenerTestUtils.progressListenerContext;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.http.NoopHttpFullRequest;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.progress.listener.ProgressUpdater;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.http.SdkHttpFullRequest;

public class ExecutionFailureExceptionReportingStageTest {


    @Test
    public void afterExecutionProgressListener_calledFrom_ExecutionPipeline() throws Exception {

        RequestPipeline<SdkHttpFullRequest, Response<String>> requestPipeline = Mockito.mock(RequestPipeline.class);
        ProgressListener progressListener = Mockito.mock(ProgressListener.class);

        SdkRequestOverrideConfiguration config = SdkRequestOverrideConfiguration.builder()
                                                                                .addProgressListener(progressListener)
                                                                                .build();

        SdkRequest request = createSdkHttpRequest(config).build();
        ProgressUpdater progressUpdater = new ProgressUpdater(request, null);
        RequestExecutionContext requestExecutionContext = progressListenerContext(false, request,
                                                                                  progressUpdater);

        ExecutionFailureExceptionReportingStage executionFailureExceptionReportingStage = new ExecutionFailureExceptionReportingStage(requestPipeline);
        when(requestPipeline.execute(any(), any())).thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () -> executionFailureExceptionReportingStage.execute(new NoopHttpFullRequest(), requestExecutionContext));

        Mockito.verify(progressListener, Mockito.times(0)).requestPrepared(any());
        Mockito.verify(progressListener, Mockito.times(0)).requestBytesSent(any());
        Mockito.verify(progressListener, Mockito.times(0)).responseHeaderReceived(any());
        Mockito.verify(progressListener, Mockito.times(0)).responseBytesReceived(any());
        Mockito.verify(progressListener, Mockito.times(0)).executionSuccess(any());
        Mockito.verify(progressListener, Mockito.times(1)).attemptFailure(any());
    }
}
