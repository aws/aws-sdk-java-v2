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
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.config.AwsAsyncClientConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.client.AsyncClientHandler;
import software.amazon.awssdk.core.client.BaseAsyncClientHandler;
import software.amazon.awssdk.core.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.core.http.ExecutionContext;

/**
 * Default implementation of {@link AsyncClientHandler}.
 */
@Immutable
@ThreadSafe
@SdkProtectedApi
final class AwsAsyncClientHandlerImpl extends BaseAsyncClientHandler {
    private final AwsAsyncClientConfiguration clientConfiguration;
    private final ServiceAdvancedConfiguration serviceAdvancedConfiguration;

    AwsAsyncClientHandlerImpl(AwsAsyncClientConfiguration clientConfiguration, ServiceAdvancedConfiguration
        serviceAdvancedConfiguration) {
        super(clientConfiguration, serviceAdvancedConfiguration, new AmazonAsyncHttpClient(clientConfiguration));
        this.clientConfiguration = clientConfiguration;
        this.serviceAdvancedConfiguration = serviceAdvancedConfiguration;
    }

    @Override
    protected ExecutionContext createExecutionContext(SdkRequest originalRequest) {
        return AwsClientHandlerUtils.createExecutionContext(originalRequest, clientConfiguration, serviceAdvancedConfiguration);
    }
}
