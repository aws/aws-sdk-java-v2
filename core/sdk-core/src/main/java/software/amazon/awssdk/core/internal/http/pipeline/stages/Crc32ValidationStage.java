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

import static software.amazon.awssdk.core.client.config.SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.Crc32Validation;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public final class Crc32ValidationStage
    implements RequestPipeline<Pair<SdkHttpFullRequest, SdkHttpFullResponse>, Pair<SdkHttpFullRequest, SdkHttpFullResponse>> {

    private final boolean calculateCrc32FromCompressedData;

    public Crc32ValidationStage(HttpClientDependencies dependencies) {

        //TODO: move CRC32_FROM_COMPRESSED_DATA_ENABLED to aws-core once this stage gets removed
        this.calculateCrc32FromCompressedData = dependencies.clientConfiguration().option(CRC32_FROM_COMPRESSED_DATA_ENABLED);
    }

    @Override
    public Pair<SdkHttpFullRequest, SdkHttpFullResponse> execute(Pair<SdkHttpFullRequest, SdkHttpFullResponse> input,
                                                                 RequestExecutionContext context) throws Exception {
        return Pair.of(input.left(),
                       Crc32Validation.validate(calculateCrc32FromCompressedData, input.right()));
    }

}