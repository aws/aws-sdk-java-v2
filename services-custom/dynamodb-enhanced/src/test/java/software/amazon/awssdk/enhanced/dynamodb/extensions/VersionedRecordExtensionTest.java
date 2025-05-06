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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.ImmutableFakeVersionedItem;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class VersionedRecordExtensionTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private final VersionedRecordExtension versionedRecordExtension = VersionedRecordExtension.builder().build();

    @Test
    public void beforeRead_doesNotTransformObject() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);

        ReadModification result =
            versionedRecordExtension.afterRead(DefaultDynamoDbExtensionContext
                                                   .builder()
                                                   .items(fakeItemMap)
                                                   .tableMetadata(FakeItem.getTableMetadata())
                                                   .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result, is(ReadModification.builder().build()));
    }

    @Test
    public void beforeWrite_initialVersion_expressionIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();

        WriteModification result =
            versionedRecordExtension.beforeWrite(
                DefaultDynamoDbExtensionContext
                    .builder()
                    .items(FakeItem.getTableSchema().itemToMap(fakeItem, true))
                    .tableMetadata(FakeItem.getTableMetadata())
                    .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.additionalConditionalExpression(),
                   is(Expression.builder()
                                .expression("attribute_not_exists(#AMZN_MAPPED_version)")
                                .expressionNames(singletonMap("#AMZN_MAPPED_version", "version"))
                                .build()));
    }

    @Test
    public void beforeWrite_initialVersion_transformedItemIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemWithInitialVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        fakeItemWithInitialVersion.put("version", AttributeValue.builder().n("1").build());

        WriteModification result =
            versionedRecordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                                     .builder()
                                                     .items(FakeItem.getTableSchema().itemToMap(fakeItem, true))
                                                     .tableMetadata(FakeItem.getTableMetadata())
                                                     .operationContext(PRIMARY_CONTEXT).build());


        assertThat(result.transformedItem(), is(fakeItemWithInitialVersion));
    }

    @Test
    public void beforeWrite_initialVersionDueToExplicitNull_transformedItemIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> inputMap =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        inputMap.put("version", AttributeValue.builder().nul(true).build());
        Map<String, AttributeValue> fakeItemWithInitialVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        fakeItemWithInitialVersion.put("version", AttributeValue.builder().n("1").build());

        WriteModification result =
            versionedRecordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                                     .builder()
                                                     .items(inputMap)
                                                     .tableMetadata(FakeItem.getTableMetadata())
                                                     .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), is(fakeItemWithInitialVersion));
    }

    @Test
    public void beforeWrite_existingVersion_expressionIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();
        fakeItem.setVersion(13);

        WriteModification result =
            versionedRecordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                                     .builder()
                                                     .items(FakeItem.getTableSchema().itemToMap(fakeItem, true))
                                                     .tableMetadata(FakeItem.getTableMetadata())
                                                     .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.additionalConditionalExpression(),
                   is(Expression.builder()
                                .expression("#AMZN_MAPPED_version = :old_version_value")
                                .expressionNames(singletonMap("#AMZN_MAPPED_version", "version"))
                                .expressionValues(singletonMap(":old_version_value",
                                                               AttributeValue.builder().n("13").build()))
                                .build()));
    }

    @Test
    public void beforeWrite_existingVersion_transformedItemIsCorrect() {
        FakeItem fakeItem = createUniqueFakeItem();
        fakeItem.setVersion(13);
        Map<String, AttributeValue> fakeItemWithInitialVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        fakeItemWithInitialVersion.put("version", AttributeValue.builder().n("14").build());

        WriteModification result =
            versionedRecordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                                     .builder()
                                                     .items(FakeItem.getTableSchema().itemToMap(fakeItem, true))
                                                     .tableMetadata(FakeItem.getTableMetadata())
                                                     .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), is(fakeItemWithInitialVersion));
    }

    @Test
    public void beforeWrite_returnsNoOpModification_ifVersionAttributeNotDefined() {
        FakeItemWithSort fakeItemWithSort = createUniqueFakeItemWithSort();
        Map<String, AttributeValue> itemMap =
            new HashMap<>(FakeItemWithSort.getTableSchema().itemToMap(fakeItemWithSort, true));

        WriteModification writeModification = versionedRecordExtension.beforeWrite( DefaultDynamoDbExtensionContext.builder()
                                                                                                                   .items(itemMap)
                                                                                                                   .operationContext(PRIMARY_CONTEXT)
                                                                                                                   .tableMetadata(FakeItemWithSort.getTableMetadata())
                                                                                                                   .build());
        assertThat(writeModification, is(WriteModification.builder().build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void beforeWrite_throwsIllegalArgumentException_ifVersioPnAttributeIsWrongType() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemWithBadVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        fakeItemWithBadVersion.put("version", AttributeValue.builder().s("14").build());

        versionedRecordExtension.beforeWrite(
            DefaultDynamoDbExtensionContext.builder()
                                           .items(fakeItemWithBadVersion)
                                           .operationContext(PRIMARY_CONTEXT)
                                           .tableMetadata(FakeItem.getTableMetadata())
                                           .build());
    }

    @Test
    public void beforeWrite_versionEqualsStartAt_treatedAsInitialVersion() {
        VersionedRecordExtension recordExtension = VersionedRecordExtension.builder()
                                                                           .startAt(5L)
                                                                           .build();

        FakeItem fakeItem = createUniqueFakeItem();
        fakeItem.setVersion(5);

        Map<String, AttributeValue> inputMap =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(FakeItem.getTableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.additionalConditionalExpression().expression(),
                   is("attribute_not_exists(#AMZN_MAPPED_version)"));
    }

    @ParameterizedTest
    @MethodSource("customStartAtAndIncrementValues")
    public void customStartingValueAndIncrement_worksAsExpected(Long startAt, Long incrementBy, String expectedVersion) {
        VersionedRecordExtension.Builder recordExtensionBuilder = VersionedRecordExtension.builder();
        if (startAt != null) {
            recordExtensionBuilder.startAt(startAt);
        }
        if (incrementBy != null) {
            recordExtensionBuilder.incrementBy(incrementBy);
        }

        VersionedRecordExtension recordExtension = recordExtensionBuilder.build();

        FakeItem fakeItem = createUniqueFakeItem();

        Map<String, AttributeValue> inputMap =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));

        Map<String, AttributeValue> expectedInitialVersion =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));

        expectedInitialVersion.put("version", AttributeValue.builder().n(expectedVersion).build());

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(FakeItem.getTableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), is(expectedInitialVersion));
        assertThat(result.additionalConditionalExpression(),
                   is(Expression.builder()
                                .expression("attribute_not_exists(#AMZN_MAPPED_version)")
                                .expressionNames(singletonMap("#AMZN_MAPPED_version", "version"))
                                .build()));
    }

    public static Stream<Arguments> customStartAtAndIncrementValues() {
        return Stream.of(
            Arguments.of(0L,1L,"1"),
            Arguments.of(3L,2L,"5"),
            Arguments.of(3L,null,"4"),
            Arguments.of(null,3L,"3"));
    }

    @ParameterizedTest
    @MethodSource("customFailingStartAtAndIncrementValues")
    public void customStartingValueAndIncrement_shouldThrow(Long startAt, Long incrementBy) {
        assertThrows(IllegalArgumentException.class, () -> VersionedRecordExtension.builder()
                                                                               .startAt(startAt)
                                                                               .incrementBy(incrementBy)
                                                                               .build());
    }

    public static Stream<Arguments> customFailingStartAtAndIncrementValues() {
        return Stream.of(
            Arguments.of(-2L, 1L),
            Arguments.of(3L, 0L));
    }

    @Test
    public void beforeWrite_versionNotEqualsAnnotationStartAt_notTreatedAsInitialVersion() {
        FakeVersionedThroughAnnotationItem item = new FakeVersionedThroughAnnotationItem();
        item.setId(UUID.randomUUID().toString());
        item.setVersion(10L);

        TableSchema<FakeVersionedThroughAnnotationItem> schema =
            TableSchema.fromBean(FakeVersionedThroughAnnotationItem.class);

        Map<String, AttributeValue> inputMap = new HashMap<>(schema.itemToMap(item, true));

        VersionedRecordExtension recordExtension = VersionedRecordExtension.builder().build();

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(schema.tableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.additionalConditionalExpression().expression(),
                   is("#AMZN_MAPPED_version = :old_version_value"));
    }

    @Test
    public void beforeWrite_versionEqualsAnnotationStartAt_isTreatedAsInitialVersion() {
        FakeVersionedThroughAnnotationItem item = new FakeVersionedThroughAnnotationItem();
        item.setId(UUID.randomUUID().toString());
        item.setVersion(3L);

        TableSchema<FakeVersionedThroughAnnotationItem> schema =
            TableSchema.fromBean(FakeVersionedThroughAnnotationItem.class);

        Map<String, AttributeValue> inputMap = new HashMap<>(schema.itemToMap(item, true));

        VersionedRecordExtension recordExtension = VersionedRecordExtension.builder().build();

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(schema.tableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.additionalConditionalExpression().expression(),
                   is("attribute_not_exists(#AMZN_MAPPED_version)"));
    }


    @DynamoDbBean
    public static class FakeVersionedThroughAnnotationItem {
        private String id;
        private Long version;

        public FakeVersionedThroughAnnotationItem() {
        }

        @DynamoDbPartitionKey
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @DynamoDbVersionAttribute(startAt = 3, incrementBy = 2)
        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
    }


    @Test
    public void customStartingValueAndIncrementWithAnnotation_worksAsExpected() {
        VersionedRecordExtension recordExtension = VersionedRecordExtension.builder().build();

        FakeVersionedThroughAnnotationItem item = new FakeVersionedThroughAnnotationItem();
        item.setId(UUID.randomUUID().toString());

        TableSchema<FakeVersionedThroughAnnotationItem> schema = TableSchema.fromBean(FakeVersionedThroughAnnotationItem.class);

        Map<String, AttributeValue> inputMap = new HashMap<>(schema.itemToMap(item, true));

        Map<String, AttributeValue> expectedInitialVersion = new HashMap<>(schema.itemToMap(item, true));
        expectedInitialVersion.put("version", AttributeValue.builder().n("5").build());

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(schema.tableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), is(expectedInitialVersion));
        assertThat(result.additionalConditionalExpression(),
                   is(Expression.builder()
                                .expression("attribute_not_exists(#AMZN_MAPPED_version)")
                                .expressionNames(singletonMap("#AMZN_MAPPED_version", "version"))
                                .build()));
    }

    @Test
    public void customAnnotationValuesAndBuilderValues_annotationShouldTakePrecedence() {
        VersionedRecordExtension recordExtension = VersionedRecordExtension.builder()
                                                        .startAt(5L)
                                                        .incrementBy(2L)
                                                        .build();

        FakeVersionedThroughAnnotationItem item = new FakeVersionedThroughAnnotationItem();
        item.setId(UUID.randomUUID().toString());

        TableSchema<FakeVersionedThroughAnnotationItem> schema = TableSchema.fromBean(FakeVersionedThroughAnnotationItem.class);

        Map<String, AttributeValue> inputMap = new HashMap<>(schema.itemToMap(item, true));

        Map<String, AttributeValue> expectedInitialVersion = new HashMap<>(schema.itemToMap(item, true));
        expectedInitialVersion.put("version", AttributeValue.builder().n("5").build());

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(schema.tableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), is(expectedInitialVersion));
        assertThat(result.additionalConditionalExpression(),
                   is(Expression.builder()
                                .expression("attribute_not_exists(#AMZN_MAPPED_version)")
                                .expressionNames(singletonMap("#AMZN_MAPPED_version", "version"))
                                .build()));
    }

    @DynamoDbBean
    public static class FakeVersionedThroughAnnotationItemWithExplicitDefaultValues {
        private String id;
        private Long version;

        public FakeVersionedThroughAnnotationItemWithExplicitDefaultValues() {
        }

        @DynamoDbPartitionKey
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @DynamoDbVersionAttribute(startAt = 0, incrementBy = 1)
        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
    }

    @Test
    public void customAnnotationDefaultValuesAndBuilderValues_annotationShouldTakePrecedence() {
        VersionedRecordExtension recordExtension = VersionedRecordExtension.builder()
                                                                           .startAt(5L)
                                                                           .incrementBy(2L)
                                                                           .build();

        FakeVersionedThroughAnnotationItemWithExplicitDefaultValues item = new FakeVersionedThroughAnnotationItemWithExplicitDefaultValues();
        item.setId(UUID.randomUUID().toString());

        TableSchema<FakeVersionedThroughAnnotationItemWithExplicitDefaultValues> schema = TableSchema.fromBean(FakeVersionedThroughAnnotationItemWithExplicitDefaultValues.class);

        Map<String, AttributeValue> inputMap = new HashMap<>(schema.itemToMap(item, true));

        Map<String, AttributeValue> expectedInitialVersion = new HashMap<>(schema.itemToMap(item, true));
        expectedInitialVersion.put("version", AttributeValue.builder().n("1").build());

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(schema.tableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), is(expectedInitialVersion));
        assertThat(result.additionalConditionalExpression(),
                   is(Expression.builder()
                                .expression("attribute_not_exists(#AMZN_MAPPED_version)")
                                .expressionNames(singletonMap("#AMZN_MAPPED_version", "version"))
                                .build()));
    }

    @DynamoDbBean
    public static class FakeVersionedThroughAnnotationItemWithInvalidValues {
        private String id;
        private Long version;

        public FakeVersionedThroughAnnotationItemWithInvalidValues() {
        }

        @DynamoDbPartitionKey
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @DynamoDbVersionAttribute(startAt = -1, incrementBy = -1)
        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
    }

    @Test
    public void invalidAnnotationValues_shouldThrowException() {
        FakeVersionedThroughAnnotationItemWithInvalidValues item = new FakeVersionedThroughAnnotationItemWithInvalidValues();
        item.setId(UUID.randomUUID().toString());

        assertThrows(IllegalArgumentException.class, () -> TableSchema.fromBean(FakeVersionedThroughAnnotationItemWithInvalidValues.class));
    }

    @ParameterizedTest
    @MethodSource("customIncrementForExistingVersionValues")
    public void customIncrementForExistingVersion_worksAsExpected(Long startAt, Long incrementBy,
                                                                  Long existingVersion, String expectedNextVersion) {
        VersionedRecordExtension.Builder recordExtensionBuilder = VersionedRecordExtension.builder();
        if (startAt != null) {
            recordExtensionBuilder.startAt(startAt);
        }
        if (incrementBy != null) {
            recordExtensionBuilder.incrementBy(incrementBy);
        }
        VersionedRecordExtension recordExtension = recordExtensionBuilder.build();

        FakeItem fakeItem = createUniqueFakeItem();
        fakeItem.setVersion(existingVersion.intValue());

        Map<String, AttributeValue> inputMap =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));

        Map<String, AttributeValue> expectedVersionedItem =
            new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));
        expectedVersionedItem.put("version", AttributeValue.builder().n(expectedNextVersion).build());

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(FakeItem.getTableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), is(expectedVersionedItem));
        assertThat(result.additionalConditionalExpression().expression(),
                   is("#AMZN_MAPPED_version = :old_version_value"));
    }

    @ParameterizedTest
    @MethodSource("customIncrementForExistingVersionValues")
    public void customIncrementForExistingVersion_withImmutableSchema_worksAsExpected(Long startAt, Long incrementBy,
                                                                  Long existingVersion, String expectedNextVersion) {
        VersionedRecordExtension.Builder recordExtensionBuilder = VersionedRecordExtension.builder();
        if (startAt != null) {
            recordExtensionBuilder.startAt(startAt);
        }
        if (incrementBy != null) {
            recordExtensionBuilder.incrementBy(incrementBy);
        }
        VersionedRecordExtension recordExtension = recordExtensionBuilder.build();

        ImmutableFakeVersionedItem fakeItem = ImmutableFakeVersionedItem
            .builder()
            .id(UUID.randomUUID().toString())
            .version(existingVersion)
            .build();

        Map<String, AttributeValue> inputMap =
            new HashMap<>(ImmutableFakeVersionedItem.getTableSchema().itemToMap(fakeItem, true));

        Map<String, AttributeValue> expectedVersionedItem =
            new HashMap<>(ImmutableFakeVersionedItem.getTableSchema().itemToMap(fakeItem, true));
        expectedVersionedItem.put("version", AttributeValue.builder().n(expectedNextVersion).build());

        WriteModification result =
            recordExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                            .builder()
                                            .items(inputMap)
                                            .tableMetadata(ImmutableFakeVersionedItem.getTableMetadata())
                                            .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), is(expectedVersionedItem));
        assertThat(result.additionalConditionalExpression().expression(),
                   is("#AMZN_MAPPED_version = :old_version_value"));
    }

    public static Stream<Arguments> customIncrementForExistingVersionValues() {
        return Stream.of(
            Arguments.of(0L, 1L, 5L, "6"),
            Arguments.of(3L, 2L, 7L, "9"),
            Arguments.of(3L, null, 10L, "11"),
            Arguments.of(null, 3L, 4L, "7"));
    }
}
