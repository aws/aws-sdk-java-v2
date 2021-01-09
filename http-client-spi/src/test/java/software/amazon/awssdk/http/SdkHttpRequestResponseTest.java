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

package software.amazon.awssdk.http;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.Test;

/**
 * Verify that {@link DefaultSdkHttpFullRequest} and {@link DefaultSdkHttpFullResponse} properly fulfill the contracts in their
 * interfaces.
 */
public class SdkHttpRequestResponseTest {
    @Test
    public void optionalValuesAreOptional() {
        assertThat(validRequest().contentStreamProvider()).isNotPresent();
        assertThat(validResponse().content()).isNotPresent();
        assertThat(validResponse().statusText()).isNotPresent();
    }

    @Test
    public void mapsAreNotCopiedWhenRoundTrippedToBuilderWithoutModification() {
        SdkHttpFullRequest request = validRequestWithMaps();
        SdkHttpFullRequest request2 = request.toBuilder().build();
        assertThat(request2.headers()).isSameAs(request.headers());
        assertThat(request2.rawQueryParameters()).isSameAs(request.rawQueryParameters());

        SdkHttpResponse response = validResponseWithMaps();
        SdkHttpResponse response2 = response.toBuilder().build();
        assertThat(response2.headers()).isSameAs(response.headers());
    }

    @Test
    public void requestHeaderMapsAreCopiedWhenModified() {
        assertRequestHeaderMapsAreCopied(b -> b.putHeader("foo", "bar"));
        assertRequestHeaderMapsAreCopied(b -> b.putHeader("foo", singletonList("bar")));
        assertRequestHeaderMapsAreCopied(b -> b.appendHeader("foo", "bar"));
        assertRequestHeaderMapsAreCopied(b -> b.headers(emptyMap()));
        assertRequestHeaderMapsAreCopied(b -> b.clearHeaders());
        assertRequestHeaderMapsAreCopied(b -> b.removeHeader("Accept"));
    }

    @Test
    public void requestQueryStringMapsAreCopiedWhenModified() {
        assertRequestQueryStringMapsAreCopied(b -> b.putRawQueryParameter("foo", "bar"));
        assertRequestQueryStringMapsAreCopied(b -> b.putRawQueryParameter("foo", singletonList("bar")));
        assertRequestQueryStringMapsAreCopied(b -> b.appendRawQueryParameter("foo", "bar"));
        assertRequestQueryStringMapsAreCopied(b -> b.rawQueryParameters(emptyMap()));
        assertRequestQueryStringMapsAreCopied(b -> b.clearQueryParameters());
        assertRequestQueryStringMapsAreCopied(b -> b.removeQueryParameter("Accept"));
    }

    @Test
    public void responseHeaderMapsAreCopiedWhenModified() {
        assertResponseHeaderMapsAreCopied(b -> b.putHeader("foo", "bar"));
        assertResponseHeaderMapsAreCopied(b -> b.putHeader("foo", singletonList("bar")));
        assertResponseHeaderMapsAreCopied(b -> b.appendHeader("foo", "bar"));
        assertResponseHeaderMapsAreCopied(b -> b.headers(emptyMap()));
        assertResponseHeaderMapsAreCopied(b -> b.clearHeaders());
        assertResponseHeaderMapsAreCopied(b -> b.removeHeader("Accept"));
    }

    private void assertRequestHeaderMapsAreCopied(Consumer<SdkHttpRequest.Builder> mutation) {
        SdkHttpFullRequest request = validRequestWithMaps();
        Map<String, List<String>> originalQuery = new LinkedHashMap<>(request.headers());
        SdkHttpFullRequest.Builder builder = request.toBuilder();

        assertThat(request.headers()).isEqualTo(builder.headers());

        builder.applyMutation(mutation);
        SdkHttpFullRequest request2 = builder.build();

        assertThat(request.headers()).isEqualTo(originalQuery);
        assertThat(request.headers()).isNotEqualTo(request2.headers());
    }

    private void assertRequestQueryStringMapsAreCopied(Consumer<SdkHttpRequest.Builder> mutation) {
        SdkHttpFullRequest request = validRequestWithMaps();
        Map<String, List<String>> originalQuery = new LinkedHashMap<>(request.rawQueryParameters());
        SdkHttpFullRequest.Builder builder = request.toBuilder();

        assertThat(request.rawQueryParameters()).isEqualTo(builder.rawQueryParameters());

        builder.applyMutation(mutation);
        SdkHttpFullRequest request2 = builder.build();

        assertThat(request.rawQueryParameters()).isEqualTo(originalQuery);
        assertThat(request.rawQueryParameters()).isNotEqualTo(request2.rawQueryParameters());
    }

    private void assertResponseHeaderMapsAreCopied(Consumer<SdkHttpResponse.Builder> mutation) {
        SdkHttpResponse response = validResponseWithMaps();
        Map<String, List<String>> originalQuery = new LinkedHashMap<>(response.headers());
        SdkHttpResponse.Builder builder = response.toBuilder();

        assertThat(response.headers()).isEqualTo(builder.headers());

        builder.applyMutation(mutation);
        SdkHttpResponse response2 = builder.build();

        assertThat(response.headers()).isEqualTo(originalQuery);
        assertThat(response.headers()).isNotEqualTo(response2.headers());
    }

    @Test
    public void headersAreUnmodifiable() {
        assertThatThrownBy(() -> validRequest().headers().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> validResponse().headers().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> validRequest().toBuilder().headers().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> validResponse().toBuilder().headers().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> validRequest().toBuilder().build().headers().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> validResponse().toBuilder().build().headers().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void queryStringsAreUnmodifiable() {
        assertThatThrownBy(() -> validRequest().rawQueryParameters().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> validRequest().toBuilder().rawQueryParameters().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> validRequest().toBuilder().build().rawQueryParameters().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void uriConversionIsCorrect() {
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost"))).isEqualTo("http://localhost");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(80))).isEqualTo("http://localhost");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(8080))).isEqualTo("http://localhost:8080");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(443))).isEqualTo("http://localhost:443");
        assertThat(normalizedUri(b -> b.protocol("https").host("localhost").port(443))).isEqualTo("https://localhost");
        assertThat(normalizedUri(b -> b.protocol("https").host("localhost").port(8443))).isEqualTo("https://localhost:8443");
        assertThat(normalizedUri(b -> b.protocol("https").host("localhost").port(80))).isEqualTo("https://localhost:80");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(80).encodedPath("foo")))
                .isEqualTo("http://localhost/foo");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(80).encodedPath("/foo")))
                .isEqualTo("http://localhost/foo");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(80).encodedPath("foo/")))
                .isEqualTo("http://localhost/foo/");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(80).encodedPath("/foo/")))
                .isEqualTo("http://localhost/foo/");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(80).putRawQueryParameter("foo", "bar ")))
                .isEqualTo("http://localhost?foo=bar%20");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(80).encodedPath("/foo").putRawQueryParameter("foo", "bar")))
                .isEqualTo("http://localhost/foo?foo=bar");
        assertThat(normalizedUri(b -> b.protocol("http").host("localhost").port(80).encodedPath("foo/").putRawQueryParameter("foo", "bar")))
                .isEqualTo("http://localhost/foo/?foo=bar");
    }

    private String normalizedUri(Consumer<SdkHttpRequest.Builder> builderMutator) {
        return validRequestBuilder().applyMutation(builderMutator).build().getUri().toString();
    }

    @Test
    public void protocolNormalizationIsCorrect() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> normalizedProtocol(null));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> normalizedProtocol("foo"));
        assertThat(normalizedProtocol("http")).isEqualTo("http");
        assertThat(normalizedProtocol("https")).isEqualTo("https");
        assertThat(normalizedProtocol("HtTp")).isEqualTo("http");
        assertThat(normalizedProtocol("HtTpS")).isEqualTo("https");
    }

    private String normalizedProtocol(String protocol) {
        return validRequestBuilder().protocol(protocol).build().protocol();
    }

    @Test
    public void hostNormalizationIsCorrect() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> normalizedHost(null));
        assertThat(normalizedHost("foo")).isEqualTo("foo");
    }

    private String normalizedHost(String host) {
        return validRequestBuilder().host(host).build().host();
    }

    @Test
    public void portNormalizationIsCorrect() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> normalizedPort("http", -2));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> normalizedPort("https", -2));
        assertThat(normalizedPort("http", -1)).isEqualTo(80);
        assertThat(normalizedPort("http", null)).isEqualTo(80);
        assertThat(normalizedPort("https", -1)).isEqualTo(443);
        assertThat(normalizedPort("https", null)).isEqualTo(443);
        assertThat(normalizedPort("http", 8080)).isEqualTo(8080);
        assertThat(normalizedPort("https", 8443)).isEqualTo(8443);
    }

    private int normalizedPort(String protocol, Integer port) {
        return validRequestBuilder().protocol(protocol).port(port).build().port();
    }

    @Test
    public void requestPathNormalizationIsCorrect() {
        assertThat(normalizedPath(null)).isEqualTo("");
        assertThat(normalizedPath("/")).isEqualTo("/");
        assertThat(normalizedPath(" ")).isEqualTo("/ ");
        assertThat(normalizedPath(" /")).isEqualTo("/ /");
        assertThat(normalizedPath("/ ")).isEqualTo("/ ");
        assertThat(normalizedPath("/ /")).isEqualTo("/ /");
        assertThat(normalizedPath(" / ")).isEqualTo("/ / ");
        assertThat(normalizedPath("/Foo/")).isEqualTo("/Foo/");
        assertThat(normalizedPath("Foo/")).isEqualTo("/Foo/");
        assertThat(normalizedPath("Foo")).isEqualTo("/Foo");
        assertThat(normalizedPath("/Foo/bar/")).isEqualTo("/Foo/bar/");
        assertThat(normalizedPath("Foo/bar/")).isEqualTo("/Foo/bar/");
        assertThat(normalizedPath("/Foo/bar")).isEqualTo("/Foo/bar");
        assertThat(normalizedPath("Foo/bar")).isEqualTo("/Foo/bar");
        assertThat(normalizedPath("%2F")).isEqualTo("/%2F");
    }

    private String normalizedPath(String path) {
        return validRequestBuilder().encodedPath(path).build().encodedPath();
    }

    @Test
    public void requestMethodNormalizationIsCorrect() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> normalizedMethod(null));
        assertThat(normalizedMethod(SdkHttpMethod.POST)).isEqualTo(SdkHttpMethod.POST);
    }

    private SdkHttpMethod normalizedMethod(SdkHttpMethod method) {
        return validRequestBuilder().method(method).build().method();
    }

    @Test
    public void requestQueryParamNormalizationIsCorrect() {
        headerOrQueryStringNormalizationIsCorrect(() -> new BuilderProxy() {
            private final SdkHttpRequest.Builder builder = validRequestBuilder();

            @Override
            public BuilderProxy setValue(String key, String value) {
                builder.putRawQueryParameter(key, value);
                return this;
            }

            @Override
            public BuilderProxy appendValue(String key, String value) {
                builder.appendRawQueryParameter(key, value);
                return this;
            }

            @Override
            public BuilderProxy setValues(String key, List<String> values) {
                builder.putRawQueryParameter(key, values);
                return this;
            }

            @Override
            public BuilderProxy removeValue(String key) {
                builder.removeQueryParameter(key);
                return this;
            }

            @Override
            public BuilderProxy clearValues() {
                builder.clearQueryParameters();
                return this;
            }

            @Override
            public BuilderProxy setMap(Map<String, List<String>> map) {
                builder.rawQueryParameters(map);
                return this;
            }

            @Override
            public Map<String, List<String>> getMap() {
                return builder.build().rawQueryParameters();
            }
        });
    }

    @Test
    public void requestHeaderNormalizationIsCorrect() {
        headerOrQueryStringNormalizationIsCorrect(() -> new BuilderProxy() {
            private final SdkHttpRequest.Builder builder = validRequestBuilder();

            @Override
            public BuilderProxy setValue(String key, String value) {
                builder.putHeader(key, value);
                return this;
            }

            @Override
            public BuilderProxy appendValue(String key, String value) {
                builder.appendHeader(key, value);
                return this;
            }

            @Override
            public BuilderProxy setValues(String key, List<String> values) {
                builder.putHeader(key, values);
                return this;
            }

            @Override
            public BuilderProxy removeValue(String key) {
                builder.removeHeader(key);
                return this;
            }

            @Override
            public BuilderProxy clearValues() {
                builder.clearHeaders();
                return this;
            }

            @Override
            public BuilderProxy setMap(Map<String, List<String>> map) {
                builder.headers(map);
                return this;
            }

            @Override
            public Map<String, List<String>> getMap() {
                return builder.build().headers();
            }
        });
    }

    @Test
    public void responseStatusCodeNormalizationIsCorrect() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> normalizedStatusCode(-1));
        assertThat(normalizedStatusCode(200)).isEqualTo(200);
    }

    private int normalizedStatusCode(int statusCode) {
        return validResponseBuilder().statusCode(statusCode).build().statusCode();
    }

    @Test
    public void responseHeaderNormalizationIsCorrect() {
        headerOrQueryStringNormalizationIsCorrect(() -> new BuilderProxy() {
            private final SdkHttpFullResponse.Builder builder = validResponseBuilder();

            @Override
            public BuilderProxy setValue(String key, String value) {
                builder.putHeader(key, value);
                return this;
            }

            @Override
            public BuilderProxy appendValue(String key, String value) {
                builder.appendHeader(key, value);
                return this;
            }

            @Override
            public BuilderProxy setValues(String key, List<String> values) {
                builder.putHeader(key, values);
                return this;
            }

            @Override
            public BuilderProxy removeValue(String key) {
                builder.removeHeader(key);
                return this;
            }

            @Override
            public BuilderProxy clearValues() {
                builder.clearHeaders();
                return this;
            }

            @Override
            public BuilderProxy setMap(Map<String, List<String>> map) {
                builder.headers(map);
                return this;
            }

            @Override
            public Map<String, List<String>> getMap() {
                return builder.build().headers();
            }
        });
    }

    @Test
    public void testSdkHttpFullRequestBuilderNoQueryParams() {
        URI uri = URI.create("https://github.com/aws/aws-sdk-java-v2/issues/2034");
        final SdkHttpFullRequest sdkHttpFullRequest = SdkHttpFullRequest.builder().method(SdkHttpMethod.POST).uri(uri).build();
        assertThat(sdkHttpFullRequest.getUri().getQuery()).isNullOrEmpty();
    }

    @Test
    public void testSdkHttpFullRequestBuilderUriWithQueryParams() {
        URI uri = URI.create("https://github.com/aws/aws-sdk-java-v2/issues/2034?reqParam=1234&oParam=3456%26reqParam%3D5678");
        final SdkHttpFullRequest sdkHttpFullRequest =
            SdkHttpFullRequest.builder().method(SdkHttpMethod.POST).uri(uri).build();
        assertThat(sdkHttpFullRequest.getUri().getQuery()).contains("reqParam=1234", "oParam=3456&reqParam=5678");
    }

    @Test
    public void testSdkHttpFullRequestBuilderUriWithQueryParamWithoutValue() {
        final String expected = "https://github.com/aws/aws-sdk-for-java-v2?foo";
        URI myUri = URI.create(expected);
        SdkHttpFullRequest actual = SdkHttpFullRequest.builder().method(SdkHttpMethod.POST).uri(myUri).build();
        assertThat(actual.getUri()).hasToString(expected);
    }

    @Test
    public void testSdkHttpRequestBuilderNoQueryParams() {
        URI uri = URI.create("https://github.com/aws/aws-sdk-java-v2/issues/2034");
        final SdkHttpRequest sdkHttpRequest = SdkHttpRequest.builder().method(SdkHttpMethod.POST).uri(uri).build();
        assertThat(sdkHttpRequest.getUri().getQuery()).isNullOrEmpty();
    }

    @Test
    public void testSdkHttpRequestBuilderUriWithQueryParams() {
        URI uri = URI.create("https://github.com/aws/aws-sdk-java-v2/issues/2034?reqParam=1234&oParam=3456%26reqParam%3D5678");
        final SdkHttpRequest sdkHttpRequest =
            SdkHttpRequest.builder().method(SdkHttpMethod.POST).uri(uri).build();
        assertThat(sdkHttpRequest.getUri().getQuery()).contains("reqParam=1234", "oParam=3456&reqParam=5678");
    }

    @Test
    public void testSdkHttpRequestBuilderUriWithQueryParamsIgnoreOtherQueryParams() {
        URI uri = URI.create("https://github.com/aws/aws-sdk-java-v2/issues/2034?reqParam=1234&oParam=3456%26reqParam%3D5678");
        final SdkHttpRequest sdkHttpRequest =
            SdkHttpRequest.builder().method(SdkHttpMethod.POST).appendRawQueryParameter("clean", "up").uri(uri).build();
        assertThat(sdkHttpRequest.getUri().getQuery()).contains("reqParam=1234", "oParam=3456&reqParam=5678")
                                                      .doesNotContain("clean");
    }

    @Test
    public void testSdkHttpRequestBuilderUriWithQueryParamWithoutValue() {
        final String expected = "https://github.com/aws/aws-sdk-for-java-v2?foo";
        URI myUri = URI.create(expected);
        SdkHttpRequest actual = SdkHttpRequest.builder().method(SdkHttpMethod.POST).uri(myUri).build();
        assertThat(actual.getUri()).hasToString(expected);
    }

    private interface BuilderProxy {
        BuilderProxy setValue(String key, String value);

        BuilderProxy appendValue(String key, String value);

        BuilderProxy setValues(String key, List<String> values);

        BuilderProxy removeValue(String key);

        BuilderProxy clearValues();

        BuilderProxy setMap(Map<String, List<String>> map);

        Map<String, List<String>> getMap();
    }

    private void headerOrQueryStringNormalizationIsCorrect(Supplier<BuilderProxy> builderFactory) {
        assertMapIsInitiallyEmpty(builderFactory);

        setValue_SetsSingleValueCorrectly(builderFactory);

        setValue_SettingMultipleKeysAppendsToMap(builderFactory);

        setValue_OverwritesExistingValue(builderFactory);

        setValues_SetsAllValuesCorrectly(builderFactory);

        setValue_OverwritesAllExistingValues(builderFactory);

        removeValue_OnlyRemovesThatKey(builderFactory);

        clearValues_RemovesAllExistingValues(builderFactory);

        setMap_OverwritesAllExistingValues(builderFactory);

        appendWithExistingValues_AddsValueToList(builderFactory);

        appendWithNoValues_AddsSingleElementToList(builderFactory);
    }

    private void assertMapIsInitiallyEmpty(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setMap(emptyMap()).getMap()).isEmpty();
    }

    private void setValue_SetsSingleValueCorrectly(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValue("Foo", "Bar").getMap()).satisfies(params -> {
            assertThat(params).containsOnlyKeys("Foo");
            assertThat(params.get("Foo")).containsExactly("Bar");
        });
    }

    private void setValue_SettingMultipleKeysAppendsToMap(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValue("Foo", "Bar").setValue("Foo2", "Bar2").getMap()).satisfies(params -> {
            assertThat(params).containsOnlyKeys("Foo", "Foo2");
            assertThat(params.get("Foo")).containsExactly("Bar");
            assertThat(params.get("Foo2")).containsExactly("Bar2");
        });
    }

    private void setValue_OverwritesExistingValue(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValue("Foo", "Bar").setValue("Foo", "Bar2").getMap()).satisfies(params -> {
            assertThat(params).containsOnlyKeys("Foo");
            assertThat(params.get("Foo")).containsExactly("Bar2");
        });
    }

    private void setValues_SetsAllValuesCorrectly(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValues("Foo", Arrays.asList("Bar", "Baz")).getMap()).satisfies(params -> {
            assertThat(params).containsOnlyKeys("Foo");
            assertThat(params.get("Foo")).containsExactly("Bar", "Baz");
        });
    }

    private void setValue_OverwritesAllExistingValues(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValues("Foo", Arrays.asList("Bar", "Baz")).setValue("Foo", "Bar").getMap())
                .satisfies(params -> {
            assertThat(params).containsOnlyKeys("Foo");
            assertThat(params.get("Foo")).containsExactly("Bar");
        });
    }

    private void removeValue_OnlyRemovesThatKey(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValues("Foo", Arrays.asList("Bar", "Baz"))
                                 .setValues("Foo2", Arrays.asList("Bar", "Baz"))
                                 .removeValue("Foo").getMap())
        .satisfies(params -> {
            assertThat(params).doesNotContainKeys("Foo");
            assertThat(params.get("Foo2")).containsExactly("Bar", "Baz");
        });
    }

    private void clearValues_RemovesAllExistingValues(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValues("Foo", Arrays.asList("Bar", "Baz")).clearValues().getMap())
                .doesNotContainKeys("Foo");
    }

    private void setMap_OverwritesAllExistingValues(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValues("Foo", Arrays.asList("Bar", "Baz"))
                                 .setMap(singletonMap("Foo2", singletonList("Baz2"))).getMap())
                .satisfies(params -> {
            assertThat(params).containsOnlyKeys("Foo2");
            assertThat(params.get("Foo2")).containsExactly("Baz2");
        });
    }

    private void appendWithExistingValues_AddsValueToList(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().setValues("Key", Arrays.asList("Foo", "Bar"))
                                 .appendValue("Key", "Baz").getMap())
            .satisfies(params -> {
                assertThat(params).containsOnly(new AbstractMap.SimpleEntry<>("Key", Arrays.asList("Foo", "Bar", "Baz")));
            });
    }

    private void appendWithNoValues_AddsSingleElementToList(Supplier<BuilderProxy> builderFactory) {
        assertThat(builderFactory.get().appendValue("AppendNotExists", "Baz").getMap())
            .satisfies(params -> {
                assertThat(params).containsOnly(new AbstractMap.SimpleEntry<>("AppendNotExists", Arrays.asList("Baz")));
            });
    }

    private SdkHttpFullRequest validRequestWithMaps() {
        return validRequestWithMapsBuilder().build();
    }

    private SdkHttpFullRequest.Builder validRequestWithMapsBuilder() {
        return validRequestBuilder().putHeader("Accept", "*/*")
                                    .putRawQueryParameter("Accept", "*/*");
    }

    private SdkHttpFullRequest validRequest() {
        return validRequestBuilder().build();
    }

    private SdkHttpFullRequest.Builder validRequestBuilder() {
        return SdkHttpFullRequest.builder()
                                 .protocol("http")
                                 .host("localhost")
                                 .method(SdkHttpMethod.GET);
    }

    private SdkHttpResponse validResponseWithMaps() {
        return validResponseWithMapsBuilder().build();
    }

    private SdkHttpResponse.Builder validResponseWithMapsBuilder() {
        return validResponseBuilder().putHeader("Accept", "*/*");
    }

    private SdkHttpFullResponse validResponse() {
        return validResponseBuilder().build();
    }

    private SdkHttpFullResponse.Builder validResponseBuilder() {
        return SdkHttpFullResponse.builder().statusCode(500);
    }
}
