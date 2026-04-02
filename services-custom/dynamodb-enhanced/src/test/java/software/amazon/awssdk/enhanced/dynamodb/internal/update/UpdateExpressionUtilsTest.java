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
        attributes.put("attr", AttributeValue.builder().s("attrValue").build());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, TABLE_METADATA);

        assertThat(result).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr")
                     .value(":AMZN_MAPPED_attr")
                     .putExpressionName("#AMZN_MAPPED_attr", "attr")
                     .putExpressionValue(":AMZN_MAPPED_attr", AttributeValue.builder().s("attrValue").build())
                     .build()));
    }

    @Test
    public void setActionsFor_multipleAttributes_createsMultipleSetActions() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attr", AttributeValue.builder().s("attrValue").build());
        attributes.put("attrTwo", AttributeValue.builder().n("2").build());

        List<SetAction> result = UpdateExpressionUtils.setActionsFor(attributes, TABLE_METADATA);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr")
                     .value(":AMZN_MAPPED_attr")
                     .putExpressionName("#AMZN_MAPPED_attr", "attr")
                     .putExpressionValue(":AMZN_MAPPED_attr", AttributeValue.builder().s("attrValue").build())
                     .build(),

            SetAction.builder()
                     .path("#AMZN_MAPPED_attrTwo")
                     .value(":AMZN_MAPPED_attrTwo")
                     .putExpressionName("#AMZN_MAPPED_attrTwo", "attrTwo")
                     .putExpressionValue(":AMZN_MAPPED_attrTwo", AttributeValue.builder().n("2").build())
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
        attributes.put("attr", AttributeValue.builder().s("attrValue").build());

        List<RemoveAction> result = UpdateExpressionUtils.removeActionsFor(attributes);

        assertThat(result).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attr")
                        .putExpressionName("#AMZN_MAPPED_attr", "attr")
                        .build()));
    }

    @Test
    public void removeActionsFor_multipleAttributes_createsMultipleRemoveActions() {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("attr", AttributeValue.builder().nul(true).build());
        attributes.put("attrTwo", AttributeValue.builder().nul(true).build());

        List<RemoveAction> result = UpdateExpressionUtils.removeActionsFor(attributes);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attr")
                        .putExpressionName("#AMZN_MAPPED_attr", "attr")
                        .build(),
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attrTwo")
                        .putExpressionName("#AMZN_MAPPED_attrTwo", "attrTwo")
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
        UpdateExpression reqExpression =
            UpdateExpression.builder()
                            .addAction(
                                SetAction.builder()
                                         .path("reqAttr.name")
                                         .value(":reqAttrValue")
                                         .putExpressionValue(":reqAttrValue", AttributeValue.builder().s("john").build())
                                         .build())
                            .build();

        UpdateExpression extExpression =
            UpdateExpression.builder()
                            .addAction(RemoveAction.builder().path("extAttr[0]").build())
                            .build();

        Set<String> result = UpdateExpressionUtils.attributesPresentInOtherExpressions(
            Arrays.asList(reqExpression, extExpression));

        assertThat(result).containsExactlyInAnyOrder("reqAttr", "extAttr");
    }

    @Test
    public void generateItemSetExpression_excludesNullAttributes() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().s("attrValue").build());
        itemMap.put("attrToRemove", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionUtils.generateItemSetExpression(itemMap, TABLE_METADATA);

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr")
                     .value(":AMZN_MAPPED_attr")
                     .putExpressionName("#AMZN_MAPPED_attr", "attr")
                     .putExpressionValue(":AMZN_MAPPED_attr", AttributeValue.builder().s("attrValue").build())
                     .build());
    }

    @Test
    public void generateItemRemoveExpression_skipsNonNullValuesAndExcludedAttributes() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().s("attrValue").build());
        itemMap.put("attrToRemove", AttributeValue.builder().nul(true).build());
        itemMap.put("excludedAttr", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionUtils.generateItemRemoveExpression(itemMap, singleton(
            "excludedAttr"));

        assertThat(result.removeActions()).containsExactly(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attrToRemove")
                        .putExpressionName("#AMZN_MAPPED_attrToRemove", "attrToRemove")
                        .build());
    }

    @Test
    public void generateItemSetExpression_whenOnlyNullAttributes_returnsNoSetActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionUtils.generateItemSetExpression(itemMap, TABLE_METADATA);

        assertThat(result.setActions()).isEmpty();
    }

    @Test
    public void generateItemRemoveExpression_whenOnlyNonNullAttributes_returnsNoRemoveActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().s("attrValue").build());

        UpdateExpression result = UpdateExpressionUtils.generateItemRemoveExpression(itemMap, Collections.emptySet());

        assertThat(result.removeActions()).isEmpty();
    }
}

