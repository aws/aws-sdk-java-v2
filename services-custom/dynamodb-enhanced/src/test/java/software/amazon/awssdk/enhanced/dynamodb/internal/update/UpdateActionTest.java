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

package software.amazon.awssdk.enhanced.dynamodb.internal.update;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateAction;

public class UpdateActionTest {

    @Test
    public void build_minimal() {
        UpdateAction action = UpdateAction.builder().build();
        assertThat(action.type()).isNull();
        assertThat(action.attributeName()).isNull();
        assertThat(action.expression()).isNull();
        assertThat(action.expressionNames()).isEmpty();
        assertThat(action.expressionValues()).isEmpty();
    }

}