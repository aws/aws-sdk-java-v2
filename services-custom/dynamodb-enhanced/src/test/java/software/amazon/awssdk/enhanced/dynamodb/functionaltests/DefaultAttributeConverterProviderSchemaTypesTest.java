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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.assertReconstructedCollections;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.assertWrittenItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.DETAILS_NOTE;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.DIRECT_VALUE;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.ITEM_ID;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.allBranchesDocument;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.allBranchesStaticImmutableSchema;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.allBranchesStaticSchema;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.completeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.countersAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.detailsSchema;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.eventsAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.labelsAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.oneCounter;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.oneEvent;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.oneLabel;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedAllBranchesBean;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedAllBranchesImmutable;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedAllBranchesStaticImmutable;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedAllBranchesStaticRecord;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedConverterImmutable;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedConverterRecord;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedDefaultProviderBean;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedDefaultProviderImmutable;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedStaticImmutableRecord;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedStaticRecord;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.staticImmutableRecordSchema;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.staticRecordSchema;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.typedCollectionsDocument;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.AllBranchesBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.AllBranchesDetails;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.AllBranchesImmutable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithArrayList;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithHashMap;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithHashSet;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithObject;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithUnsupported;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.AllBranchesStaticImmutable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.AllBranchesStaticRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.ConverterImmutable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.ConverterRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.DefaultProviderBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.DefaultProviderImmutable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.FlattenedChildBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.OuterFlattenedBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.SingleGsiPartitionBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.SingleGsiSortBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.StaticImmutableRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.StaticRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.TestEnum;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.UnsupportedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

/**
 * Schema factories for bean, immutable, static mutable, static immutable, and document types.
 * <p>
 * In-memory factory cases run once. They cover successful conversion, construction failures, omitted
 * providers, flatten, and default GSI key order.
 * Table put and get cases run for each {@link DynamoDbEnhancedClientType}.
 */
public class DefaultAttributeConverterProviderSchemaTypesTest extends LocalDynamoDbTestBase {

    @BeforeAll
    public static void startLocalDynamoDb() {
        localDynamoDb().start();
    }

    @AfterAll
    public static void stopLocalDynamoDbForJunit5() {
        localDynamoDb().stop();
    }

    @BeforeEach
    public void clearBeanSchemaCache() throws Exception {
        Method clear = BeanTableSchema.class.getDeclaredMethod("clearSchemaCache");
        clear.setAccessible(true);
        clear.invoke(null);
    }

    @Test
    @DisplayName("Bean schema converts supported collections to DynamoDB and back")
    public void itemToMapThenMapToItem_whenFromBeanConverterRecordIsPopulated_roundTripsSupportedCollections() {
        TableSchema<ConverterRecord> schema = TableSchema.fromBean(ConverterRecord.class);
        ConverterRecord item = populatedConverterRecord();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ConverterRecord read = schema.mapToItem(map);

        assertWrittenCollections(map);
        assertReconstructedCollections(read);
    }

    @Test
    @DisplayName("Immutable schema converts supported collections to DynamoDB and back")
    public void itemToMapThenMapToItem_whenFromImmutableClassIsPopulated_roundTripsSupportedCollections() {
        TableSchema<ConverterImmutable> schema = TableSchema.fromImmutableClass(ConverterImmutable.class);
        ConverterImmutable item = populatedConverterImmutable();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ConverterImmutable read = schema.mapToItem(map);

        assertWrittenCollections(map);
        assertThat(read.counters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(read.labels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(read.events()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Document schema converts typed collections to DynamoDB and back")
    public void itemToMapThenMapToItem_whenDocumentHasTypedCollections_roundTripsSupportedCollections() {
        TableSchema<EnhancedDocument> schema = TableSchema.documentSchemaBuilder().build();
        EnhancedDocument document = typedCollectionsDocument();

        Map<String, AttributeValue> map = schema.itemToMap(document, false);
        EnhancedDocument read = schema.mapToItem(map);

        assertWrittenCollections(map);
        assertThat(read.get("counters", EnhancedType.mapOf(String.class, Integer.class)))
            .isInstanceOf(LinkedHashMap.class)
            .isEqualTo(oneCounter());
        assertThat(read.get("labels", EnhancedType.setOf(String.class)))
            .isInstanceOf(LinkedHashSet.class)
            .isEqualTo(oneLabel());
        assertThat(read.get("events", EnhancedType.listOf(String.class)))
            .isInstanceOf(ArrayList.class)
            .isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Static schema converts supported collections to DynamoDB and back")
    public void itemToMapThenMapToItem_whenStaticTableSchemaRecordIsPopulated_roundTripsSupportedCollections() {
        StaticTableSchema<StaticRecord> schema = staticRecordSchema();
        StaticRecord item = populatedStaticRecord();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        StaticRecord read = schema.mapToItem(map);

        assertWrittenCollections(map);
        assertThat(read.getCounters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(read.getLabels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(read.getEvents()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Bean schema uses the default provider when converters are omitted")
    public void itemToMapThenMapToItem_whenFromBeanConverterProvidersOmitted_roundTripsSupportedCollections() {
        DynamoDbBean annotation = DefaultProviderBean.class.getAnnotation(DynamoDbBean.class);
        TableSchema<DefaultProviderBean> schema = TableSchema.fromBean(DefaultProviderBean.class);
        DefaultProviderBean item = populatedDefaultProviderBean();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        DefaultProviderBean read = schema.mapToItem(map);

        assertThat(annotation.converterProviders()).containsExactly(DefaultAttributeConverterProvider.class);
        assertWrittenCollections(map);
        assertThat(read.getCounters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(read.getLabels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(read.getEvents()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Immutable schema uses the default provider when converters are omitted")
    public void itemToMapThenMapToItem_whenFromImmutableConverterProvidersOmitted_roundTripsSupportedCollections() {
        DynamoDbImmutable annotation = DefaultProviderImmutable.class.getAnnotation(DynamoDbImmutable.class);
        TableSchema<DefaultProviderImmutable> schema =
            TableSchema.fromImmutableClass(DefaultProviderImmutable.class);
        DefaultProviderImmutable item = populatedDefaultProviderImmutable();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        DefaultProviderImmutable read = schema.mapToItem(map);

        assertThat(annotation.converterProviders()).containsExactly(DefaultAttributeConverterProvider.class);
        assertWrittenCollections(map);
        assertThat(read.counters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(read.labels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(read.events()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Flattened child collections convert without an explicit flatten bean class")
    public void itemToMapThenMapToItem_whenFlattenOmitsBeanClass_roundTripsFlattenedChildCollections() throws Exception {
        DynamoDbFlatten flatten = OuterFlattenedBean.class.getMethod("getChild").getAnnotation(DynamoDbFlatten.class);
        FlattenedChildBean child = new FlattenedChildBean();
        child.setCounters(oneCounter());
        child.setLabels(oneLabel());
        child.setEvents(oneEvent());
        OuterFlattenedBean item = new OuterFlattenedBean();
        item.setId(ITEM_ID);
        item.setChild(child);
        TableSchema<OuterFlattenedBean> schema = TableSchema.fromBean(OuterFlattenedBean.class);

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        OuterFlattenedBean read = schema.mapToItem(map);

        assertThat(flatten.dynamoDbBeanClass()).isEqualTo(Object.class);
        assertWrittenCollections(map);
        assertThat(read.getChild().getCounters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(read.getChild().getLabels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(read.getChild().getEvents()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Secondary partition key without order is an unspecified string key")
    public void fromBean_whenSecondaryPartitionKeyOmitsOrder_usesUnspecifiedStringKey() throws Exception {
        DynamoDbSecondaryPartitionKey annotation =
            SingleGsiPartitionBean.class.getMethod("getGsiKey")
                                        .getAnnotation(DynamoDbSecondaryPartitionKey.class);
        TableSchema<SingleGsiPartitionBean> schema = TableSchema.fromBean(SingleGsiPartitionBean.class);
        IndexMetadata gsi = index(schema, "gsi");

        assertThat(annotation.order()).isEqualTo(Order.UNSPECIFIED);
        assertThat(gsi.partitionKeys()).hasSize(1);
        assertThat(gsi.partitionKeys().get(0).attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(gsi.partitionKeys().get(0).order()).isEqualTo(Order.UNSPECIFIED);
    }

    @Test
    @DisplayName("Secondary sort key without order is an unspecified integer key")
    public void fromBean_whenSecondarySortKeyOmitsOrder_usesUnspecifiedIntegerKey() throws Exception {
        DynamoDbSecondarySortKey annotation =
            SingleGsiSortBean.class.getMethod("getGsiSort").getAnnotation(DynamoDbSecondarySortKey.class);
        TableSchema<SingleGsiSortBean> schema = TableSchema.fromBean(SingleGsiSortBean.class);
        IndexMetadata gsi = index(schema, "gsi");

        assertThat(annotation.order()).isEqualTo(Order.UNSPECIFIED);
        assertThat(gsi.sortKeys()).hasSize(1);
        assertThat(gsi.sortKeys().get(0).attributeValueType()).isEqualTo(AttributeValueType.N);
        assertThat(gsi.sortKeys().get(0).order()).isEqualTo(Order.UNSPECIFIED);
    }

    @Test
    @DisplayName("Static schema generates collection converters when providers are omitted")
    public void itemToMapThenMapToItem_whenStaticTableSchemaProvidersOmitted_roundTripsGeneratedCollectionConverters() {
        StaticTableSchema<StaticRecord> schema = staticRecordSchema();
        StaticRecord item = populatedStaticRecord();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        StaticRecord read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("counters")).isInstanceOf(MapAttributeConverter.class);
        assertThat(schema.converterForAttribute("labels")).isInstanceOf(SetAttributeConverter.class);
        assertThat(schema.converterForAttribute("events")).isInstanceOf(ListAttributeConverter.class);
        assertWrittenCollections(map);
        assertThat(read.getCounters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(read.getLabels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(read.getEvents()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Static immutable schema generates collection converters when providers are omitted")
    public void itemToMapThenMapToItem_whenStaticImmutableProvidersOmitted_roundTripsGeneratedCollectionConverters() {
        StaticImmutableTableSchema<StaticImmutableRecord, StaticImmutableRecord.Builder> schema =
            staticImmutableRecordSchema();
        StaticImmutableRecord item = populatedStaticImmutableRecord();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        StaticImmutableRecord read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("counters")).isInstanceOf(MapAttributeConverter.class);
        assertThat(schema.converterForAttribute("labels")).isInstanceOf(SetAttributeConverter.class);
        assertThat(schema.converterForAttribute("events")).isInstanceOf(ListAttributeConverter.class);
        assertWrittenCollections(map);
        assertThat(read.counters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(read.labels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(read.events()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Document schema reads generated collection classes when providers are omitted")
    public void documentSchemaBuilder_whenAttributeConverterProvidersOmitted_readsGeneratedCollectionClasses() {
        TableSchema<EnhancedDocument> schema = TableSchema.documentSchemaBuilder().build();
        Map<String, AttributeValue> attributeMap = completeItem();

        EnhancedDocument read = schema.mapToItem(attributeMap);

        assertThat(read.get("counters", EnhancedType.mapOf(String.class, Integer.class)))
            .isInstanceOf(LinkedHashMap.class)
            .isEqualTo(oneCounter());
        assertThat(read.get("labels", EnhancedType.setOf(String.class)))
            .isInstanceOf(LinkedHashSet.class)
            .isEqualTo(oneLabel());
        assertThat(read.get("events", EnhancedType.listOf(String.class)))
            .isInstanceOf(ArrayList.class)
            .isEqualTo(oneEvent());
    }

    @Test
    @DisplayName("Rejects a bean schema when its Object attribute has no converter")
    public void fromBean_whenBeanDeclaresObject_throwsConverterNotFound() {
        assertThatThrownBy(() -> TableSchema.fromBean(BeanWithObject.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("Rejects a bean schema that declares an unsupported attribute type")
    public void fromBean_whenBeanDeclaresUnsupportedType_throwsIllegalStateException() {
        assertThatThrownBy(() -> TableSchema.fromBean(BeanWithUnsupported.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("Rejects a bean schema that declares an ArrayList attribute")
    public void fromBean_whenBeanDeclaresArrayList_throwsIllegalStateException() {
        EnhancedType<ArrayList<String>> type = new EnhancedType<ArrayList<String>>() { };
        assertThatThrownBy(() -> TableSchema.fromBean(BeanWithArrayList.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Rejects a bean schema that declares a HashSet attribute")
    public void fromBean_whenBeanDeclaresHashSet_throwsIllegalStateException() {
        EnhancedType<HashSet<String>> type = new EnhancedType<HashSet<String>>() { };
        assertThatThrownBy(() -> TableSchema.fromBean(BeanWithHashSet.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Rejects a bean schema that declares a HashMap attribute")
    public void fromBean_whenBeanDeclaresHashMap_throwsIllegalStateException() {
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() { };
        assertThatThrownBy(() -> TableSchema.fromBean(BeanWithHashMap.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Bean table put and get convert every default lookup type")
    public void putItemThenGetItem_whenBeanSchemaHasAllLookupTypes_roundTripsConvertedValues(
        DynamoDbEnhancedClientType clientType) {
        SchemaTableClient client = openTable(clientType);
        try {
            TableSchema<AllBranchesBean> schema = TableSchema.fromBean(AllBranchesBean.class);

            client.putItem(schema, populatedAllBranchesBean());
            AllBranchesBean result = client.getItem(schema, itemKey());

            assertAllBranchesItem(client.storedItem());
            assertThat(result.getDirect()).isEqualTo(DIRECT_VALUE);
            assertThat(result.getStatus()).isEqualTo(TestEnum.OPEN);
            assertThat(result.getDetails().getNote()).isEqualTo(DETAILS_NOTE);
            assertThat(result.getCounters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
            assertThat(result.getLabels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
            assertThat(result.getEvents()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Immutable table put and get convert every default lookup type")
    public void putItemThenGetItem_whenImmutableSchemaHasAllLookupTypes_roundTripsConvertedValues(
        DynamoDbEnhancedClientType clientType) {
        SchemaTableClient client = openTable(clientType);
        try {
            TableSchema<AllBranchesImmutable> schema = TableSchema.fromImmutableClass(AllBranchesImmutable.class);

            client.putItem(schema, populatedAllBranchesImmutable());
            AllBranchesImmutable result = client.getItem(schema, itemKey());

            assertAllBranchesItem(client.storedItem());
            assertAllBranchesImmutable(result);
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Static table put and get convert every default lookup type")
    public void putItemThenGetItem_whenStaticSchemaHasAllLookupTypes_roundTripsConvertedValues(
        DynamoDbEnhancedClientType clientType) {
        SchemaTableClient client = openTable(clientType);
        try {
            TableSchema<AllBranchesStaticRecord> schema = allBranchesStaticSchema();

            client.putItem(schema, populatedAllBranchesStaticRecord());
            AllBranchesStaticRecord result = client.getItem(schema, itemKey());

            assertAllBranchesItem(client.storedItem());
            assertThat(result.getDirect()).isEqualTo(DIRECT_VALUE);
            assertThat(result.getStatus()).isEqualTo(TestEnum.OPEN);
            assertThat(result.getDetails().getNote()).isEqualTo(DETAILS_NOTE);
            assertThat(result.getCounters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
            assertThat(result.getLabels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
            assertThat(result.getEvents()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Static immutable table put and get convert every default lookup type")
    public void putItemThenGetItem_whenStaticImmutableSchemaHasAllLookupTypes_roundTripsConvertedValues(
        DynamoDbEnhancedClientType clientType) {
        SchemaTableClient client = openTable(clientType);
        try {
            TableSchema<AllBranchesStaticImmutable> schema = allBranchesStaticImmutableSchema();

            client.putItem(schema, populatedAllBranchesStaticImmutable());
            AllBranchesStaticImmutable result = client.getItem(schema, itemKey());

            assertAllBranchesItem(client.storedItem());
            assertThat(result.direct()).isEqualTo(DIRECT_VALUE);
            assertThat(result.status()).isEqualTo(TestEnum.OPEN);
            assertThat(result.details().getNote()).isEqualTo(DETAILS_NOTE);
            assertThat(result.counters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
            assertThat(result.labels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
            assertThat(result.events()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Document table put and get convert every default lookup type")
    public void putItemThenGetItem_whenDocumentSchemaHasAllLookupTypes_roundTripsConvertedValues(
        DynamoDbEnhancedClientType clientType) {
        SchemaTableClient client = openTable(clientType);
        try {
            TableSchema<AllBranchesDetails> details = detailsSchema();
            TableSchema<EnhancedDocument> schema =
                TableSchema.documentSchemaBuilder()
                           .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id", AttributeValueType.S)
                           .build();

            client.putItem(schema, allBranchesDocument(details));
            EnhancedDocument result = client.getItem(schema, itemKey());

            assertAllBranchesItem(client.storedItem());
            assertThat(result.get("direct", EnhancedType.of(String.class))).isEqualTo(DIRECT_VALUE);
            assertThat(result.get("status", EnhancedType.of(TestEnum.class))).isEqualTo(TestEnum.OPEN);
            assertThat(result.get("details", EnhancedType.documentOf(AllBranchesDetails.class, details)).getNote())
                .isEqualTo(DETAILS_NOTE);
            assertThat(result.get("counters", EnhancedType.mapOf(String.class, Integer.class)))
                .isInstanceOf(LinkedHashMap.class)
                .isEqualTo(oneCounter());
            assertThat(result.get("labels", EnhancedType.setOf(String.class)))
                .isInstanceOf(LinkedHashSet.class)
                .isEqualTo(oneLabel());
            assertThat(result.get("events", EnhancedType.listOf(String.class)))
                .isInstanceOf(ArrayList.class)
                .isEqualTo(oneEvent());
        } finally {
            client.deleteTable();
        }
    }

    private SchemaTableClient openTable(DynamoDbEnhancedClientType clientType) {
        SchemaTableClient client = clientType == DynamoDbEnhancedClientType.SYNC
                                   ? new SyncSchemaTableClient()
                                   : new AsyncSchemaTableClient();
        client.createTable();
        return client;
    }

    private static Key itemKey() {
        return Key.builder().partitionValue(ITEM_ID).build();
    }

    private static void assertWrittenCollections(Map<String, AttributeValue> map) {
        assertThat(map.get("counters")).isEqualTo(countersAttribute());
        assertThat(map.get("labels")).isEqualTo(labelsAttribute());
        assertThat(map.get("events")).isEqualTo(eventsAttribute());
    }

    private static void assertAllBranchesItem(Map<String, AttributeValue> item) {
        assertWrittenItem(item);
        assertThat(item.get("direct").s()).isEqualTo(DIRECT_VALUE);
        assertThat(item.get("status").s()).isEqualTo(TestEnum.OPEN.toString());
        assertThat(item.get("details").m().get("note").s()).isEqualTo(DETAILS_NOTE);
    }

    private static void assertAllBranchesImmutable(AllBranchesImmutable result) {
        assertThat(result.id()).isEqualTo(ITEM_ID);
        assertThat(result.direct()).isEqualTo(DIRECT_VALUE);
        assertThat(result.status()).isEqualTo(TestEnum.OPEN);
        assertThat(result.details().getNote()).isEqualTo(DETAILS_NOTE);
        assertThat(result.counters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(result.labels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(result.events()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    private static IndexMetadata index(TableSchema<?> schema, String indexName) {
        for (IndexMetadata index : schema.tableMetadata().indices()) {
            if (indexName.equals(index.name())) {
                return index;
            }
        }
        throw new AssertionError("Missing index " + indexName);
    }

    private interface SchemaTableClient {
        void createTable();

        void deleteTable();

        <T> void putItem(TableSchema<T> schema, T item);

        <T> T getItem(TableSchema<T> schema, Key key);

        Map<String, AttributeValue> storedItem();
    }

    private final class SyncSchemaTableClient implements SchemaTableClient {
        private final DynamoDbClient dynamoDbClient = localDynamoDb().createClient();
        private final DynamoDbEnhancedClient enhancedClient =
            DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        private final String tableName = getConcreteTableName("table-name");
        private final DynamoDbTable<ConverterRecord> table =
            enhancedClient.table(tableName, TableSchema.fromBean(ConverterRecord.class));

        @Override
        public void createTable() {
            table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        }

        @Override
        public void deleteTable() {
            dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
        }

        @Override
        public <T> void putItem(TableSchema<T> schema, T item) {
            enhancedClient.table(tableName, schema).putItem(item);
        }

        @Override
        public <T> T getItem(TableSchema<T> schema, Key key) {
            return enhancedClient.table(tableName, schema).getItem(key);
        }

        @Override
        public Map<String, AttributeValue> storedItem() {
            return dynamoDbClient.getItem(r -> r.tableName(tableName)
                                          .key(Collections.singletonMap("id", AttributeValue.fromS(ITEM_ID)))
                                          .consistentRead(true))
                           .item();
        }
    }

    private final class AsyncSchemaTableClient implements SchemaTableClient {
        private final DynamoDbAsyncClient dynamoDbAsyncClient = localDynamoDb().createAsyncClient();
        private final DynamoDbEnhancedAsyncClient enhancedClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build();
        private final String tableName = getConcreteTableName("table-name");
        private final DynamoDbAsyncTable<ConverterRecord> table =
            enhancedClient.table(tableName, TableSchema.fromBean(ConverterRecord.class));

        @Override
        public void createTable() {
            table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
        }

        @Override
        public void deleteTable() {
            dynamoDbAsyncClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build()).join();
        }

        @Override
        public <T> void putItem(TableSchema<T> schema, T item) {
            enhancedClient.table(tableName, schema).putItem(item).join();
        }

        @Override
        public <T> T getItem(TableSchema<T> schema, Key key) {
            return enhancedClient.table(tableName, schema).getItem(key).join();
        }

        @Override
        public Map<String, AttributeValue> storedItem() {
            return dynamoDbAsyncClient.getItem(r -> r.tableName(tableName)
                                          .key(Collections.singletonMap("id", AttributeValue.fromS(ITEM_ID)))
                                          .consistentRead(true))
                           .join()
                           .item();
        }
    }
}
