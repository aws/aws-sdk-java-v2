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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class AsyncRequestBodyConfigurationTest {

    @Test
    void equalsHashCode() {
        EqualsVerifier.forClass(AsyncRequestBodySplitConfiguration.class)
                      .verify();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void nonPositiveValue_shouldThrowException(long size) {
        assertThatThrownBy(() ->
                               AsyncRequestBodySplitConfiguration.builder()
                                                                 .chunkSizeInBytes(size)
                                                                 .build())
            .hasMessageContaining("must be positive");
        assertThatThrownBy(() ->
                               AsyncRequestBodySplitConfiguration.builder()
                                                                 .bufferSizeInBytes(size)
                                                                 .build())
            .hasMessageContaining("must be positive");
    }

    @Test
    void toBuilder_shouldCopyAllFields() {
        AsyncRequestBodySplitConfiguration config = AsyncRequestBodySplitConfiguration.builder()
                                                                                     .bufferSizeInBytes(1L)
                                                                                     .chunkSizeInBytes(2L)
                                                                                     .build();

        assertThat(config.toBuilder().build()).isEqualTo(config);
    }
}
