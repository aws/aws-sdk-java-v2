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

package software.amazon.awssdk.http;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class SdkHttpExecutionAttributesTest {

    @Test
    void equalsAndHashcode() {
        EqualsVerifier.forClass(SdkHttpExecutionAttributes.class)
                      .withNonnullFields("attributes")
                      .verify();
    }

    @Test
    void getAttribute_shouldReturnCorrectValue() {
        SdkHttpExecutionAttributes attributes = SdkHttpExecutionAttributes.builder()
                                                                          .put(TestExecutionAttribute.TEST_KEY_FOO, "test")
                                                                          .build();
        assertThat(attributes.getAttribute(TestExecutionAttribute.TEST_KEY_FOO)).isEqualTo("test");
    }

    private static final class TestExecutionAttribute<T> extends SdkHttpExecutionAttribute<T> {

        private static final TestExecutionAttribute<String> TEST_KEY_FOO = new TestExecutionAttribute<>(String.class);

        private TestExecutionAttribute(Class valueType) {
            super(valueType);
        }
    }
}
