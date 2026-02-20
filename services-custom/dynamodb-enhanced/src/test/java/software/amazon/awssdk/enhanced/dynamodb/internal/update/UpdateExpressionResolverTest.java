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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UpdateExpressionResolverTest {

    private final TableMetadata mockTableMetadata = mock(TableMetadata.class);

    @Test
    public void resolve_emptyInputs_returnsEmptyUpdateExpression() {
        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(mockTableMetadata)
                                                                    .nonKeyAttributes(Collections.emptyMap())
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertNull(result);
    }

    @Test
    public void resolve_nonNullAttributes_generatesSetActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("itemAttr1Name", AttributeValue.builder().s("itemAttr1Value").build());
        itemMap.put("itemAttr2Name", AttributeValue.builder().n("itemAttr2Value").build());

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(mockTableMetadata)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.removeActions()).isEmpty();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).hasSize(2).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("#AMZN_MAPPED_itemAttr1Name")
                     .value(":AMZN_MAPPED_itemAttr1Name")
                     .putExpressionName("#AMZN_MAPPED_itemAttr1Name", "itemAttr1Name")
                     .putExpressionValue(":AMZN_MAPPED_itemAttr1Name", AttributeValue.builder().s("itemAttr1Value").build())
                     .build(),

            SetAction.builder()
                     .path("#AMZN_MAPPED_itemAttr2Name")
                     .value(":AMZN_MAPPED_itemAttr2Name")
                     .putExpressionName("#AMZN_MAPPED_itemAttr2Name", "itemAttr2Name")
                     .putExpressionValue(":AMZN_MAPPED_itemAttr2Name", AttributeValue.builder().n("itemAttr2Value").build())
                     .build());
    }

    @Test
    public void resolve_nullAttributes_generatesRemoveActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("itemAttr1Name", AttributeValue.builder().nul(true).build());
        itemMap.put("itemAttr2Name", AttributeValue.builder().nul(true).build());

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(mockTableMetadata)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.setActions()).isEmpty();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.removeActions()).hasSize(2).containsExactlyInAnyOrder(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_itemAttr1Name")
                        .putExpressionName("#AMZN_MAPPED_itemAttr1Name", "itemAttr1Name")
                        .build(),

            RemoveAction.builder()
                        .path("#AMZN_MAPPED_itemAttr2Name")
                        .putExpressionName("#AMZN_MAPPED_itemAttr2Name", "itemAttr2Name")
                        .build());
    }

    @Test
    public void resolve_mixedAttributes_generatesBothActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("setAttrName", AttributeValue.builder().s("setAttrValue").build());
        itemMap.put("removeAttrName", AttributeValue.builder().nul(true).build());

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(mockTableMetadata)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_setAttrName")
                     .value(":AMZN_MAPPED_setAttrName")
                     .putExpressionName("#AMZN_MAPPED_setAttrName", "setAttrName")
                     .putExpressionValue(":AMZN_MAPPED_setAttrName", AttributeValue.builder().s("setAttrValue").build())
                     .build()));

        assertThat(result.removeActions()).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_removeAttrName")
                        .putExpressionName("#AMZN_MAPPED_removeAttrName", "removeAttrName")
                        .build()));
    }

    @Test
    public void resolve_withItemAndExtensionExpression_mergesActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("itemAttrName", AttributeValue.builder().s("itemAttrValue").build());

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(AddAction.builder()
                                                .path("extensionAttrName")
                                                .value(":extensionAttrValue")
                                                .putExpressionValue(":extensionAttrValue",
                                                                    AttributeValue.builder().n("1").build())
                                                .build())
                            .build();

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(mockTableMetadata)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .extensionExpression(extensionExpression)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.removeActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_itemAttrName")
                     .value(":AMZN_MAPPED_itemAttrName")
                     .putExpressionName("#AMZN_MAPPED_itemAttrName", "itemAttrName")
                     .putExpressionValue(":AMZN_MAPPED_itemAttrName", AttributeValue.builder().s("itemAttrValue").build())
                     .build()));

        assertThat(result.addActions()).isEqualTo(Collections.singletonList(
            AddAction.builder()
                     .path("extensionAttrName")
                     .value(":extensionAttrValue")
                     .putExpressionValue(":extensionAttrValue", AttributeValue.builder().n("1").build())
                     .build()));
    }

    @Test
    public void resolve_withAllExpressionTypes_mergesInCorrectOrder() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("itemAttrName", AttributeValue.builder().s("itemAttrValue").build());

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(AddAction.builder()
                                                .path("extensionAttrName")
                                                .value(":extensionAttrName")
                                                .putExpressionValue(":extensionAttrName", AttributeValue.builder().s(
                                                    "extensionAttrValue").build())
                                                .build())
                            .build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("requestAttrName")
                                                .value(":requestAttrName")
                                                .putExpressionValue(":requestAttrName", AttributeValue.builder().s(
                                                    "requestAttrValue").build())
                                                .build())
                            .build();

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(mockTableMetadata)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .extensionExpression(extensionExpression)
                                                                    .requestExpression(requestExpression)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.removeActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).hasSize(2).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("#AMZN_MAPPED_itemAttrName")
                     .value(":AMZN_MAPPED_itemAttrName")
                     .putExpressionName("#AMZN_MAPPED_itemAttrName", "itemAttrName")
                     .putExpressionValue(":AMZN_MAPPED_itemAttrName", AttributeValue.builder().s("itemAttrValue").build())
                     .build(),
            SetAction.builder()
                     .path("requestAttrName")
                     .value(":requestAttrName")
                     .putExpressionValue(":requestAttrName", AttributeValue.builder().s("requestAttrValue").build())
                     .build());

        assertThat(result.addActions()).isEqualTo(Collections.singletonList(
            AddAction.builder()
                     .path("extensionAttrName")
                     .value(":extensionAttrName")
                     .putExpressionValue(":extensionAttrName", AttributeValue.builder().s("extensionAttrValue").build())
                     .build()));
    }

    @Test
    public void resolve_attributeUsedInOtherExpression_filteredOutFromRemoveActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("itemAttr1Name", AttributeValue.builder().nul(true).build());
        itemMap.put("itemAttr2Name", AttributeValue.builder().nul(true).build());

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("itemAttr1Name")
                                                .value(":itemAttr1Value")
                                                .putExpressionName("#itemAttr1Name", "itemAttr1Name")
                                                .putExpressionValue(":itemAttr1Value", AttributeValue.builder().s(
                                                    "itemAttr1Value_new").build())
                                                .build())
                            .build();

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(mockTableMetadata)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .requestExpression(requestExpression)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("itemAttr1Name")
                     .value(":itemAttr1Value")
                     .putExpressionName("#itemAttr1Name", "itemAttr1Name")
                     .putExpressionValue(":itemAttr1Value", AttributeValue.builder().s("itemAttr1Value_new").build())
                     .build()));

        // only itemAttr2Name, itemAttr1Name filtered out (because was present in a set expression)
        assertThat(result.removeActions()).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_itemAttr2Name")
                        .putExpressionName("#AMZN_MAPPED_itemAttr2Name", "itemAttr2Name")
                        .build()));
    }

    @Test
    public void generateItemSetExpression_andFiltersNullValues() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("validItemAttrName", AttributeValue.builder().s("validItemAttrValue").build());
        itemMap.put("nullItemAttrName", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionResolver.generateItemSetExpression(itemMap, mockTableMetadata);

        assertThat(result).isNotNull();
        assertThat(result.removeActions()).isEmpty();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_validItemAttrName")
                     .value(":AMZN_MAPPED_validItemAttrName")
                     .putExpressionName("#AMZN_MAPPED_validItemAttrName", "validItemAttrName")
                     .putExpressionValue(":AMZN_MAPPED_validItemAttrName",
                                         AttributeValue.builder().s("validItemAttrValue").build())
                     .build()));
    }

    @Test
    public void generateItemRemoveExpression_includesOnlyNullValues() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("validItemAttrName", AttributeValue.builder().s("validItemAttrValue").build());
        itemMap.put("nullItemAttrName", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionResolver.generateItemRemoveExpression(itemMap, Collections.emptySet());

        assertThat(result).isNotNull();
        assertThat(result.setActions()).isEmpty();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.removeActions()).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_nullItemAttrName")
                        .putExpressionName("#AMZN_MAPPED_nullItemAttrName", "nullItemAttrName")
                        .build()));
    }

    @Test
    public void generateItemRemoveExpression_excludesNonRemovableAttributes() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("nullItemAttr1Name", AttributeValue.builder().nul(true).build());
        itemMap.put("nullItemAttr2Name", AttributeValue.builder().nul(true).build());

        UpdateExpression result = UpdateExpressionResolver.generateItemRemoveExpression(
            itemMap, Collections.singleton("nullItemAttr1Name"));

        assertThat(result).isNotNull();
        assertThat(result.setActions()).isEmpty();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.removeActions()).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_nullItemAttr2Name")
                        .putExpressionName("#AMZN_MAPPED_nullItemAttr2Name", "nullItemAttr2Name")
                        .build()));
    }

    @Test
    public void builder_allFields_buildsSuccessfully() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        UpdateExpression extensionExpr = UpdateExpression.builder().build();
        UpdateExpression requestExpr = UpdateExpression.builder().build();

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(mockTableMetadata)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .extensionExpression(extensionExpr)
                                                                    .requestExpression(requestExpr)
                                                                    .build();

        assertThat(resolver).isNotNull();
    }
}