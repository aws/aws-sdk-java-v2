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

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EndpointTest {
    private static final EndpointAttributeKey<String> TEST_STRING_ATTR =
        new EndpointAttributeKey<>("StringAttr", String.class);

    /**
     * Representative AWS URLs used across parameterized tests.
     * Consistent with the set used in {@link EndpointUrlTest}.
     */
    static List<String> representativeUrls() {
        return Arrays.asList(
            "https://s3.us-east-1.amazonaws.com",
            "https://s3.us-east-1.amazonaws.com/bucket/key",
            "https://localhost:8080/path",
            "https://s3.amazonaws.com/",
            "https://dynamodb.us-west-2.amazonaws.com"
        );
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(Endpoint.class)
            .verify();
    }

    @Test
    public void build_maximal() {
        Endpoint endpoint = Endpoint.builder()
                                    .url(URI.create("https://myservice.aws"))
                                    .putHeader("foo", "bar")
                                    .putHeader("foo", "baz")
                                    .putAttribute(TEST_STRING_ATTR, "baz")
                                    .build();

        Map<String, List<String>> expectedHeaders = new HashMap<>();
        expectedHeaders.put("foo", Arrays.asList("bar", "baz"));

        assertThat(endpoint.url()).isEqualTo(URI.create("https://myservice.aws"));
        assertThat(endpoint.headers()).isEqualTo(expectedHeaders);
        assertThat(endpoint.attribute(TEST_STRING_ATTR)).isEqualTo("baz");
    }

    @Test
    public void toBuilder_unmodified_equalToOriginal() {
        Endpoint original = Endpoint.builder()
            .url(URI.create("https://myservice.aws"))
            .putHeader("foo", "bar")
            .putAttribute(TEST_STRING_ATTR, "baz")
            .build();

        assertThat(original.toBuilder().build()).isEqualTo(original);
    }

    @Test
    public void toBuilder_headersModified_notReflectedInOriginal() {
        Endpoint original = Endpoint.builder()
                                    .putHeader("foo", "bar")
                                    .build();

        original.toBuilder()
            .putHeader("foo", "baz")
            .build();

        assertThat(original.headers().get("foo")).containsExactly("bar");
    }

    @Test
    public void toBuilder_attrsModified_notReflectedInOriginal() {
        Endpoint original = Endpoint.builder()
                                    .putAttribute(TEST_STRING_ATTR, "foo")
                                    .build();

        original.toBuilder()
                .putAttribute(TEST_STRING_ATTR, "bar")
                .build();

        assertThat(original.attribute(TEST_STRING_ATTR)).isEqualTo("foo");
    }

    // -----------------------------------------------------------------------
    // Property 6: Endpoint url(URI) round-trip
    // Validates: Requirements 4.3, 4.4
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("representativeUrls")
    void urlRoundTrip(String url) {
        URI uri = URI.create(url);
        Endpoint endpoint = Endpoint.builder().url(uri).build();
        assertThat(endpoint.url()).isEqualTo(uri);
    }

    // -----------------------------------------------------------------------
    // Property 7: Endpoint equality across construction paths
    // Validates: Requirement 4.7
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("representativeUrls")
    void equalityAcrossConstructionPaths(String url) {
        Endpoint viaUri = Endpoint.builder()
                                  .url(URI.create(url))
                                  .build();
        Endpoint viaEndpointUrl = Endpoint.builder()
                                          .endpointUrl(EndpointUrl.parse(url))
                                          .build();
        assertThat(viaUri).isEqualTo(viaEndpointUrl);
        assertThat(viaEndpointUrl).isEqualTo(viaUri);
        assertThat(viaUri.hashCode()).isEqualTo(viaEndpointUrl.hashCode());
    }

    // -----------------------------------------------------------------------
    // endpointUrl() accessor
    // Validates: Requirements 4.5, 4.6
    // -----------------------------------------------------------------------

    @Test
    void endpointUrl_viaUrlBuilder_returnsCorrectEndpointUrl() {
        URI uri = URI.create("https://s3.us-east-1.amazonaws.com/bucket/key");
        Endpoint endpoint = Endpoint.builder().url(uri).build();

        EndpointUrl endpointUrl = endpoint.endpointUrl();
        assertThat(endpointUrl).isNotNull();
        assertThat(endpointUrl.scheme()).isEqualTo("https");
        assertThat(endpointUrl.host()).isEqualTo("s3.us-east-1.amazonaws.com");
        assertThat(endpointUrl.port()).isEqualTo(-1);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/bucket/key");
    }

    @Test
    void endpointUrl_viaEndpointUrlBuilder_returnsCorrectEndpointUrl() {
        EndpointUrl expected = EndpointUrl.parse("https://localhost:8080/path");
        Endpoint endpoint = Endpoint.builder().endpointUrl(expected).build();

        assertThat(endpoint.endpointUrl()).isSameAs(expected);
    }

    // -----------------------------------------------------------------------
    // toBuilder() preserves EndpointUrl
    // Validates: Requirements 4.5, 4.6
    // -----------------------------------------------------------------------

    @Test
    void toBuilder_preservesEndpointUrl() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://s3.us-east-1.amazonaws.com/bucket/key");
        Endpoint original = Endpoint.builder()
                                    .endpointUrl(endpointUrl)
                                    .putHeader("x-amz-test", "value")
                                    .putAttribute(TEST_STRING_ATTR, "attr")
                                    .build();

        Endpoint rebuilt = original.toBuilder().build();

        assertThat(rebuilt.endpointUrl()).isSameAs(endpointUrl);
        assertThat(rebuilt.url()).isEqualTo(original.url());
        assertThat(rebuilt.headers()).isEqualTo(original.headers());
        assertThat(rebuilt.attribute(TEST_STRING_ATTR)).isEqualTo("attr");
    }
}

