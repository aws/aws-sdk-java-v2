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

package software.amazon.awssdk.protocol.tests;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

import java.util.concurrent.ExecutorService;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;

/**
 * Verifies that the SDK clients correctly manage resources with which they have been configured.
 */
public class ResourceManagementTest {
    @Test
    public void httpClientNotShutdown() {
        SdkHttpClient httpClient = mock(SdkHttpClient.class);
        syncClientBuilder().httpClient(httpClient).build().close();
        verify(httpClient, never()).close();
    }

    @Test
    public void asyncHttpClientNotShutdown() {
        SdkAsyncHttpClient httpClient = mock(SdkAsyncHttpClient.class);
        asyncClientBuilder().asyncHttpClient(httpClient).build().close();
        verify(httpClient, never()).close();
    }

    @Test
    public void httpClientFromBuilderShutdown() {
        SdkHttpClient httpClient = mock(SdkHttpClient.class);
        SdkHttpClient.Builder httpClientBuilder = mock(SdkHttpClient.Builder.class);

        when(httpClientBuilder.buildWithDefaults(any())).thenReturn(httpClient);

        syncClientBuilder().httpClientBuilder(httpClientBuilder).build().close();
        verify(httpClient).close();
    }

    @Test
    public void asyncHttpClientFromBuilderShutdown() {
        SdkAsyncHttpClient httpClient = mock(SdkAsyncHttpClient.class);
        SdkAsyncHttpClient.Builder httpClientBuilder = mock(SdkAsyncHttpClient.Builder.class);

        when(httpClientBuilder.buildWithDefaults(any())).thenReturn(httpClient);

        asyncClientBuilder().asyncHttpClientBuilder(httpClientBuilder).build().close();
        verify(httpClient).close();
    }

    @Test
    public void executorFromBuilderNotShutdown() {
        ExecutorService executor = mock(ExecutorService.class);

        asyncClientBuilder().asyncConfiguration(c -> c.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor)).build().close();

        verify(executor, never()).shutdown();
        verify(executor, never()).shutdownNow();
    }

    public ProtocolRestJsonClientBuilder syncClientBuilder() {
        return ProtocolRestJsonClient.builder()
                                     .region(Region.US_EAST_1)
                                     .credentialsProvider(AnonymousCredentialsProvider.create());
    }

    public ProtocolRestJsonAsyncClientBuilder asyncClientBuilder() {
        return ProtocolRestJsonAsyncClient.builder()
                                          .region(Region.US_EAST_1)
                                          .credentialsProvider(AnonymousCredentialsProvider.create());
    }
}
