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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.config.options.AwsClientOptionValidation;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.AsyncClientHandler;
import software.amazon.awssdk.core.client.ClientExecutionParams;
import software.amazon.awssdk.core.client.SdkAsyncClientHandler;
import software.amazon.awssdk.core.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;

/**
 * Async client handler for AWS SDK clients.
 */
@ThreadSafe
@Immutable
public final class AwsAsyncClientHandler extends SdkAsyncClientHandler implements AsyncClientHandler {

    private final SdkClientConfiguration clientConfiguration;

    public AwsAsyncClientHandler(SdkClientConfiguration clientConfiguration) {
        super(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        AwsClientOptionValidation.validateAsyncClientOptions(clientConfiguration);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams) {
        return super.execute(addErrorResponseHandler(executionParams));
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer) {
        return super.execute(addErrorResponseHandler(executionParams), asyncResponseTransformer);
    }

    @Override
    protected ExecutionContext createExecutionContext(SdkRequest originalRequest) {
        return AwsClientHandlerUtils.createExecutionContext(originalRequest, clientConfiguration);
    }

}
