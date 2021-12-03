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
import software.amazon.awssdk.enhanced.dynamodb.internal.update.AddUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.DefaultUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.DeleteUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.RemoveUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.SetUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateActionType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public interface UpdateAction {

    UpdateActionType type();

    String expression();

    Map<String, String> expressionNames();

    Map<String, AttributeValue> expressionValues();

    String attributeName();

    static Builder builder() {
        return DefaultUpdateAction.builder();
    }

    static UpdateAction setAttribute(String attributeName, AttributeValue value) {
        return SetUpdateAction.setAttribute(attributeName, value, UpdateBehavior.WRITE_ALWAYS);
    }

    static UpdateAction setAttribute(String attributeName, AttributeValue value, UpdateBehavior updateBehavior) {
        return SetUpdateAction.setAttribute(attributeName, value, updateBehavior);
    }

    static UpdateAction removeAttribute(String attributeName) {
        return RemoveUpdateAction.remove(attributeName);
    }

    static UpdateAction addToValue(String attributeName, AttributeValue deltaValue) {
        return AddUpdateAction.addValue(attributeName, deltaValue);
    }

    static UpdateAction addToValueWithStartValue(String attributeName, AttributeValue deltaValue, AttributeValue startValue) {
        return SetUpdateAction.addWithStartValue(attributeName, deltaValue, startValue);
    }

    static UpdateAction addToSet(String attributeName, AttributeValue value) {
        return AddUpdateAction.addToSet(attributeName, value);
    }

    static UpdateAction deleteFromSet(String attributeName, AttributeValue value) {
        return DeleteUpdateAction.removeElements(attributeName, value);
    }

    static UpdateAction appendToList(String attributeName, AttributeValue listValue) {
       return SetUpdateAction.appendToList(attributeName, listValue, false);
    }

    static UpdateAction prependToList(String attributeName, AttributeValue listValue) {
        return SetUpdateAction.appendToList(attributeName, listValue, true);
    }

    static UpdateAction setListItemAt(String attributeName, int index, AttributeValue value) {
        return SetUpdateAction.updateListItem(attributeName, index, value);
    }

    static UpdateAction removeFromListAt(String attributeName, int index) {
        return RemoveUpdateAction.removeFromList(attributeName, index);
    }

    interface Builder {

        Builder type(UpdateActionType type);

        Builder attributeName(String attributeName);

        Builder expression(String actionExpression);

        Builder expressionNames(Map<String, String> expressionNames);

        Builder expressionValues(Map<String, AttributeValue> expressionValues);

        UpdateAction build();
    }
}
