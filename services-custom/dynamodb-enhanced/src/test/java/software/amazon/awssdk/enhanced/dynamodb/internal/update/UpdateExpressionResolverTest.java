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
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy.LEGACY;
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Unit tests for {@link UpdateExpressionResolver}.
 * <p>
 * Naming convention (merge strategy is reflected in each test name):
 * <ul>
 *   <li>{@code resolve_legacy_*} — {@link software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy#LEGACY}
 *       (default; also when merge strategy is unset or {@code null})</li>
 *   <li>{@code resolve_prioritizeHigherSource_*} —
 *       {@link software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy#PRIORITIZE_HIGHER_SOURCE}</li>
 * </ul>
 */
public class UpdateExpressionResolverTest {

    private static final TableMetadata TABLE_METADATA = StaticTableMetadata.builder().build();

    // --------------------------------------------------------------
    // LEGACY — default merge strategy (order map, extension, request)
    // --------------------------------------------------------------

    @Test
    public void resolve_legacy_emptyInputs_returnsNull() {
        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
                                                                    .nonKeyAttributes(Collections.emptyMap())
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertNull(result);
    }

    @Test
    public void resolve_legacy_nonNullAttributes_generatesSetActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().s("attrValue").build());
        itemMap.put("attrTwo", AttributeValue.builder().n("2").build());

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.removeActions()).isEmpty();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).hasSize(2).containsExactlyInAnyOrder(
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
    public void resolve_legacy_nullAttributes_generatesRemoveActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().nul(true).build());
        itemMap.put("attrTwo", AttributeValue.builder().nul(true).build());

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.setActions()).isEmpty();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.removeActions()).hasSize(2).containsExactlyInAnyOrder(
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
    public void resolve_legacy_mixedAttributes_generatesBothActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().s("attrValue").build());
        itemMap.put("attrToRemove", AttributeValue.builder().nul(true).build());

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr")
                     .value(":AMZN_MAPPED_attr")
                     .putExpressionName("#AMZN_MAPPED_attr", "attr")
                     .putExpressionValue(":AMZN_MAPPED_attr", AttributeValue.builder().s("attrValue").build())
                     .build()));

        assertThat(result.removeActions()).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attrToRemove")
                        .putExpressionName("#AMZN_MAPPED_attrToRemove", "attrToRemove")
                        .build()));
    }

    @Test
    public void resolve_legacy_withPojoAndExtensionExpressions_mergesActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().s("attrValue").build());

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(AddAction.builder()
                                                .path("extAttr")
                                                .value(":extAttrValue")
                                                .putExpressionValue(":extAttrValue",
                                                                    AttributeValue.builder().n("1").build())
                                                .build())
                            .build();

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .extensionExpression(extensionExpression)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.removeActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr")
                     .value(":AMZN_MAPPED_attr")
                     .putExpressionName("#AMZN_MAPPED_attr", "attr")
                     .putExpressionValue(":AMZN_MAPPED_attr", AttributeValue.builder().s("attrValue").build())
                     .build()));

        assertThat(result.addActions()).isEqualTo(Collections.singletonList(
            AddAction.builder()
                     .path("extAttr")
                     .value(":extAttrValue")
                     .putExpressionValue(":extAttrValue", AttributeValue.builder().n("1").build())
                     .build()));
    }

    @Test
    public void resolve_legacy_withAllExpressionTypes_mergesActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().s("attrValue").build());

        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(AddAction.builder()
                                .path("extAttr")
                                .value(":extAttrValue")
                                .putExpressionValue(":extAttrValue",
                                                    AttributeValue.builder().s("extAttrValue").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("reqAttr")
                                .value(":reqAttrValue")
                                .putExpressionValue(":reqAttrValue",
                                                    AttributeValue.builder().s("reqAttrValue").build())
                                .build())
            .build();

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
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
                     .path("#AMZN_MAPPED_attr")
                     .value(":AMZN_MAPPED_attr")
                     .putExpressionName("#AMZN_MAPPED_attr", "attr")
                     .putExpressionValue(":AMZN_MAPPED_attr", AttributeValue.builder().s("attrValue").build())
                     .build(),
            SetAction.builder()
                     .path("reqAttr")
                     .value(":reqAttrValue")
                     .putExpressionValue(":reqAttrValue", AttributeValue.builder().s("reqAttrValue").build())
                     .build());

        assertThat(result.addActions()).isEqualTo(Collections.singletonList(
            AddAction.builder()
                     .path("extAttr")
                     .value(":extAttrValue")
                     .putExpressionValue(":extAttrValue", AttributeValue.builder().s("extAttrValue").build())
                     .build()));
    }

    @Test
    public void resolve_legacy_attributeUsedInOtherExpression_filteredOutFromRemoveActions() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().nul(true).build());
        itemMap.put("attrTwo", AttributeValue.builder().nul(true).build());

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("attr")
                                .value(":reqAttrValue")
                                .putExpressionName("#attr", "attr")
                                .putExpressionValue(":reqAttrValue", AttributeValue.builder().s("reqAttrValue").build())
                                .build())
            .build();

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .requestExpression(requestExpression)
                                                                    .build();

        UpdateExpression result = resolver.resolve();

        assertThat(result).isNotNull();
        assertThat(result.addActions()).isEmpty();
        assertThat(result.deleteActions()).isEmpty();

        assertThat(result.setActions()).isEqualTo(Collections.singletonList(
            SetAction.builder()
                     .path("attr")
                     .value(":reqAttrValue")
                     .putExpressionName("#attr", "attr")
                     .putExpressionValue(":reqAttrValue", AttributeValue.builder().s("reqAttrValue").build())
                     .build()));

        // Only attrTwo remains for REMOVE because attr is referenced by request expression.
        assertThat(result.removeActions()).isEqualTo(Collections.singletonList(
            RemoveAction.builder()
                        .path("#AMZN_MAPPED_attrTwo")
                        .putExpressionName("#AMZN_MAPPED_attrTwo", "attrTwo")
                        .build()));
    }

    @Test
    public void resolve_legacy_builder_preservesConfiguredFields() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        UpdateExpression extensionExpr = UpdateExpression.builder().build();
        UpdateExpression requestExpr = UpdateExpression.builder().build();

        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
                                                                    .nonKeyAttributes(itemMap)
                                                                    .extensionExpression(extensionExpr)
                                                                    .requestExpression(requestExpr)
                                                                    .build();

        assertThat(resolver.tableMetadata()).isSameAs(TABLE_METADATA);
        assertThat(resolver.nonKeyAttributes()).isEmpty();
        assertThat(resolver.extensionExpression()).isSameAs(extensionExpr);
        assertThat(resolver.requestExpression()).isSameAs(requestExpr);
        assertThat(resolver.updateExpressionMergeStrategy()).isEqualTo(LEGACY);
    }

    @Test
    public void resolve_legacy_builder_whenNonKeyAttributesNull_defaultsToEmptyMap() {
        UpdateExpressionResolver resolver = UpdateExpressionResolver.builder()
                                                                    .tableMetadata(TABLE_METADATA)
                                                                    .nonKeyAttributes(null)
                                                                    .build();

        assertThat(resolver.nonKeyAttributes()).isEmpty();
        assertThat(resolver.resolve()).isNull();
    }

    // ---------------------------------------------------------
    // LEGACY — no non-key order map / no request expression (extension only)
    // ---------------------------------------------------------

    @Test
    public void resolve_legacy_whenOnlyExtensionActionPresent_returnsExtensionExpression() {
        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(AddAction.builder()
                                .path("counter")
                                .value(":inc")
                                .putExpressionValue(":inc", AttributeValue.builder().n("1").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .build()
                                                          .resolve();

        assertThat(result).isEqualTo(extensionExpression);
    }

    // -------------------------------------------------------
    // LEGACY — overlapping paths are concatenated
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
                                                          .tableMetadata(TABLE_METADATA)
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
                                                          .tableMetadata(TABLE_METADATA)
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

    @Test
    public void resolve_legacy_mergeStrategyNull_defaultsToConcatenation() {
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
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(null)
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
    // PRIORITIZE_HIGHER_SOURCE — three-source list overlap
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_listTouchedByPojoExtensionAndRequest_keepsRequestSetActionsOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("list", AttributeValue.builder().l(AttributeValue.builder().s("pojo").build()).build());

        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("list[0]")
                                .value(":extensionValue")
                                .putExpressionValue(":extensionValue", AttributeValue.builder().s("ext-value").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("list[1]")
                                .value(":requestValue")
                                .putExpressionValue(":requestValue", AttributeValue.builder().s("req-value").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("list[1]")
                     .value(":requestValue")
                     .putExpressionValue(":requestValue", AttributeValue.builder().s("req-value").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE — sibling list indices (same top-level name)
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
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
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
    // PRIORITIZE_HIGHER_SOURCE — exact same scalar path
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
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
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
    // PRIORITIZE_HIGHER_SOURCE — object parent/child overlap
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_objectRootVersusNestedRequestPath_keepsRequestNestedSetOnly() {
        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("customer")
                                .value(":customer")
                                .putExpressionValue(":customer", AttributeValue.builder().m(Collections.emptyMap()).build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("customer.name")
                                .value(":name")
                                .putExpressionValue(":name", AttributeValue.builder().s("john").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("customer.name")
                     .value(":name")
                     .putExpressionValue(":name", AttributeValue.builder().s("john").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE — list-of-objects parent/child
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_objectListRootVersusNestedRequestPath_keepsRequestNestedSetOnly() {
        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("orders[0]")
                                .value(":order0")
                                .putExpressionValue(":order0", AttributeValue.builder().m(Collections.emptyMap()).build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("orders[0].id")
                                .value(":id")
                                .putExpressionValue(":id", AttributeValue.builder().s("orderId").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("orders[0].id")
                     .value(":id")
                     .putExpressionValue(":id", AttributeValue.builder().s("orderId").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE — nested object paths under same top-level name
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_objectNestedSiblingsUnderSameTopLevelName_keepsRequestOnly() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("customer.name")
                                                .value(":name")
                                                .putExpressionValue(":name", AttributeValue.builder().s("john").build())
                                                .build())
                            .build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("customer.address.city")
                                                .value(":city")
                                                .putExpressionValue(":city", AttributeValue.builder().s("london").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("customer.address.city")
                     .value(":city")
                     .putExpressionValue(":city", AttributeValue.builder().s("london").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE — POJO-extension only (no request)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_pojoAndExtensionShareAttributeWithoutRequest_keepsExtensionActionOnly() {
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
                                                          .tableMetadata(TABLE_METADATA)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
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
    // PRIORITIZE_HIGHER_SOURCE — extension-request only (no POJO)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_extensionAndRequestShareAttributeWithoutPojo_keepsRequestActionOnly() {
        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("status")
                                .value(":ext")
                                .putExpressionValue(":ext", AttributeValue.builder().s("ext-val").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("status")
                                .value(":req")
                                .putExpressionValue(":req", AttributeValue.builder().s("req-val").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
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
    // PRIORITIZE_HIGHER_SOURCE — completely disjoint sources
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_allSourcesDisjointPaths_keepsEveryAction() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("pojoAttr", AttributeValue.builder().s("1").build());

        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("extAttr")
                                .value(":extAttrValue")
                                .putExpressionValue(":extAttrValue", AttributeValue.builder().s("2").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("reqAttr")
                                .value(":reqAttrValue")
                                .putExpressionValue(":reqAttrValue", AttributeValue.builder().s("3").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("#AMZN_MAPPED_pojoAttr")
                     .value(":AMZN_MAPPED_pojoAttr")
                     .putExpressionName("#AMZN_MAPPED_pojoAttr", "pojoAttr")
                     .putExpressionValue(":AMZN_MAPPED_pojoAttr", AttributeValue.builder().s("1").build())
                     .build(),
            SetAction.builder()
                     .path("extAttr")
                     .value(":extAttrValue")
                     .putExpressionValue(":extAttrValue", AttributeValue.builder().s("2").build())
                     .build(),
            SetAction.builder()
                     .path("reqAttr")
                     .value(":reqAttrValue")
                     .putExpressionValue(":reqAttrValue", AttributeValue.builder().s("3").build())
                     .build());
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE — POJO REMOVE vs request SET
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_pojoRemoveVsRequestSet_keepsRequestSetOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("attr", AttributeValue.builder().nul(true).build());

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("attr")
                                .value(":val")
                                .putExpressionValue(":val", AttributeValue.builder().s("new-value").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .nonKeyAttributes(itemMap)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactly(
            SetAction.builder()
                     .path("attr")
                     .value(":val")
                     .putExpressionValue(":val", AttributeValue.builder().s("new-value").build())
                     .build());
        assertThat(result.removeActions()).isEmpty();
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE — three-source exact same path
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_threeSourcesSamePath_keepsRequestOnly() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("counter", AttributeValue.builder().n("1").build());

        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("counter")
                                .value(":ext")
                                .putExpressionValue(":ext", AttributeValue.builder().n("2").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("counter")
                                .value(":req")
                                .putExpressionValue(":req", AttributeValue.builder().n("3").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
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
    // PRIORITIZE_HIGHER_SOURCE — expression attribute names (#list → list)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_expressionNamePlaceholder_groupsByResolvedTopLevelName() {
        UpdateExpression extensionExpression = UpdateExpression
            .builder()
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
                                                          .tableMetadata(TABLE_METADATA)
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
    // PRIORITIZE_HIGHER_SOURCE — multiple request actions on one top-level name
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_requestMultipleActionsOnSameTopLevelName_allKept() {
        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("list[2]")
                                .value(":ext")
                                .putExpressionValue(":ext", AttributeValue.builder().s("ext").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
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
                                                          .tableMetadata(TABLE_METADATA)
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
    // PRIORITIZE_HIGHER_SOURCE — extension vs request on different nested paths (same first segment)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_nestedPathsSameFirstSegment_keepsRequestOnly() {
        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("a.b")
                                .value(":b")
                                .putExpressionValue(":b", AttributeValue.builder().s("from-ext").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("a.c")
                                .value(":c")
                                .putExpressionValue(":c", AttributeValue.builder().s("from-req").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
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
    // PRIORITIZE_HIGHER_SOURCE — all sources null → returns null
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_allSourcesNull_returnsNull() {
        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();
        assertNull(result);
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE — request only (no extension, no POJO)
    // -------------------------------------------------------

    @Test
    public void resolve_prioritizeHigherSource_requestOnlyNoOtherSources_returnsRequestAsIs() {
        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("attr")
                                .value(":val")
                                .putExpressionValue(":val", AttributeValue.builder().s("v").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
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

    @Test
    public void resolve_prioritizeHigherSource_ownedAttributesButNoResolvedPathMatches_returnsNull() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("#missing")
                                                .value(":v")
                                                .putExpressionName("#other", "logicalAttr")
                                                .putExpressionValue(":v", AttributeValue.builder().s("value").build())
                                                .build())
                            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .extensionExpression(extensionExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertNull(result);
    }

    // -------------------------------------------------------
    // PRIORITIZE_HIGHER_SOURCE — DELETE action from extension vs SET from request on same attribute
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
                                                          .tableMetadata(TABLE_METADATA)
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
    // PRIORITIZE_HIGHER_SOURCE — extension ADD vs request SET on same attribute
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
                                                          .tableMetadata(TABLE_METADATA)
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
    // PRIORITIZE_HIGHER_SOURCE — POJO REMOVE vs extension SET (no request)
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
                                                          .tableMetadata(TABLE_METADATA)
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

    /*
     * LEGACY: concatenate all SETs in order: item → extension → request (6 actions).
     * attr1 appears in all three (3 SETs); attr2 in ext+req (2); attr3 only from pojo.
     */
    @Test
    public void resolve_legacy_multiSourceOverlap_concatenatesAllSetActions() {
        Map<String, AttributeValue> itemMap = new LinkedHashMap<>();
        itemMap.put("attr1", AttributeValue.builder().s("value1_pojo").build());
        itemMap.put("attr3", AttributeValue.builder().s("value3_pojo").build());

        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("attr1")
                                .value(":e1")
                                .putExpressionValue(":e1", AttributeValue.builder().s("value1_ext").build())
                                .build())
            .addAction(SetAction.builder()
                                .path("attr2")
                                .value(":e2")
                                .putExpressionValue(":e2", AttributeValue.builder().s("value2_ext").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("attr1")
                                .value(":r1")
                                .putExpressionValue(":r1", AttributeValue.builder().s("value1_req").build())
                                .build())
            .addAction(SetAction.builder()
                                .path("attr2")
                                .value(":r2")
                                .putExpressionValue(":r2", AttributeValue.builder().s("value2_req").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr1")
                     .value(":AMZN_MAPPED_attr1")
                     .putExpressionName("#AMZN_MAPPED_attr1", "attr1")
                     .putExpressionValue(":AMZN_MAPPED_attr1", AttributeValue.builder().s("value1_pojo").build())
                     .build(),
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr3")
                     .value(":AMZN_MAPPED_attr3")
                     .putExpressionName("#AMZN_MAPPED_attr3", "attr3")
                     .putExpressionValue(":AMZN_MAPPED_attr3", AttributeValue.builder().s("value3_pojo").build())
                     .build(),
            SetAction.builder()
                     .path("attr1")
                     .value(":e1")
                     .putExpressionValue(":e1", AttributeValue.builder().s("value1_ext").build())
                     .build(),
            SetAction.builder()
                     .path("attr2")
                     .value(":e2")
                     .putExpressionValue(":e2", AttributeValue.builder().s("value2_ext").build())
                     .build(),
            SetAction.builder()
                     .path("attr1")
                     .value(":r1")
                     .putExpressionValue(":r1", AttributeValue.builder().s("value1_req").build())
                     .build(),
            SetAction.builder()
                     .path("attr2")
                     .value(":r2")
                     .putExpressionValue(":r2", AttributeValue.builder().s("value2_req").build())
                     .build());
    }

    /*
     * PRIORITIZE_HIGHER_SOURCE: one SET per name; request beats extension beats item.
     * Same data as the LEGACY test above (3 actions).
     * Winners: attr1 & attr2 → request; attr3 → item only.
     */
    @Test
    public void resolve_prioritizeHigherSource_multiSourceOverlap_oneSetActionPerTopLevelName() {
        Map<String, AttributeValue> itemMap = new LinkedHashMap<>();
        itemMap.put("attr1", AttributeValue.builder().s("value1_pojo").build());
        itemMap.put("attr3", AttributeValue.builder().s("value3_pojo").build());

        UpdateExpression extensionExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("attr1")
                                .value(":e1")
                                .putExpressionValue(":e1", AttributeValue.builder().s("value1_ext").build())
                                .build())
            .addAction(SetAction.builder()
                                .path("attr2")
                                .value(":e2")
                                .putExpressionValue(":e2", AttributeValue.builder().s("value2_ext").build())
                                .build())
            .build();

        UpdateExpression requestExpression = UpdateExpression
            .builder()
            .addAction(SetAction.builder()
                                .path("attr1")
                                .value(":r1")
                                .putExpressionValue(":r1", AttributeValue.builder().s("value1_req").build())
                                .build())
            .addAction(SetAction.builder()
                                .path("attr2")
                                .value(":r2")
                                .putExpressionValue(":r2", AttributeValue.builder().s("value2_req").build())
                                .build())
            .build();

        UpdateExpression result = UpdateExpressionResolver.builder()
                                                          .tableMetadata(TABLE_METADATA)
                                                          .nonKeyAttributes(itemMap)
                                                          .extensionExpression(extensionExpression)
                                                          .requestExpression(requestExpression)
                                                          .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE)
                                                          .build()
                                                          .resolve();

        assertThat(result.setActions()).containsExactlyInAnyOrder(
            SetAction.builder()
                     .path("attr1")
                     .value(":r1")
                     .putExpressionValue(":r1", AttributeValue.builder().s("value1_req").build())
                     .build(),
            SetAction.builder()
                     .path("attr2")
                     .value(":r2")
                     .putExpressionValue(":r2", AttributeValue.builder().s("value2_req").build())
                     .build(),
            SetAction.builder()
                     .path("#AMZN_MAPPED_attr3")
                     .value(":AMZN_MAPPED_attr3")
                     .putExpressionName("#AMZN_MAPPED_attr3", "attr3")
                     .putExpressionValue(":AMZN_MAPPED_attr3", AttributeValue.builder().s("value3_pojo").build())
                     .build());
    }
}