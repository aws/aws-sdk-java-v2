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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Merge customer supplied query params into the marshalled request.
 */
@SdkInternalApi
public class MergeCustomQueryParamsStage implements MutableRequestToRequestPipeline {

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        return request.rawQueryParameters(mergeParams(request, context));
    }

    private Map<String, List<String>> mergeParams(SdkHttpFullRequest.Builder request, RequestExecutionContext context) {
        Map<String, List<String>> merged = new LinkedHashMap<>(request.rawQueryParameters().size());
        merged.putAll(request.rawQueryParameters());
        context.requestConfig().rawQueryParameters()
               .forEach((key, val) -> merged.put(key, CollectionUtils.mergeLists(merged.get(key), val)));
        return merged;
    }
}
