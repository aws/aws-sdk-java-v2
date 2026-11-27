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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingCases.CountingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.ConvertedBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.ConvertedChild;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.ConvertedImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.ConvertedParent;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.ProviderBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.StaticConverted;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.Token;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.TokenConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.NamingConverterBeans.TokenMap;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Checks that a converter owns the stored value and the strategy owns the attribute name.
 */
class AttributeNamingStrategyConverterTest {

    /**
     * Returns the omitted strategy, the built-in strategies, and the custom strategy.
     */
    static Stream<Arguments> strategies() {
        return AttributeNamingCases.schemaStrategies();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromBean_whenConverted_storesStrategyNameAndConverterValue(String label, AttributeNamingStrategy strategy) {
        TableSchema<ConvertedBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(ConvertedBean.class)
                                                                                      .attributeNamingStrategy(strategy)
                                                                                      .build());
        ConvertedBean item = new ConvertedBean();
        item.setId(new Token("key"));
        item.setCustomerCode(new Token("c-1"));
        item.setPayload(new TokenMap("map"));

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);
        String codeName = AttributeNamingCases.storedName(strategy, "customerCode");

        assertThat(stored).containsEntry("PK", stringValue(NamingConverterBeans.TOKEN_PREFIX + "key"));
        assertThat(stored).containsEntry(codeName, stringValue(NamingConverterBeans.TOKEN_PREFIX + "c-1"));
        assertThat(stored.get(AttributeNamingCases.storedName(strategy, "payload")).m())
            .containsEntry("innerKey", stringValue("map"));
        assertThat(schema.mapToItem(stored).getCustomerCode()).isEqualTo(new Token("c-1"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromImmutableClass_whenConverted_storesStrategyNameAndConverterValue(String label,
                                                                              AttributeNamingStrategy strategy) {
        TableSchema<ConvertedImmutable> schema = TableSchema.fromImmutableClass(
            ImmutableTableSchemaParams.builder(ConvertedImmutable.class)
                                      .attributeNamingStrategy(strategy)
                                      .build());
        ConvertedImmutable item = ConvertedImmutable.builder().id("key").customerCode(new Token("c-1")).build();
        String codeName = AttributeNamingCases.storedName(strategy, "customerCode");

        assertThat(schema.itemToMap(item, true)).containsEntry(codeName, stringValue(NamingConverterBeans.TOKEN_PREFIX + "c-1"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void staticTableSchema_whenConverted_nameIsStrategyInputAndConverterOwnsValue(String label,
                                                                                  AttributeNamingStrategy strategy) {
        CountingStrategy counting = new CountingStrategy(strategy == null ? source -> source : strategy);
        TableSchema<StaticConverted> schema = staticSchema(counting);
        StaticConverted item = new StaticConverted();
        item.setId("key");
        item.setCustomerCode(new Token("c-1"));

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);
        String codeName = AttributeNamingCases.storedName(strategy, "customerCode");

        assertThat(stored).containsEntry(codeName, stringValue(NamingConverterBeans.TOKEN_PREFIX + "c-1"));
        assertThat(counting.sources()).contains("customerCode");
        assertThat(counting.sources()).doesNotContain("innerKey");
        assertThat(schema.mapToItem(stored).getCustomerCode()).isEqualTo(new Token("c-1"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void staticImmutableTableSchema_whenConverted_nameIsStrategyInput(String label, AttributeNamingStrategy strategy) {
        TableSchema<ConvertedImmutable> schema = StaticImmutableTableSchema
            .builder(ConvertedImmutable.class, ConvertedImmutable.Builder.class)
            .newItemBuilder(ConvertedImmutable::builder, ConvertedImmutable.Builder::build)
            .attributeNamingStrategy(strategy)
            .addAttribute(String.class, attribute -> attribute.name("id")
                                                             .getter(ConvertedImmutable::id)
                                                             .setter(ConvertedImmutable.Builder::id)
                                                             .tags(primaryPartitionKey()))
            .addAttribute(EnhancedType.of(Token.class), attribute -> attribute.name("customerCode")
                                                                             .getter(ConvertedImmutable::customerCode)
                                                                             .setter(ConvertedImmutable.Builder::customerCode)
                                                                             .attributeConverter(new TokenConverter()))
            .build();
        ConvertedImmutable item = ConvertedImmutable.builder().id("key").customerCode(new Token("c-1")).build();

        assertThat(schema.itemToMap(item, true))
            .containsEntry(AttributeNamingCases.storedName(strategy, "customerCode"),
                           stringValue(NamingConverterBeans.TOKEN_PREFIX + "c-1"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromBean_whenProviderSuppliesConverter_strategyStillRenamesAttribute(String label,
                                                                              AttributeNamingStrategy strategy) {
        TableSchema<ProviderBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(ProviderBean.class)
                                                                                     .attributeNamingStrategy(strategy)
                                                                                     .build());
        ProviderBean item = new ProviderBean();
        item.setId("key");
        item.setCustomerCode(new Token("c-1"));

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);

        assertThat(stored).containsEntry(AttributeNamingCases.storedName(strategy, "customerCode"),
                                         stringValue(NamingConverterBeans.TOKEN_PREFIX + "c-1"));
        assertThat(schema.mapToItem(stored).getCustomerCode()).isEqualTo(new Token("c-1"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void staticTableSchema_whenListUsesElementConverter_listNameIsResolved(String label,
                                                                           AttributeNamingStrategy strategy) {
        TableSchema<StaticConverted> schema = StaticTableSchema.builder(StaticConverted.class)
            .newItemSupplier(StaticConverted::new)
            .attributeNamingStrategy(strategy)
            .addAttribute(String.class, attribute -> attribute.name("id")
                                                             .getter(StaticConverted::getId)
                                                             .setter(StaticConverted::setId)
                                                             .tags(primaryPartitionKey()))
            .addAttribute(EnhancedType.listOf(Token.class), attribute -> attribute.name("tokens")
                .getter(StaticConverted::getTokens)
                .setter(StaticConverted::setTokens)
                .attributeConverter(ListAttributeConverter.create(new TokenConverter())))
            .build();
        StaticConverted item = new StaticConverted();
        item.setId("key");
        item.setTokens(Arrays.asList(new Token("a"), new Token("b")));

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);
        String tokenName = AttributeNamingCases.storedName(strategy, "tokens");

        assertThat(stored.get(tokenName).l()).containsExactly(stringValue(NamingConverterBeans.TOKEN_PREFIX + "a"),
                                                              stringValue(NamingConverterBeans.TOKEN_PREFIX + "b"));
        assertThat(schema.mapToItem(stored).getTokens()).containsExactly(new Token("a"), new Token("b"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromBean_whenDiscoveredChildHasConverter_childInheritsStrategy(String label,
                                                                        AttributeNamingStrategy strategy) {
        TableSchema<ConvertedParent> schema = TableSchema.fromBean(
            BeanTableSchemaParams.builder(ConvertedParent.class)
                                 .attributeNamingStrategy(strategy)
                                 .build());
        ConvertedChild child = new ConvertedChild();
        child.setCustomerCode(new Token("c-1"));
        ConvertedParent item = new ConvertedParent();
        item.setId("key");
        item.setChild(child);

        Map<String, AttributeValue> stored = schema.itemToMap(item, true);
        String childName = AttributeNamingCases.storedName(strategy, "child");
        String codeName = AttributeNamingCases.storedName(strategy, "customerCode");

        assertThat(stored.get(childName).m()).containsEntry(codeName, stringValue(NamingConverterBeans.TOKEN_PREFIX + "c-1"));
        assertThat(label).isNotBlank();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void fromBean_whenConvertedAttributeIsNull_usesResolvedName(String label, AttributeNamingStrategy strategy) {
        TableSchema<ConvertedBean> schema = TableSchema.fromBean(BeanTableSchemaParams.builder(ConvertedBean.class)
                                                                                      .attributeNamingStrategy(strategy)
                                                                                      .build());
        ConvertedBean item = new ConvertedBean();
        item.setId(new Token("key"));
        String codeName = AttributeNamingCases.storedName(strategy, "customerCode");

        assertThat(schema.itemToMap(item, false)).containsEntry(codeName, nullAttributeValue());
        assertThat(schema.itemToMap(item, true)).doesNotContainKey(codeName);
        assertThat(label).isNotBlank();
    }

    /**
     * Returns a static schema whose converted attribute name is the strategy input.
     */
    private static TableSchema<StaticConverted> staticSchema(AttributeNamingStrategy strategy) {
        return StaticTableSchema.builder(StaticConverted.class)
                                .newItemSupplier(StaticConverted::new)
                                .attributeNamingStrategy(strategy)
                                .addAttribute(String.class, attribute -> attribute.name("id")
                                                                                 .getter(StaticConverted::getId)
                                                                                 .setter(StaticConverted::setId)
                                                                                 .tags(primaryPartitionKey()))
                                .addAttribute(EnhancedType.of(Token.class),
                                              attribute -> attribute.name("customerCode")
                                                                   .getter(StaticConverted::getCustomerCode)
                                                                   .setter(StaticConverted::setCustomerCode)
                                                                   .attributeConverter(new TokenConverter()))
                                .build();
    }
}
