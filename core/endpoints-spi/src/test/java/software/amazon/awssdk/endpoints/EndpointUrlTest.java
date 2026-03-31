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
     * Representative URLs used across parameterized tests.
     * Note - this does not test IPv6 as URI represents these differently in some cases.
     * IPV6 is tested in non-parameterized tests.
     */
    static List<String> validUrls() {
        return Arrays.asList(
            "https://s3.us-east-1.amazonaws.com",
            "https://s3.us-east-1.amazonaws.com/bucket/key",
            "https://localhost:8080/path",
            "https://s3.amazonaws.com/",
            "http://192.168.0.1:8080/path"
        );
    }

    @ParameterizedTest
    @MethodSource("validUrls")
    void parseToUriRoundTrip(String url) {
        assertThat(EndpointUrl.parse(url).toUri()).isEqualTo(URI.create(url));
    }

    @ParameterizedTest
    @MethodSource("validUrls")
    void preParsedEquivalence_components(String url) {
        EndpointUrl parsed = EndpointUrl.parse(url);
        EndpointUrl constructed = EndpointUrl.of(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed.scheme()).isEqualTo(parsed.scheme());
        assertThat(constructed.host()).isEqualTo(parsed.host());
        assertThat(constructed.port()).isEqualTo(parsed.port());
        assertThat(constructed.encodedPath()).isEqualTo(parsed.encodedPath());
    }

    @ParameterizedTest
    @MethodSource("validUrls")
    void preParsedEquivalence_toUri(String url) {
        EndpointUrl parsed = EndpointUrl.parse(url);
        EndpointUrl constructed = EndpointUrl.of(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed.toUri()).isEqualTo(parsed.toUri());
    }

    @ParameterizedTest
    @MethodSource("validUrls")
    void preParsedEquivalence_objectEquality(String url) {
        EndpointUrl parsed = EndpointUrl.parse(url);
        EndpointUrl constructed = EndpointUrl.of(parsed.scheme(), parsed.host(), parsed.port(), parsed.encodedPath());

        assertThat(constructed).isEqualTo(parsed);
        assertThat(constructed.hashCode()).isEqualTo(parsed.hashCode());
    }

    @ParameterizedTest
    @MethodSource("validUrls")
    void fromUriRoundTripIdentity(String url) {
        URI original = URI.create(url);
        assertThat(EndpointUrl.fromUri(original).toUri()).isSameAs(original);
    }

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

    @ParameterizedTest
    @MethodSource("validUrls")
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
}
