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

package software.amazon.awssdk.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.AmazonWebServiceRequest;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.RequestConfig;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.config.AdvancedClientOption;
import software.amazon.awssdk.core.config.AsyncClientConfiguration;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.MutableClientConfiguration;
import software.amazon.awssdk.core.config.defaults.GlobalClientConfigurationDefaults;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;

@RunWith(MockitoJUnitRunner.class)
public class AsyncClientHandlerImplTest {
    private AsyncClientHandlerImpl syncClientHandler;

    @Mock
    private AwsCredentialsProvider credentialsProvider;

    private AwsCredentials awsCredentials = AwsCredentials.create("public", "private");

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
                .asyncHttpClient(httpClient)
                .endpoint(URI.create("http://test.com"));

        mutableClientConfiguration.overrideConfiguration(
            ClientOverrideConfiguration.builder()
                                       .advancedOption(AdvancedClientOption.SIGNER_PROVIDER, new NoOpSignerProvider())
                                       .retryPolicy(RetryPolicy.builder().numRetries(0).build())
                                       .build());

        new GlobalClientConfigurationDefaults().applyAsyncDefaults(mutableClientConfiguration);

        return mutableClientConfiguration;
    }
}
