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
}
