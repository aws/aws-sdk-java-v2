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
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersClientContextParams;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointParams;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public class ClientBuilderTests {
    @Test
    public void syncBuilder_setCustomProvider_interceptorUsesProvider() {
        RestJsonEndpointProvidersEndpointProvider mockProvider = mock(RestJsonEndpointProvidersEndpointProvider.class);
        SdkHttpClient mockClient = mock(SdkHttpClient.class);
        when(mockClient.clientName()).thenReturn("MockHttpClient");

        when(mockClient.prepareRequest(any())).thenThrow(new RuntimeException("boom"));

        when(mockProvider.resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(Endpoint.builder()
                                                                  .url(URI.create("https://my-service.com"))
                                                                  .build()));

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

        verify(mockProvider).resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class));

        ArgumentCaptor<HttpExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
        verify(mockClient).prepareRequest(httpRequestCaptor.capture());

        URI requestUri = httpRequestCaptor.getValue().httpRequest().getUri();

        assertThat(requestUri.getScheme()).isEqualTo("https");
        assertThat(requestUri.getHost()).isEqualTo("my-service.com");
    }

    @Test
    public void asyncBuilder_setCustomProvider_interceptorUsesProvider() {
        RestJsonEndpointProvidersEndpointProvider mockProvider = mock(RestJsonEndpointProvidersEndpointProvider.class);
        SdkAsyncHttpClient mockClient = mock(SdkAsyncHttpClient.class);
        when(mockClient.clientName()).thenReturn("MockHttpClient");

        when(mockClient.execute(any())).thenAnswer(i -> {
            AsyncExecuteRequest r = i.getArgument(0, AsyncExecuteRequest.class);
            r.responseHandler().onError(new RuntimeException("boom"));
            return CompletableFutureUtils.failedFuture(new RuntimeException());
        });

        when(mockProvider.resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(Endpoint.builder()
                                                                  .url(URI.create("https://my-service.com"))
                                                                  .build()));

        RestJsonEndpointProvidersAsyncClient client =
            RestJsonEndpointProvidersAsyncClient.builder()
                                                .endpointProvider(mockProvider)
                                                .httpClient(mockClient)
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(
                                                    StaticCredentialsProvider.create(
                                                        AwsBasicCredentials.create("akid", "skid")))
                                                .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        }).join())
            .hasRootCauseMessage("boom");


        verify(mockProvider).resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class));

        ArgumentCaptor<AsyncExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);
        verify(mockClient).execute(httpRequestCaptor.capture());

        URI requestUri = httpRequestCaptor.getValue().request().getUri();

        assertThat(requestUri.getScheme()).isEqualTo("https");
        assertThat(requestUri.getHost()).isEqualTo("my-service.com");

    }

    @Test
    public void sync_clientContextParamsSetOnBuilder_includedInExecutionAttributes() {
        ExecutionInterceptor mockInterceptor = mock(ExecutionInterceptor.class);
        when(mockInterceptor.modifyRequest(any(), any())).thenThrow(new RuntimeException("oops"));

        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(mockInterceptor))
            .booleanClientContextParam(true)
            .stringClientContextParam("hello")
            .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        })).hasMessageContaining("oops");

        ArgumentCaptor<ExecutionAttributes> attributesCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
        verify(mockInterceptor).modifyRequest(any(), attributesCaptor.capture());

        ExecutionAttributes executionAttrs = attributesCaptor.getValue();

        AttributeMap clientContextParams = executionAttrs.getAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS);

        assertThat(clientContextParams.get(RestJsonEndpointProvidersClientContextParams.BOOLEAN_CLIENT_CONTEXT_PARAM))
            .isEqualTo(true);
        assertThat(clientContextParams.get(RestJsonEndpointProvidersClientContextParams.STRING_CLIENT_CONTEXT_PARAM))
            .isEqualTo("hello");

    }

    @Test
    public void async_clientContextParamsSetOnBuilder_includedInExecutionAttributes() {
        ExecutionInterceptor mockInterceptor = mock(ExecutionInterceptor.class);
        when(mockInterceptor.modifyRequest(any(), any())).thenThrow(new RuntimeException("oops"));

        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(mockInterceptor))
            .booleanClientContextParam(true)
            .stringClientContextParam("hello")
            .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        }).join()).hasMessageContaining("oops");

        ArgumentCaptor<ExecutionAttributes> attributesCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
        verify(mockInterceptor).modifyRequest(any(), attributesCaptor.capture());

        ExecutionAttributes executionAttrs = attributesCaptor.getValue();

        AttributeMap clientContextParams = executionAttrs.getAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS);

        assertThat(clientContextParams.get(RestJsonEndpointProvidersClientContextParams.BOOLEAN_CLIENT_CONTEXT_PARAM))
            .isEqualTo(true);
        assertThat(clientContextParams.get(RestJsonEndpointProvidersClientContextParams.STRING_CLIENT_CONTEXT_PARAM))
            .isEqualTo("hello");

    }

    private RestJsonEndpointProvidersClientBuilder syncClientBuilder() {
        return RestJsonEndpointProvidersClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(
                                                  StaticCredentialsProvider.create(
                                                      AwsBasicCredentials.create("akid", "skid")));
    }

    private RestJsonEndpointProvidersAsyncClientBuilder asyncClientBuilder() {
        return RestJsonEndpointProvidersAsyncClient.builder()
                                            .region(Region.US_WEST_2)
                                            .credentialsProvider(
                                                StaticCredentialsProvider.create(
                                                    AwsBasicCredentials.create("akid", "skid")));
    }
}
