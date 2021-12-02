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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class EnhancedTypeDocumentationConfigurationTest {

    @Test
    public void defaultBuilder_defaultToFalse() {
        EnhancedTypeDocumentConfiguration configuration =
            EnhancedTypeDocumentConfiguration.builder().build();
        assertThat(configuration.ignoreNulls()).isFalse();
        assertThat(configuration.preserveEmptyObject()).isFalse();
    }

    @Test
    public void equalsHashCode() {
        EnhancedTypeDocumentConfiguration configuration =
            EnhancedTypeDocumentConfiguration.builder()
                                             .preserveEmptyObject(true)
                                             .ignoreNulls(false)
                                             .build();

        EnhancedTypeDocumentConfiguration another =
            EnhancedTypeDocumentConfiguration.builder()
                                             .preserveEmptyObject(true)
                                             .ignoreNulls(false)
                                             .build();

        EnhancedTypeDocumentConfiguration different =
            EnhancedTypeDocumentConfiguration.builder()
                                             .preserveEmptyObject(false)
                                             .ignoreNulls(true)
                                             .build();

        assertThat(configuration).isEqualTo(another);
        assertThat(configuration.hashCode()).isEqualTo(another.hashCode());
        assertThat(configuration).isNotEqualTo(different);
        assertThat(configuration.hashCode()).isNotEqualTo(different.hashCode());
    }
}
