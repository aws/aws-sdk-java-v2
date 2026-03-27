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

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UpdateExpressionUtilsTest {

    private static final TableMetadata TABLE_METADATA = StaticTableMetadata.builder().build();

    @Test
    public void ifNotExists_mapsKeyAndValueToIfNotExistsExpression() {
        String result = UpdateExpressionUtils.ifNotExists("key", "value");

        assertThat(result).isEqualTo("if_not_exists(#AMZN_MAPPED_key, :AMZN_MAPPED_value)");
    }

    @Test
    public void setActionsFor_emptyMap_returnsEmptyList() {
        List<SetAction> result = UpdateExpressionUtils.setActionsFor(Collections.emptyMap(), TABLE_METADATA);

        assertThat(result).isEmpty();
    }

    @Test
    public void setActionsFor_singleAttribute_createsSetAction() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attrName", AttributeValue.builder().s("attrValue").build());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, TABLE_METADATA);

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

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, TABLE_METADATA);

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
    public void setActionsFor_twoLevelDottedPath_producesSingleMappedSetAction() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("level1.level2", AttributeValue.builder().s("attrValue").build());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, TABLE_METADATA);

        assertThat(result).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_level1_level2")
                     .value(":AMZN_MAPPED_level1_level2")
                     .putExpressionName("#AMZN_MAPPED_level1_level2", "level1.level2")
                     .putExpressionValue(":AMZN_MAPPED_level1_level2", AttributeValue.builder().s("attrValue").build())
                     .build()));
    }

    @Test
    public void setActionsFor_threeLevelDottedPath_producesSingleMappedSetAction() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("level1.level2.level3", AttributeValue.builder().s("attrValue").build());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, TABLE_METADATA);

        assertThat(result).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_level1_level2_level3")
                     .value(":AMZN_MAPPED_level1_level2_level3")
                     .putExpressionName("#AMZN_MAPPED_level1_level2_level3", "level1.level2.level3")
                     .putExpressionValue(":AMZN_MAPPED_level1_level2_level3", AttributeValue.builder().s("attrValue").build())
                     .build()));
    }

    @Test
    public void setActionsFor_dashAndUnderscoreNames_producesDistinctMappedSetActions() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attrWithDash", AttributeValue.builder().s("#value").build());
        attributes.put("attrWithUnderscore", AttributeValue.builder().s("_value").build());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, TABLE_METADATA);

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
    public void removeActionsFor_twoLevelDottedPath_producesSingleMappedRemoveAction() {
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
    public void removeActionsFor_threeLevelDottedPath_producesSingleMappedRemoveAction() {
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
    public void removeActionsFor_dashAndUnderscoreNames_producesDistinctMappedRemoveActions() {
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

    @Test
    public void attributesPresentInOtherExpressions_skipsNullExpressions() {
        UpdateExpression expression =
            UpdateExpression.builder().addAction(
                                SetAction.builder()
                                         .path("customer.name")
                                         .value(":name")
                                         .putExpressionValue(":name", AttributeValue.builder().s("john").build())
                                         .build())
                            .build();

        Set<String> result = UpdateExpressionUtils.attributesPresentInOtherExpressions(Arrays.asList(null, expression));

        assertThat(result).containsExactly("customer");
    }

    @Test
    public void attributesPresentInOtherExpressions_unionsTopLevelNamesFromEachExpression() {
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(
                                SetAction.builder()
                                         .path("customer.name")
                                         .value(":name")
                                         .putExpressionValue(":name", AttributeValue.builder().s("john").build())
                                         .build())
                            .build();

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(RemoveAction.builder().path("orders[0]").build())
                            .build();

        Set<String> result = UpdateExpressionUtils.attributesPresentInOtherExpressions(
            Arrays.asList(requestExpression, extensionExpression));

        assertThat(result).containsExactlyInAnyOrder("customer", "orders");
    }

    @Test
    public void generateItemSetExpression_excludesNullAttributes() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("setAttr", AttributeValue.builder().s("set-value").build());
        itemMap.put("removeAttr", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionUtils.generateItemSetExpression(itemMap, TABLE_METADATA);

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("#AMZN_MAPPED_setAttr")
                     .value(":AMZN_MAPPED_setAttr")
                     .putExpressionName("#AMZN_MAPPED_setAttr", "setAttr")
                     .putExpressionValue(":AMZN_MAPPED_setAttr", AttributeValue.builder().s("set-value").build())
                     .build());
    }

    @Test
    public void generateItemRemoveExpression_skipsNonNullValuesAndExcludedAttributes() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("setAttr", AttributeValue.builder().s("set-value").build());
        itemMap.put("removeAttr", AttributeValue.builder().nul(true).build());
        itemMap.put("excludedFromRemovalAttr", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionUtils.generateItemRemoveExpression(itemMap, singleton(
            "excludedFromRemovalAttr"));

        assertThat(result.removeActions()).containsExactly(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_removeAttr")
                        .putExpressionName("#AMZN_MAPPED_removeAttr", "removeAttr")
                        .build());
    }

    @Test
    public void generateItemSetExpression_whenOnlyNullAttributes_returnsNoSetActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("removeAttr", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionUtils.generateItemSetExpression(itemMap, TABLE_METADATA);

        assertThat(result.setActions()).isEmpty();
    }

    @Test
    public void generateItemRemoveExpression_whenOnlyNonNullAttributes_returnsNoRemoveActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("setAttr", AttributeValue.builder().s("set-value").build());

        UpdateExpression result = UpdateExpressionUtils.generateItemRemoveExpression(itemMap, Collections.emptySet());

        assertThat(result.removeActions()).isEmpty();
    }

    @Test
    public void resolveTopLevelAttributeName_whenTokenizedNestedPath_returnsTopLevelName() {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#customer", "customer");
        expressionNames.put("#name", "name");

        String result = UpdateExpressionUtils.resolveTopLevelAttributeName("#customer.#name[0]", expressionNames);

        assertThat(result).isEqualTo("customer");
    }

    @Test
    public void resolveTopLevelAttributeName_whenLiteralPathWithListIndex_returnsTopLevelName() {
        String result = UpdateExpressionUtils.resolveTopLevelAttributeName("orders[1]", null);

        assertThat(result).isEqualTo("orders");
    }
}

