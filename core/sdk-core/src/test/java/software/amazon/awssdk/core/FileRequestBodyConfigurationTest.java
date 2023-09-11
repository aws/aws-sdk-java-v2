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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class FileRequestBodyConfigurationTest {

    @Test
    void equalsHashCode() {
        EqualsVerifier.forClass(FileRequestBodyConfiguration.class)
                      .verify();
    }

    @Test
    void invalidRequest_shouldThrowException() {
        assertThatThrownBy(() -> FileRequestBodyConfiguration.builder()
                                                             .path(Paths.get("."))
                                                             .position(-1L)
                                                             .build())
            .hasMessage("position must not be negative");

        assertThatThrownBy(() -> FileRequestBodyConfiguration.builder()
                                                             .path(Paths.get("."))
                                                             .numBytesToRead(-1L)
                                                             .build())
            .hasMessage("numBytesToRead must not be negative");

        assertThatThrownBy(() -> FileRequestBodyConfiguration.builder()
                                                             .path(Paths.get("."))
                                                             .chunkSizeInBytes(0)
                                                             .build())
            .hasMessage("chunkSizeInBytes must be positive");
        assertThatThrownBy(() -> FileRequestBodyConfiguration.builder()
                                                             .path(Paths.get("."))
                                                             .chunkSizeInBytes(-5)
                                                             .build())
            .hasMessage("chunkSizeInBytes must be positive");
        assertThatThrownBy(() -> FileRequestBodyConfiguration.builder()
                                                             .build())
            .hasMessage("path");
    }

    @Test
    void toBuilder_shouldCopyAllProperties() {
        FileRequestBodyConfiguration config = FileRequestBodyConfiguration.builder()
                                                                          .path(Paths.get(".")).numBytesToRead(100L)
                                                                          .position(1L)
                                                                          .chunkSizeInBytes(1024)
                                                                          .build();

        assertThat(config.toBuilder().build()).isEqualTo(config);
    }

}
