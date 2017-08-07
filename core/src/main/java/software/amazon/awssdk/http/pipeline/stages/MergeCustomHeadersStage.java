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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.MutableRequestToRequestPipeline;

/**
 * Merge customer supplied headers into the marshalled request.
 */
public class MergeCustomHeadersStage implements MutableRequestToRequestPipeline {

    private final ClientConfiguration config;

    public MergeCustomHeadersStage(HttpClientDependencies dependencies) {
        this.config = dependencies.clientConfiguration();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        return request.headers(mergeHeaders(request.getHeaders(),
                                            config.overrideConfiguration().additionalHttpHeaders(),
                                            adaptHeaders(context.requestConfig().getCustomRequestHeaders())));
    }

    @SafeVarargs
    private final Map<String, List<String>> mergeHeaders(Map<String, List<String>>... headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map<String, List<String>> header : headers) {
            header.forEach((k, v) -> result.computeIfAbsent(k, ignored -> new ArrayList<>()).addAll(v));
        }
        return result;
    }

    // TODO change this representation
    private Map<String, List<String>> adaptHeaders(Map<String, String> toConvert) {
        Map<String, List<String>> adapted = new HashMap<>(toConvert.size());
        toConvert.forEach((k, v) -> adapted.put(k, Collections.singletonList(v)));
        return adapted;
    }
}
