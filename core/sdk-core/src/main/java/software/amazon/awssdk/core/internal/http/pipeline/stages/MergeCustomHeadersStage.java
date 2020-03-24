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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Merge customer supplied headers into the marshalled request.
 */
@SdkInternalApi
public class MergeCustomHeadersStage implements MutableRequestToRequestPipeline {

    private final SdkClientConfiguration config;

    public MergeCustomHeadersStage(HttpClientDependencies dependencies) {
        this.config = dependencies.clientConfiguration();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        return request.headers(mergeHeaders(request.headers(),
                                            config.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS),
                                            adaptHeaders(context.requestConfig().headers())));
    }

    @SafeVarargs
    private final Map<String, List<String>> mergeHeaders(Map<String, List<String>>... headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map<String, List<String>> header : headers) {
            header.forEach((headerName, headerValues) -> {
                List<String> resultHeaderValues = result.computeIfAbsent(headerName, ignored -> new ArrayList<>());
                if (SdkHttpUtils.isSingleHeader(headerName)) {
                    resultHeaderValues.clear();
                }
                resultHeaderValues.addAll(headerValues);
            });
        }
        return result;
    }

    private Map<String, List<String>> adaptHeaders(Map<String, List<String>> toConvert) {
        Map<String, List<String>> adapted = new TreeMap<>();
        toConvert.forEach((name, value) -> adapted.put(name, new ArrayList<>(value)));
        return adapted;
    }
}
