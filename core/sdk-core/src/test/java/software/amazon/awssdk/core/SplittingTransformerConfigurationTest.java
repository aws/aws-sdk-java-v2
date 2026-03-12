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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class SplittingTransformerConfigurationTest {

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(SplittingTransformerConfiguration.class)
                      .withNonnullFields("bufferSizeInBytes")
                      .verify();

    }

    @Test
    void toBuilder() {
        SplittingTransformerConfiguration configuration =
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(4444L)
                                             .build();

        SplittingTransformerConfiguration another = configuration.toBuilder().build();
        assertThat(configuration).isEqualTo(another);
    }
}
