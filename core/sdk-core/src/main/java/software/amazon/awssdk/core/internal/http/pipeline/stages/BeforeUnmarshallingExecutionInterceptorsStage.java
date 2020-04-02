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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Pair;

/**
 * Invoke the {@link ExecutionInterceptor#beforeUnmarshalling} callback to allow for pre-processing on the {@link SdkHttpResponse}
 * before it is handed off to the unmarshaller.
 */
@SdkInternalApi
public class BeforeUnmarshallingExecutionInterceptorsStage
    implements RequestPipeline<Pair<SdkHttpFullRequest, SdkHttpFullResponse>, SdkHttpFullResponse> {

    @Override
    public SdkHttpFullResponse execute(Pair<SdkHttpFullRequest, SdkHttpFullResponse> input,
                                RequestExecutionContext context) throws Exception {
        context.interceptorChain().beforeUnmarshalling(context.executionContext().interceptorContext(),
                                                       context.executionAttributes());
        InterruptMonitor.checkInterrupted(input.right());
        return input.right();
    }
}
