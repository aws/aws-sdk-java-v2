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

package software.amazon.awssdk.awscore.config;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awsauth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.SdkMutableClientConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * An implementation of {@link AwsSyncClientConfiguration} and {@link AwsAsyncClientConfiguration} that
 * provides fluent write and read methods for all configuration properties.
 *
 * <p>This class is mutable and not thread safe.</p>
 */
//TODO: remove duplicate codes
@SdkInternalApi
public final class AwsMutableClientConfiguration
    extends SdkMutableClientConfiguration
    implements AwsSyncClientConfiguration, AwsAsyncClientConfiguration, Cloneable {

    // ClientConfiguration
    private ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder().build();
    private AwsCredentialsProvider credentialsProvider;
    private URI endpoint;
    private SdkHttpClient httpClient;
    private SdkAsyncHttpClient asyncHttpClient;

    // AsyncClientConfiguration
    private ScheduledExecutorService asyncExecutorService;

    @Override
    public ClientOverrideConfiguration overrideConfiguration() {
        return overrideConfiguration;
    }

    public AwsMutableClientConfiguration overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
        this.overrideConfiguration = overrideConfiguration;
        return this;
    }

    @Override
    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    public AwsMutableClientConfiguration credentialsProvider(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    @Override
    public URI endpoint() {
        return endpoint;
    }

    public AwsMutableClientConfiguration endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    @Override
    public ScheduledExecutorService asyncExecutorService() {
        return asyncExecutorService;
    }

    public AwsMutableClientConfiguration asyncExecutorService(ScheduledExecutorService executorService) {
        this.asyncExecutorService = executorService;
        return this;
    }

    @Override
    public SdkHttpClient httpClient() {
        return httpClient;
    }

    public AwsMutableClientConfiguration httpClient(SdkHttpClient sdkHttpClient) {
        this.httpClient = sdkHttpClient;
        return this;
    }

    @Override
    public SdkAsyncHttpClient asyncHttpClient() {
        return asyncHttpClient;
    }

    public AwsMutableClientConfiguration asyncHttpClient(SdkAsyncHttpClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
        return this;
    }

    @Override
    public AwsMutableClientConfiguration clone() {
        return (AwsMutableClientConfiguration) super.clone();
    }
}
