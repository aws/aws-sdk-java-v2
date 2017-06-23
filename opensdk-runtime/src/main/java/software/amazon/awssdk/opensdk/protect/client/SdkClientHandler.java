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

package software.amazon.awssdk.opensdk.protect.client;

import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.ClientHandlerImpl;
import software.amazon.awssdk.client.ClientHandlerParams;
import software.amazon.awssdk.opensdk.BaseRequest;

/**
 * Client handler for Open SDK generated clients. Handles exception translation and delegates to the default implementation of
 * {@link ClientHandler}.
 */
@ThreadSafe
@Immutable
public class SdkClientHandler extends ClientHandler {

    private final ClientHandler delegateHandler;

    public SdkClientHandler(ClientHandlerParams handlerParams) {
        this.delegateHandler = new ClientHandlerImpl(handlerParams);
    }

    @Override
    public <InputT, OutputT> OutputT execute(
            ClientExecutionParams<InputT, OutputT> executionParams) {
        return delegateHandler.execute(addRequestConfig(executionParams));
    }

    @Override
    public void close() throws Exception {
        delegateHandler.close();
    }

    private <InputT, OutputT>
        ClientExecutionParams<InputT, OutputT> addRequestConfig(ClientExecutionParams<InputT, OutputT> params) {
        return params.withRequestConfig(new RequestConfigAdapter((BaseRequest) params.getInput()));
    }

}
