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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class EndpointTest {
    private static final EndpointAttributeKey<String> TEST_STRING_ATTR =
        new EndpointAttributeKey<>("StringAttr", String.class);

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

    @Test
    public void equality_acrossConstructionPaths() {
        URI uri = URI.create("https://[::1]:8080/path?key=value#frag");

        Endpoint viaUri = Endpoint.builder().url(uri).build();
        Endpoint viaFromString = Endpoint.builder()
                                         .endpointUrl(EndpointUrl.fromString("https://[::1]:8080/path?key=value#frag"))
                                         .build();
        Endpoint viaFromComponents = Endpoint.builder()
                                             .endpointUrl(EndpointUrl.fromComponents("https", "[::1]", 8080, "/path",
                                                                                     "?key=value#frag"))
                                             .build();

        assertThat(viaUri).isEqualTo(viaFromString);
        assertThat(viaUri).isEqualTo(viaFromComponents);
        assertThat(viaUri.hashCode()).isEqualTo(viaFromString.hashCode());
        assertThat(viaUri.hashCode()).isEqualTo(viaFromComponents.hashCode());
    }

    @Test
    public void inequality_differentUrlComponents() {
        Endpoint endpoint1 = Endpoint.builder()
                                     .endpointUrl(EndpointUrl.fromString("https://example.com/path?key=value"))
                                     .build();
        Endpoint endpoint2 = Endpoint.builder()
                                     .endpointUrl(EndpointUrl.fromString("https://example.com/path"))
                                     .build();

        assertThat(endpoint1).isNotEqualTo(endpoint2);
    }

    @Test
    public void endpointUrlAccessor_returnsCorrectComponents() {
        Endpoint endpoint = Endpoint.builder()
                                    .url(URI.create("https://s3.us-east-1.amazonaws.com:443/bucket"))
                                    .build();

        EndpointUrl endpointUrl = endpoint.endpointUrl();
        assertThat(endpointUrl.scheme()).isEqualTo("https");
        assertThat(endpointUrl.host()).isEqualTo("s3.us-east-1.amazonaws.com");
        assertThat(endpointUrl.port()).isEqualTo(443);
        assertThat(endpointUrl.encodedPath()).isEqualTo("/bucket");
        assertThat(endpointUrl.queryAndFragment()).isEmpty();
    }

    @Test
    public void build_noHeadersOrAttributes_returnsEmptyMaps() {
        Endpoint endpoint = Endpoint.builder()
                                    .endpointUrl(EndpointUrl.fromString("https://example.com"))
                                    .build();

        assertThat(endpoint.headers()).isEmpty();
        assertThat(endpoint.attribute(TEST_STRING_ATTR)).isNull();
    }

    @Test
    public void headers_isUnmodifiable() {
        Endpoint noHeaders = Endpoint.builder()
                                     .endpointUrl(EndpointUrl.fromString("https://example.com"))
                                     .build();
        Endpoint withHeaders = Endpoint.builder()
                                       .endpointUrl(EndpointUrl.fromString("https://example.com"))
                                       .putHeader("foo", "bar")
                                       .build();

        assertThatThrownBy(() -> noHeaders.headers().put("a", Arrays.asList("b")))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> withHeaders.headers().put("a", Arrays.asList("b")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * A single attribute is staged inline rather than in a map, so exercise the read path for one, two and
     * three attributes as well as overwriting the staged entry.
     */
    @Test
    public void putAttribute_variousArities_allReadable() {
        EndpointAttributeKey<String> second = new EndpointAttributeKey<>("Second", String.class);
        EndpointAttributeKey<String> third = new EndpointAttributeKey<>("Third", String.class);

        Endpoint one = Endpoint.builder()
                               .endpointUrl(EndpointUrl.fromString("https://example.com"))
                               .putAttribute(TEST_STRING_ATTR, "a")
                               .build();
        assertThat(one.attribute(TEST_STRING_ATTR)).isEqualTo("a");
        assertThat(one.attribute(second)).isNull();

        Endpoint two = Endpoint.builder()
                               .endpointUrl(EndpointUrl.fromString("https://example.com"))
                               .putAttribute(TEST_STRING_ATTR, "a")
                               .putAttribute(second, "b")
                               .build();
        assertThat(two.attribute(TEST_STRING_ATTR)).isEqualTo("a");
        assertThat(two.attribute(second)).isEqualTo("b");

        Endpoint three = Endpoint.builder()
                                 .endpointUrl(EndpointUrl.fromString("https://example.com"))
                                 .putAttribute(TEST_STRING_ATTR, "a")
                                 .putAttribute(second, "b")
                                 .putAttribute(third, "c")
                                 .build();
        assertThat(three.attribute(TEST_STRING_ATTR)).isEqualTo("a");
        assertThat(three.attribute(second)).isEqualTo("b");
        assertThat(three.attribute(third)).isEqualTo("c");
    }

    @Test
    public void putAttribute_sameKeyTwice_lastValueWins() {
        EndpointAttributeKey<String> second = new EndpointAttributeKey<>("Second", String.class);

        Endpoint staged = Endpoint.builder()
                                  .endpointUrl(EndpointUrl.fromString("https://example.com"))
                                  .putAttribute(TEST_STRING_ATTR, "first")
                                  .putAttribute(TEST_STRING_ATTR, "second")
                                  .build();
        assertThat(staged.attribute(TEST_STRING_ATTR)).isEqualTo("second");

        // Same key overwritten after the builder has been promoted to a map.
        Endpoint promoted = Endpoint.builder()
                                    .endpointUrl(EndpointUrl.fromString("https://example.com"))
                                    .putAttribute(TEST_STRING_ATTR, "first")
                                    .putAttribute(second, "other")
                                    .putAttribute(TEST_STRING_ATTR, "second")
                                    .build();
        assertThat(promoted.attribute(TEST_STRING_ATTR)).isEqualTo("second");
        assertThat(promoted.attribute(second)).isEqualTo("other");
    }

    @Test
    public void toBuilder_roundTripsAllAttributeArities() {
        EndpointAttributeKey<String> second = new EndpointAttributeKey<>("Second", String.class);

        Endpoint none = Endpoint.builder()
                                .endpointUrl(EndpointUrl.fromString("https://example.com"))
                                .build();
        assertThat(none.toBuilder().build()).isEqualTo(none);

        Endpoint one = none.toBuilder().putAttribute(TEST_STRING_ATTR, "a").build();
        assertThat(one.toBuilder().build()).isEqualTo(one);

        Endpoint two = one.toBuilder().putAttribute(second, "b").build();
        assertThat(two.toBuilder().build()).isEqualTo(two);
        assertThat(two.attribute(TEST_STRING_ATTR)).isEqualTo("a");
        assertThat(two.attribute(second)).isEqualTo("b");
    }
}
