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

package software.amazon.awssdk.core.client.handler;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOptionValidation;
import software.amazon.awssdk.core.internal.handler.BaseSyncClientHandler;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.sync.ResponseTransformer;

/**
 * Client handler for SDK clients.
 */
@ThreadSafe
@Immutable
@SdkProtectedApi
public class SdkSyncClientHandler extends BaseSyncClientHandler implements SyncClientHandler {

    protected SdkSyncClientHandler(SdkClientConfiguration clientConfiguration) {
        super(clientConfiguration, new AmazonSyncHttpClient(clientConfiguration));
        SdkClientOptionValidation.validateSyncClientOptions(clientConfiguration);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ClientExecutionParams<InputT, OutputT> executionParams) {
        return super.execute(executionParams);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ResponseTransformer<OutputT, ReturnT> responseTransformer) {
        return super.execute(executionParams, responseTransformer);
    }

}
