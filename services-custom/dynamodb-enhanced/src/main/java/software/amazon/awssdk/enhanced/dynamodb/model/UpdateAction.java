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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.RemoveUpdateAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public interface UpdateAction {

    String actionExpression();

    Map<String, String> expressionNames();

    Map<String, AttributeValue> expressionValues();

    static UpdateAction removeFor(String attributeName) {
        return RemoveUpdateAction.remove(attributeName);
    }

    static UpdateAction removeAction(String expression,
                                     Map<String, String> expressionNames,
                                     Map<String, AttributeValue> expressionValues) {
        return RemoveUpdateAction.builder().build();
    }

    interface Builder {
        Builder actionExpression(String actionExpression);

        Builder expressionNames(Map<String, String> expressionNames);

        Builder expressionValues(Map<String, AttributeValue> expressionValues);

        UpdateAction build();
    }
}
