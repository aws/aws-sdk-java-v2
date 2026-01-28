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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UpdateExpressionUtilsTest {

    private final TableMetadata mockTableMetadata = mock(TableMetadata.class);

    @Test
    public void ifNotExists_createsCorrectExpression() {
        String result = UpdateExpressionUtils.ifNotExists("key", "value");

        assertThat(result).isEqualTo("if_not_exists(#AMZN_MAPPED_key, :AMZN_MAPPED_value)");
    }

    @Test
    public void setActionsFor_emptyMap_returnsEmptyList() {
        List<SetAction> result = UpdateExpressionUtils.setActionsFor(Collections.emptyMap(), mockTableMetadata);

        assertThat(result).isEmpty();
    }

    @Test
    public void setActionsFor_singleAttribute_createsSetAction() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attrName", AttributeValue.builder().s("attrValue").build());
        when(mockTableMetadata.primaryKeys()).thenReturn(Collections.emptyList());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, mockTableMetadata);

        assertThat(result).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attrName")
                     .value(":AMZN_MAPPED_attrName")
                     .putExpressionName("#AMZN_MAPPED_attrName", "attrName")
                     .putExpressionValue(":AMZN_MAPPED_attrName", AttributeValue.builder().s("attrValue").build())
                     .build()));
    }

    @Test
    public void setActionsFor_multipleAttributes_createsMultipleSetActions() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attr1Name", AttributeValue.builder().s("attr1Value").build());
        attributes.put("attr2Name", AttributeValue.builder().n("attr2Value").build());
        when(mockTableMetadata.primaryKeys()).thenReturn(Collections.emptyList());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, mockTableMetadata);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr1Name")
                     .value(":AMZN_MAPPED_attr1Name")
                     .putExpressionName("#AMZN_MAPPED_attr1Name", "attr1Name")
                     .putExpressionValue(":AMZN_MAPPED_attr1Name", AttributeValue.builder().s("attr1Value").build())
                     .build(),

            SetAction.builder()
                     .path("#AMZN_MAPPED_attr2Name")
                     .value(":AMZN_MAPPED_attr2Name")
                     .putExpressionName("#AMZN_MAPPED_attr2Name", "attr2Name")
                     .putExpressionValue(":AMZN_MAPPED_attr2Name", AttributeValue.builder().n("attr2Value").build())
                     .build());
    }

    @Test
    public void setActionsFor_nestedAttribute_handlesCorrectly() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("level1.level2", AttributeValue.builder().s("attrValue").build());
        when(mockTableMetadata.primaryKeys()).thenReturn(Collections.emptyList());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, mockTableMetadata);

        assertThat(result).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_level1_level2")
                     .value(":AMZN_MAPPED_level1_level2")
                     .putExpressionName("#AMZN_MAPPED_level1_level2", "level1.level2")
                     .putExpressionValue(":AMZN_MAPPED_level1_level2", AttributeValue.builder().s("attrValue").build())
                     .build()));
    }

    @Test
    public void setActionsFor_deeplyNestedAttribute_handlesCorrectly() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("level1.level2.level3", AttributeValue.builder().s("attrValue").build());
        when(mockTableMetadata.primaryKeys()).thenReturn(Collections.emptyList());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, mockTableMetadata);

        assertThat(result).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_level1_level2_level3")
                     .value(":AMZN_MAPPED_level1_level2_level3")
                     .putExpressionName("#AMZN_MAPPED_level1_level2_level3", "level1.level2.level3")
                     .putExpressionValue(":AMZN_MAPPED_level1_level2_level3", AttributeValue.builder().s("attrValue").build())
                     .build()));
    }

    @Test
    public void setActionsFor_attributeWithSpecialCharacters_handlesCorrectly() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attrWithDash", AttributeValue.builder().s("#value").build());
        attributes.put("attrWithUnderscore", AttributeValue.builder().s("_value").build());
        when(mockTableMetadata.primaryKeys()).thenReturn(Collections.emptyList());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, mockTableMetadata);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attrWithDash")
                     .value(":AMZN_MAPPED_attrWithDash")
                     .putExpressionName("#AMZN_MAPPED_attrWithDash", "attrWithDash")
                     .putExpressionValue(":AMZN_MAPPED_attrWithDash", AttributeValue.builder().s("#value").build())
                     .build(),

            SetAction.builder()
                     .path("#AMZN_MAPPED_attrWithUnderscore")
                     .value(":AMZN_MAPPED_attrWithUnderscore")
                     .putExpressionName("#AMZN_MAPPED_attrWithUnderscore", "attrWithUnderscore")
                     .putExpressionValue(":AMZN_MAPPED_attrWithUnderscore", AttributeValue.builder().s("_value").build())
                     .build());
    }

    @Test
    public void removeActionsFor_emptyMap_returnsEmptyList() {
        List<RemoveAction> result = UpdateExpressionUtils.removeActionsFor(Collections.emptyMap());

        assertThat(result).isEmpty();
    }

    @Test
    public void removeActionsFor_singleAttribute_createsRemoveAction() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attrName", AttributeValue.builder().s("attrValue").build());

        List<RemoveAction> result = UpdateExpressionUtils.removeActionsFor(attributes);

        assertThat(result).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attrName")
                        .putExpressionName("#AMZN_MAPPED_attrName", "attrName")
                        .build()));
    }

    @Test
    public void removeActionsFor_multipleAttributes_createsMultipleRemoveActions() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attr1Name", AttributeValue.builder().nul(true).build());
        attributes.put("attr2Name", AttributeValue.builder().nul(true).build());

        List<RemoveAction> result = UpdateExpressionUtils.removeActionsFor(attributes);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attr1Name")
                        .putExpressionName("#AMZN_MAPPED_attr1Name", "attr1Name")
                        .build(),
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attr2Name")
                        .putExpressionName("#AMZN_MAPPED_attr2Name", "attr2Name")
                        .build());
    }

    @Test
    public void removeActionsFor_nestedAttribute_handlesCorrectly() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("level1.level2", AttributeValue.builder().s("attrValue").build());

        List<RemoveAction> result = UpdateExpressionUtils.removeActionsFor(attributes);

        assertThat(result).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_level1_level2")
                        .putExpressionName("#AMZN_MAPPED_level1_level2", "level1.level2")
                        .build()));
    }

    @Test
    public void removeActionsFor_deeplyNestedAttribute_handlesCorrectly() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("level1.level2.level3", AttributeValue.builder().s("attrValue").build());

        List<RemoveAction> result = UpdateExpressionUtils.removeActionsFor(attributes);

        assertThat(result).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_level1_level2_level3")
                        .putExpressionName("#AMZN_MAPPED_level1_level2_level3", "level1.level2.level3")
                        .build()));
    }

    @Test
    public void removeActionsFor_attributeWithSpecialCharacters_handlesCorrectly() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attrWithDash", AttributeValue.builder().s("#value").build());
        attributes.put("attrWithUnderscore", AttributeValue.builder().s("_value").build());

        List<RemoveAction> result = UpdateExpressionUtils.removeActionsFor(attributes);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attrWithDash")
                        .putExpressionName("#AMZN_MAPPED_attrWithDash", "attrWithDash")
                        .build(),

            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attrWithUnderscore")
                        .putExpressionName("#AMZN_MAPPED_attrWithUnderscore", "attrWithUnderscore")
                        .build());
    }
}

