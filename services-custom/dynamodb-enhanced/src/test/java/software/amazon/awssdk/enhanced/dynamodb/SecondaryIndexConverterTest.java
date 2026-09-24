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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.IntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SdkBytesAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Tests converter validation for global and local secondary index key attributes.
 * <p>
 * The tests accept scalar string, number, and binary converter types from default providers, custom providers, and
 * attribute converters. They reject nonscalar converter types and unsupported attributes during schema construction,
 * while also verifying that custom conversion can make an otherwise unsupported key type scalar.
 */
public class SecondaryIndexConverterTest {

    @Test
    @DisplayName("A built-in S converter is valid for a GSI partition key")
    void build_whenStringGsiPartitionKey_selectsStringConverterWithTypeS() {
        StaticTableSchema<StringGsiItem> schema = stringGsiSchema();

        assertThat(schema.converterForAttribute("gsiPk")).isInstanceOf(StringAttributeConverter.class);
        assertThat(schema.tableMetadata().scalarAttributeType("gsiPk")).contains(ScalarAttributeType.S);
    }

    @Test
    @DisplayName("A built-in N converter is valid for a GSI sort key")
    void build_whenIntegerGsiSortKey_selectsIntegerConverterWithTypeN() {
        StaticTableSchema<IntegerGsiSortItem> schema =
            StaticTableSchema.builder(IntegerGsiSortItem.class)
                             .newItemSupplier(IntegerGsiSortItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(IntegerGsiSortItem::getPk)
                                                               .setter(IntegerGsiSortItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(String.class, a -> a.name("gsiPk")
                                                               .getter(IntegerGsiSortItem::getGsiPk)
                                                               .setter(IntegerGsiSortItem::setGsiPk)
                                                               .tags(secondaryPartitionKey("gsi")))
                             .addAttribute(Integer.class, a -> a.name("gsiSk")
                                                                .getter(IntegerGsiSortItem::getGsiSk)
                                                                .setter(IntegerGsiSortItem::setGsiSk)
                                                                .tags(secondarySortKey("gsi")))
                             .build();

        assertThat(schema.converterForAttribute("gsiSk")).isInstanceOf(IntegerAttributeConverter.class);
        assertThat(schema.tableMetadata().scalarAttributeType("gsiSk")).contains(ScalarAttributeType.N);
    }

    @Test
    @DisplayName("A built-in B converter is valid for a GSI partition key")
    void build_whenSdkBytesGsiPartitionKey_selectsSdkBytesConverterWithTypeB() {
        StaticTableSchema<SdkBytesGsiItem> schema =
            StaticTableSchema.builder(SdkBytesGsiItem.class)
                             .newItemSupplier(SdkBytesGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(SdkBytesGsiItem::getPk)
                                                               .setter(SdkBytesGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(SdkBytes.class, a -> a.name("gsiPk")
                                                                 .getter(SdkBytesGsiItem::getGsiPk)
                                                                 .setter(SdkBytesGsiItem::setGsiPk)
                                                                 .tags(secondaryPartitionKey("gsi")))
                             .build();

        assertThat(schema.converterForAttribute("gsiPk")).isInstanceOf(SdkBytesAttributeConverter.class);
        assertThat(schema.tableMetadata().scalarAttributeType("gsiPk")).contains(ScalarAttributeType.B);
    }

    @Test
    @DisplayName("A built-in S converter is valid for an LSI sort key")
    void build_whenStringLsiSortKey_selectsStringConverterWithTypeS() {
        StaticTableSchema<LsiItem> schema = lsiSchema();

        assertThat(schema.converterForAttribute("lsiSk")).isInstanceOf(StringAttributeConverter.class);
        assertThat(schema.tableMetadata().scalarAttributeType("lsiSk")).contains(ScalarAttributeType.S);
    }

    @Test
    @DisplayName("A provider-selected custom S converter is valid for a GSI key")
    void build_whenCustomStringConverterForGsiKey_selectsCustomConverterWithTypeS() {
        RecordingCustomProvider customProvider = new RecordingCustomProvider(AttributeValueType.S);
        StaticTableSchema<CustomTypeGsiItem> schema =
            customTypeGsiSchema(customProvider, AttributeConverterProvider.defaultProvider(), null);

        assertThat(customProvider.requestedTypes())
            .filteredOn(type -> type.equals(EnhancedType.of(CustomType.class)))
            .hasSize(2);
        assertThat(schema.converterForAttribute("gsiPk")).isInstanceOf(CustomTypeConverter.class);
        assertThat(schema.tableMetadata().scalarAttributeType("gsiPk")).contains(ScalarAttributeType.S);
    }

    @Test
    @DisplayName("An attribute-level custom B converter is valid for a GSI key")
    void build_whenAttributeLevelBinaryConverterForGsiKey_skipsProviderAndSelectsTypeB() {
        RecordingCustomProvider schemaProvider = new RecordingCustomProvider(AttributeValueType.S);
        CustomTypeConverter binaryConverter = new CustomTypeConverter(AttributeValueType.B);
        StaticTableSchema<CustomTypeGsiItem> schema =
            customTypeGsiSchema(schemaProvider, AttributeConverterProvider.defaultProvider(), binaryConverter);

        assertThat(schemaProvider.requestedTypes())
            .filteredOn(type -> type.equals(EnhancedType.of(CustomType.class)))
            .isEmpty();
        assertThat(schema.converterForAttribute("gsiPk")).isSameAs(binaryConverter);
        assertThat(schema.tableMetadata().scalarAttributeType("gsiPk")).contains(ScalarAttributeType.B);
    }

    @Test
    @DisplayName("A BOOL converter is rejected as a GSI key")
    void build_whenBooleanGsiPartitionKey_throwsUnsuitableKeyType() {
        StaticTableSchema.Builder<BooleanGsiItem> builder =
            StaticTableSchema.builder(BooleanGsiItem.class)
                             .newItemSupplier(BooleanGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(BooleanGsiItem::getPk)
                                                               .setter(BooleanGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(Boolean.class, a -> a.name("gsiPk")
                                                                .getter(BooleanGsiItem::getGsiPk)
                                                                .setter(BooleanGsiItem::setGsiPk)
                                                                .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type BOOL is not a suitable type to be used as a key.");
    }

    @Test
    @DisplayName("An L converter is rejected as an LSI key")
    void build_whenStringListLsiSortKey_throwsUnsuitableKeyType() {
        StaticTableSchema.Builder<ListLsiItem> builder =
            StaticTableSchema.builder(ListLsiItem.class)
                             .newItemSupplier(ListLsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ListLsiItem::getPk)
                                                               .setter(ListLsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(EnhancedType.listOf(String.class),
                                           a -> a.name("lsiSk")
                                                 .getter(ListLsiItem::getLsiSk)
                                                 .setter(ListLsiItem::setLsiSk)
                                                 .tags(secondarySortKey("lsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'lsiSk' of type L is not a suitable type to be used as a key.");
    }

    @Test
    @DisplayName("An M map converter is rejected as a GSI key")
    void build_whenStringToIntegerMapGsiPartitionKey_throwsUnsuitableKeyType() {
        StaticTableSchema.Builder<MapGsiItem> builder =
            StaticTableSchema.builder(MapGsiItem.class)
                             .newItemSupplier(MapGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(MapGsiItem::getPk)
                                                               .setter(MapGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(EnhancedType.mapOf(String.class, Integer.class),
                                           a -> a.name("gsiPk")
                                                 .getter(MapGsiItem::getGsiPk)
                                                 .setter(MapGsiItem::setGsiPk)
                                                 .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type M is not a suitable type to be used as a key.");
    }

    @Test
    @DisplayName("An SS converter is rejected as a GSI key")
    void build_whenStringSetGsiPartitionKey_throwsUnsuitableKeyType() {
        StaticTableSchema.Builder<StringSetGsiItem> builder =
            StaticTableSchema.builder(StringSetGsiItem.class)
                             .newItemSupplier(StringSetGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(StringSetGsiItem::getPk)
                                                               .setter(StringSetGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(EnhancedType.setOf(String.class),
                                           a -> a.name("gsiPk")
                                                 .getter(StringSetGsiItem::getGsiPk)
                                                 .setter(StringSetGsiItem::setGsiPk)
                                                 .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type SS is not a suitable type to be used as a key.");
    }

    @Test
    @DisplayName("An NS converter is rejected as a GSI key")
    void build_whenIntegerSetGsiPartitionKey_throwsUnsuitableKeyType() {
        StaticTableSchema.Builder<IntegerSetGsiItem> builder =
            StaticTableSchema.builder(IntegerSetGsiItem.class)
                             .newItemSupplier(IntegerSetGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(IntegerSetGsiItem::getPk)
                                                               .setter(IntegerSetGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(EnhancedType.setOf(Integer.class),
                                           a -> a.name("gsiPk")
                                                 .getter(IntegerSetGsiItem::getGsiPk)
                                                 .setter(IntegerSetGsiItem::setGsiPk)
                                                 .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type NS is not a suitable type to be used as a key.");
    }

    @Test
    @DisplayName("A BS converter is rejected as a GSI key")
    void build_whenSdkBytesSetGsiPartitionKey_throwsUnsuitableKeyType() {
        StaticTableSchema.Builder<SdkBytesSetGsiItem> builder =
            StaticTableSchema.builder(SdkBytesSetGsiItem.class)
                             .newItemSupplier(SdkBytesSetGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(SdkBytesSetGsiItem::getPk)
                                                               .setter(SdkBytesSetGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(EnhancedType.setOf(SdkBytes.class),
                                           a -> a.name("gsiPk")
                                                 .getter(SdkBytesSetGsiItem::getGsiPk)
                                                 .setter(SdkBytesSetGsiItem::setGsiPk)
                                                 .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type BS is not a suitable type to be used as a key.");
    }

    @Test
    @DisplayName("An M document converter is rejected as a GSI key")
    void build_whenSchemaBearingDocumentGsiPartitionKey_throwsUnsuitableKeyType() {
        TableSchema<DocumentType> documentSchema =
            StaticTableSchema.builder(DocumentType.class).newItemSupplier(DocumentType::new).build();
        StaticTableSchema.Builder<DocumentGsiItem> builder =
            StaticTableSchema.builder(DocumentGsiItem.class)
                             .newItemSupplier(DocumentGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(DocumentGsiItem::getPk)
                                                               .setter(DocumentGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(EnhancedType.documentOf(DocumentType.class, documentSchema),
                                           a -> a.name("gsiPk")
                                                 .getter(DocumentGsiItem::getGsiPk)
                                                 .setter(DocumentGsiItem::setGsiPk)
                                                 .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type M is not a suitable type to be used as a key.");
    }

    @Test
    @DisplayName("A provider-selected custom M converter is rejected as a GSI key")
    void build_whenCustomMapConverterForGsiKey_callsProviderTwiceThenThrowsUnsuitableKeyType() {
        RecordingCustomProvider customProvider = new RecordingCustomProvider(AttributeValueType.M);
        StaticTableSchema.Builder<CustomTypeGsiItem> builder =
            customTypeGsiBuilder(customProvider, AttributeConverterProvider.defaultProvider(), null);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type M is not a suitable type to be used as a key.");
        assertThat(customProvider.requestedTypes())
            .filteredOn(type -> type.equals(EnhancedType.of(CustomType.class)))
            .hasSize(2);
    }

    @Test
    @DisplayName("An attribute-level custom M converter is rejected as a GSI key")
    void build_whenAttributeLevelMapConverterForGsiKey_skipsProviderAndThrowsUnsuitableKeyType() {
        ReturningNullProvider schemaProvider = new ReturningNullProvider();
        CustomTypeConverter mapConverter = new CustomTypeConverter(AttributeValueType.M);
        StaticTableSchema.Builder<CustomTypeGsiItem> builder =
            customTypeGsiBuilder(schemaProvider, AttributeConverterProvider.defaultProvider(), mapConverter);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type M is not a suitable type to be used as a key.");
        assertThat(schemaProvider.requestedTypes()).doesNotContain(EnhancedType.of(CustomType.class));
    }

    @Test
    @DisplayName("An Object index attribute fails converter lookup")
    void build_whenObjectGsiPartitionKey_throwsConverterNotFound() {
        StaticTableSchema.Builder<ObjectGsiItem> builder =
            StaticTableSchema.builder(ObjectGsiItem.class)
                             .newItemSupplier(ObjectGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ObjectGsiItem::getPk)
                                                               .setter(ObjectGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(Object.class, a -> a.name("gsiPk")
                                                               .getter(ObjectGsiItem::getGsiPk)
                                                               .setter(ObjectGsiItem::setGsiPk)
                                                               .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("An unsupported index attribute fails during converter selection")
    void build_whenUnsupportedTypeGsiPartitionKey_throwsConverterNotFound() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);
        StaticTableSchema.Builder<UnsupportedGsiItem> builder =
            StaticTableSchema.builder(UnsupportedGsiItem.class)
                             .newItemSupplier(UnsupportedGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(UnsupportedGsiItem::getPk)
                                                               .setter(UnsupportedGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(UnsupportedType.class, a -> a.name("gsiPk")
                                                                        .getter(UnsupportedGsiItem::getGsiPk)
                                                                        .setter(UnsupportedGsiItem::setGsiPk)
                                                                        .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A concrete list index attribute fails during converter selection")
    void build_whenConcreteArrayListGsiPartitionKey_throwsConverterNotFound() {
        EnhancedType<ArrayList<String>> type = new EnhancedType<ArrayList<String>>() { };
        StaticTableSchema.Builder<ArrayListGsiItem> builder =
            StaticTableSchema.builder(ArrayListGsiItem.class)
                             .newItemSupplier(ArrayListGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ArrayListGsiItem::getPk)
                                                               .setter(ArrayListGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(type, a -> a.name("gsiPk")
                                                       .getter(ArrayListGsiItem::getGsiPk)
                                                       .setter(ArrayListGsiItem::setGsiPk)
                                                       .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A custom provider makes an Object GSI key scalar")
    void build_whenObjectGsiKeyWithObjectProvider_selectsStringConverterWithTypeS() {
        ObjectProvider objectProvider = new ObjectProvider();
        StaticTableSchema<ObjectGsiItem> schema =
            StaticTableSchema.builder(ObjectGsiItem.class)
                             .newItemSupplier(ObjectGsiItem::new)
                             .attributeConverterProviders(objectProvider,
                                                          AttributeConverterProvider.defaultProvider())
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ObjectGsiItem::getPk)
                                                               .setter(ObjectGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(Object.class, a -> a.name("gsiPk")
                                                               .getter(ObjectGsiItem::getGsiPk)
                                                               .setter(ObjectGsiItem::setGsiPk)
                                                               .tags(secondaryPartitionKey("gsi")))
                             .build();

        assertThat(schema.converterForAttribute("gsiPk")).isInstanceOf(ObjectStringConverter.class);
        assertThat(objectProvider.requestedTypes())
            .filteredOn(type -> type.equals(EnhancedType.of(Object.class)))
            .hasSize(2);
        assertThat(schema.tableMetadata().scalarAttributeType("gsiPk")).contains(ScalarAttributeType.S);
    }

    @Test
    @DisplayName("An attribute-level converter makes an Object GSI key scalar")
    void build_whenObjectGsiKeyWithAttributeLevelConverter_skipsProviderAndSelectsTypeB() {
        ReturningNullProvider schemaProvider = new ReturningNullProvider();
        ObjectBinaryConverter binaryConverter = new ObjectBinaryConverter();
        StaticTableSchema<ObjectGsiItem> schema =
            StaticTableSchema.builder(ObjectGsiItem.class)
                             .newItemSupplier(ObjectGsiItem::new)
                             .attributeConverterProviders(schemaProvider,
                                                          AttributeConverterProvider.defaultProvider())
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ObjectGsiItem::getPk)
                                                               .setter(ObjectGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(Object.class, a -> a.name("gsiPk")
                                                               .getter(ObjectGsiItem::getGsiPk)
                                                               .setter(ObjectGsiItem::setGsiPk)
                                                               .attributeConverter(binaryConverter)
                                                               .tags(secondaryPartitionKey("gsi")))
                             .build();

        assertThat(schemaProvider.requestedTypes()).doesNotContain(EnhancedType.of(Object.class));
        assertThat(schema.converterForAttribute("gsiPk")).isSameAs(binaryConverter);
        assertThat(schema.tableMetadata().scalarAttributeType("gsiPk")).contains(ScalarAttributeType.B);
    }

    @Test
    @DisplayName("A Collection index attribute is rejected because SS is not a suitable key type")
    void build_whenStringCollectionGsiPartitionKey_throwsIllegalArgumentExceptionForSsKey() {
        EnhancedType<Collection<String>> type = EnhancedType.collectionOf(String.class);
        StaticTableSchema.Builder<CollectionGsiItem> builder =
            StaticTableSchema.builder(CollectionGsiItem.class)
                             .newItemSupplier(CollectionGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(CollectionGsiItem::getPk)
                                                               .setter(CollectionGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(type,
                                           a -> a.name("gsiPk")
                                                 .getter(CollectionGsiItem::getGsiPk)
                                                 .setter(CollectionGsiItem::setGsiPk)
                                                 .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type SS is not a suitable type to be used as a key.");
    }

    @Test
    @DisplayName("An Iterable index attribute is rejected because SS is not a suitable key type")
    void build_whenStringIterableGsiPartitionKey_throwsIllegalArgumentExceptionForSsKey() {
        EnhancedType<Iterable<String>> type = new EnhancedType<Iterable<String>>() { };
        StaticTableSchema.Builder<IterableGsiItem> builder =
            StaticTableSchema.builder(IterableGsiItem.class)
                             .newItemSupplier(IterableGsiItem::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(IterableGsiItem::getPk)
                                                               .setter(IterableGsiItem::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(type,
                                           a -> a.name("gsiPk")
                                                 .getter(IterableGsiItem::getGsiPk)
                                                 .setter(IterableGsiItem::setGsiPk)
                                                 .tags(secondaryPartitionKey("gsi")));

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute 'gsiPk' of type SS is not a suitable type to be used as a key.");
    }

    private static StaticTableSchema<StringGsiItem> stringGsiSchema() {
        return StaticTableSchema.builder(StringGsiItem.class)
                                .newItemSupplier(StringGsiItem::new)
                                .addAttribute(String.class, a -> a.name("pk")
                                                                  .getter(StringGsiItem::getPk)
                                                                  .setter(StringGsiItem::setPk)
                                                                  .tags(primaryPartitionKey()))
                                .addAttribute(String.class, a -> a.name("gsiPk")
                                                                  .getter(StringGsiItem::getGsiPk)
                                                                  .setter(StringGsiItem::setGsiPk)
                                                                  .tags(secondaryPartitionKey("gsi")))
                                .build();
    }

    private static StaticTableSchema<LsiItem> lsiSchema() {
        return StaticTableSchema.builder(LsiItem.class)
                                .newItemSupplier(LsiItem::new)
                                .addAttribute(String.class, a -> a.name("pk")
                                                                  .getter(LsiItem::getPk)
                                                                  .setter(LsiItem::setPk)
                                                                  .tags(primaryPartitionKey()))
                                .addAttribute(String.class, a -> a.name("lsiSk")
                                                                  .getter(LsiItem::getLsiSk)
                                                                  .setter(LsiItem::setLsiSk)
                                                                  .tags(secondarySortKey("lsi")))
                                .build();
    }

    private static StaticTableSchema<CustomTypeGsiItem> customTypeGsiSchema(
        AttributeConverterProvider first,
        AttributeConverterProvider second,
        AttributeConverter<CustomType> attributeConverter) {
        return customTypeGsiBuilder(first, second, attributeConverter).build();
    }

    private static StaticTableSchema.Builder<CustomTypeGsiItem> customTypeGsiBuilder(
        AttributeConverterProvider first,
        AttributeConverterProvider second,
        AttributeConverter<CustomType> attributeConverter) {
        return StaticTableSchema.builder(CustomTypeGsiItem.class)
                                .newItemSupplier(CustomTypeGsiItem::new)
                                .attributeConverterProviders(first, second)
                                .addAttribute(String.class, a -> a.name("pk")
                                                                  .getter(CustomTypeGsiItem::getPk)
                                                                  .setter(CustomTypeGsiItem::setPk)
                                                                  .tags(primaryPartitionKey()))
                                .addAttribute(CustomType.class, a -> {
                                    a.name("gsiPk")
                                     .getter(CustomTypeGsiItem::getGsiPk)
                                     .setter(CustomTypeGsiItem::setGsiPk)
                                     .tags(secondaryPartitionKey("gsi"));
                                    if (attributeConverter != null) {
                                        a.attributeConverter(attributeConverter);
                                    }
                                });
    }

    static final class StringGsiItem {
        private String pk;
        private String gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public String getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(String gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class IntegerGsiSortItem {
        private String pk;
        private String gsiPk;
        private Integer gsiSk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public String getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(String gsiPk) {
            this.gsiPk = gsiPk;
        }

        public Integer getGsiSk() {
            return gsiSk;
        }

        public void setGsiSk(Integer gsiSk) {
            this.gsiSk = gsiSk;
        }
    }

    static final class SdkBytesGsiItem {
        private String pk;
        private SdkBytes gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public SdkBytes getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(SdkBytes gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class LsiItem {
        private String pk;
        private String lsiSk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public String getLsiSk() {
            return lsiSk;
        }

        public void setLsiSk(String lsiSk) {
            this.lsiSk = lsiSk;
        }
    }

    static final class CustomTypeGsiItem {
        private String pk;
        private CustomType gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public CustomType getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(CustomType gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class BooleanGsiItem {
        private String pk;
        private Boolean gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Boolean getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(Boolean gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class ListLsiItem {
        private String pk;
        private List<String> lsiSk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public List<String> getLsiSk() {
            return lsiSk;
        }

        public void setLsiSk(List<String> lsiSk) {
            this.lsiSk = lsiSk;
        }
    }

    static final class MapGsiItem {
        private String pk;
        private Map<String, Integer> gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Map<String, Integer> getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(Map<String, Integer> gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class StringSetGsiItem {
        private String pk;
        private Set<String> gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Set<String> getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(Set<String> gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class IntegerSetGsiItem {
        private String pk;
        private Set<Integer> gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Set<Integer> getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(Set<Integer> gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class SdkBytesSetGsiItem {
        private String pk;
        private Set<SdkBytes> gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Set<SdkBytes> getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(Set<SdkBytes> gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class DocumentGsiItem {
        private String pk;
        private DocumentType gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public DocumentType getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(DocumentType gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class ObjectGsiItem {
        private String pk;
        private Object gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Object getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(Object gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class UnsupportedGsiItem {
        private String pk;
        private UnsupportedType gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public UnsupportedType getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(UnsupportedType gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class ArrayListGsiItem {
        private String pk;
        private ArrayList<String> gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public ArrayList<String> getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(ArrayList<String> gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class CollectionGsiItem {
        private String pk;
        private Collection<String> gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Collection<String> getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(Collection<String> gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class IterableGsiItem {
        private String pk;
        private Iterable<String> gsiPk;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Iterable<String> getGsiPk() {
            return gsiPk;
        }

        public void setGsiPk(Iterable<String> gsiPk) {
            this.gsiPk = gsiPk;
        }
    }

    static final class CustomType {
    }

    static final class UnsupportedType {
    }

    static final class DocumentType {
    }

    static final class CustomTypeConverter implements AttributeConverter<CustomType> {
        private final AttributeValueType attributeValueType;

        CustomTypeConverter(AttributeValueType attributeValueType) {
            this.attributeValueType = attributeValueType;
        }

        @Override
        public AttributeValue transformFrom(CustomType input) {
            if (attributeValueType == AttributeValueType.M) {
                return AttributeValue.fromM(Collections.singletonMap("v", AttributeValue.fromS("x")));
            }
            if (attributeValueType == AttributeValueType.B) {
                return AttributeValue.fromB(SdkBytes.fromUtf8String("x"));
            }
            return AttributeValue.fromS("custom:x");
        }

        @Override
        public CustomType transformTo(AttributeValue input) {
            return new CustomType();
        }

        @Override
        public EnhancedType<CustomType> type() {
            return EnhancedType.of(CustomType.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return attributeValueType;
        }
    }

    static final class RecordingCustomProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();
        private final CustomTypeConverter converter;

        RecordingCustomProvider(AttributeValueType attributeValueType) {
            this.converter = new CustomTypeConverter(attributeValueType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            if (EnhancedType.of(CustomType.class).equals(enhancedType)) {
                return (AttributeConverter<T>) converter;
            }
            return null;
        }

        List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }
    }

    static final class ObjectStringConverter implements AttributeConverter<Object> {
        @Override
        public AttributeValue transformFrom(Object input) {
            return AttributeValue.fromS(String.valueOf(input));
        }

        @Override
        public Object transformTo(AttributeValue input) {
            return input.s();
        }

        @Override
        public EnhancedType<Object> type() {
            return EnhancedType.of(Object.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    static final class ObjectBinaryConverter implements AttributeConverter<Object> {
        @Override
        public AttributeValue transformFrom(Object input) {
            return AttributeValue.fromB(SdkBytes.fromUtf8String(String.valueOf(input)));
        }

        @Override
        public Object transformTo(AttributeValue input) {
            return input.b().asUtf8String();
        }

        @Override
        public EnhancedType<Object> type() {
            return EnhancedType.of(Object.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.B;
        }
    }

    static final class ObjectProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();
        private final ObjectStringConverter converter = new ObjectStringConverter();

        @Override
        @SuppressWarnings("unchecked")
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            if (EnhancedType.of(Object.class).equals(enhancedType)) {
                return (AttributeConverter<T>) converter;
            }
            return null;
        }

        List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }
    }

    static final class ReturningNullProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            return null;
        }

        List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }
    }
}
