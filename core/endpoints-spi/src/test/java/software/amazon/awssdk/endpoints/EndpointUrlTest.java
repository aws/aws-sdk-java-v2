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

package software.amazon.awssdk.endpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link EndpointUrl}.
 */
public class EndpointUrlTest {

    /**
     * Representative AWS URLs used across parameterized tests.
     */
    static List<String> urls() {
        return Arrays.asList(
            "https://s3.us-east-1.amazonaws.com",
            "https://s3.us-east-1.amazonaws.com/bucket/key",
            "https://localhost:8080/path",
            "https://s3.amazonaws.com/",
            "https://[::1]:8080/path",
            "https://dynamodb.us-west-2.amazonaws.com"
        );
    }

    @ParameterizedTest
    @MethodSource("urls")
    void componentFidelity_scheme(String url) {
        EndpointUrl endpointUrl = EndpointUrl.fromString(url);
        URI uri = URI.create(url);
        assertThat(endpointUrl.scheme()).isEqualTo(uri.getScheme());
    }

    /**
     * Non-IPv6 URLs for host fidelity comparison against {@code URI.getHost()}.
     *
     * <p>IPv6 is excluded because {@code EndpointUrl.fromString()} always stores the host WITH brackets
     * (e.g., "[::1]"), while {@code URI.getHost()} behavior for IPv6 varies across JDK versions
     * (Java 8 strips brackets, Java 17+ keeps them). IPv6 host parsing is verified directly
     * in the dedicated edge-case tests below.</p>
     */
    static List<String> nonIpv6Urls() {
        return Arrays.asList(
            "https://s3.us-east-1.amazonaws.com",
            "https://s3.us-east-1.amazonaws.com/bucket/key",
            "https://localhost:8080/path",
            "https://s3.amazonaws.com/",
            "https://dynamodb.us-west-2.amazonaws.com"
        );
    }

    @ParameterizedTest
    @MethodSource("nonIpv6Urls")
    void componentFidelity_host(String url) {
        EndpointUrl endpointUrl = EndpointUrl.fromString(url);
        URI uri = URI.create(url);
        assertThat(endpointUrl.host()).isEqualTo(uri.getHost());
    }

    @ParameterizedTest
    @MethodSource("urls")
    void componentFidelity_port(String url) {
        EndpointUrl endpointUrl = EndpointUrl.fromString(url);
        URI uri = URI.create(url);
        assertThat(endpointUrl.port()).isEqualTo(uri.getPort());
    }

    @ParameterizedTest
    @MethodSource("urls")
    void componentFidelity_encodedPath(String url) {
        EndpointUrl endpointUrl = EndpointUrl.fromString(url);
        URI uri = URI.create(url);
        String expectedPath = uri.getRawPath() != null ? uri.getRawPath() : "";
        assertThat(endpointUrl.encodedPath()).isEqualTo(expectedPath);
    }

    @ParameterizedTest
    @MethodSource("urls")
    void fromStringToUriRoundTrip(String url) {
        assertThat(EndpointUrl.fromString(url).toUri()).isEqualTo(URI.create(url));
    }

    @ParameterizedTest
    @MethodSource("urls")
    void toUriCaching_fromString(String url) {
        EndpointUrl endpointUrl = EndpointUrl.fromString(url);
        URI first = endpointUrl.toUri();
        URI second = endpointUrl.toUri();
        assertThat(first).isSameAs(second);
    }

    @Test
    void toUriCaching_fromComponents() {
        EndpointUrl endpointUrl = EndpointUrl.fromComponents("https", "s3.us-east-1.amazonaws.com", -1, "");
        URI first = endpointUrl.toUri();
        URI second = endpointUrl.toUri();
        assertThat(first).isSameAs(second);
    }

    @Test
    void toUriCaching_fromUri() {
        URI original = URI.create("https://s3.us-east-1.amazonaws.com");
        EndpointUrl endpointUrl = EndpointUrl.fromUri(original);
        URI first = endpointUrl.toUri();
        URI second = endpointUrl.toUri();
        assertThat(first).isSameAs(second);
    }

    @ParameterizedTest
    @MethodSource("urls")
    void fromUriRoundTripIdentity(String url) {
        URI original = URI.create(url);
        assertThat(EndpointUrl.fromUri(original).toUri()).isSameAs(original);
    }

    @Test
    void malformedUrl_noSchemeSeparator() {
        assertThatThrownBy(() -> EndpointUrl.fromString("not-a-url"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedUrl_emptyString() {
        assertThatThrownBy(() -> EndpointUrl.fromString(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedUrl_singleColon() {
        assertThatThrownBy(() -> EndpointUrl.fromString("https:host"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromString_noPath_encodedPathIsEmpty() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://dynamodb.us-west-2.amazonaws.com");
        assertThat(endpointUrl.encodedPath()).isEmpty();
    }

    @Test
    void fromString_trailingSlash_encodedPathIsSlash() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://s3.amazonaws.com/");
        assertThat(endpointUrl.encodedPath()).isEqualTo("/");
    }

    @Test
    void fromString_ipv6WithPort() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://[::1]:8080/path");
        assertThat(endpointUrl.host()).isEqualTo("[::1]");
        assertThat(endpointUrl.port()).isEqualTo(8080);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
    }

    @Test
    void fromString_ipv6WithoutPort() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://[::1]/path");
        assertThat(endpointUrl.host()).isEqualTo("[::1]");
        assertThat(endpointUrl.port()).isEqualTo(-1);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
    }

    @Test
    void fromString_explicitPort() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://localhost:8080/path");
        assertThat(endpointUrl.scheme()).isEqualTo("https");
        assertThat(endpointUrl.host()).isEqualTo("localhost");
        assertThat(endpointUrl.port()).isEqualTo(8080);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
    }

    @Test
    void fromString_noPort() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://s3.us-east-1.amazonaws.com");
        assertThat(endpointUrl.port()).isEqualTo(-1);
    }

    static List<String> preParsedEquivalenceUrls() {
        return Arrays.asList(
            // Simple URL (no port, no path)
            "https://s3.us-east-1.amazonaws.com",
            // URL with port only
            "https://runtime.sagemaker.us-east-1.amazonaws.com:8443",
            // URL with path only
            "https://places.geo.us-east-1.amazonaws.com/v2",
            // URL with both port and path
            "https://localhost:8443/v2/api",
            // URL with trailing slash
            "https://s3.amazonaws.com/",
            // HTTP scheme with port and path
            "http://localhost:8080/path",
            // URL with deeper path
            "https://s3.us-west-2.amazonaws.com/prefix/key"
        );
    }

    @ParameterizedTest
    @MethodSource("preParsedEquivalenceUrls")
    void preParsedEquivalence_components(String url) {
        EndpointUrl parsed = EndpointUrl.fromString(url);
        EndpointUrl constructed = EndpointUrl.fromComponents(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed.scheme()).isEqualTo(parsed.scheme());
        assertThat(constructed.host()).isEqualTo(parsed.host());
        assertThat(constructed.port()).isEqualTo(parsed.port());
        assertThat(constructed.encodedPath()).isEqualTo(parsed.encodedPath());
    }

    @ParameterizedTest
    @MethodSource("preParsedEquivalenceUrls")
    void preParsedEquivalence_toUri(String url) {
        EndpointUrl parsed = EndpointUrl.fromString(url);
        EndpointUrl constructed = EndpointUrl.fromComponents(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed.toUri()).isEqualTo(parsed.toUri());
    }

    @ParameterizedTest
    @MethodSource("preParsedEquivalenceUrls")
    void preParsedEquivalence_objectEquality(String url) {
        EndpointUrl parsed = EndpointUrl.fromString(url);
        EndpointUrl constructed = EndpointUrl.fromComponents(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed).isEqualTo(parsed);
        assertThat(constructed.hashCode()).isEqualTo(parsed.hashCode());
    }

    @Test
    void fromString_withQueryOnly() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://example.com/path?key=value&foo=bar");
        assertThat(endpointUrl.scheme()).isEqualTo("https");
        assertThat(endpointUrl.host()).isEqualTo("example.com");
        assertThat(endpointUrl.port()).isEqualTo(-1);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
        assertThat(endpointUrl.queryAndFragment()).isEqualTo("?key=value&foo=bar");
    }

    @Test
    void fromString_withFragmentOnly() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://example.com/path#section");
        assertThat(endpointUrl.scheme()).isEqualTo("https");
        assertThat(endpointUrl.host()).isEqualTo("example.com");
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
        assertThat(endpointUrl.queryAndFragment()).isEqualTo("#section");
    }

    @Test
    void fromString_withQueryAndFragment() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://example.com/path?key=value#section");
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
        assertThat(endpointUrl.queryAndFragment()).isEqualTo("?key=value#section");
    }

    @Test
    void fromString_noQueryOrFragment() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://example.com/path");
        assertThat(endpointUrl.queryAndFragment()).isEmpty();
    }

    @Test
    void fromString_queryWithNoPath() {
        EndpointUrl endpointUrl = EndpointUrl.fromString("https://example.com?key=value");
        assertThat(endpointUrl.host()).isEqualTo("example.com");
        assertThat(endpointUrl.encodedPath()).isEmpty();
        assertThat(endpointUrl.queryAndFragment()).isEqualTo("?key=value");
    }

    @Test
    void fromString_withQueryAndFragment_toUriRoundTrip() {
        String url = "https://example.com/path?key=value#section";
        EndpointUrl endpointUrl = EndpointUrl.fromString(url);
        assertThat(endpointUrl.toUri()).isEqualTo(URI.create(url));
    }

    @Test
    void fromUri_withQueryAndFragment_preservesComponents() {
        URI uri = URI.create("https://example.com/path?key=value#section");
        EndpointUrl endpointUrl = EndpointUrl.fromUri(uri);
        assertThat(endpointUrl.scheme()).isEqualTo("https");
        assertThat(endpointUrl.host()).isEqualTo("example.com");
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
        assertThat(endpointUrl.queryAndFragment()).isEqualTo("?key=value#section");
        assertThat(endpointUrl.toUri()).isSameAs(uri);
    }

    @Test
    void fromUri_noQueryOrFragment() {
        URI uri = URI.create("https://example.com/path");
        EndpointUrl endpointUrl = EndpointUrl.fromUri(uri);
        assertThat(endpointUrl.queryAndFragment()).isEmpty();
    }

    @Test
    void fromComponents_withQueryAndFragment_toUriIncludesThem() {
        EndpointUrl endpointUrl = EndpointUrl.fromComponents("https", "example.com", -1, "/path", "?key=value#section");
        assertThat(endpointUrl.toUri()).isEqualTo(URI.create("https://example.com/path?key=value#section"));
    }

    @Test
    void fromComponents_withQueryAndFragment_preservedInEquality() {
        EndpointUrl with = EndpointUrl.fromComponents("https", "example.com", -1, "/path", "?key=value");
        EndpointUrl without = EndpointUrl.fromComponents("https", "example.com", -1, "/path");
        assertThat(with).isNotEqualTo(without);
    }

    @Test
    void fromComponents_withEmptyQueryAndFragment_equalsOfWithoutIt() {
        EndpointUrl withEmpty = EndpointUrl.fromComponents("https", "example.com", -1, "/path", "");
        EndpointUrl without = EndpointUrl.fromComponents("https", "example.com", -1, "/path");
        assertThat(withEmpty).isEqualTo(without);
    }
}
