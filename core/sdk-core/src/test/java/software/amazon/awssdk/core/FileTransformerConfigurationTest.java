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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static software.amazon.awssdk.core.FileTransformerConfiguration.FailureBehavior.DELETE;
import static software.amazon.awssdk.core.FileTransformerConfiguration.FileWriteOption.CREATE_NEW;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FileTransformerConfigurationTest {

    @ParameterizedTest
    @EnumSource(
        value = FileTransformerConfiguration.FileWriteOption.class,
        names = {"CREATE_NEW", "CREATE_OR_REPLACE_EXISTING", "CREATE_OR_APPEND_TO_EXISTING"})
    void position_whenUsedWithNotWriteToPosition_shouldThrowIllegalArgumentException(
        FileTransformerConfiguration.FileWriteOption fileWriteOption) {
        FileTransformerConfiguration.Builder builder = FileTransformerConfiguration.builder()
            .position(123L)
            .failureBehavior(DELETE)
            .fileWriteOption(fileWriteOption);
        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(fileWriteOption.name());
    }

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(FileTransformerConfiguration.class)
                      .withNonnullFields("fileWriteOption", "failureBehavior")
                      .verify();

    }

    @Test
    void toBuilder() {
        FileTransformerConfiguration configuration =
            FileTransformerConfiguration.builder()
                                        .failureBehavior(DELETE)
                                        .fileWriteOption(CREATE_NEW)
                                        .build();

        FileTransformerConfiguration another = configuration.toBuilder().build();
        assertThat(configuration).isEqualTo(another);
    }
}
