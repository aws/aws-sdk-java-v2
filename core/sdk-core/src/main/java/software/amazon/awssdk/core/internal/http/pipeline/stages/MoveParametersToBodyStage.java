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

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
@ReviewBeforeRelease("Might only need to do this for certain protocols - ie query?")
// TODO how is this going to work with streaming input posts in asyncland
public final class MoveParametersToBodyStage implements MutableRequestToRequestPipeline {
    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder input, RequestExecutionContext context) {
        if (shouldPutParamsInBody(input, context)) {
            return changeQueryParametersToFormData(input);
        }
        return input;
    }

    private boolean shouldPutParamsInBody(SdkHttpFullRequest.Builder input,
                                          RequestExecutionContext context) {
        return input.method() == SdkHttpMethod.POST &&
               input.content() == null &&
               !CollectionUtils.isNullOrEmpty(input.rawQueryParameters());
    }

    @SdkProtectedApi
    public static SdkHttpFullRequest.Builder changeQueryParametersToFormData(SdkHttpFullRequest.Builder input) {
        byte[] params = SdkHttpUtils.encodeAndFlattenFormData(input.rawQueryParameters()).orElse("")
                                    .getBytes(StandardCharsets.UTF_8);

        return input.clearQueryParameters()
                    .content(new ByteArrayInputStream(params))
                    .putHeader("Content-Length", singletonList(String.valueOf(params.length)))
                    .putHeader("Content-Type", singletonList("application/x-www-form-urlencoded; charset=" +
                                                             lowerCase(StandardCharsets.UTF_8.toString())));
    }
}
