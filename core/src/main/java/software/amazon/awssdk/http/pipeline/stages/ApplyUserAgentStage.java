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

import static software.amazon.awssdk.http.AmazonHttpClient.HEADER_USER_AGENT;

import software.amazon.awssdk.RequestClientOptions;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.util.RuntimeHttpUtils;

/**
 * Apply any custom user agent supplied, otherwise instrument the user agent with info about the SDK and environment.
 */
public class ApplyUserAgentStage implements MutableRequestToRequestPipeline {

    private final ClientConfiguration clientConfig;

    public ApplyUserAgentStage(HttpClientDependencies dependencies) {
        this.clientConfig = dependencies.clientConfiguration();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        RequestClientOptions opts = context.requestConfig().getRequestClientOptions();
        if (opts != null) {
            return request.header(HEADER_USER_AGENT, RuntimeHttpUtils
                    .getUserAgent(clientConfig, opts.getClientMarker(RequestClientOptions.Marker.USER_AGENT)));
        } else {
            return request.header(HEADER_USER_AGENT, RuntimeHttpUtils.getUserAgent(clientConfig, null));
        }
    }
}
