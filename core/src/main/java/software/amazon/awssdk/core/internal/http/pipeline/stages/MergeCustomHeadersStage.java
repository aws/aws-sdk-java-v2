/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.core.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Merge customer supplied headers into the marshalled request.
 */
public class MergeCustomHeadersStage implements MutableRequestToRequestPipeline {

    private final SdkClientConfiguration config;

    public MergeCustomHeadersStage(HttpClientDependencies dependencies) {
        this.config = dependencies.clientConfiguration();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        return request.headers(mergeHeaders(request.headers(),
                                            config.overrideConfiguration().additionalHttpHeaders(),
                                            adaptHeaders(context.requestConfig().headers()
                                                    .orElse(Collections.emptyMap()))));
    }

    @SafeVarargs
    private final Map<String, List<String>> mergeHeaders(Map<String, List<String>>... headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map<String, List<String>> header : headers) {
            header.forEach((k, v) -> result.computeIfAbsent(k, ignored -> new ArrayList<>()).addAll(v));
        }
        return result;
    }

    private Map<String, List<String>> adaptHeaders(Map<String, List<String>> toConvert) {
        Map<String, List<String>> adapted = new TreeMap<>();
        toConvert.forEach((name, value) -> adapted.put(name, new ArrayList<>(value)));
        return adapted;
    }
}
