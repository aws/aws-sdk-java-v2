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

package software.amazon.awssdk.authcrt.signer.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.BinaryUtils;

public class CrtHttpRequestConverterTest {

    CrtHttpRequestConverter converter;

    @Before
    public void setup() {
        converter = new CrtHttpRequestConverter();
    }

    @Test
    public void request_withHeaders_isConvertedToCrtFormat() {
        String data = "data";
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.POST)
                                                       .contentStreamProvider(() -> new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)))
                                                       .putHeader("x-amz-archive-description", "test  test")
                                                       .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                       .encodedPath("/")
                                                       .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                                       .build();

        HttpRequest crtHttpRequest = converter.requestToCrt(request);

        assertThat(crtHttpRequest.getMethod()).isEqualTo("POST");
        assertThat(crtHttpRequest.getEncodedPath()).isEqualTo("/");

        List<HttpHeader> headers = crtHttpRequest.getHeaders();
        HttpHeader[] headersAsArray = crtHttpRequest.getHeadersAsArray();
        assertThat(headers.size()).isEqualTo(2);
        assertThat(headersAsArray.length).isEqualTo(2);
        assertThat(headers.get(0).getName()).isEqualTo("Host");
        assertThat(headers.get(0).getValue()).isEqualTo("demo.us-east-1.amazonaws.com");
        assertThat(headersAsArray[1].getName()).isEqualTo("x-amz-archive-description");
        assertThat(headersAsArray[1].getValue()).isEqualTo("test  test");

        assertStream(data, crtHttpRequest.getBodyStream());
        assertHttpRequestSame(request, crtHttpRequest);
    }

    @Test
    public void request_withQueryParams_isConvertedToCrtFormat() {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .putRawQueryParameter("param1", "value1")
                                                       .putRawQueryParameter("param2", Arrays.asList("value2-1", "value2-2"))
                                                       .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                       .encodedPath("/path")
                                                       .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                                       .build();
        HttpRequest crtHttpRequest = converter.requestToCrt(request);
        assertThat(crtHttpRequest.getMethod()).isEqualTo("GET");
        assertThat(crtHttpRequest.getEncodedPath()).isEqualTo("/path?param1=value1&param2=value2-1&param2=value2-2");
        assertHttpRequestSame(request, crtHttpRequest);
    }

    @Test
    public void request_withEmptyPath_isConvertedToCrtFormat() {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                       .encodedPath("")
                                                       .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                                       .build();
        HttpRequest crtHttpRequest = converter.requestToCrt(request);
        assertThat(crtHttpRequest.getEncodedPath()).isEqualTo("/");
        assertHttpRequestSame(request, crtHttpRequest);
    }

    @Test
    public void request_byteArray_isConvertedToCrtStream() {
        byte[] data = new byte[144];
        Arrays.fill(data, (byte) 0x2A);

        HttpRequestBodyStream crtStream = converter.toCrtStream(data);

        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16*1024]);
        crtStream.sendRequestBody(byteBuffer);
        byteBuffer.flip();

        String result = new String(BinaryUtils.copyBytesFrom(byteBuffer), StandardCharsets.UTF_8);
        assertThat(result.length()).isEqualTo(144);
        assertThat(result).containsPattern("^\\*+$");
    }

    private void assertHttpRequestSame(SdkHttpFullRequest originalRequest, HttpRequest crtRequest) {
        SdkHttpFullRequest sdkRequest = converter.crtRequestToHttp(originalRequest, crtRequest);

        assertThat(sdkRequest.method()).isEqualTo(originalRequest.method());
        assertThat(sdkRequest.protocol()).isEqualTo(originalRequest.protocol());
        assertThat(sdkRequest.host()).isEqualTo(originalRequest.host());
        assertThat(sdkRequest.encodedPath()).isEqualTo(originalRequest.encodedPath());
        assertThat(sdkRequest.headers()).isEqualTo(originalRequest.headers());
        assertThat(sdkRequest.rawQueryParameters()).isEqualTo(originalRequest.rawQueryParameters());
    }

    private void assertStream(String expectedData, HttpRequestBodyStream crtStream) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16*1024]);
        crtStream.sendRequestBody(byteBuffer);
        byteBuffer.flip();

        String result = new String(BinaryUtils.copyBytesFrom(byteBuffer), StandardCharsets.UTF_8);
        assertThat(result.length()).isEqualTo(expectedData.length());
        assertThat(result).isEqualTo(expectedData);
    }

}