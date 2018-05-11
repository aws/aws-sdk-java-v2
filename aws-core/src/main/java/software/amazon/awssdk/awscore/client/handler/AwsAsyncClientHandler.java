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
import software.amazon.awssdk.awscore.config.AwsAsyncClientConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.AsyncClientHandler;
import software.amazon.awssdk.core.client.BaseClientHandler;
import software.amazon.awssdk.core.client.ClientExecutionParams;

/**
 * Async client handler for AWS SDK clients.
 */
@ThreadSafe
@Immutable
public final class AwsAsyncClientHandler extends BaseClientHandler implements AsyncClientHandler {

    private final AsyncClientHandler delegateHandler;

    public AwsAsyncClientHandler(AwsAsyncClientConfiguration clientConfiguration, ServiceAdvancedConfiguration
        advancedClientOption) {
        super(clientConfiguration, advancedClientOption);
        this.delegateHandler = new AwsAsyncClientHandlerImpl(clientConfiguration, advancedClientOption);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams) {
        return delegateHandler.execute(addErrorResponseHandler(executionParams));
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer) {
        return delegateHandler.execute(addErrorResponseHandler(executionParams), asyncResponseTransformer);
    }

    @Override
    public void close() {
        delegateHandler.close();
    }
}
