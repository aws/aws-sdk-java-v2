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

package software.amazon.awssdk.awscore.client.handler;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.internal.client.config.AwsClientOptionValidation;
import software.amazon.awssdk.awscore.internal.client.handler.AwsClientHandlerUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkSyncClientHandler;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.sync.ResponseTransformer;

/**
 * Client handler for AWS SDK clients.
 */
@ThreadSafe
@Immutable
@ReviewBeforeRelease("This looks identical to the Sdk version, revisit when we add APIG back")
@SdkProtectedApi
public final class AwsSyncClientHandler extends SdkSyncClientHandler implements SyncClientHandler {

    private final SdkClientConfiguration clientConfiguration;

    public AwsSyncClientHandler(SdkClientConfiguration clientConfiguration) {
        super(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        AwsClientOptionValidation.validateSyncClientOptions(clientConfiguration);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ClientExecutionParams<InputT, OutputT> executionParams) {
        return super.execute(addErrorResponseHandler(executionParams));
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ResponseTransformer<OutputT, ReturnT> responseTransformer) {
        return super.execute(addErrorResponseHandler(executionParams), responseTransformer);
    }

    @Override
    protected ExecutionContext createExecutionContext(SdkRequest originalRequest) {
        return AwsClientHandlerUtils.createExecutionContext(originalRequest, clientConfiguration);
    }
}
