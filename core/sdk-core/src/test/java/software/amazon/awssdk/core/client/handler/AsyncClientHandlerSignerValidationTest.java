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

package software.amazon.awssdk.core.client.handler;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class AsyncClientHandlerSignerValidationTest {
    private final SdkRequest request = ValidSdkObjects.sdkRequest();

    @Mock
    private Marshaller<SdkRequest> marshaller;

    @Mock
    private SdkAsyncHttpClient httpClient;

    @Mock
    private Signer signer;

    @Mock
    private HttpResponseHandler<SdkResponse> responseHandler;

    private CompletableFuture<Void> httpClientFuture = CompletableFuture.completedFuture(null);

    private ArgumentCaptor<AsyncExecuteRequest> executeRequestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);

    @Before
    public void setup() {
        when(httpClient.execute(executeRequestCaptor.capture())).thenReturn(httpClientFuture);
        when(signer.credentialType()).thenReturn(CredentialType.TOKEN);
    }

    @Test
    public void execute_requestHasHttpEndpoint_usesBearerAuth_fails() {
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("http").build();
        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);

        SdkClientConfiguration config = testClientConfiguration();

        SdkAsyncClientHandler sdkAsyncClientHandler = new SdkAsyncClientHandler(config);
        CompletableFuture<SdkResponse> execute = sdkAsyncClientHandler.execute(executionParams());

        assertThatThrownBy(execute::get)
            .hasCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("plaintext HTTP endpoint");
    }

    @Test
    public void execute_interceptorChangesToHttp_usesBearerAuth_fails() {
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("https").build();
        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);

        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
                return context.httpRequest()
                              .toBuilder()
                              .protocol("http")
                              .build();
            }
        };

        SdkClientConfiguration config = testClientConfiguration()
            .toBuilder()
            .option(SdkClientOption.EXECUTION_INTERCEPTORS, Collections.singletonList(interceptor))
            .build();

        SdkAsyncClientHandler sdkAsyncClientHandler = new SdkAsyncClientHandler(config);

        CompletableFuture<SdkResponse> execute = sdkAsyncClientHandler.execute(executionParams());

        assertThatThrownBy(execute::get)
            .hasCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("plaintext HTTP endpoint");
    }

    @Test
    public void execute_interceptorChangesToHttps_usesBearerAuth_succeeds() throws Exception {
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("http").build();

        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);
        when(signer.sign(any(), any())).thenReturn(httpRequest);

        SdkResponse mockSdkResponse = VoidSdkResponse.builder().build();
        when(responseHandler.handle(any(), any())).thenReturn(mockSdkResponse);

        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
                return context.httpRequest()
                              .toBuilder()
                              .protocol("https")
                              .build();
            }
        };

        SdkClientConfiguration config = testClientConfiguration()
            .toBuilder()
            .option(SdkClientOption.EXECUTION_INTERCEPTORS, Collections.singletonList(interceptor))
            .build();

        SdkAsyncClientHandler sdkAsyncClientHandler = new SdkAsyncClientHandler(config);
        CompletableFuture<SdkResponse> execute = sdkAsyncClientHandler.execute(executionParams());

        SdkAsyncHttpResponseHandler capturedHandler = executeRequestCaptor.getValue().responseHandler();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", Arrays.asList("bar"));
        capturedHandler.onHeaders(SdkHttpFullResponse.builder()
                                                     .statusCode(200)
                                                     .headers(headers)
                                                     .build());
        capturedHandler.onStream(new EmptyPublisher<>());

        assertThatNoException().isThrownBy(execute::get);
    }

    @Test
    public void execute_requestHasHttpsEndpoint_usesBearerAuth_succeeds() throws Exception {
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("https").build();

        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);
        when(signer.sign(any(), any())).thenReturn(httpRequest);


        SdkResponse mockSdkResponse = VoidSdkResponse.builder().build();
        when(responseHandler.handle(any(), any())).thenReturn(mockSdkResponse);

        SdkAsyncClientHandler sdkAsyncClientHandler = new SdkAsyncClientHandler(testClientConfiguration());
        CompletableFuture<SdkResponse> execute = sdkAsyncClientHandler.execute(executionParams());

        SdkAsyncHttpResponseHandler capturedHandler = executeRequestCaptor.getValue().responseHandler();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", Arrays.asList("bar"));
        capturedHandler.onHeaders(SdkHttpFullResponse.builder()
                                                     .statusCode(200)
                                                     .headers(headers)
                                                     .build());
        capturedHandler.onStream(new EmptyPublisher<>());

        assertThatNoException().isThrownBy(execute::get);
    }

    @Test
    public void execute_requestHasHttpEndpoint_doesNotBearerAuth_succeeds() throws Exception {
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("http").build();

        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);
        when(signer.credentialType()).thenReturn(CredentialType.of("AWS"));
        when(signer.sign(any(), any())).thenReturn(httpRequest);


        SdkResponse mockSdkResponse = VoidSdkResponse.builder().build();
        when(responseHandler.handle(any(), any())).thenReturn(mockSdkResponse);

        SdkAsyncClientHandler sdkAsyncClientHandler = new SdkAsyncClientHandler(testClientConfiguration());
        CompletableFuture<SdkResponse> execute = sdkAsyncClientHandler.execute(executionParams());

        SdkAsyncHttpResponseHandler capturedHandler = executeRequestCaptor.getValue().responseHandler();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", Arrays.asList("bar"));
        capturedHandler.onHeaders(SdkHttpFullResponse.builder()
                                                     .statusCode(200)
                                                     .headers(headers)
                                                     .build());
        capturedHandler.onStream(new EmptyPublisher<>());

        assertThatNoException().isThrownBy(execute::get);
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> executionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
            .withInput(request)
            .withMarshaller(marshaller)
            .withResponseHandler(responseHandler);
    }

    private SdkClientConfiguration testClientConfiguration() {
        return HttpTestUtils.testClientConfiguration()
                            .toBuilder()
                            .option(SdkClientOption.ASYNC_HTTP_CLIENT, httpClient)
                            .option(SdkAdvancedClientOption.SIGNER, signer)
                            .build();
    }
}
