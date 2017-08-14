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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.SdkResponse;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.config.AdvancedClientOption;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.config.MutableClientConfiguration;
import software.amazon.awssdk.config.SyncClientConfiguration;
import software.amazon.awssdk.config.defaults.GlobalClientConfigurationDefaults;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.runtime.transform.Marshaller;

@RunWith(MockitoJUnitRunner.class)
public class SyncClientHandlerImplTest {
    private SyncClientHandlerImpl syncClientHandler;

    @Mock
    private AwsCredentialsProvider credentialsProvider;

    private AwsCredentials awsCredentials = new AwsCredentials("public", "private");

    @Mock
    private AmazonWebServiceRequest request;

    @Mock
    private RequestConfig requestConfig;

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
    private HttpResponseHandler<AmazonServiceException> errorResponseHandler;

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
        AmazonServiceException exception = new AmazonServiceException("Uh oh!");

        // Given
        expectRetrievalFromMocks();
        when(httpClientCall.call()).thenReturn(SdkHttpFullResponse.builder().statusCode(500).build()); // Failed HTTP call
        when(errorResponseHandler.handle(any(), any())).thenReturn(exception); // Error response handler call

        // When
        assertThatThrownBy(() -> syncClientHandler.execute(clientExecutionParams())).isEqualTo(exception);

        // Then
        verifyNoMoreInteractions(responseHandler); // No response handler calls
    }

    private void expectRetrievalFromMocks() {
        when(credentialsProvider.getCredentials()).thenReturn(awsCredentials);
        when(requestConfig.getOriginalRequest()).thenReturn(request);
        when(marshaller.marshall(request)).thenReturn(marshalledRequest);
        when(httpClient.prepareRequest(any(), any())).thenReturn(httpClientCall);
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(marshaller)
                .withRequestConfig(requestConfig)
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler);
    }

    public SyncClientConfiguration clientConfiguration() {
        MutableClientConfiguration mutableClientConfiguration = new MutableClientConfiguration()
                .credentialsProvider(credentialsProvider)
                .httpClient(httpClient);

        mutableClientConfiguration.overrideConfiguration(
                ClientOverrideConfiguration.builder()
                                           .advancedOption(AdvancedClientOption.SIGNER_PROVIDER, new NoOpSignerProvider())
                                           .build());

        new GlobalClientConfigurationDefaults().applySyncDefaults(mutableClientConfiguration);

        return mutableClientConfiguration;
    }
}
