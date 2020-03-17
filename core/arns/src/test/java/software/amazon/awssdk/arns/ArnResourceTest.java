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
import org.junit.Test;

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
}
