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

package software.amazon.awssdk.http.apache.async.internal;

import static software.amazon.awssdk.utils.Validate.notNull;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.impl.ApacheHttpRequestFactory;
import software.amazon.awssdk.http.apache.internal.impl.ApacheSdkHttpClientConfig;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.apache.internal.utils.ApacheUtils;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;

/**
 * Implementation {@link SdkAsyncHttpClient} using Apache HttpClient.
 */
public class ApacheAsyncHttpClient implements SdkAsyncHttpClient {
    private final ExecutorService exec = Executors.newCachedThreadPool();
    private final ApacheHttpRequestFactory apacheHttpRequestFactory = new ApacheHttpRequestFactory();
    private final ConnectionManagerAwareHttpClient httpClient;
    private final ApacheHttpRequestConfig requestConfig;
    private final ApacheSdkHttpClientConfig configuration;

    ApacheAsyncHttpClient(ConnectionManagerAwareHttpClient httpClient,
                          ApacheHttpRequestConfig requestConfig,
                          ApacheSdkHttpClientConfig configuration) {
        this.httpClient = notNull(httpClient, "httpClient must not be null.");
        this.requestConfig = notNull(requestConfig, "requestConfig must not be null.");
        this.configuration = notNull(configuration, "configuration must not be null");
    }

    @Override
    public AbortableRunnable prepareRequest(SdkHttpRequest request, SdkRequestContext context,
                                            SdkHttpRequestProvider requestProvider, SdkHttpResponseHandler handler) {
        HttpRequestBase apacheRequest = apacheHttpRequestFactory.create(request, requestProvider, requestConfig);
        HttpClientContext clientContext = ApacheUtils.newClientContext(configuration.proxyConfiguration());
        return new AbortableRunnableImpl(exec, httpClient, apacheRequest, clientContext, handler);
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return configuration.getOption(key);
    }

    @Override
    public void close() {
        httpClient.getHttpClientConnectionManager().shutdown();
        exec.shutdown();
    }
}
