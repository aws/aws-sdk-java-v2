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

package software.amazon.awssdk.http.apache5.internal.impl;

import java.net.URISyntaxException;
import org.apache.hc.core5.http.HttpEntityContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.BufferedHttpEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache5.internal.Apache5HttpRequestConfig;
import software.amazon.awssdk.http.apache5.internal.RepeatableInputStreamRequestEntity;

class ApacheHttpRequestFactoryTest {

    private Apache5HttpRequestConfig requestConfig;
    private Apache5HttpRequestFactory instance;

    @BeforeEach
    public void setup() {
        instance = new Apache5HttpRequestFactory();
        requestConfig = Apache5HttpRequestConfig.builder()
                                                .connectionAcquireTimeout(Duration.ZERO)
                                                .connectionTimeout(Duration.ZERO)
                                                .socketTimeout(Duration.ZERO)
                                                .build();
    }

    @Test
    public void createSetsHostHeaderByDefault() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        HttpUriRequestBase result = instance.create(request, requestConfig);
        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost:12345", hostHeaders[0].getValue());
    }

    @Test
    public void createRespectsUserHostHeader() {
        String hostOverride = "virtual.host:123";
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
            .uri(URI.create("http://localhost:12345/"))
            .method(SdkHttpMethod.HEAD)
            .putHeader("Host", hostOverride)
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
            .request(sdkRequest)
            .build();

        HttpUriRequestBase result = instance.create(request, requestConfig);

        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals(hostOverride, hostHeaders[0].getValue());
    }

    @Test
    public void createRespectsLowercaseUserHostHeader() {
        String hostOverride = "virtual.host:123";
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
            .uri(URI.create("http://localhost:12345/"))
            .method(SdkHttpMethod.HEAD)
            .putHeader("host", hostOverride)
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
            .request(sdkRequest)
            .build();

        HttpUriRequestBase result = instance.create(request, requestConfig);

        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals(hostOverride, hostHeaders[0].getValue());
    }

    @Test
    public void putRequest_withTransferEncodingChunked_isChunkedAndDoesNotIncludeHeader() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:12345/"))
                                                  .method(SdkHttpMethod.PUT)
                                                  .putHeader("Transfer-Encoding", "chunked")
                                                  .build();
        InputStream inputStream = new ByteArrayInputStream("TestStream".getBytes(StandardCharsets.UTF_8));
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(sdkRequest)
                                                       .contentStreamProvider(() -> inputStream)
                                                       .build();
        HttpUriRequestBase result = instance.create(request, requestConfig);
        Header[] transferEncodingHeaders = result.getHeaders("Transfer-Encoding");
        assertThat(transferEncodingHeaders).isEmpty();

        assertThat(result).isInstanceOf(HttpEntityContainer.class);
        HttpEntity httpEntity = ((HttpEntityContainer) result).getEntity();

        assertThat(httpEntity.isChunked()).isTrue();
        assertThat(httpEntity).isNotInstanceOf(BufferedHttpEntity.class);
        assertThat(httpEntity).isInstanceOf(RepeatableInputStreamRequestEntity.class);
    }

    @Test
    public void defaultHttpPortsAreNotInDefaultHostHeader() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:80/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        HttpUriRequestBase result = instance.create(request, requestConfig);
        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost", hostHeaders[0].getValue());

        sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("https://localhost:443/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        result = instance.create(request, requestConfig);
        hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost", hostHeaders[0].getValue());
    }

    @Test
    public void pathWithLeadingSlash_shouldEncode() {
        assertThat(sanitizedUri("/foobar")).isEqualTo("http://localhost/%2Ffoobar");
    }

    @Test
    public void pathWithOnlySlash_shouldEncode() {
        assertThat(sanitizedUri("/")).isEqualTo("http://localhost/%2F");
    }

    @Test
    public void pathWithoutSlash_shouldReturnSameUri() {
        assertThat(sanitizedUri("path")).isEqualTo("http://localhost/path");
    }

    @Test
    public void pathWithSpecialChars_shouldPreserveEncoding() {
        assertThat(sanitizedUri("/special-chars-%40%24%25")).isEqualTo("http://localhost/%2Fspecial-chars-%40%24%25");
    }

    private String sanitizedUri(String path) {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:80"))
                                                  .encodedPath("/" + path)
                                                  .method(SdkHttpMethod.HEAD)
                                                  .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(sdkRequest)
                                                       .build();

        try {
            return instance.create(request, requestConfig).getUri().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
