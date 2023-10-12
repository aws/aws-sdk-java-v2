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

package software.amazon.awssdk.services.protocolquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;

public class MoveQueryParamsToBodyTest {

    private static final String CUSTOM_PARAM_NAME = "CustomParamName";
    private static final String CUSTOM_PARAM_VALUE = "CustomParamValue";
    private static final AwsCredentialsProvider CREDENTIALS = StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    private SdkHttpClient syncMockHttpClient;
    private SdkAsyncHttpClient asyncMockHttpClient;
    private ProtocolQueryClient syncClient;
    private ProtocolQueryAsyncClient asyncClient;

    @BeforeEach
    public void setup() throws IOException {
        syncMockHttpClient = mock(SdkHttpClient.class);
        ExecutableHttpRequest mockRequest = mock(ExecutableHttpRequest.class);
        when(mockRequest.call()).thenThrow(new IOException("IO error!"));
        when(syncMockHttpClient.prepareRequest(any())).thenReturn(mockRequest);

        asyncMockHttpClient = mock(SdkAsyncHttpClient.class);
    }

    @AfterEach
    public void teardown() {
        if (syncClient != null) {
            syncClient.close();
        }
        syncClient = null;

        if (asyncClient != null) {
            asyncClient.close();
        }
        asyncClient = null;
    }

    private void verifyParametersMovedToBody_syncClient(ArgumentCaptor<HttpExecuteRequest> requestCaptor) throws IOException {
        ContentStreamProvider requestContent = requestCaptor.getValue().contentStreamProvider().get();
        String contentString = IoUtils.toUtf8String(requestContent.newStream());

        assertThat(contentString).contains(CUSTOM_PARAM_NAME + "=" + CUSTOM_PARAM_VALUE);
    }

    private void verifyParametersMovedToBody_asyncClient(ArgumentCaptor<AsyncExecuteRequest> requestCaptor) {
        SdkHttpContentPublisher content = requestCaptor.getValue().requestContentPublisher();
        List<ByteBuffer> chunks = Flowable.fromPublisher(content).toList().blockingGet();
        String contentString = new String(chunks.get(0).array());
        assertThat(contentString).contains(CUSTOM_PARAM_NAME + "=" + CUSTOM_PARAM_VALUE);
    }

    @Test
    public void customInterceptor_syncClient_additionalQueryParamsAdded_paramsAlsoMovedToBody() throws IOException {
        syncClient = ProtocolQueryClient.builder()
                                        .overrideConfiguration(o -> o.addExecutionInterceptor(new AdditionalQueryParamInterceptor()))
                                        .region(Region.US_WEST_2)
                                        .credentialsProvider(CREDENTIALS)
                                        .httpClient(syncMockHttpClient)
                                        .build();

        ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

        assertThatThrownBy(() -> syncClient.membersInQueryParams(r -> r.stringQueryParam("hello")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("IO");

        verify(syncMockHttpClient, atLeast(1)).prepareRequest(requestCaptor.capture());
        verifyParametersMovedToBody_syncClient(requestCaptor);
    }

    @Test
    public void customInterceptor_asyncClient_additionalQueryParamsAdded_paramsAlsoMovedToBody() throws IOException {
        asyncClient = ProtocolQueryAsyncClient.builder()
                                        .overrideConfiguration(o -> o.addExecutionInterceptor(new AdditionalQueryParamInterceptor()))
                                        .region(Region.US_WEST_2)
                                        .credentialsProvider(CREDENTIALS)
                                        .httpClient(asyncMockHttpClient)
                                        .build();

        ArgumentCaptor<AsyncExecuteRequest> requestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);
        asyncClient.membersInQueryParams(r -> r.stringQueryParam("hello"));

        verify(asyncMockHttpClient, atLeast(1)).execute(requestCaptor.capture());
        verifyParametersMovedToBody_asyncClient(requestCaptor);
    }

    @Test
    public void requestOverrideConfiguration_syncClient_additionalQueryParamsAdded_paramsAlsoMovedToBody() throws IOException {
        syncClient = ProtocolQueryClient.builder()
                                        .region(Region.US_WEST_2)
                                        .credentialsProvider(CREDENTIALS)
                                        .httpClient(syncMockHttpClient)
                                        .build();

        ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

        assertThatThrownBy(() -> syncClient.membersInQueryParams(r -> r.stringQueryParam("hello")
                                                                       .overrideConfiguration(createOverrideConfigWithQueryParams())))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("IO");

        verify(syncMockHttpClient, atLeast(1)).prepareRequest(requestCaptor.capture());
        verifyParametersMovedToBody_syncClient(requestCaptor);
    }

    @Test
    public void requestOverrideConfiguration_asyncClient_additionalQueryParamsAdded_paramsAlsoMovedToBody() throws IOException {
        asyncClient = ProtocolQueryAsyncClient.builder()
                                        .region(Region.US_WEST_2)
                                        .credentialsProvider(CREDENTIALS)
                                        .httpClient(asyncMockHttpClient)
                                        .build();

        ArgumentCaptor<AsyncExecuteRequest> requestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);

        asyncClient.membersInQueryParams(r -> r.stringQueryParam("hello").overrideConfiguration(createOverrideConfigWithQueryParams()));
        verify(asyncMockHttpClient, atLeast(1)).execute(requestCaptor.capture());
        verifyParametersMovedToBody_asyncClient(requestCaptor);
    }

    @Test
    public void syncClient_noQueryParamsAdded_onlyContainsOriginalContent() throws IOException {
        syncClient = ProtocolQueryClient.builder()
                                        .region(Region.US_WEST_2)
                                        .credentialsProvider(CREDENTIALS)
                                        .httpClient(syncMockHttpClient)
                                        .build();

        ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

        assertThatThrownBy(() -> syncClient.allTypes(r -> r.stringMember("hello")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("IO");

        verify(syncMockHttpClient, atLeast(1)).prepareRequest(requestCaptor.capture());
        ContentStreamProvider requestContent = requestCaptor.getValue().contentStreamProvider().get();
        String contentString = IoUtils.toUtf8String(requestContent.newStream());

        assertThat(contentString).isEqualTo("Action=QueryService.AllTypes&Version=2016-03-11&StringMember=hello");
    }

    @Test
    public void asyncClient_noQueryParamsAdded_onlyContainsOriginalContent() throws IOException {
        asyncClient = ProtocolQueryAsyncClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(CREDENTIALS)
                                              .httpClient(asyncMockHttpClient)
                                              .build();

        ArgumentCaptor<AsyncExecuteRequest> requestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);
        asyncClient.allTypes(r -> r.stringMember("hello"));

        verify(asyncMockHttpClient, atLeast(1)).execute(requestCaptor.capture());

        SdkHttpContentPublisher content = requestCaptor.getValue().requestContentPublisher();
        List<ByteBuffer> chunks = Flowable.fromPublisher(content).toList().blockingGet();
        String contentString = new String(chunks.get(0).array());

        assertThat(contentString).isEqualTo("Action=QueryService.AllTypes&Version=2016-03-11&StringMember=hello");
    }

    private AwsRequestOverrideConfiguration createOverrideConfigWithQueryParams() {
        Map<String, List<String>> queryMap = new HashMap<>();
        List<String> paramValues = new ArrayList<>();
        paramValues.add(CUSTOM_PARAM_VALUE);
        queryMap.put(CUSTOM_PARAM_NAME, paramValues);
        return AwsRequestOverrideConfiguration.builder().rawQueryParameters(queryMap).build();
    }

    private static class AdditionalQueryParamInterceptor implements ExecutionInterceptor {
        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            return context.httpRequest().toBuilder()
                                        .putRawQueryParameter(CUSTOM_PARAM_NAME, CUSTOM_PARAM_VALUE)
                                        .build();
        }
    }
}
