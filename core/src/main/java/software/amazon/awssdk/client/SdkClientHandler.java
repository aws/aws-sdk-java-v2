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

package software.amazon.awssdk.client;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.ServiceAdvancedConfiguration;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.config.SyncClientConfiguration;
import software.amazon.awssdk.internal.AmazonWebServiceRequestAdapter;
import software.amazon.awssdk.internal.http.response.AwsErrorResponseHandler;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * Client handler for SDK clients.
 */
@ThreadSafe
@Immutable
public class SdkClientHandler extends ClientHandler {

    private final ClientHandler delegateHandler;

    public SdkClientHandler(SyncClientConfiguration syncClientConfiguration,
                            ServiceAdvancedConfiguration serviceAdvancedConfiguration) {
        super(syncClientConfiguration, serviceAdvancedConfiguration);
        this.delegateHandler = new SyncClientHandlerImpl(syncClientConfiguration, serviceAdvancedConfiguration);
    }

    @Override
    public <InputT extends SdkRequest, OutputT> OutputT execute(
            ClientExecutionParams<InputT, OutputT> executionParams) {
        return delegateHandler.execute(
                addRequestConfig(executionParams)
                        // TODO this is a hack to get the build working. Also doesn't deal with AwsResponseHandlerAdapter
                        .withErrorResponseHandler(
                                new AwsErrorResponseHandler(executionParams.getErrorResponseHandler(), new AwsRequestMetrics())));

    }

    @Override
    public void close() throws Exception {
        delegateHandler.close();
    }

    private <InputT extends SdkRequest, OutputT> ClientExecutionParams<InputT, OutputT> addRequestConfig(
            ClientExecutionParams<InputT, OutputT> params) {
        return params.withRequestConfig(new AmazonWebServiceRequestAdapter((AmazonWebServiceRequest) params.getInput()));
    }

}
