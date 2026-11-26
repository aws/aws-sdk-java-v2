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
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.nullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.CityBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.CityImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.OuterCycle;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.ParentBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.ParentImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.RecursiveBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.RecursiveImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.SuppliedCity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingNestedBeans.SuppliedParent;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Checks nested and flattened names.
 * A discovered child inherits the parent strategy. A supplied schema keeps the names it already resolved.
 */
class AttributeNamingStrategyNestedFlattenTest {

    /**
     * Returns the four built-in strategies and the custom strategy.
     */
    static Stream<Arguments> strategies() {
        return AttributeNamingCases.builtInAndCustom();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromBean_whenChildIsDiscovered_childInheritsStrategy(String label, AttributeNamingStrategy strategy) {
        TableSchema<ParentBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(ParentBean.class)
                                                                                   .attributeNamingStrategy(strategy)
                                                                                   .build());
        CityBean city = city("Rome", "RM");
        ParentBean item = new ParentBean();
        item.setId("key");
        item.setHomeCity(city);
        item.setCityRecord(CityImmutable.builder().cityName("Paris").code("PR").build());
        item.setCityList(Collections.singletonList(city));
        item.setCityMap(Collections.singletonMap("rome", city));
        item.setFlatCity(city("Milan", "MI"));

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);
        String cityName = AttributeNamingCases.storedName(strategy, "cityName");

        assertThat(stored.get(AttributeNamingCases.storedName(strategy, "homeCity")).m())
            .containsEntry(cityName, stringValue("Rome"))
            .containsEntry("CITY", stringValue("RM"));
        assertThat(stored.get(AttributeNamingCases.storedName(strategy, "cityRecord")).m())
            .containsEntry(cityName, stringValue("Paris"))
            .containsEntry("CITY", stringValue("PR"));
        assertThat(stored.get(AttributeNamingCases.storedName(strategy, "cityList")).l().get(0).m())
            .containsEntry(cityName, stringValue("Rome"));
        assertThat(stored.get(AttributeNamingCases.storedName(strategy, "cityMap")).m().get("rome").m())
            .containsEntry(cityName, stringValue("Rome"));
        assertThat(stored).containsEntry(cityName, stringValue("Milan"));
        assertThat(stored).containsEntry("CITY", stringValue("MI"));
        assertThat(stored).doesNotContainKey(AttributeNamingCases.storedName(strategy, "flatCity"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromImmutableClass_whenChildIsDiscovered_childInheritsStrategy(String label,
                                                                        AttributeNamingStrategy strategy) {
        TableSchema<ParentImmutable> schema = TableSchema.fromImmutableClass(
            ImmutableTableSchemaParams.builder(ParentImmutable.class)
                                      .attributeNamingStrategy(strategy)
                                      .build());
        ParentImmutable item = ParentImmutable.builder()
                                              .id("key")
                                              .homeCity(city("Rome", "RM"))
                                              .flatCity(city("Milan", "MI"))
                                              .build();
        Map<String, AttributeValue> stored = schema.itemToMap(item, true);
        String cityName = AttributeNamingCases.storedName(strategy, "cityName");

        assertThat(stored.get(AttributeNamingCases.storedName(strategy, "homeCity")).m())
            .containsEntry(cityName, stringValue("Rome"))
            .containsEntry("CITY", stringValue("RM"));
        assertThat(stored).containsEntry(cityName, stringValue("Milan"));
        assertThat(stored).doesNotContainKey(AttributeNamingCases.storedName(strategy, "flatCity"));
        assertThat(label).isNotBlank();
    }

    @Test
    void fromBean_whenNestedChildIsNull_doesNotWriteAMapAndReadsNull() {
        TableSchema<ParentBean> schema = TableSchema.fromBean(
            BeanTableSchemaParams.builder(ParentBean.class)
                                 .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                 .build());
        ParentBean item = new ParentBean();
        item.setId("key");

        Map<String, AttributeValue> stored = schema.itemToMap(item, false);

        assertThat(stored).containsEntry("home_city", nullAttributeValue());
        assertThat(schema.mapToItem(stored).getHomeCity()).isNull();
        assertThat(schema.itemToMap(item, true)).doesNotContainKey("home_city");
    }

    @Test
    void staticTableSchema_whenSuppliedChildUsesAnotherStrategy_eachSchemaKeepsItsNames() {
        TableSchema<SuppliedCity> child = StaticTableSchema.builder(SuppliedCity.class)
                                                           .newItemSupplier(SuppliedCity::new)
                                                           .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                                           .addAttribute(String.class, attribute -> attribute.name("cityName")
                                                                                                            .getter(SuppliedCity::getCityName)
                                                                                                            .setter(SuppliedCity::setCityName))
                                                           .build();
        TableSchema<SuppliedParent> parent = StaticTableSchema.builder(SuppliedParent.class)
                                                              .newItemSupplier(SuppliedParent::new)
                                                              .attributeNamingStrategy(AttributeNamingStrategies.UPPER_SNAKE_CASE)
                                                              .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                                               .getter(SuppliedParent::getCustomerId)
                                                                                                               .setter(SuppliedParent::setCustomerId)
                                                                                                               .tags(primaryPartitionKey()))
                                                              .addAttribute(EnhancedType.documentOf(SuppliedCity.class, child),
                                                                            attribute -> attribute.name("cityRecord")
                                                                                                 .getter(SuppliedParent::getCityRecord)
                                                                                                 .setter(SuppliedParent::setCityRecord))
                                                              .flatten(child, SuppliedParent::getFlatCity, SuppliedParent::setFlatCity)
                                                              .build();
        SuppliedCity city = new SuppliedCity();
        city.setCityName("Rome");
        SuppliedParent item = new SuppliedParent();
        item.setCustomerId("c-1");
        item.setCityRecord(city);
        item.setFlatCity(city);

        Map<String, AttributeValue> stored = parent.itemToMap(item, true);

        assertThat(stored).containsEntry("CUSTOMER_ID", stringValue("c-1"));
        assertThat(stored).containsEntry("city_name", stringValue("Rome"));
        assertThat(stored.get("CITY_RECORD").m()).containsEntry("city_name", stringValue("Rome"));
        assertThat(stored).doesNotContainKey("CITY_NAME");
    }

    @Test
    void staticTableSchema_whenSuppliedChildHasNoStrategy_parentDoesNotTransformThoseNames() {
        TableSchema<SuppliedCity> child = StaticTableSchema.builder(SuppliedCity.class)
                                                           .newItemSupplier(SuppliedCity::new)
                                                           .addAttribute(String.class, attribute -> attribute.name("cityName")
                                                                                                            .getter(SuppliedCity::getCityName)
                                                                                                            .setter(SuppliedCity::setCityName))
                                                           .build();
        TableSchema<SuppliedParent> parent = StaticTableSchema.builder(SuppliedParent.class)
                                                              .newItemSupplier(SuppliedParent::new)
                                                              .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                                              .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                                               .getter(SuppliedParent::getCustomerId)
                                                                                                               .setter(SuppliedParent::setCustomerId)
                                                                                                               .tags(primaryPartitionKey()))
                                                              .flatten(child, SuppliedParent::getFlatCity, SuppliedParent::setFlatCity)
                                                              .build();
        SuppliedCity city = new SuppliedCity();
        city.setCityName("Rome");
        SuppliedParent item = new SuppliedParent();
        item.setCustomerId("c-1");
        item.setFlatCity(city);

        Map<String, AttributeValue> stored = parent.itemToMap(item, true);

        assertThat(stored).containsEntry("customer_id", stringValue("c-1"));
        assertThat(stored).containsEntry("cityName", stringValue("Rome"));
        assertThat(stored).doesNotContainKey("city_name");
    }

    @Test
    void staticTableSchema_whenExtended_keepsNamesResolvedByTheParentSchema() {
        TableSchema<SuppliedParent> base = StaticTableSchema.builder(SuppliedParent.class)
                                                            .newItemSupplier(SuppliedParent::new)
                                                            .addAttribute(String.class, attribute -> attribute.name("customerId")
                                                                                                             .getter(SuppliedParent::getCustomerId)
                                                                                                             .setter(SuppliedParent::setCustomerId)
                                                                                                             .tags(primaryPartitionKey()))
                                                            .build();
        TableSchema<SuppliedParent> extended = StaticTableSchema.builder(SuppliedParent.class)
                                                                .newItemSupplier(SuppliedParent::new)
                                                                .attributeNamingStrategy(AttributeNamingStrategies.LOWER_SNAKE_CASE)
                                                                .extend((StaticTableSchema<SuppliedParent>) base)
                                                                .addAttribute(String.class, attribute -> attribute.name("orderId")
                                                                                                                 .getter(SuppliedParent::getOrderId)
                                                                                                                 .setter(SuppliedParent::setOrderId))
                                                                .build();

        assertThat(extended.attributeNames()).contains("customerId", "order_id");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromBean_whenTypeContainsItself_constructionFinishes(String label, AttributeNamingStrategy strategy) {
        TableSchema<RecursiveBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(RecursiveBean.class)
                                                                                      .attributeNamingStrategy(strategy)
                                                                                      .build());
        RecursiveBean child = new RecursiveBean();
        child.setId("child");
        child.setCityName("Paris");
        RecursiveBean item = new RecursiveBean();
        item.setId("parent");
        item.setCityName("Rome");
        item.setChild(child);
        item.setChildren(Collections.singletonList(child));

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);
        String cityName = AttributeNamingCases.storedName(strategy, "cityName");

        assertThat(stored).containsEntry(cityName, stringValue("Rome"));
        assertThat(stored.get(AttributeNamingCases.storedName(strategy, "child")).m())
            .containsEntry(cityName, stringValue("Paris"));
        assertThat(stored.get(AttributeNamingCases.storedName(strategy, "children")).l().get(0).m())
            .containsEntry(cityName, stringValue("Paris"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromImmutableClass_whenTypeContainsItself_constructionFinishes(String label,
                                                                        AttributeNamingStrategy strategy) {
        TableSchema<RecursiveImmutable> schema = TableSchema.fromImmutableClass(
            ImmutableTableSchemaParams.builder(RecursiveImmutable.class)
                                      .attributeNamingStrategy(strategy)
                                      .build());
        RecursiveImmutable child = RecursiveImmutable.builder().id("child").cityName("Paris").build();
        RecursiveImmutable item = RecursiveImmutable.builder().id("parent").cityName("Rome").child(child).build();
        String cityName = AttributeNamingCases.storedName(strategy, "cityName");

        assertThat(schema.itemToMap(item, true).get(AttributeNamingCases.storedName(strategy, "child")).m())
            .containsEntry(cityName, stringValue("Paris"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromBean_whenCycleCrossesAnotherBean_constructionFinishes(String label, AttributeNamingStrategy strategy) {
        TableSchema<OuterCycle> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(OuterCycle.class)
                                                                                   .attributeNamingStrategy(strategy)
                                                                                   .build());

        assertThat(schema.attributeNames()).contains(AttributeNamingCases.storedName(strategy, "inner"));
        assertThat(label).isNotBlank();
    }

    /**
     * Returns a city bean with the given values.
     */
    private static CityBean city(String cityName, String code) {
        CityBean city = new CityBean();
        city.setCityName(cityName);
        city.setCode(code);
        return city;
    }
}
