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
package software.amazon.awssdk.core.client.builder;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class ClientHttpConfigurationTest {

    @Mock
    private SdkHttpClient sdkHttpClient;

    @Mock
    private SdkHttpClientFactory sdkClientFactory;

    @Test
    public void setSdkHttpClient_OnlySetsClient() {
        final ClientHttpConfiguration orig = ClientHttpConfiguration.builder()
                .httpClient(sdkHttpClient)
                .build();
        assertThat(orig.httpClient()).hasValue(sdkHttpClient);
        assertThat(orig.httpClientFactory()).isEmpty();
    }

    @Test
    public void setSdkClientFactory_OnlySetsFactory() {
        final ClientHttpConfiguration orig = ClientHttpConfiguration.builder()
                .httpClientFactory(sdkClientFactory)
                .build();
        assertThat(orig.httpClientFactory()).hasValue(sdkClientFactory);
        assertThat(orig.httpClient()).isEmpty();
    }

    @Test
    public void sdkHttpClientSet_IsPreservedInToBuilder() {
        final ClientHttpConfiguration config = ClientHttpConfiguration.builder()
                .httpClient(sdkHttpClient)
                .build()
                .toBuilder()
                .build();
        assertThat(config.httpClient()).hasValue(sdkHttpClient);
        assertThat(config.httpClientFactory()).isEmpty();
    }

    @Test
    public void sdkClientFactorySet_IsPreservedInToBuilder() {
        final ClientHttpConfiguration config = ClientHttpConfiguration.builder()
                .httpClientFactory(sdkClientFactory)
                .build()
                .toBuilder()
                .build();
        assertThat(config.httpClientFactory()).hasValue(sdkClientFactory);
        assertThat(config.httpClient()).isEmpty();
    }

    // TODO code review, is this the correct behavior?
    @Test
    public void neitherClientNorFactorySet_DoesNotThrowsException() {
        ClientHttpConfiguration.builder().build();
    }

}
