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

package software.amazon.awssdk.core.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class ExecutionAttributesTest {
    private static final ExecutionAttribute<String> ATTR_1 = new ExecutionAttribute<>("Attr1");
    private static final ExecutionAttribute<String> ATTR_2 = new ExecutionAttribute<>("Attr2");

    @Test
    public void equals_identity_returnsTrue() {
        ExecutionAttributes executionAttributes = ExecutionAttributes.builder()
                .put(ATTR_1, "hello")
                .put(ATTR_2, "world")
                .build();

        assertThat(executionAttributes.equals(executionAttributes)).isTrue();
    }

    @Test
    public void equals_sameAttributes_returnsTrue() {
        ExecutionAttributes executionAttributes1 = ExecutionAttributes.builder()
                .put(ATTR_1, "hello")
                .put(ATTR_2, "world")
                .build();

        ExecutionAttributes executionAttributes2 = ExecutionAttributes.builder()
                .put(ATTR_1, "hello")
                .put(ATTR_2, "world")
                .build();

        assertThat(executionAttributes1).isEqualTo(executionAttributes2);
        assertThat(executionAttributes2).isEqualTo(executionAttributes1);
    }

    @Test
    public void equals_differentAttributes_returnsFalse() {
        ExecutionAttributes executionAttributes1 = ExecutionAttributes.builder()
                .put(ATTR_1, "HELLO")
                .put(ATTR_2, "WORLD")
                .build();

        ExecutionAttributes executionAttributes2 = ExecutionAttributes.builder()
                .put(ATTR_1, "hello")
                .put(ATTR_2, "world")
                .build();

        assertThat(executionAttributes1).isNotEqualTo(executionAttributes2);
        assertThat(executionAttributes2).isNotEqualTo(executionAttributes1);
    }


    @Test
    public void hashCode_objectsEqual_valuesEqual() {
        ExecutionAttributes executionAttributes1 = ExecutionAttributes.builder()
                .put(ATTR_1, "hello")
                .put(ATTR_2, "world")
                .build();

        ExecutionAttributes executionAttributes2 = ExecutionAttributes.builder()
                .put(ATTR_1, "hello")
                .put(ATTR_2, "world")
                .build();

        assertThat(executionAttributes1.hashCode()).isEqualTo(executionAttributes2.hashCode());
    }
}
