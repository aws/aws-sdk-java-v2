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
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.RequestToRequestPipeline;

/**
 * Runs the {@link RequestHandler#beforeRequest(SdkHttpFullRequest)} callback to pre-process the marshalled request before
 * making an HTTP call.
 */
public class BeforeRequestHandlersStage implements RequestToRequestPipeline {

    @Override
    public SdkHttpFullRequest execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        AwsCredentials credentials = context.credentialsProvider().getCredentials();
        SdkHttpFullRequest toReturn = request.toBuilder().handlerContext(AwsHandlerKeys.AWS_CREDENTIALS, credentials).build();
        // Apply any additional service specific request handlers that need to be run
        for (RequestHandler requestHandler : context.requestHandlers()) {
            toReturn = requestHandler.beforeRequest(toReturn);
        }
        return toReturn;
    }
}
