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

package software.amazon.awssdk.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.AwsRequest;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.config.AdvancedClientOption;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.MutableClientConfiguration;
import software.amazon.awssdk.core.config.SyncClientConfiguration;
import software.amazon.awssdk.core.config.defaults.GlobalClientConfigurationDefaults;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;

@RunWith(MockitoJUnitRunner.class)
public class SyncClientHandlerImplTest {
    private SyncClientHandlerImpl syncClientHandler;

    @Mock
    private AwsCredentialsProvider credentialsProvider;

    private AwsCredentials awsCredentials = AwsCredentials.create("public", "private");

    @Mock
    private AwsRequest request;

    @Mock
    private Marshaller<Request<SdkRequest>, SdkRequest> marshaller;

    private Request<SdkRequest> marshalledRequest = new DefaultRequest<>(request, "");

    @Mock
    private SdkHttpClient httpClient;

    @Mock
    private AbortableCallable<SdkHttpFullResponse> httpClientCall;

    @Mock
    private HttpResponseHandler<SdkResponse> responseHandler;

    @Mock
    private HttpResponseHandler<SdkServiceException> errorResponseHandler;

    @Mock
    private SdkResponse response;

    @Before
    public void setup() {
        this.syncClientHandler = new SyncClientHandlerImpl(clientConfiguration(), null);
    }

    @Test
    public void successfulExecutionCallsResponseHandler() throws Exception {
        // Given
        expectRetrievalFromMocks();
        when(httpClientCall.call()).thenReturn(SdkHttpFullResponse.builder().statusCode(200).build()); // Successful HTTP call
        when(responseHandler.handle(any(), any())).thenReturn(response); // Response handler call

        // When
        SdkResponse response = syncClientHandler.execute(clientExecutionParams());

        // Then
        verifyNoMoreInteractions(errorResponseHandler); // No error handler calls
        assertThat(response).isEqualTo(this.response); // Response handler result returned
    }

    @Test
    public void failedExecutionCallsErrorResponseHandler() throws Exception {
        SdkServiceException exception = new SdkServiceException("Uh oh!");

        // Given
        expectRetrievalFromMocks();
        when(httpClientCall.call()).thenReturn(SdkHttpFullResponse.builder().statusCode(500).build()); // Failed HTTP call
        when(errorResponseHandler.handle(any(), any())).thenReturn(exception); // Error response handler call

        // When
        assertThatThrownBy(() -> syncClientHandler.execute(clientExecutionParams())).isEqualTo(exception);

        // Then
        verifyNoMoreInteractions(responseHandler); // No response handler calls
    }

    @Test(expected = IllegalStateException.class)
    public void clientHandlerThrowsExceptionWhenCredentialProviderReturnsNull() {
        when(credentialsProvider.getCredentials()).thenReturn(null);
        syncClientHandler.execute(clientExecutionParams());
    }

    private void expectRetrievalFromMocks() {
        when(credentialsProvider.getCredentials()).thenReturn(awsCredentials);
        when(marshaller.marshall(request)).thenReturn(marshalledRequest);
        when(httpClient.prepareRequest(any(), any())).thenReturn(httpClientCall);
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(marshaller)
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler);
    }

    public SyncClientConfiguration clientConfiguration() {
        MutableClientConfiguration mutableClientConfiguration = new MutableClientConfiguration()
                .endpoint(URI.create("http://test.com"))
                .credentialsProvider(credentialsProvider)
                .httpClient(httpClient);

        mutableClientConfiguration.overrideConfiguration(
            ClientOverrideConfiguration.builder()
                                       .advancedOption(AdvancedClientOption.SIGNER_PROVIDER, new NoOpSignerProvider())
                                       .retryPolicy(RetryPolicy.NONE)
                                       .build());

        new GlobalClientConfigurationDefaults().applySyncDefaults(mutableClientConfiguration);

        return mutableClientConfiguration;
    }
}
