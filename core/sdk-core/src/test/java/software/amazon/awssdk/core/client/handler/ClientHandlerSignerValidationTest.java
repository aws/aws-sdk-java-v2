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

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
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
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class ClientHandlerSignerValidationTest {
    private final SdkRequest request = ValidSdkObjects.sdkRequest();

    @Mock
    private Marshaller<SdkRequest> marshaller;

    @Mock
    private SdkHttpClient httpClient;

    @Mock
    private Signer signer;

    @Mock
    private HttpResponseHandler<SdkResponse> responseHandler;

    private CompletableFuture<Void> httpClientFuture = CompletableFuture.completedFuture(null);

    @Mock
    private ExecutableHttpRequest httpClientCall;

    @Before
    public void setup() throws Exception {
        when(httpClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(httpClientCall);
        when(httpClientCall.call()).thenReturn(
            HttpExecuteResponse.builder()
                               .response(SdkHttpFullResponse.builder()
                                                            .statusCode(200)
                                                            .build())
                               .build());
        when(signer.credentialType()).thenReturn(CredentialType.TOKEN);
        SdkResponse mockSdkResponse = VoidSdkResponse.builder().build();
        when(responseHandler.handle(any(), any())).thenReturn(mockSdkResponse);
    }

    @Test
    public void execute_requestHasHttpEndpoint_usesBearerAuth_fails() {
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("http").build();
        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);

        SdkClientConfiguration config = testClientConfiguration();

        SdkSyncClientHandler sdkSyncClientHandler = new SdkSyncClientHandler(config);

        assertThatThrownBy(() -> sdkSyncClientHandler.execute(executionParams()))
            .isInstanceOf(SdkClientException.class)
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

        SdkSyncClientHandler sdkSyncClientHandler = new SdkSyncClientHandler(config);

        assertThatThrownBy(() -> sdkSyncClientHandler.execute(executionParams()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("plaintext HTTP endpoint");
    }

    @Test
    public void execute_interceptorChangesToHttps_usesBearerAuth_succeeds() {
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("http").build();

        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);
        when(signer.sign(any(), any())).thenReturn(httpRequest);

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

        SdkSyncClientHandler sdkSyncClientHandler = new SdkSyncClientHandler(config);

        assertThatNoException().isThrownBy(() -> sdkSyncClientHandler.execute(executionParams()));
    }

    @Test
    public void execute_requestHasHttpsEndpoint_usesBearerAuth_succeeds(){
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("https").build();

        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);
        when(signer.sign(any(), any())).thenReturn(httpRequest);

        SdkSyncClientHandler sdkSyncClientHandler = new SdkSyncClientHandler(testClientConfiguration());

        assertThatNoException().isThrownBy(() -> sdkSyncClientHandler.execute(executionParams()));
    }

    @Test
    public void execute_requestHasHttpEndpoint_doesNotBearerAuth_succeeds(){
        SdkHttpFullRequest httpRequest = ValidSdkObjects.sdkHttpFullRequest().protocol("http").build();

        when(marshaller.marshall(any(SdkRequest.class))).thenReturn(httpRequest);
        when(signer.sign(any(), any())).thenReturn(httpRequest);
        when(signer.credentialType()).thenReturn(CredentialType.of("AWS"));

        SdkSyncClientHandler sdkSyncClientHandler = new SdkSyncClientHandler(testClientConfiguration());

        assertThatNoException().isThrownBy(() -> sdkSyncClientHandler.execute(executionParams()));
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
                            .option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient)
                            .option(SdkAdvancedClientOption.SIGNER, signer)
                            .build();
    }
}
