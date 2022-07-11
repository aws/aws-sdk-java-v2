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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import io.reactivex.Flowable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;

/**
 * Verify that the "authtype" C2J trait for request type is honored for each requests.
 */
public class NoneAuthTypeRequestTest {

    private SdkHttpClient httpClient;
    private SdkAsyncHttpClient httpAsyncClient;
    private ProtocolRestJsonClient jsonClient;
    private ProtocolRestJsonAsyncClient jsonAsyncClient;
    private ProtocolRestXmlClient xmlClient;
    private ProtocolRestXmlAsyncClient xmlAsyncClient;


    @Before
    public void setup() throws IOException {
        httpClient = Mockito.mock(SdkHttpClient.class);
        httpAsyncClient = Mockito.mock(SdkAsyncHttpClient.class);
        jsonClient = initializeSync(ProtocolRestJsonClient.builder()).build();
        jsonAsyncClient = initializeAsync(ProtocolRestJsonAsyncClient.builder()).build();
        xmlClient = initializeSync(ProtocolRestXmlClient.builder()).build();
        xmlAsyncClient = initializeAsync(ProtocolRestXmlAsyncClient.builder()).build();

        SdkHttpFullResponse successfulHttpResponse = SdkHttpResponse.builder()
                                                                    .statusCode(200)
                                                                    .putHeader("Content-Length", "0")
                                                                    .build();

        ExecutableHttpRequest request = Mockito.mock(ExecutableHttpRequest.class);

        Mockito.when(request.call()).thenReturn(HttpExecuteResponse.builder()
                                                                   .response(successfulHttpResponse)
                                                                   .build());
        Mockito.when(httpClient.prepareRequest(any())).thenReturn(request);
        Mockito.when(httpAsyncClient.execute(any())).thenAnswer(invocation -> {
            AsyncExecuteRequest asyncExecuteRequest = invocation.getArgument(0, AsyncExecuteRequest.class);
            asyncExecuteRequest.responseHandler().onHeaders(successfulHttpResponse);
            asyncExecuteRequest.responseHandler().onStream(Flowable.empty());
            return CompletableFuture.completedFuture(null);
        });
    }

    @Test
    public void sync_json_authorization_is_absent_for_noneAuthType() {
        jsonClient.operationWithNoneAuthType(o -> o.booleanMember(true));
        assertThat(getSyncRequest().firstMatchingHeader("Authorization")).isNotPresent();
    }

    @Test
    public void sync_json_authorization_is_present_for_defaultAuth() {
        jsonClient.jsonValuesOperation();
        assertThat(getSyncRequest().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void async_json_authorization_is_absent_for_noneAuthType() {
        jsonAsyncClient.operationWithNoneAuthType(o -> o.booleanMember(true));
        assertThat(getAsyncRequest().firstMatchingHeader("Authorization")).isNotPresent();
    }

    @Test
    public void async_json_authorization_is_present_for_defaultAuth() {
        jsonAsyncClient.jsonValuesOperation();
        assertThat(getAsyncRequest().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void sync_xml_authorization_is_absent_for_noneAuthType() {
        xmlClient.operationWithNoneAuthType(o -> o.booleanMember(true));
        assertThat(getSyncRequest().firstMatchingHeader("Authorization")).isNotPresent();
    }

    @Test
    public void sync_xml_authorization_is_present_for_defaultAuth() {
        xmlClient.jsonValuesOperation(json -> json.jsonValueMember("one"));
        assertThat(getSyncRequest().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void async_xml_authorization_is_absent_for_noneAuthType() {
        xmlAsyncClient.operationWithNoneAuthType(o -> o.booleanMember(true));
        assertThat(getAsyncRequest().firstMatchingHeader("Authorization")).isNotPresent();
    }

    @Test
    public void async_xml_authorization_is_present_for_defaultAuth() {
        xmlAsyncClient.jsonValuesOperation(json -> json.jsonValueMember("one"));
        assertThat(getAsyncRequest().firstMatchingHeader("Authorization")).isPresent();
    }

    private SdkHttpRequest getSyncRequest() {
        ArgumentCaptor<HttpExecuteRequest> captor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
        Mockito.verify(httpClient).prepareRequest(captor.capture());
        return captor.getValue().httpRequest();
    }

    private SdkHttpRequest getAsyncRequest() {
        ArgumentCaptor<AsyncExecuteRequest> captor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);
        Mockito.verify(httpAsyncClient).execute(captor.capture());
        return captor.getValue().request();
    }

    private <T extends AwsSyncClientBuilder<T, ?> & AwsClientBuilder<T, ?>> T initializeSync(T syncClientBuilder) {
        return initialize(syncClientBuilder.httpClient(httpClient).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("123", "12344"))));
    }

    private <T extends AwsAsyncClientBuilder<T, ?> & AwsClientBuilder<T, ?>> T initializeAsync(T asyncClientBuilder) {
        return initialize(asyncClientBuilder.httpClient(httpAsyncClient));
    }

    private <T extends AwsClientBuilder<T, ?>> T initialize(T clientBuilder) {
        return clientBuilder.region(Region.US_WEST_2);
    }
}