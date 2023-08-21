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

package software.amazon.awssdk.arns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ArnResourceTest {

    @Test
    public void toBuilder() {
        ArnResource oneResource = ArnResource.fromString("bucket:foobar:1");
        ArnResource anotherResource = oneResource.toBuilder().build();
        assertThat(oneResource).isEqualTo(anotherResource);
        assertThat(oneResource.hashCode()).isEqualTo(anotherResource.hashCode());
    }

    @Test
    public void hashCodeEquals() {
        ArnResource oneResource = ArnResource.fromString("bucket:foobar:1");
        ArnResource anotherResource = oneResource.toBuilder().qualifier("test").build();
        assertThat(oneResource).isNotEqualTo(anotherResource);
        assertThat(oneResource.hashCode()).isNotEqualTo(anotherResource.hashCode());
    }

    @Test
    public void arnResource_nullResource_shouldThrowException() {
        assertThatThrownBy(() -> ArnResource.builder()
                                            .build()).hasMessageContaining("resource must not be null.");
    }

    @Test
    public void arnResourceFromBuilder_shouldParseCorrectly() {
        ArnResource arnResource = ArnResource.builder()
                                             .resource("bucket:foobar:1")
                                             .resourceType("foobar")
                                             .qualifier("1").build();
        assertThat(arnResource.qualifier()).isEqualTo(Optional.of("1"));
        assertThat(arnResource.resourceType()).isEqualTo(Optional.of("foobar"));
        assertThat(arnResource.resource()).isEqualTo("bucket:foobar:1");
    }

    @Test
    public void hashCodeEquals_minimalProperties() {
        ArnResource arnResource = ArnResource.builder().resource("resource").build();
        ArnResource anotherResource = arnResource.toBuilder().build();
        assertThat(arnResource.equals(anotherResource)).isTrue();
        assertThat(arnResource.hashCode()).isEqualTo(anotherResource.hashCode());
    }

    @ParameterizedTest
    @MethodSource("resources")
    void arnResource_ParsesCorrectly(String resource, ArnResource expected) {
        ArnResource arnResource = ArnResource.fromString(resource);

        assertThat(arnResource.resourceType()).isEqualTo(expected.resourceType());
        assertThat(arnResource.resource()).isEqualTo(expected.resource());
        assertThat(arnResource.qualifier()).isEqualTo(expected.qualifier());
    }

    private static List<Arguments> resources() {
        return Arrays.asList(
            Arguments.of("myresource", ArnResource.builder().resource("myresource").build()),
            Arguments.of("alias/foo/bar", ArnResource.builder().resourceType("alias").resource("foo/bar").build()),
            Arguments.of("alias//", ArnResource.builder().resourceType("alias").resource("/").build()),
            Arguments.of("alias//a", ArnResource.builder().resourceType("alias").resource("/a").build()),
            Arguments.of("alias///a", ArnResource.builder().resourceType("alias").resource("//a").build()),
            Arguments.of("alias///a/b", ArnResource.builder().resourceType("alias").resource("//a/b").build()),
            Arguments.of("alias/foo", ArnResource.builder().resourceType("alias").resource("foo").build()),
            Arguments.of("alias/foo:quali", ArnResource.builder().resourceType("alias").resource("foo").qualifier("quali").build()),
            Arguments.of("alias/foo:bar:quali", ArnResource.builder().resourceType("alias").resource("foo:bar").qualifier("quali").build()),
            Arguments.of("alias:foo", ArnResource.builder().resourceType("alias").resource("foo").build()),
            Arguments.of("alias:foo.bar", ArnResource.builder().resourceType("alias").resource("foo.bar").build()),
            Arguments.of("alias:foo/bar", ArnResource.builder().resourceType("alias").resource("foo/bar").build()),
            Arguments.of("alias:foo/bar/baz", ArnResource.builder().resourceType("alias").resource("foo/bar/baz").build()),
            Arguments.of("alias:foo:quali", ArnResource.builder().resourceType("alias").resource("foo").qualifier("quali").build()),
            Arguments.of("alias:foo:bar:quali", ArnResource.builder().resourceType("alias").resource("foo:bar").qualifier("quali").build())
        );
    }
}
