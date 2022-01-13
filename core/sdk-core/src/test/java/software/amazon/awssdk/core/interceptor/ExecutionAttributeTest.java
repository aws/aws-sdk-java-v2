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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExecutionAttributeTest {

    @Test
    @DisplayName("Ensure that two different ExecutionAttributes are not allowed to have the same name (and hash key)")
    void testUniqueness() {
        String name = "ExecutionAttributeTest";
        ExecutionAttribute<Integer> first = new ExecutionAttribute<>(name);
        assertThatThrownBy(() -> {
            ExecutionAttribute<Integer> second = new ExecutionAttribute<>(name);
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(name);
    }
}