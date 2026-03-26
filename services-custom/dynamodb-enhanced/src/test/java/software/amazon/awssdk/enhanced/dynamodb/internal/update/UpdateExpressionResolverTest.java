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
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
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

    // -------------------------------------------------------
    // Null-safety: nonKeyAttributes not explicitly set
    // -------------------------------------------------------

    @Test
    public void resolve_legacy_defaultBuilderWithoutNonKeyAttributes_returnsExtensionExpression() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(AddAction.builder()
                                                .path("counter")
                                                .value(":inc")
                                                .putExpressionValue(":inc", AttributeValue.builder().n("1").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .build()
                                                          .resolve();

        assertThat(result).isEqualTo(extensionExpression);
    }

    // -------------------------------------------------------
    // LEGACY mode: overlapping paths are concatenated
    // -------------------------------------------------------

    @Test
    public void resolve_legacy_overlappingPojoAndRequest_concatenatesBothActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("counter", AttributeValue.builder().n("10").build());

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":req")
                                                .putExpressionValue(":req", AttributeValue.builder().n("20").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .nonKeyAttributes(itemMap)
                                                          .requestExpression(requestExpression)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("#AMZN_MAPPED_counter")
                     .value(":AMZN_MAPPED_counter")
                     .putExpressionName("#AMZN_MAPPED_counter", "counter")
                     .putExpressionValue(":AMZN_MAPPED_counter", AttributeValue.builder().n("10").build())
                     .build(),
            SetAction.builder()
                     .path("counter")
                     .value(":req")
                     .putExpressionValue(":req", AttributeValue.builder().n("20").build())
                     .build());
    }

    @Test
    public void resolve_legacy_overlappingExtensionAndRequest_concatenatesBothActions() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":ext")
                                                .putExpressionValue(":ext", AttributeValue.builder().n("10").build())
                                                .build())
                            .build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":req")
                                                .putExpressionValue(":req", AttributeValue.builder().n("20").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("counter")
                     .value(":ext")
                     .putExpressionValue(":ext", AttributeValue.builder().n("10").build())
                     .build(),
            SetAction.builder()
                     .path("counter")
                     .value(":req")
                     .putExpressionValue(":req", AttributeValue.builder().n("20").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: three-source list overlap
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_listTouchedByPojoExtensionAndRequest_keepsRequestSetActionsOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("list", AttributeValue.builder().l(AttributeValue.builder().s("pojo").build()).build());

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("list[0]")
                                                .value(":extensionValue")
                                                .putExpressionValue(":extensionValue", AttributeValue.builder().s("ext").build())
                                                .build())
                            .build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("list[1]")
                                                .value(":requestValue")
                                                .putExpressionValue(":requestValue", AttributeValue.builder().s("req").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("list[1]")
                     .value(":requestValue")
                     .putExpressionValue(":requestValue", AttributeValue.builder().s("req").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: sibling list indices (same top-level name)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_siblingListIndicesUnderSameAttribute_keepsRequestSetOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("list[0]")
                                                .value(":v0")
                                                .putExpressionValue(":v0", AttributeValue.builder().s("v0").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("list[1]")
                                                .value(":v1")
                                                .putExpressionValue(":v1", AttributeValue.builder().s("v1").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("list[1]")
                     .value(":v1")
                     .putExpressionValue(":v1", AttributeValue.builder().s("v1").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: exact same scalar path
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_identicalPathFromThreeSources_keepsRequestSetOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":ext")
                                                .putExpressionValue(":ext", AttributeValue.builder().n("10").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":req")
                                                .putExpressionValue(":req", AttributeValue.builder().n("20").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("counter")
                     .value(":req")
                     .putExpressionValue(":req", AttributeValue.builder().n("20").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: object parent/child overlap
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_objectRootVersusNestedRequestPath_keepsRequestNestedSetOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("profile")
                                                .value(":profile")
                                                .putExpressionValue(":profile",
                                                                    AttributeValue.builder().m(Collections.emptyMap()).build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("profile.name")
                                                .value(":name")
                                                .putExpressionValue(":name", AttributeValue.builder().s("alice").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("profile.name")
                     .value(":name")
                     .putExpressionValue(":name", AttributeValue.builder().s("alice").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: list-of-objects parent/child
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_objectListRootVersusNestedRequestPath_keepsRequestNestedSetOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("items[0]")
                                                .value(":item0")
                                                .putExpressionValue(":item0",
                                                                    AttributeValue.builder().m(Collections.emptyMap()).build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("items[0].name")
                                                .value(":name")
                                                .putExpressionValue(":name", AttributeValue.builder().s("new-name").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("items[0].name")
                     .value(":name")
                     .putExpressionValue(":name", AttributeValue.builder().s("new-name").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: nested object paths under same top-level name
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_objectNestedSiblingsUnderSameTopLevelName_keepsRequestOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("profile.name")
                                                .value(":name")
                                                .putExpressionValue(":name", AttributeValue.builder().s("alice").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("profile.address.city")
                                                .value(":city")
                                                .putExpressionValue(":city", AttributeValue.builder().s("seattle").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("profile.address.city")
                     .value(":city")
                     .putExpressionValue(":city", AttributeValue.builder().s("seattle").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: POJO-extension only (no request)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_pojoAndExtensionShareAttributeWithoutRequest_keepsExtensionActionsOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("counter", AttributeValue.builder().n("10").build());

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":ext")
                                                .putExpressionValue(":ext", AttributeValue.builder().n("20").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("counter")
                     .value(":ext")
                     .putExpressionValue(":ext", AttributeValue.builder().n("20").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: extension-request only (no POJO)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_extensionAndRequestShareAttributeWithoutPojo_keepsRequestActionsOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("status")
                                                .value(":ext")
                                                .putExpressionValue(":ext", AttributeValue.builder().s("ext-val").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("status")
                                                .value(":req")
                                                .putExpressionValue(":req", AttributeValue.builder().s("req-val").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("status")
                     .value(":req")
                     .putExpressionValue(":req", AttributeValue.builder().s("req-val").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: completely disjoint sources
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_allSourcesDisjointPaths_keepsEveryAction() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attrA", AttributeValue.builder().s("a").build());

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("attrB")
                                                .value(":b")
                                                .putExpressionValue(":b", AttributeValue.builder().s("b").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("attrC")
                                                .value(":c")
                                                .putExpressionValue(":c", AttributeValue.builder().s("c").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("attrC")
                     .value(":c")
                     .putExpressionValue(":c", AttributeValue.builder().s("c").build())
                     .build(),
            SetAction.builder()
                     .path("attrB")
                     .value(":b")
                     .putExpressionValue(":b", AttributeValue.builder().s("b").build())
                     .build(),
            SetAction.builder()
                     .path("#AMZN_MAPPED_attrA")
                     .value(":AMZN_MAPPED_attrA")
                     .putExpressionName("#AMZN_MAPPED_attrA", "attrA")
                     .putExpressionValue(":AMZN_MAPPED_attrA", AttributeValue.builder().s("a").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: POJO REMOVE vs request SET
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_pojoRemoveVsRequestSet_keepsRequestSetOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attrX", AttributeValue.builder().nul(true).build());

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("attrX")
                                                .value(":val")
                                                .putExpressionValue(":val", AttributeValue.builder().s("new-value").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .nonKeyAttributes(itemMap)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(
                                                              PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("attrX")
                     .value(":val")
                     .putExpressionValue(":val", AttributeValue.builder().s("new-value").build())
                     .build());
        assertThat(result.removeActions()).isEmpty();
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: three-source exact same path
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_threeSourcesSamePath_keepsRequestOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("counter", AttributeValue.builder().n("1").build());

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":ext")
                                                .putExpressionValue(":ext", AttributeValue.builder().n("2").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":req")
                                                .putExpressionValue(":req", AttributeValue.builder().n("3").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("counter")
                     .value(":req")
                     .putExpressionValue(":req", AttributeValue.builder().n("3").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: expression attribute names (#list → list)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_expressionNamePlaceholder_groupsByResolvedTopLevelName() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("#l[0]")
                                                .value(":v0")
                                                .putExpressionName("#l", "list")
                                                .putExpressionValue(":v0", AttributeValue.builder().s("ext").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("list[1]")
                                                .value(":v1")
                                                .putExpressionValue(":v1", AttributeValue.builder().s("req").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("list[1]")
                     .value(":v1")
                     .putExpressionValue(":v1", AttributeValue.builder().s("req").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: multiple request actions on one top-level name
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_requestMultipleActionsOnSameTopLevelName_allKept() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("list[2]")
                                                .value(":ext")
                                                .putExpressionValue(":ext", AttributeValue.builder().s("ext").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("list[0]")
                                                .value(":a")
                                                .putExpressionValue(":a", AttributeValue.builder().s("a").build())
                                                .build())
                            .addAction(SetAction.builder()
                                                .path("list[1]")
                                                .value(":b")
                                                .putExpressionValue(":b", AttributeValue.builder().s("b").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("list[0]")
                     .value(":a")
                     .putExpressionValue(":a", AttributeValue.builder().s("a").build())
                     .build(),
            SetAction.builder()
                     .path("list[1]")
                     .value(":b")
                     .putExpressionValue(":b", AttributeValue.builder().s("b").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: extension vs request on different nested paths (same first segment)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_nestedPathsSameFirstSegment_keepsRequestOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("a.b")
                                                .value(":b")
                                                .putExpressionValue(":b", AttributeValue.builder().s("from-ext").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("a.c")
                                                .value(":c")
                                                .putExpressionValue(":c", AttributeValue.builder().s("from-req").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("a.c")
                     .value(":c")
                     .putExpressionValue(":c", AttributeValue.builder().s("from-req").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: all sources null → returns null
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_allSourcesNull_returnsNull() {
        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();
        assertNull(result);
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: request only (no extension, no POJO)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_requestOnlyNoOtherSources_returnsRequestAsIs() {
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("attr")
                                                .value(":val")
                                                .putExpressionValue(":val", AttributeValue.builder().s("v").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("attr")
                     .value(":val")
                     .putExpressionValue(":val", AttributeValue.builder().s("v").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: DELETE action from extension vs SET from request on same attribute
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_extensionDeleteVsRequestSet_keepsRequestSetOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(DeleteAction.builder()
                                                   .path("tags")
                                                   .value(":toRemove")
                                                   .putExpressionValue(":toRemove",
                                                                      AttributeValue.builder()
                                                                                    .ss("old-tag")
                                                                                    .build())
                                                   .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("tags")
                                                .value(":newTags")
                                                .putExpressionValue(":newTags",
                                                                    AttributeValue.builder()
                                                                                  .ss("new-tag")
                                                                                  .build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("tags")
                     .value(":newTags")
                     .putExpressionValue(":newTags", AttributeValue.builder().ss("new-tag").build())
                     .build());
        assertThat(result.deleteActions()).isEmpty();
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: extension ADD vs request SET on same attribute
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_extensionAddVsRequestSet_keepsRequestSetOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(AddAction.builder()
                                                .path("counter")
                                                .value(":inc")
                                                .putExpressionValue(":inc", AttributeValue.builder().n("1").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":val")
                                                .putExpressionValue(":val", AttributeValue.builder().n("100").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("counter")
                     .value(":val")
                     .putExpressionValue(":val", AttributeValue.builder().n("100").build())
                     .build());
        assertThat(result.addActions()).isEmpty();
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: POJO REMOVE vs extension SET (no request)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_pojoRemoveVsExtensionSet_keepsExtensionOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("status", AttributeValue.builder().nul(true).build());

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("status")
                                                .value(":val")
                                                .putExpressionValue(":val", AttributeValue.builder().s("active").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("status")
                     .value(":val")
                     .putExpressionValue(":val", AttributeValue.builder().s("active").build())
                     .build());
        assertThat(result.removeActions()).isEmpty();
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE: partial ownership — request owns one attr, extension owns another
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_partialOwnership_eachSourceKeepsOwnedAttributes() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":extC")
                                                .putExpressionValue(":extC", AttributeValue.builder().n("10").build())
                                                .build())
                            .addAction(SetAction.builder()
                                                .path("status")
                                                .value(":extS")
                                                .putExpressionValue(":extS", AttributeValue.builder().s("ext-status").build())
                                                .build())
                            .build();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("counter")
                                                .value(":req")
                                                .putExpressionValue(":req", AttributeValue.builder().n("99").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(mockTableMetadata)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("counter")
                     .value(":req")
                     .putExpressionValue(":req", AttributeValue.builder().n("99").build())
                     .build(),
            SetAction.builder()
                     .path("status")
                     .value(":extS")
                     .putExpressionValue(":extS", AttributeValue.builder().s("ext-status").build())
                     .build());
    }
}