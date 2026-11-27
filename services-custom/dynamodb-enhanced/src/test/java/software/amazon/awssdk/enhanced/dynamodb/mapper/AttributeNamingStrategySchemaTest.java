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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.nullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingCases.CountingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingSchemaBeans.AnnotatedBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingSchemaBeans.CustomerBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingSchemaBeans.CustomerImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingSchemaBeans.NullableBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingSchemaBeans.StarAnnotatedBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingSchemaBeans.StaticCustomer;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingSchemaBeans.StaticImmutableCustomer;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingSchemaBeans.TwoMapBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Checks stored names for each schema type.
 * An annotated name stays as written. A static name is the strategy input.
 */
class AttributeNamingStrategySchemaTest {

    /**
     * Returns the omitted strategy, the built-in strategies, and the custom strategy.
     */
    static Stream<Arguments> schemaStrategies() {
        return AttributeNamingCases.schemaStrategies();
    }

    /**
     * Returns the four built-in strategies.
     */
    static Stream<Arguments> builtInOnly() {
        return AttributeNamingCases.builtInOnly();
    }

    @ParameterizedTest
    @MethodSource("schemaStrategies")
    void fromBean_whenStrategySupplied_storesStrategyNameAndKeepsAnnotation(String label,
                                                                            AttributeNamingStrategy strategy) {
        TableSchema<CustomerBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(CustomerBean.class)
                                                                                     .attributeNamingStrategy(strategy)
                                                                                     .build());
        CustomerBean item = new CustomerBean();
        item.setId("key");
        item.setCustomerId("c-1");

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);

        assertThat(stored).containsEntry("PK", stringValue("key"));
        assertThat(stored).containsEntry(AttributeNamingCases.storedName(strategy, "customerId"), stringValue("c-1"));
        assertThat(schema.attributeNames()).contains("PK", AttributeNamingCases.storedName(strategy, "customerId"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("schemaStrategies")
    void fromImmutableClass_whenStrategySupplied_storesStrategyNameAndKeepsAnnotation(String label,
                                                                                      AttributeNamingStrategy strategy) {
        TableSchema<CustomerImmutable> schema = TableSchema.fromImmutableClass(
            ImmutableTableSchemaParams.builder(CustomerImmutable.class)
                                      .attributeNamingStrategy(strategy)
                                      .build());
        CustomerImmutable item = CustomerImmutable.builder().id("key").customerId("c-1").build();
        Map<String, AttributeValue> stored = schema.itemToMap(item, true);

        assertThat(stored).containsEntry("PK", stringValue("key"));
        assertThat(stored).containsEntry(AttributeNamingCases.storedName(strategy, "customerId"), stringValue("c-1"));
        assertThat(label).isNotBlank();
    }

    @Test
    void fromBean_whenClassOnly_keepsJavaNames() {
        TableSchema<CustomerBean> schema = TableSchema.fromBean(CustomerBean.class);
        assertThat(schema.attributeNames()).containsExactlyInAnyOrder("PK", "customerId");
    }

    @Test
    void fromImmutableClass_whenClassOnly_keepsJavaNames() {
        TableSchema<CustomerImmutable> schema = TableSchema.fromImmutableClass(CustomerImmutable.class);
        assertThat(schema.attributeNames()).containsExactlyInAnyOrder("PK", "customerId");
    }

    @Test
    void fromClass_whenBeanOrImmutable_keepsJavaNames() {
        assertThat(TableSchema.fromClass(CustomerBean.class).attributeNames())
            .containsExactlyInAnyOrder("PK", "customerId");
        assertThat(TableSchema.fromClass(CustomerImmutable.class).attributeNames())
            .containsExactlyInAnyOrder("PK", "customerId");
    }

    @ParameterizedTest
    @MethodSource("schemaStrategies")
    void staticTableSchema_whenStrategySupplied_transformsEveryName(String label, AttributeNamingStrategy strategy) {
        TableSchema<StaticCustomer> schema = staticCustomerSchema(strategy);
        StaticCustomer item = new StaticCustomer();
        item.setId("key");
        item.setCustomerId("c-1");

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);
        String keyName = AttributeNamingCases.storedName(strategy, "PK");
        String customerName = AttributeNamingCases.storedName(strategy, "customerId");

        assertThat(stored).containsEntry(keyName, stringValue("key"));
        assertThat(stored).containsEntry(customerName, stringValue("c-1"));
        assertThat(schema.tableMetadata().primaryPartitionKey()).isEqualTo(keyName);
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("schemaStrategies")
    void staticImmutableTableSchema_whenStrategySupplied_transformsEveryName(String label,
                                                                             AttributeNamingStrategy strategy) {
        TableSchema<StaticImmutableCustomer> schema = staticImmutableCustomerSchema(strategy);
        StaticImmutableCustomer item = StaticImmutableCustomer.builder().id("key").customerId("c-1").build();
        String keyName = AttributeNamingCases.storedName(strategy, "PK");

        assertThat(schema.itemToMap(item, true)).containsEntry(keyName, stringValue("key"));
        assertThat(schema.itemToMap(item, true))
            .containsEntry(AttributeNamingCases.storedName(strategy, "customerId"), stringValue("c-1"));
        assertThat(label).isNotBlank();
    }

    @Test
    void documentSchemaBuilder_whenCallerSuppliesKeys_keepsThoseKeys() {
        TableSchema<?> schema = TableSchema.documentSchemaBuilder()
                                           .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                 "customerId",
                                                                 AttributeValueType.S)
                                           .addIndexSortKey(TableMetadata.primaryIndexName(),
                                                            "orderId",
                                                            AttributeValueType.S)
                                           .build();

        assertThat(schema.tableMetadata().primaryPartitionKey()).isEqualTo("customerId");
        assertThat(schema.tableMetadata().primarySortKey()).contains("orderId");
    }

    @ParameterizedTest
    @MethodSource("builtInOnly")
    void staticTableSchema_whenSourceHasNoWord_failsConstruction(String label, AttributeNamingStrategy strategy) {
        assertThatThrownBy(() -> StaticTableSchema.builder(StaticCustomer.class)
                                                   .newItemSupplier(StaticCustomer::new)
                                                   .attributeNamingStrategy(strategy)
                                                   .addAttribute(String.class, attribute -> attribute.name("___")
                                                                                                    .getter(StaticCustomer::getId)
                                                                                                    .setter(StaticCustomer::setId))
                                                   .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("___");
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("builtInOnly")
    void staticTableSchema_whenResolvedNamesCollide_failsConstruction(String label, AttributeNamingStrategy strategy) {
        String resolved = strategy.transform("customerId");
        assertThatThrownBy(() -> StaticTableSchema.builder(StaticCustomer.class)
                                                   .newItemSupplier(StaticCustomer::new)
                                                   .attributeNamingStrategy(strategy)
                                                   .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                                    .getter(StaticCustomer::getCustomerId)
                                                                                                    .setter(StaticCustomer::setCustomerId))
                                                   .addAttribute(String.class, attribute -> attribute.name("customer_id")
                                                                                                    .getter(StaticCustomer::getCustomerCode)
                                                                                                    .setter(StaticCustomer::setCustomerCode))
                                                   .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(resolved);
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("schemaStrategies")
    void fromBean_whenTwoFlattenedMaps_failsConstruction(String label, AttributeNamingStrategy strategy) {
        assertThatThrownBy(() -> TableSchema.fromBean(BeanTableSchemaParams.builder(TwoMapBean.class)
                                                                           .attributeNamingStrategy(strategy)
                                                                           .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only one flattened map");
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("schemaStrategies")
    void fromBean_whenValuesAreNull_usesResolvedNamesAndExistingNullPolicy(String label,
                                                                           AttributeNamingStrategy strategy) {
        TableSchema<NullableBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(NullableBean.class)
                                                                                     .attributeNamingStrategy(strategy)
                                                                                     .build());
        NullableBean item = new NullableBean();
        item.setId("key");
        item.setNicknames(Arrays.asList("ada", null));
        Map<String, String> labels = new HashMap<>();
        labels.put("city", null);
        item.setLabels(labels);

        String customerName = AttributeNamingCases.storedName(strategy, "customerId");
        String nicknameName = AttributeNamingCases.storedName(strategy, "nicknames");
        String labelName = AttributeNamingCases.storedName(strategy, "labels");

        Map<String, AttributeValue> withNulls = schema.itemToMap(item, false);
        assertThat(withNulls).containsEntry(customerName, nullAttributeValue());
        assertThat(withNulls.get(nicknameName).l()).contains(nullAttributeValue());
        assertThat(withNulls.get(labelName).m()).containsEntry("city", nullAttributeValue());

        Map<String, AttributeValue> ignoredNulls = schema.itemToMap(item, true);
        assertThat(ignoredNulls).doesNotContainKey(customerName);
        assertThat(ignoredNulls.get(nicknameName).l()).contains(nullAttributeValue());
        assertThat(ignoredNulls.get(labelName).m()).containsEntry("city", nullAttributeValue());

        NullableBean restored = schema.mapToItem(withNulls);
        assertThat(restored.getCustomerId()).isNull();
        assertThat(restored.getNicknames()).containsExactly("ada", null);
        assertThat(restored.getLabels()).containsEntry("city", null);
        assertThat(label).isNotBlank();
    }

    @Test
    void fromBean_whenStrategySchemaIsBuilt_classCacheKeepsTheSchemaWithoutStrategy() {
        TableSchema<CustomerBean> cached = TableSchema.fromBean(CustomerBean.class);
        TableSchema<CustomerBean> withStrategy = TableSchema.fromBean(
            BeanTableSchemaParams.builder(CustomerBean.class)
                                 .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                 .build());
        TableSchema<CustomerBean> cachedAgain = TableSchema.fromBean(CustomerBean.class);
        TableSchema<CustomerBean> secondStrategy = TableSchema.fromBean(
            BeanTableSchemaParams.builder(CustomerBean.class)
                                 .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                 .build());

        assertThat(cachedAgain).isSameAs(cached);
        assertThat(withStrategy).isNotSameAs(cached);
        assertThat(secondStrategy).isNotSameAs(withStrategy);
        assertThat(cached.attributeNames()).contains("customerId");
        assertThat(withStrategy.attributeNames()).contains("customer_id").doesNotContain("customerId");
        assertThat(TableSchema.fromBean(BeanTableSchemaParams.builder(CustomerBean.class)
                                                             .attributeNamingStrategy(AttributeNamingStrategies.UPPER_SNAKE_CASE)
                                                             .build())
                              .attributeNames()).contains("CUSTOMER_ID");
    }

    @Test
    void fromBean_whenCustomStrategy_receivesSourceOnceAndDoesNotSplitResult() {
        CountingStrategy strategy = new CountingStrategy(source -> "HTTPRequest");
        TableSchema<CustomerBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(CustomerBean.class)
                                                                                     .attributeNamingStrategy(strategy)
                                                                                     .build());
        CustomerBean item = new CustomerBean();
        item.setId("key");
        item.setCustomerId("c-1");

        int callsAfterConstruction = strategy.calls();
        Map<String, AttributeValue> stored = schema.itemToMap(item, true);

        assertThat(strategy.sources()).containsExactly("customerId");
        assertThat(stored).containsEntry("HTTPRequest", stringValue("c-1"));
        assertThat(stored).doesNotContainKey("http_request");
        assertThat(strategy.calls()).isEqualTo(callsAfterConstruction);
    }

    @Test
    void staticTableSchema_whenCustomStrategyThrows_constructionStopsWithThatException() {
        AttributeNamingStrategy strategy = source -> {
            throw new IllegalStateException("naming failed");
        };

        assertThatThrownBy(() -> staticCustomerSchema(strategy))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("naming failed");
    }

    @Test
    void staticTableSchema_whenCustomStrategyReturnsNull_constructionFails() {
        assertThatThrownBy(() -> staticCustomerSchema(source -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void staticTableSchema_whenCustomStrategyReturnsDuplicate_constructionFails() {
        AttributeNamingStrategy strategy = source -> "same";

        assertThatThrownBy(() -> StaticTableSchema.builder(StaticCustomer.class)
                                                   .newItemSupplier(StaticCustomer::new)
                                                   .attributeNamingStrategy(strategy)
                                                   .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                                    .getter(StaticCustomer::getCustomerId)
                                                                                                    .setter(StaticCustomer::setCustomerId))
                                                   .addAttribute(String.class, attribute -> attribute.name("orderId")
                                                                                                    .getter(StaticCustomer::getCustomerCode)
                                                                                                    .setter(StaticCustomer::setCustomerCode))
                                                   .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("same");
    }

    @ParameterizedTest
    @MethodSource("builtInOnly")
    void fromBean_whenAttributeAnnotated_keepsWrittenName(String label, AttributeNamingStrategy strategy) {
        TableSchema<AnnotatedBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(AnnotatedBean.class)
                                                                                      .attributeNamingStrategy(strategy)
                                                                                      .build());

        assertThat(schema.attributeNames()).contains("___", "HTTPRequest");
        assertThat(schema.attributeNames()).contains(strategy.transform("customerId"));
        assertThat(schema.attributeNames()).doesNotContain(strategy.transform("HTTPRequest"));
        assertThat(label).isNotBlank();
    }

    @Test
    void fromBean_whenAnnotatedStar_keepsStar() {
        TableSchema<StarAnnotatedBean> schema = TableSchema.fromBean(
            BeanTableSchemaParams.builder(StarAnnotatedBean.class)
                                 .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                 .build());

        assertThat(schema.attributeNames()).contains("*");
    }

    @Test
    void staticTableSchema_whenStrategyReturnsStar_acceptsThatName() {
        TableSchema<StaticCustomer> schema = StaticTableSchema.builder(StaticCustomer.class)
                                                              .newItemSupplier(StaticCustomer::new)
                                                              .attributeNamingStrategy(source -> "*")
                                                              .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                                               .getter(StaticCustomer::getCustomerId)
                                                                                                               .setter(StaticCustomer::setCustomerId)
                                                                                                               .tags(primaryPartitionKey()))
                                                              .build();

        assertThat(schema.attributeNames()).contains("*");
    }

    @Test
    void staticTableSchema_whenResolvedNameExceeds255Characters_constructionSucceeds() {
        String source = String.join("", Collections.nCopies(256, "a"));
        TableSchema<StaticCustomer> schema = StaticTableSchema.builder(StaticCustomer.class)
                                                              .newItemSupplier(StaticCustomer::new)
                                                              .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                                              .addAttribute(String.class, attribute -> attribute.name(source)
                                                                                                               .getter(StaticCustomer::getCustomerId)
                                                                                                               .setter(StaticCustomer::setCustomerId)
                                                                                                               .tags(primaryPartitionKey()))
                                                              .build();

        assertThat(schema.attributeNames()).containsExactly(source);
    }

    @Test
    void mapToItem_whenKeysAreJavaNames_leavesRenamedPropertyEmpty() {
        TableSchema<CustomerBean> schema = TableSchema.fromBean(
            BeanTableSchemaParams.builder(CustomerBean.class)
                                 .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                 .build());
        Map<String, AttributeValue> javaNames = new HashMap<>();
        javaNames.put("PK", stringValue("key"));
        javaNames.put("customerId", stringValue("c-1"));

        CustomerBean missed = schema.mapToItem(javaNames);
        assertThat(missed.getId()).isEqualTo("key");
        assertThat(missed.getCustomerId()).isNull();

        Map<String, AttributeValue> storedNames = new HashMap<>();
        storedNames.put("PK", stringValue("key"));
        storedNames.put("customer_id", stringValue("c-1"));
        assertThat(schema.mapToItem(storedNames).getCustomerId()).isEqualTo("c-1");
    }

    @Test
    void staticTableSchema_whenUpperCamelTransformsA_B_keepsFirstResult() {
        TableSchema<StaticCustomer> schema = StaticTableSchema.builder(StaticCustomer.class)
                                                              .newItemSupplier(StaticCustomer::new)
                                                              .attributeNamingStrategy(AttributeNamingStrategies.UPPER_CAMEL_CASE)
                                                              .addAttribute(String.class, attribute -> attribute.name("A_B")
                                                                                                               .getter(StaticCustomer::getCustomerId)
                                                                                                               .setter(StaticCustomer::setCustomerId)
                                                                                                               .tags(primaryPartitionKey()))
                                                              .build();

        assertThat(schema.attributeNames()).containsExactly("AB");
    }

    /**
     * Returns a static schema that transforms {@code PK} and {@code customerId}.
     */
    private static TableSchema<StaticCustomer> staticCustomerSchema(AttributeNamingStrategy strategy) {
        return StaticTableSchema.builder(StaticCustomer.class)
                                .newItemSupplier(StaticCustomer::new)
                                .attributeNamingStrategy(strategy)
                                .addAttribute(String.class, attribute -> attribute.name("PK")
                                                                                 .getter(StaticCustomer::getId)
                                                                                 .setter(StaticCustomer::setId)
                                                                                 .tags(primaryPartitionKey()))
                                .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                 .getter(StaticCustomer::getCustomerId)
                                                                                 .setter(StaticCustomer::setCustomerId))
                                .build();
    }

    /**
     * Returns a static immutable schema that transforms {@code PK} and {@code customerId}.
     */
    private static TableSchema<StaticImmutableCustomer> staticImmutableCustomerSchema(AttributeNamingStrategy strategy) {
        return StaticImmutableTableSchema.builder(StaticImmutableCustomer.class, StaticImmutableCustomer.Builder.class)
                                         .newItemBuilder(StaticImmutableCustomer::builder, StaticImmutableCustomer.Builder::build)
                                         .attributeNamingStrategy(strategy)
                                         .addAttribute(String.class, attribute -> attribute.name("PK")
                                                                                          .getter(StaticImmutableCustomer::id)
                                                                                          .setter(StaticImmutableCustomer.Builder::id)
                                                                                          .tags(primaryPartitionKey()))
                                         .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                          .getter(StaticImmutableCustomer::customerId)
                                                                                          .setter(StaticImmutableCustomer.Builder::customerId))
                                         .build();
    }
}
