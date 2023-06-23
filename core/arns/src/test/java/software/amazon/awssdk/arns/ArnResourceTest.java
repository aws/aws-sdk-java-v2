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

import java.util.Optional;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.arns.ArnResource.ArnResourceFormat;

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

    @Test
    public void toString_resourceIdOnly() {
        ArnResource arnResource = ArnResource.builder().resourceId("resource").build();
        assertThat("null:resource:null").isEqualTo(arnResource.toString());
    }

    @Test
    public void toString_resourceType_resourceId() {
        ArnResource arnResource = ArnResource.builder()
                .resourceType("type")
                .resourceId("resource")
                .build();
        assertThat("type:resource:null").isEqualTo(arnResource.toString());
    }

    @Test
    public void toFormattedString_resourceType_resourceId_qualfiier() {
        ArnResource arnResource = ArnResource.builder()
                .resourceType("type")
                .resourceId("resource")
                .qualifier("qualifier")
                .build();
        assertThat("type:resource:qualifier").isEqualTo(arnResource.toStringFormatted());
    }

    @Test
    public void toFormattedString_slash__resourceType_resourceId_qualfiier() {
        ArnResource arnResource = ArnResource.builder()
                .resourceType("type")
                .resourceId("resource")
                .qualifier("qualifier")
                .resourceFormat(ArnResourceFormat.RESOURCE_WITH_SLASH)
                .build();
        assertThat("type/resource:qualifier").isEqualTo(arnResource.toStringFormatted());
    }

    @Test
    public void toStringFormatted_resourceType_resourceId_parse_slash_seperator() {
        ArnResource arnResource = ArnResource.builder()
                .resourceType("vpc")
                .resourceFormat(ArnResourceFormat.RESOURCE_WITH_SLASH)
                .resourceId("vpc-0e9801d129EXAMPLE")
                .build();
        assertThat("vpc/vpc-0e9801d129EXAMPLE").isEqualTo(arnResource.toStringFormatted());
    }

    @Test
    public void toStringFormatted_resourceType_resourceId_slash_seperator() {
        ArnResource arnResource = ArnResource.fromString("vpc/vpc-0e9801d129EXAMPLE");
        assertThat("vpc/vpc-0e9801d129EXAMPLE").isEqualTo(arnResource.toStringFormatted());
        assertThat(arnResource.resourceType()).isEqualTo(Optional.of("vpc"));
        assertThat(arnResource.resourceId()).isEqualTo("vpc-0e9801d129EXAMPLE");
        assertThat(arnResource.qualifier()).isEqualTo(Optional.empty());
    }

    @Test
    public void toStringFormatted_arn_resource() {
        ArnResource arnResource = ArnResource.fromString("vpc/vpc-0e9801d129EXAMPLE:qualifier");
        ArnResource arnResourceCpy = ArnResource.builder()
                .arnResource(arnResource)
                .build();
        assertThat("vpc/vpc-0e9801d129EXAMPLE:qualifier").isEqualTo(arnResourceCpy.toStringFormatted());
        assertThat(arnResourceCpy.resourceType()).isEqualTo(Optional.of("vpc"));
        assertThat(arnResourceCpy.resourceId()).isEqualTo("vpc-0e9801d129EXAMPLE");
        assertThat(arnResourceCpy.qualifier()).isEqualTo(Optional.of("qualifier"));
    }

}
