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

package software.amazon.awssdk.http.pipeline.stages;

import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.utils.Pair;

/**
 * Invoke the {@link RequestHandler#beforeUnmarshalling(software.amazon.awssdk.http.SdkHttpFullRequest, HttpResponse)} callback
 * to allow for pre-processing on the {@link HttpResponse} before it is handed off to the unmarshaller.
 */
public class BeforeUnmarshallingCallbackStage
        implements RequestPipeline<Pair<SdkHttpFullRequest, HttpResponse>, HttpResponse> {

    @Override
    public HttpResponse execute(Pair<SdkHttpFullRequest, HttpResponse> input,
                                RequestExecutionContext context) throws Exception {
        // TODO we should consider invoking beforeUnmarshalling regardless of success or error.
        HttpResponse toReturn = input.right();
        if (!toReturn.isSuccessful()) {
            return toReturn;
        }
        for (RequestHandler requestHandler : context.requestHandlers()) {
            toReturn = requestHandler.beforeUnmarshalling(input.left(), toReturn);
        }
        return toReturn;
    }
}
