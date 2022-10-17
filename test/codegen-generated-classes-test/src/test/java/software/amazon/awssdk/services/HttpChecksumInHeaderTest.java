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
import static software.amazon.awssdk.core.HttpChecksumConstant.HTTP_CHECKSUM_VALUE;

import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
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
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;

/**
 * Verify that the "HttpChecksum" C2J trait results in a valid checksum of the payload being included in the HTTP
 * request.
 */
public class HttpChecksumInHeaderTest {

    public static Set<String> VALID_CHECKSUM_HEADERS =
        Arrays.stream(Algorithm.values()).map(x -> "x-amz-checksum-" + x).collect(Collectors.toSet());

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

    @After
    public void clear() {
        CaptureChecksumValueInterceptor.reset();
    }

    @Test
    public void sync_json_nonStreaming_unsignedPayload_with_Sha1_in_header() {
        //  jsonClient.flexibleCheckSumOperationWithShaChecksum(r -> r.stringMember("Hello world"));
        jsonClient.operationWithChecksumNonStreaming(
            r -> r.stringMember("Hello world").checksumAlgorithm(ChecksumAlgorithm.SHA1).build());
        assertThat(getSyncRequest().firstMatchingHeader("Content-MD5")).isNotPresent();
        //Note that content will be of form "{"stringMember":"Hello world"}"
        assertThat(getSyncRequest().firstMatchingHeader("x-amz-checksum-sha1")).hasValue("M68rRwFal7o7B3KEMt3m0w39TaA=");
        // Assertion to make sure signer was not executed
        assertThat(getSyncRequest().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();

        assertThat(CaptureChecksumValueInterceptor.interceptorComputedChecksum).isEqualTo("M68rRwFal7o7B3KEMt3m0w39TaA=");

    }

    @Test
    public void aync_json_nonStreaming_unsignedPayload_with_Sha1_in_header() {
        //  jsonClient.flexibleCheckSumOperationWithShaChecksum(r -> r.stringMember("Hello world"));
        jsonAsyncClient.operationWithChecksumNonStreaming(
            r -> r.checksumAlgorithm(ChecksumAlgorithm.SHA1).stringMember("Hello world").build());
        assertThat(getAsyncRequest().firstMatchingHeader("Content-MD5")).isNotPresent();
        //Note that content will be of form "{"stringMember":"Hello world"}"
        assertThat(getAsyncRequest().firstMatchingHeader("x-amz-checksum-sha1")).hasValue("M68rRwFal7o7B3KEMt3m0w39TaA=");
        // Assertion to make sure signer was not executed
        assertThat(getAsyncRequest().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();
        assertThat(CaptureChecksumValueInterceptor.interceptorComputedChecksum).isEqualTo("M68rRwFal7o7B3KEMt3m0w39TaA=");


    }

    @Test
    public void sync_xml_nonStreaming_unsignedPayload_with_Sha1_in_header() {
        //  jsonClient.flexibleCheckSumOperationWithShaChecksum(r -> r.stringMember("Hello world"));
        xmlClient.operationWithChecksumNonStreaming(r -> r.stringMember("Hello world")
                                                          .checksumAlgorithm(software.amazon.awssdk.services.protocolrestxml.model.ChecksumAlgorithm.SHA1).build());
        assertThat(getSyncRequest().firstMatchingHeader("Content-MD5")).isNotPresent();
        //Note that content will be of form "<?xml version="1.0" encoding="UTF-8"?><stringMember>Hello world</stringMember>"
        assertThat(getSyncRequest().firstMatchingHeader("x-amz-checksum-sha1")).hasValue("FB/utBbwFLbIIt5ul3Ojuy5dKgU=");
        // Assertion to make sure signer was not executed
        assertThat(getSyncRequest().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();

        assertThat(CaptureChecksumValueInterceptor.interceptorComputedChecksum).isEqualTo("FB/utBbwFLbIIt5ul3Ojuy5dKgU=");

    }

    @Test
    public void sync_xml_nonStreaming_unsignedEmptyPayload_with_Sha1_in_header() {
        //  jsonClient.flexibleCheckSumOperationWithShaChecksum(r -> r.stringMember("Hello world"));
        xmlClient.operationWithChecksumNonStreaming(r -> r.checksumAlgorithm(software.amazon.awssdk.services.protocolrestxml.model.ChecksumAlgorithm.SHA1).build());
        assertThat(getSyncRequest().firstMatchingHeader("Content-MD5")).isNotPresent();
        //Note that content will be of form "<?xml version="1.0" encoding="UTF-8"?><stringMember>Hello world</stringMember>"
        assertThat(getSyncRequest().firstMatchingHeader("x-amz-checksum-sha1")).isNotPresent();


        Map<String, List<String>> requestHeaders = getSyncRequest().headers();

        boolean disjoint = Collections.disjoint(VALID_CHECKSUM_HEADERS, requestHeaders.keySet());
        assertThat(disjoint).isTrue();

        // Assertion to make sure signer was not executed
        assertThat(getSyncRequest().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();

        assertThat(CaptureChecksumValueInterceptor.interceptorComputedChecksum).isNull();

    }

    @Test
    public void aync_xml_nonStreaming_unsignedPayload_with_Sha1_in_header() {
        //  jsonClient.flexibleCheckSumOperationWithShaChecksum(r -> r.stringMember("Hello world"));

        xmlAsyncClient.operationWithChecksumNonStreaming(r -> r.stringMember("Hello world")
                                                               .checksumAlgorithm(software.amazon.awssdk.services.protocolrestxml.model.ChecksumAlgorithm.SHA1).build()).join();
        assertThat(getAsyncRequest().firstMatchingHeader("Content-MD5")).isNotPresent();
        //Note that content will be of form <?xml version="1.0" encoding="UTF-8"?><stringMember>Hello world</stringMember>"
        assertThat(getAsyncRequest().firstMatchingHeader("x-amz-checksum-sha1")).hasValue("FB/utBbwFLbIIt5ul3Ojuy5dKgU=");
        // Assertion to make sure signer was not executed
        assertThat(getAsyncRequest().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();
        assertThat(CaptureChecksumValueInterceptor.interceptorComputedChecksum).isEqualTo("FB/utBbwFLbIIt5ul3Ojuy5dKgU=");

    }

    @Test
    public void aync_xml_nonStreaming_unsignedEmptyPayload_with_Sha1_in_header() {
        //  jsonClient.flexibleCheckSumOperationWithShaChecksum(r -> r.stringMember("Hello world"));

        xmlAsyncClient.operationWithChecksumNonStreaming(r -> r.checksumAlgorithm(software.amazon.awssdk.services.protocolrestxml.model.ChecksumAlgorithm.SHA1).build()).join();


        Map<String, List<String>> requestHeaders = getAsyncRequest().headers();

        boolean disjoint = Collections.disjoint(VALID_CHECKSUM_HEADERS, requestHeaders.keySet());
        assertThat(disjoint).isTrue();

        assertThat(getAsyncRequest().firstMatchingHeader("Content-MD5")).isNotPresent();
        //Note that content will be of form <?xml version="1.0" encoding="UTF-8"?><stringMember>Hello world</stringMember>"
        assertThat(getAsyncRequest().firstMatchingHeader("x-amz-checksum-sha1")).isNotPresent();
        // Assertion to make sure signer was not executed
        assertThat(getAsyncRequest().firstMatchingHeader("x-amz-content-sha256")).isNotPresent();
        assertThat(CaptureChecksumValueInterceptor.interceptorComputedChecksum).isNull();

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
        return initialize(syncClientBuilder.httpClient(httpClient)
                                           .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureChecksumValueInterceptor())));
    }

    private <T extends AwsAsyncClientBuilder<T, ?> & AwsClientBuilder<T, ?>> T initializeAsync(T asyncClientBuilder) {
        return initialize(asyncClientBuilder.httpClient(httpAsyncClient)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureChecksumValueInterceptor())));
    }

    private <T extends AwsClientBuilder<T, ?>> T initialize(T clientBuilder) {
        return clientBuilder.credentialsProvider(AnonymousCredentialsProvider.create())
                            .region(Region.US_WEST_2);
    }


    private static class CaptureChecksumValueInterceptor implements ExecutionInterceptor {
        private static String interceptorComputedChecksum;

        private static void reset() {
            interceptorComputedChecksum = null;
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            interceptorComputedChecksum = executionAttributes.getAttribute(HTTP_CHECKSUM_VALUE);

        }
    }
}