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

package software.amazon.awssdk.awscore.client.handler;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.internal.client.config.AwsClientOptionValidation;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkSyncClientHandler;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.http.Crc32Validation;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Client handler for AWS SDK clients.
 */
@ThreadSafe
@Immutable
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
        ClientExecutionParams<InputT, OutputT> clientExecutionParams = addCrc32Validation(executionParams);
        return super.execute(clientExecutionParams);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ResponseTransformer<OutputT, ReturnT> responseTransformer) {
        return super.execute(executionParams, responseTransformer);
    }

    @Override
    protected <InputT extends SdkRequest, OutputT extends SdkResponse> ExecutionContext createExecutionContext(
        ClientExecutionParams<InputT, OutputT> executionParams, ExecutionAttributes executionAttributes) {
        return AwsClientHandlerUtils.createExecutionContext(executionParams, clientConfiguration, executionAttributes);
    }

    private <InputT extends SdkRequest, OutputT> ClientExecutionParams<InputT, OutputT> addCrc32Validation(
        ClientExecutionParams<InputT, OutputT> executionParams) {
        if (executionParams.getCombinedResponseHandler() != null) {
            return executionParams.withCombinedResponseHandler(
                new Crc32ValidationResponseHandler<>(executionParams.getCombinedResponseHandler()));
        } else {
            return executionParams.withResponseHandler(
                new Crc32ValidationResponseHandler<>(executionParams.getResponseHandler()));
        }
    }

    /**
     * Decorate {@link HttpResponseHandler} to validate CRC32 if needed.
     */
    private class Crc32ValidationResponseHandler<T> implements HttpResponseHandler<T> {
        private final HttpResponseHandler<T> delegate;

        private Crc32ValidationResponseHandler(HttpResponseHandler<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
            return delegate.handle(Crc32Validation.validate(isCalculateCrc32FromCompressedData(), response), executionAttributes);
        }
    }
}
