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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.net.URI;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

public class RequestAdapterTest {

    private final RequestAdapter h1Adapter = new RequestAdapter(Protocol.HTTP1_1);
    private final RequestAdapter h2Adapter = new RequestAdapter(Protocol.HTTP2);

    @Test
    public void adapt_h1Request_requestIsCorrect() {
        SdkHttpRequest request = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345/foo/bar/baz"))
                .putRawQueryParameter("foo", "bar")
                .putRawQueryParameter("bar", "baz")
                .putHeader("header1", "header1val")
                .putHeader("header2", "header2val")
                .method(SdkHttpMethod.GET)
                .build();

        HttpRequest adapted = h1Adapter.adapt(request);

        assertThat(adapted.method()).isEqualTo(HttpMethod.valueOf("GET"));
        assertThat(adapted.uri()).isEqualTo("/foo/bar/baz?foo=bar&bar=baz");
        assertThat(adapted.protocolVersion()).isEqualTo(HttpVersion.HTTP_1_1);
        assertThat(adapted.headers().getAll("Host")).containsExactly("localhost:12345");
        assertThat(adapted.headers().getAll("header1")).containsExactly("header1val");
        assertThat(adapted.headers().getAll("header2")).containsExactly("header2val");
    }

    @Test
    public void adapt_h2Request_addsSchemeExtension() {
        SdkHttpRequest request = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345/foo/bar/baz"))
                .putRawQueryParameter("foo", "bar")
                .putRawQueryParameter("bar", "baz")
                .putHeader("header1", "header1val")
                .putHeader("header2", "header2val")
                .method(SdkHttpMethod.GET)
                .build();

        HttpRequest adapted = h2Adapter.adapt(request);

        assertThat(adapted.headers().getAll(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text())).containsExactly("http");
    }

    @Test
    public void adapt_noPathContainsQueryParams() {
        SdkHttpRequest request = SdkHttpRequest.builder()
                .host("localhost:12345")
                .protocol("http")
                .putRawQueryParameter("foo", "bar")
                .putRawQueryParameter("bar", "baz")
                .putHeader("header1", "header1val")
                .putHeader("header2", "header2val")
                .method(SdkHttpMethod.GET)
                .build();

        HttpRequest adapted = h1Adapter.adapt(request);

        assertThat(adapted.method()).isEqualTo(HttpMethod.valueOf("GET"));
        assertThat(adapted.uri()).isEqualTo("/?foo=bar&bar=baz");
        assertThat(adapted.protocolVersion()).isEqualTo(HttpVersion.HTTP_1_1);
        assertThat(adapted.headers().getAll("Host")).containsExactly("localhost:12345");
    }

    @Test
    public void adapt_hostHeaderSet() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpRequest result = h1Adapter.adapt(sdkRequest);
        List<String> hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());
        assertThat(hostHeaders).containsExactly("localhost:12345");
    }

    @Test
    public void adapt_standardHttpsPort_omittedInHeader() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("https://localhost:443/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpRequest result = h1Adapter.adapt(sdkRequest);
        List<String> hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());
        assertThat(hostHeaders).containsExactly("localhost");
    }

    @Test
    public void adapt_containsQueryParamsRequiringEncoding() {
        SdkHttpRequest request = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345"))
                .putRawQueryParameter("java", "☕")
                .putRawQueryParameter("python", "\uD83D\uDC0D")
                .method(SdkHttpMethod.GET)
                .build();

        HttpRequest adapted = h1Adapter.adapt(request);

        assertThat(adapted.uri()).isEqualTo("/?java=%E2%98%95&python=%F0%9F%90%8D");
    }

    @Test
    public void adapt_pathEmpty_setToRoot() {
        SdkHttpRequest request = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345"))
                .method(SdkHttpMethod.GET)
                .build();

        HttpRequest adapted = h1Adapter.adapt(request);

        assertThat(adapted.uri()).isEqualTo("/");
    }

    @Test
    public void adapt_defaultPortUsed() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:80/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpRequest result = h1Adapter.adapt(sdkRequest);
        List<String> hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.size());
        assertEquals("localhost", hostHeaders.get(0));

        sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("https://localhost:443/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        result = h1Adapter.adapt(sdkRequest);
        hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.size());
        assertEquals("localhost", hostHeaders.get(0));
    }

    @Test
    public void adapt_nonStandardHttpPort() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:8080/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpRequest result = h1Adapter.adapt(sdkRequest);
        List<String> hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());

        assertThat(hostHeaders).containsExactly("localhost:8080");
    }

    @Test
    public void adapt_nonStandardHttpsPort() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("https://localhost:8443/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpRequest result = h1Adapter.adapt(sdkRequest);
        List<String> hostHeaders = result.headers()
                .getAll(HttpHeaderNames.HOST.toString());

        assertThat(hostHeaders).containsExactly("localhost:8443");
    }
}
