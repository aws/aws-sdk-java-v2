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

package software.amazon.awssdk.services.endpointproviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.rules.RestJsonEndpointProvidersEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public class ClientBuilderTests {
    @Test
    public void syncBuilder_setCustomProvider_interceptorUsesProvider() {
        RestJsonEndpointProvidersEndpointProvider mockProvider = mock(RestJsonEndpointProvidersEndpointProvider.class);
        SdkHttpClient mockClient = mock(SdkHttpClient.class);
        when(mockClient.clientName()).thenReturn("MockHttpClient");

        when(mockClient.prepareRequest(any())).thenThrow(new RuntimeException("boom"));

        when(mockProvider.resolveEndpoint(any())).thenReturn(Endpoint.builder()
                                                                     .url(URI.create("https://my-service.com"))
                                                                     .build());

        RestJsonEndpointProvidersClient client =
            RestJsonEndpointProvidersClient.builder()
                                           .endpointProvider(mockProvider)
                                           .httpClient(mockClient)
                                           .region(Region.US_WEST_2)
                                           .credentialsProvider(StaticCredentialsProvider.create(
                                               AwsBasicCredentials.create("akid", "skid")))
                                           .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        })).hasMessageContaining("boom");

        verify(mockProvider).resolveEndpoint(any());

        ArgumentCaptor<HttpExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
        verify(mockClient).prepareRequest(httpRequestCaptor.capture());

        URI requestUri = httpRequestCaptor.getValue().httpRequest().getUri();

        assertThat(requestUri.getScheme()).isEqualTo("https");
        assertThat(requestUri.getHost()).isEqualTo("my-service.com");
    }

    @Test
    public void assyncBuilder_setCustomProvider_interceptorUsesProvider() {
        RestJsonEndpointProvidersEndpointProvider mockProvider = mock(RestJsonEndpointProvidersEndpointProvider.class);
        SdkAsyncHttpClient mockClient = mock(SdkAsyncHttpClient.class);
        when(mockClient.clientName()).thenReturn("MockHttpClient");

        when(mockClient.execute(any())).thenAnswer(i -> {
            AsyncExecuteRequest r = i.getArgument(0, AsyncExecuteRequest.class);
            r.responseHandler().onError(new RuntimeException("boom"));
            return CompletableFutureUtils.failedFuture(new RuntimeException());
        });

        when(mockProvider.resolveEndpoint(any())).thenReturn(Endpoint.builder()
                                                                     .url(URI.create("https://my-service.com"))
                                                                     .build());

        RestJsonEndpointProvidersAsyncClient client =
            RestJsonEndpointProvidersAsyncClient.builder()
                                                .endpointProvider(mockProvider)
                                                .httpClient(mockClient)
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(
                                                    StaticCredentialsProvider.create(
                                                        AwsBasicCredentials.create("akid", "skid")))
                                                .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {}).join())
            .hasRootCauseMessage("boom");


        verify(mockProvider).resolveEndpoint(any());

        ArgumentCaptor<AsyncExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);
        verify(mockClient).execute(httpRequestCaptor.capture());

        URI requestUri = httpRequestCaptor.getValue().request().getUri();

        assertThat(requestUri.getScheme()).isEqualTo("https");
        assertThat(requestUri.getHost()).isEqualTo("my-service.com");

    }
}
