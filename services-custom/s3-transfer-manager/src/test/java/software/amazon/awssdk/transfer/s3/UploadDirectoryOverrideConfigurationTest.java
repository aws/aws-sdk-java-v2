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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class UploadDirectoryOverrideConfigurationTest {

    @Test
    public void maxDepthNonNegative_shouldThrowException() {
        assertThatThrownBy(() -> UploadDirectoryOverrideConfiguration.builder()
                                                                     .maxDepth(0)
                                                                     .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");

        assertThatThrownBy(() -> UploadDirectoryOverrideConfiguration.builder()
                                                                     .maxDepth(-1)
                                                                     .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }

    @Test
    public void defaultBuilder() {
        UploadDirectoryOverrideConfiguration configuration = UploadDirectoryOverrideConfiguration.builder().build();
        assertThat(configuration.followSymbolicLinks()).isEmpty();
        assertThat(configuration.recursive()).isEmpty();
        assertThat(configuration.maxDepth()).isEmpty();
    }

    @Test
    public void defaultBuilderWithPropertySet() {
        UploadDirectoryOverrideConfiguration configuration = UploadDirectoryOverrideConfiguration.builder()
                                                                                                 .maxDepth(10)
                                                                                                 .recursive(true)
                                                                                                 .followSymbolicLinks(false)
                                                                                                 .build();
        assertThat(configuration.followSymbolicLinks()).contains(false);
        assertThat(configuration.recursive()).contains(true);
        assertThat(configuration.maxDepth()).contains(10);
    }

    @Test
    public void equalsHashCode() {
        EqualsVerifier.forClass(UploadDirectoryOverrideConfiguration.class).verify();
    }
}
