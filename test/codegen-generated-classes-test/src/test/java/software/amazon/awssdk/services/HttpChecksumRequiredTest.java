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
import static org.mockito.Matchers.any;

import io.reactivex.Flowable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
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
 * Verify that the "httpChecksumRequired" C2J trait results in a valid MD5 checksum of the payload being included in the HTTP
 * request.
 */
public class HttpChecksumRequiredTest {
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
            AsyncExecuteRequest asyncExecuteRequest = invocation.getArgumentAt(0, AsyncExecuteRequest.class);
            asyncExecuteRequest.responseHandler().onHeaders(successfulHttpResponse);
            asyncExecuteRequest.responseHandler().onStream(Flowable.empty());
            return CompletableFuture.completedFuture(null);
        });
    }

    private <T extends AwsSyncClientBuilder<T, ?> & AwsClientBuilder<T, ?>> T initializeSync(T syncClientBuilder) {
        return initialize(syncClientBuilder.httpClient(httpClient));
    }

    private <T extends AwsAsyncClientBuilder<T, ?> & AwsClientBuilder<T, ?>> T initializeAsync(T asyncClientBuilder) {
        return initialize(asyncClientBuilder.httpClient(httpAsyncClient));
    }

    private <T extends AwsClientBuilder<T, ?>> T initialize(T clientBuilder) {
        return clientBuilder.credentialsProvider(AnonymousCredentialsProvider.create())
                            .region(Region.US_WEST_2);
    }

    @Test
    public void syncJsonSupportsChecksumRequiredTrait() {
        jsonClient.operationWithRequiredChecksum(r -> r.stringMember("foo"));
        assertThat(getSyncRequest().firstMatchingHeader("Content-MD5")).hasValue("g8VCvPTPCMoU01rBlBVt9w==");
    }

    @Test
    public void syncStreamingInputJsonSupportsChecksumRequiredTrait() {
        jsonClient.streamingInputOperationWithRequiredChecksum(r -> {}, RequestBody.fromString("foo"));
        assertThat(getSyncRequest().firstMatchingHeader("Content-MD5")).hasValue("rL0Y20zC+Fzt72VPzMSk2A==");
    }

    @Test
    public void syncStreamingInputXmlSupportsChecksumRequiredTrait() {
        xmlClient.streamingInputOperationWithRequiredChecksum(r -> {}, RequestBody.fromString("foo"));
        assertThat(getSyncRequest().firstMatchingHeader("Content-MD5")).hasValue("rL0Y20zC+Fzt72VPzMSk2A==");
    }

    @Test
    public void syncXmlSupportsChecksumRequiredTrait() {
        xmlClient.operationWithRequiredChecksum(r -> r.stringMember("foo"));
        assertThat(getSyncRequest().firstMatchingHeader("Content-MD5")).hasValue("vqm481l+Lv0zEvdu+duE6Q==");
    }

    @Test
    public void asyncJsonSupportsChecksumRequiredTrait() {
        jsonAsyncClient.operationWithRequiredChecksum(r -> r.stringMember("foo")).join();
        assertThat(getAsyncRequest().firstMatchingHeader("Content-MD5")).hasValue("g8VCvPTPCMoU01rBlBVt9w==");
    }

    @Test
    public void asyncXmlSupportsChecksumRequiredTrait() {
        xmlAsyncClient.operationWithRequiredChecksum(r -> r.stringMember("foo")).join();
        assertThat(getAsyncRequest().firstMatchingHeader("Content-MD5")).hasValue("vqm481l+Lv0zEvdu+duE6Q==");
    }

    @Test(expected = CompletionException.class)
    public void asyncStreamingInputJsonFailsWithChecksumRequiredTrait() {
        jsonAsyncClient.streamingInputOperationWithRequiredChecksum(r -> {}, AsyncRequestBody.fromString("foo")).join();
    }

    @Test(expected = CompletionException.class)
    public void asyncStreamingInputXmlFailsWithChecksumRequiredTrait() {
        xmlAsyncClient.streamingInputOperationWithRequiredChecksum(r -> {}, AsyncRequestBody.fromString("foo")).join();
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
}
