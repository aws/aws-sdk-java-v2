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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.ifNotExists;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.operationExpression;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class UpdateExpressionUtilsTest {

    @Mock
    private TableSchema root;

    @Mock
    private TableSchema tableSchema;

    @Mock
    private TableMetadata tableMetadata;

    @Mock
    private AttributeConverter attributeConverter;

    @Mock
    private EnhancedType<?> enhancedType;

    @Test
    public void ifNotExists_generatesExpectedExpression() {
        String result = ifNotExists("version", "1");
        assertThat(result).isEqualTo("if_not_exists(#AMZN_MAPPED_version, :AMZN_MAPPED_1)");
    }

    @Test
    public void setActionsFor_withPresentNestedSchemas_resolvesUpdateBehaviorUsingNestedSchema() {
        when(root.converterForAttribute("parent")).thenReturn(attributeConverter);
        when(attributeConverter.type()).thenReturn(enhancedType);
        when(enhancedType.tableSchema()).thenReturn(Optional.of(tableSchema));
        when(tableSchema.tableMetadata()).thenReturn(tableMetadata);
        when(tableMetadata.customMetadataObject(anyString(), eq(UpdateBehavior.class))).thenReturn(Optional.empty());

        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("parent_NESTED_ATTR_UPDATE_child", AttributeValue.builder().s("value").build());

        List<SetAction> actions = operationExpression(attributes, root, Collections.emptyList()).setActions();
        assertThat(actions).hasSize(1);
        SetAction action = actions.get(0);

        // check expression path and value
        assertThat(action.path()).isEqualTo("#AMZN_MAPPED_parent.#AMZN_MAPPED_child");
        assertThat(action.value()).isEqualTo(":AMZN_MAPPED_parent_child");

        // check expression names
        assertThat(action.expressionNames()).hasSize(2);
        assertThat(action.expressionNames()).containsEntry("#AMZN_MAPPED_child", "child");
        assertThat(action.expressionNames()).containsEntry("#AMZN_MAPPED_parent", "parent");

        // check expression values
        assertThat(action.expressionValues()).hasSize(1);
        assertThat(action.expressionValues()).containsEntry(":AMZN_MAPPED_parent_child", AttributeValue.fromS("value"));
    }

    @Test
    public void operationExpression_writeIfNotExists_usesIfNotExistsFunction() {
        when(root.tableMetadata()).thenReturn(tableMetadata);
        when(tableMetadata.customMetadataObject(eq("UpdateBehavior:key"), eq(UpdateBehavior.class)))
            .thenReturn(Optional.of(UpdateBehavior.WRITE_IF_NOT_EXISTS));

        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("key", AttributeValue.builder().n("1").build());

        UpdateExpression expression = operationExpression(attributes, root, Collections.emptyList());

        List<SetAction> setActions = expression.setActions();
        assertThat(setActions).hasSize(1);
        SetAction setAction = setActions.get(0);

        // check that the path is correct and the value uses the if_not_exists function
        assertThat(setAction.path()).isEqualTo("#AMZN_MAPPED_key");
        assertThat(setAction.value()).isEqualTo("if_not_exists(#AMZN_MAPPED_key, :AMZN_MAPPED_key)");

        // check that the expression names are correct
        assertThat(setAction.expressionNames()).hasSize(1);
        assertThat(setAction.expressionNames()).containsEntry("#AMZN_MAPPED_key", "key");

        // check that the expression values are correct
        assertThat(setAction.expressionValues()).hasSize(1);
        assertThat(setAction.expressionValues()).containsEntry(":AMZN_MAPPED_key", AttributeValue.fromN("1"));
    }

    @Test
    public void operationExpression_nonNestedSchema_generatesSetAndRemoveActions() {
        when(root.tableMetadata()).thenReturn(tableMetadata);
        when(tableMetadata.customMetadataObject(anyString(), eq(UpdateBehavior.class))).thenReturn(Optional.empty());

        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("key1", AttributeValue.builder().s("key1_value").build());
        attributes.put("key2", AttributeValue.builder().nul(true).build());

        UpdateExpression expression = operationExpression(attributes, root, Collections.emptyList());

        List<SetAction> setActions = expression.setActions();
        assertThat(setActions).hasSize(1);
        SetAction setAction = setActions.get(0);

        // check expression path and value for the set action
        assertThat(setAction.path()).isEqualTo("#AMZN_MAPPED_key1");
        assertThat(setAction.value()).isEqualTo(":AMZN_MAPPED_key1");

        // check that the expression names are correct for the set action
        assertThat(setAction.expressionNames()).hasSize(1);
        assertThat(setAction.expressionNames()).containsEntry("#AMZN_MAPPED_key1", "key1");

        // check that the expression values are correct for the set action
        assertThat(setAction.expressionValues()).hasSize(1);
        assertThat(setAction.expressionValues()).containsEntry(":AMZN_MAPPED_key1", AttributeValue.fromS("key1_value"));

        // check that the remove action is generated for the null value
        List<RemoveAction> removeActions = expression.removeActions();
        assertThat(removeActions).hasSize(1);
        RemoveAction removeAction = removeActions.get(0);
        assertThat(removeAction.path()).isEqualTo("#AMZN_MAPPED_key2");
        assertThat(removeAction.expressionNames()).hasSize(1);
        assertThat(removeAction.expressionNames()).containsEntry("#AMZN_MAPPED_key2", "key2");
    }

    @Test
    public void operationExpression_nestedSchema_generatesSeparateSetActions() {
        when(root.converterForAttribute("parent")).thenReturn(attributeConverter);
        when(attributeConverter.type()).thenReturn(enhancedType);
        when(enhancedType.tableSchema()).thenReturn(Optional.of(tableSchema));
        when(tableSchema.tableMetadata()).thenReturn(tableMetadata);
        when(tableMetadata.customMetadataObject(anyString(), eq(UpdateBehavior.class))).thenReturn(Optional.empty());

        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("parent_NESTED_ATTR_UPDATE_child", AttributeValue.builder().s("value").build());

        UpdateExpression expression = operationExpression(attributes, root, Collections.emptyList());

        List<SetAction> setActions = expression.setActions();
        assertThat(setActions).hasSize(1);
        SetAction setAction = setActions.get(0);

        // check expression path and value for the set action
        assertThat(setAction.path()).isEqualTo("#AMZN_MAPPED_parent.#AMZN_MAPPED_child");
        assertThat(setAction.value()).isEqualTo(":AMZN_MAPPED_parent_child");

        // check that the expression names are correct for the set action
        assertThat(setAction.expressionNames()).hasSize(2);
        assertThat(setAction.expressionNames()).containsEntry("#AMZN_MAPPED_parent", "parent");
        assertThat(setAction.expressionNames()).containsEntry("#AMZN_MAPPED_child", "child");

        // check that the expression values are correct for the set action
        assertThat(setAction.expressionValues()).hasSize(1);
        assertThat(setAction.expressionValues()).containsEntry(":AMZN_MAPPED_parent_child", AttributeValue.fromS("value"));

    }
}
