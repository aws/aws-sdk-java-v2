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
 *
 * <p>Validates correctness properties 1–5 from the design document.</p>
 */
public class EndpointUrlTest {

    /**
     * Representative AWS URLs used across parameterized tests.
     */
    static List<String> representativeUrls() {
        return Arrays.asList(
            "https://s3.us-east-1.amazonaws.com",
            "https://s3.us-east-1.amazonaws.com/bucket/key",
            "https://localhost:8080/path",
            "https://s3.amazonaws.com/",
            "https://[::1]:8080/path",
            "https://dynamodb.us-west-2.amazonaws.com"
        );
    }

    // -----------------------------------------------------------------------
    // Property 1: Component fidelity
    // Validates: Requirements 1.1, 1.2, 1.3, 1.4, 3.1, 3.2, 3.3, 3.4
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("representativeUrls")
    void componentFidelity_scheme(String url) {
        EndpointUrl endpointUrl = EndpointUrl.parse(url);
        URI uri = URI.create(url);
        assertThat(endpointUrl.scheme()).isEqualTo(uri.getScheme());
    }

    /**
     * Non-IPv6 URLs for host fidelity comparison against {@code URI.getHost()}.
     *
     * <p>IPv6 is excluded because {@code EndpointUrl.parse()} always stores the host WITH brackets
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
        EndpointUrl endpointUrl = EndpointUrl.parse(url);
        URI uri = URI.create(url);
        assertThat(endpointUrl.host()).isEqualTo(uri.getHost());
    }

    @ParameterizedTest
    @MethodSource("representativeUrls")
    void componentFidelity_port(String url) {
        EndpointUrl endpointUrl = EndpointUrl.parse(url);
        URI uri = URI.create(url);
        assertThat(endpointUrl.port()).isEqualTo(uri.getPort());
    }

    @ParameterizedTest
    @MethodSource("representativeUrls")
    void componentFidelity_encodedPath(String url) {
        EndpointUrl endpointUrl = EndpointUrl.parse(url);
        URI uri = URI.create(url);
        String expectedPath = uri.getRawPath() != null ? uri.getRawPath() : "";
        assertThat(endpointUrl.encodedPath()).isEqualTo(expectedPath);
    }

    // -----------------------------------------------------------------------
    // Property 2: Parse-toUri round-trip equivalence
    // Validates: Requirements 2.1, 3.5
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("representativeUrls")
    void parseToUriRoundTrip(String url) {
        assertThat(EndpointUrl.parse(url).toUri()).isEqualTo(URI.create(url));
    }

    // -----------------------------------------------------------------------
    // Property 3: toUri caching (idempotence)
    // Validates: Requirements 2.1, 2.2
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("representativeUrls")
    void toUriCaching_parse(String url) {
        EndpointUrl endpointUrl = EndpointUrl.parse(url);
        URI first = endpointUrl.toUri();
        URI second = endpointUrl.toUri();
        assertThat(first).isSameAs(second);
    }

    @Test
    void toUriCaching_of() {
        EndpointUrl endpointUrl = EndpointUrl.of("https", "s3.us-east-1.amazonaws.com", -1, "");
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

    // -----------------------------------------------------------------------
    // Property 4: fromUri round-trip identity
    // Validates: Requirement 2.3
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("representativeUrls")
    void fromUriRoundTripIdentity(String url) {
        URI original = URI.create(url);
        assertThat(EndpointUrl.fromUri(original).toUri()).isSameAs(original);
    }

    // -----------------------------------------------------------------------
    // Property 5: Malformed URL rejection
    // Validates: Requirement 1.7
    // -----------------------------------------------------------------------

    @Test
    void malformedUrl_noSchemeSeparator() {
        assertThatThrownBy(() -> EndpointUrl.parse("not-a-url"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedUrl_emptyString() {
        assertThatThrownBy(() -> EndpointUrl.parse(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedUrl_singleColon() {
        assertThatThrownBy(() -> EndpointUrl.parse("https:host"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // -----------------------------------------------------------------------
    // Additional edge-case coverage for Requirement 1.5 (no path → empty string)
    // and Requirement 1.6 (IPv6 host extraction)
    // -----------------------------------------------------------------------

    @Test
    void parse_noPath_encodedPathIsEmpty() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://dynamodb.us-west-2.amazonaws.com");
        assertThat(endpointUrl.encodedPath()).isEmpty();
    }

    @Test
    void parse_trailingSlash_encodedPathIsSlash() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://s3.amazonaws.com/");
        assertThat(endpointUrl.encodedPath()).isEqualTo("/");
    }

    @Test
    void parse_ipv6WithPort() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://[::1]:8080/path");
        assertThat(endpointUrl.host()).isEqualTo("[::1]");
        assertThat(endpointUrl.port()).isEqualTo(8080);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
    }

    @Test
    void parse_ipv6WithoutPort() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://[::1]/path");
        assertThat(endpointUrl.host()).isEqualTo("[::1]");
        assertThat(endpointUrl.port()).isEqualTo(-1);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
    }

    @Test
    void parse_explicitPort() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://localhost:8080/path");
        assertThat(endpointUrl.scheme()).isEqualTo("https");
        assertThat(endpointUrl.host()).isEqualTo("localhost");
        assertThat(endpointUrl.port()).isEqualTo(8080);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/path");
    }

    @Test
    void parse_noPort() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://s3.us-east-1.amazonaws.com");
        assertThat(endpointUrl.port()).isEqualTo(-1);
    }

    // -----------------------------------------------------------------------
    // Property 10: Pre-parsed EndpointUrl equivalence
    // Validates: Requirements 10.6
    //
    // For representative URL strings, verify that EndpointUrl.of(scheme, host, port, path)
    // produces identical scheme(), host(), port(), encodedPath(), and toUri() as
    // EndpointUrl.parse(fullUrl).
    // -----------------------------------------------------------------------

    /**
     * URLs covering the required categories: simple, with port, with path, with both,
     * trailing slash, and http scheme.
     */
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
        EndpointUrl parsed = EndpointUrl.parse(url);
        EndpointUrl constructed = EndpointUrl.of(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed.scheme()).isEqualTo(parsed.scheme());
        assertThat(constructed.host()).isEqualTo(parsed.host());
        assertThat(constructed.port()).isEqualTo(parsed.port());
        assertThat(constructed.encodedPath()).isEqualTo(parsed.encodedPath());
    }

    @ParameterizedTest
    @MethodSource("preParsedEquivalenceUrls")
    void preParsedEquivalence_toUri(String url) {
        EndpointUrl parsed = EndpointUrl.parse(url);
        EndpointUrl constructed = EndpointUrl.of(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed.toUri()).isEqualTo(parsed.toUri());
    }

    @ParameterizedTest
    @MethodSource("preParsedEquivalenceUrls")
    void preParsedEquivalence_objectEquality(String url) {
        EndpointUrl parsed = EndpointUrl.parse(url);
        EndpointUrl constructed = EndpointUrl.of(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed).isEqualTo(parsed);
        assertThat(constructed.hashCode()).isEqualTo(parsed.hashCode());
    }
}
