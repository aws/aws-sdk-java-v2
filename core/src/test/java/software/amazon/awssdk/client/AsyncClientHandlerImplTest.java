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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import software.amazon.awssdk.config.AsyncClientConfiguration;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.config.MutableClientConfiguration;
import software.amazon.awssdk.config.defaults.GlobalClientConfigurationDefaults;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.runtime.transform.Marshaller;

@RunWith(MockitoJUnitRunner.class)
public class AsyncClientHandlerImplTest {
    private AsyncClientHandlerImpl syncClientHandler;

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
    private SdkAsyncHttpClient httpClient;

    @Mock
    private AbortableRunnable httpClientCall;

    @Mock
    private HttpResponseHandler<SdkResponse> responseHandler;

    @Mock
    private HttpResponseHandler<AmazonServiceException> errorResponseHandler;

    @Mock
    private SdkResponse response;

    @Before
    public void setup() {
        this.syncClientHandler = new AsyncClientHandlerImpl(clientConfiguration(), null);
    }

    @Test
    public void successfulExecutionCallsResponseHandler() throws Exception {
        // Given
        ArgumentCaptor<SdkHttpResponseHandler> sdkHttpResponseHandler = ArgumentCaptor.forClass(SdkHttpResponseHandler.class);

        expectRetrievalFromMocks();
        when(httpClient.prepareRequest(any(), any(), any(), sdkHttpResponseHandler.capture())).thenReturn(httpClientCall);
        when(responseHandler.handle(any(), any())).thenReturn(response); // Response handler call

        // When
        CompletableFuture<SdkResponse> responseFuture = syncClientHandler.execute(clientExecutionParams());
        sdkHttpResponseHandler.getValue().headersReceived(SdkHttpFullResponse.builder().statusCode(200).build());
        sdkHttpResponseHandler.getValue().complete();
        SdkResponse actualResponse = responseFuture.get(1, TimeUnit.SECONDS);

        // Then
        verifyNoMoreInteractions(errorResponseHandler); // No error handler calls
        verify(httpClientCall).run(); // Response handler is invoked
        assertThat(actualResponse).isEqualTo(response); // Response handler result returned
    }

    @Test
    public void failedExecutionCallsErrorResponseHandler() throws Exception {
        AmazonServiceException exception = new AmazonServiceException("Uh oh!");

        // Given
        ArgumentCaptor<SdkHttpResponseHandler> sdkHttpResponseHandler = ArgumentCaptor.forClass(SdkHttpResponseHandler.class);

        expectRetrievalFromMocks();
        when(httpClient.prepareRequest(any(), any(), any(), sdkHttpResponseHandler.capture())).thenReturn(httpClientCall);
        when(errorResponseHandler.handle(any(), any())).thenReturn(exception); // Error response handler call

        // When
        CompletableFuture<SdkResponse> responseFuture = syncClientHandler.execute(clientExecutionParams());
        sdkHttpResponseHandler.getValue().headersReceived(SdkHttpFullResponse.builder().statusCode(500).build());
        sdkHttpResponseHandler.getValue().complete();
        assertThatThrownBy(() -> responseFuture.get(1, TimeUnit.SECONDS)).hasCause(exception);

        // Then
        verifyNoMoreInteractions(responseHandler); // Response handler is not called
    }

    private void expectRetrievalFromMocks() {
        when(credentialsProvider.getCredentials()).thenReturn(awsCredentials);
        when(requestConfig.getOriginalRequest()).thenReturn(request);
        when(marshaller.marshall(request)).thenReturn(marshalledRequest);
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(marshaller)
                .withRequestConfig(requestConfig)
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler);
    }

    public AsyncClientConfiguration clientConfiguration() {
        MutableClientConfiguration mutableClientConfiguration = new MutableClientConfiguration()
                .credentialsProvider(credentialsProvider)
                .asyncHttpClient(httpClient);

        mutableClientConfiguration.overrideConfiguration(
                ClientOverrideConfiguration.builder()
                                           .advancedOption(AdvancedClientOption.SIGNER_PROVIDER, new NoOpSignerProvider())
                                           .build());

        new GlobalClientConfigurationDefaults().applyAsyncDefaults(mutableClientConfiguration);

        return mutableClientConfiguration;
    }
}
