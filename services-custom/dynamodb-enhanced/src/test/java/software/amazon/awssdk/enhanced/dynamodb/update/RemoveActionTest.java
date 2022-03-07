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

package software.amazon.awssdk.enhanced.dynamodb.update;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class RemoveActionTest {

    private static final String PATH = "path string";
    private static final String ATTRIBUTE_TOKEN = "#attributeToken";
    private static final String ATTRIBUTE_NAME = "attribute1";

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(RemoveAction.class)
                      .usingGetClass()
                      .verify();
    }

    @Test
    void build_minimal() {
        RemoveAction action = RemoveAction.builder()
                                          .path(PATH)
                                          .build();
        assertThat(action.path()).isEqualTo(PATH);
        assertThat(action.expressionNames()).isEmpty();
    }

    @Test
    void build_maximal() {
        RemoveAction action = RemoveAction.builder()
                                          .path(PATH)
                                          .expressionNames(Collections.singletonMap(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME))
                                          .build();
        assertThat(action.path()).isEqualTo(PATH);
        assertThat(action.expressionNames()).containsEntry(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
    }

    @Test
    void copy() {
        RemoveAction action = RemoveAction.builder()
                                          .path(PATH)
                                          .expressionNames(Collections.singletonMap(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME))
                                          .build();
        RemoveAction copy = action.toBuilder().build();
        assertThat(action).isEqualTo(copy);
    }
}