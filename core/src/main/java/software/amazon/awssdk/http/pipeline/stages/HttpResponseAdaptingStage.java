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

import static software.amazon.awssdk.config.InternalAdvancedClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED;

import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponseAdapter;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.utils.Pair;

/**
 * Adapt our new {@link SdkHttpFullResponse} representation, to the legacy {@link HttpResponse} representation.
 */
public class HttpResponseAdaptingStage
        implements RequestPipeline<Pair<SdkHttpFullRequest, SdkHttpFullResponse>, Pair<SdkHttpFullRequest, HttpResponse>> {

    private final boolean calculateCrc32FromCompressedData;

    public HttpResponseAdaptingStage(HttpClientDependencies dependencies) {
        this.calculateCrc32FromCompressedData = dependencies.clientConfiguration()
                                                            .overrideConfiguration()
                                                            .advancedOption(CRC32_FROM_COMPRESSED_DATA_ENABLED);
    }

    @Override
    public Pair<SdkHttpFullRequest, HttpResponse> execute(Pair<SdkHttpFullRequest, SdkHttpFullResponse> input,
                                                          RequestExecutionContext context) throws Exception {
        return new Pair<>(input.left(),
                          SdkHttpResponseAdapter.adapt(calculateCrc32FromCompressedData, input.left(), input.right()));
    }

}
