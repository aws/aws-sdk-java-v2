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

package software.amazon.awssdk.http.crt.internal.request;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequestBase;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.crt.internal.CrtAsyncRequestContext;
import software.amazon.awssdk.http.crt.internal.CrtRequestContext;

public class CrtRequestAdapterTest {

    @Test
    public void toAsyncCrtRequest_transferEncodingPresent_doesNotAddContentLength() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader(Header.TRANSFER_ENCODING, "chunked").build();
        SdkHttpContentPublisher publisher = createProvider("content-with-known-length");

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, publisher);

        assertThat(headerNames(crtRequest)).contains(Header.TRANSFER_ENCODING)
                                           .doesNotContain(Header.CONTENT_LENGTH);
    }

    @Test
    public void toAsyncCrtRequest_noTransferEncoding_addsContentLengthFromPublisher() {
        SdkHttpFullRequest sdkRequest = requestBuilder().build();
        SdkHttpContentPublisher publisher = createProvider("content-with-known-length");

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, publisher);

        assertThat(headerNames(crtRequest)).contains(Header.CONTENT_LENGTH)
                                           .doesNotContain(Header.TRANSFER_ENCODING);
    }

    @Test
    public void toCrtRequest_transferEncodingPresent_doesNotAddContentLength() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader(Header.TRANSFER_ENCODING, "chunked").build();

        HttpRequestBase crtRequest = toCrtRequest(sdkRequest);

        assertThat(headerNames(crtRequest)).contains(Header.TRANSFER_ENCODING)
                                           .doesNotContain(Header.CONTENT_LENGTH);
    }

    @Test
    public void toCrtRequest_contentLengthOnRequest_isPreserved() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader(Header.CONTENT_LENGTH, "42").build();

        HttpRequestBase crtRequest = toCrtRequest(sdkRequest);

        assertThat(headerNames(crtRequest)).contains(Header.CONTENT_LENGTH)
                                           .doesNotContain(Header.TRANSFER_ENCODING);
    }

    private static SdkHttpFullRequest.Builder requestBuilder() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost:8080"))
                                 .method(SdkHttpMethod.POST)
                                 .encodedPath("/");
    }

    private static HttpRequestBase toAsyncCrtRequest(SdkHttpFullRequest sdkRequest, SdkHttpContentPublisher publisher) {
        AsyncExecuteRequest asyncRequest = AsyncExecuteRequest.builder()
                                                              .request(sdkRequest)
                                                              .requestContentPublisher(publisher)
                                                              .build();
        CrtAsyncRequestContext context = CrtAsyncRequestContext.builder()
                                                               .request(asyncRequest)
                                                               .readBufferSize(2000)
                                                               .protocol(Protocol.HTTP1_1)
                                                               .build();
        return CrtRequestAdapter.toAsyncCrtRequest(context);
    }

    private static HttpRequestBase toCrtRequest(SdkHttpFullRequest sdkRequest) {
        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                                                              .request(sdkRequest)
                                                              .build();
        CrtRequestContext context = CrtRequestContext.builder()
                                                     .request(executeRequest)
                                                     .readBufferSize(2000)
                                                     .build();
        return CrtRequestAdapter.toCrtRequest(context);
    }

    private static List<String> headerNames(HttpRequestBase crtRequest) {
        return crtRequest.getHeaders().stream().map(HttpHeader::getName).collect(Collectors.toList());
    }
}
