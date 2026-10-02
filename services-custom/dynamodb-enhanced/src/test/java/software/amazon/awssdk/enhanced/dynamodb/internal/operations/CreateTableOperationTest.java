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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.HASH;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithBinaryKey;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithByteBufferKey;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithCompositeGsi;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithFlattenedGsi;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithMixedCompositeGsi;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithNumericSort;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CompositeMetadataImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CrossIndexImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.MixedFlattenedImmutable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedVectorIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElementType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.StreamSpecification;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;
import software.amazon.awssdk.services.dynamodb.model.VectorDistanceFunction;
import software.amazon.awssdk.services.dynamodb.model.VectorIndex;


@RunWith(MockitoJUnitRunner.class)
public class CreateTableOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, "gsi_1");

    private static MatchedGsi matchesGsi(software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex other) {
        return new MatchedGsi(other);
    }

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private static class MatchedGsi
        extends TypeSafeMatcher<software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex> {

        private final software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex other;

        private MatchedGsi(software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex other) {
            this.other = other;
        }

        @Override
        protected boolean matchesSafely(software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex globalSecondaryIndex) {
            if (!other.indexName().equals(globalSecondaryIndex.indexName())) {
                return false;
            }

            if ((other.projection() != null && !other.projection().equals(globalSecondaryIndex.projection())) ||
                (other.projection() == null && globalSecondaryIndex.projection() != null)) {
                return false;
            }

            return containsInAnyOrder(other.keySchema().toArray(new KeySchemaElement[]{}))
                .matches(globalSecondaryIndex.keySchema());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a GlobalSecondaryIndex equivalent to [" + other.toString() + "]");
        }
    }

    @Test
    public void returnsCorrectOperationName() {
        CreateTableOperation<FakeItemWithIndices> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder().build());

        assertThat(operation.operationName().label()).isEqualTo("CreateTable");
    }

    @Test
    public void generateRequest_withLsiAndGsi() {
        Projection projection1 = Projection.builder().projectionType(ProjectionType.ALL).build();
        Projection projection2 = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
        Projection projection3 = Projection.builder()
                                           .projectionType(ProjectionType.INCLUDE)
                                           .nonKeyAttributes("key1", "key2")
                                           .build();
        ProvisionedThroughput provisionedThroughput1 = ProvisionedThroughput.builder()
                                                                            .readCapacityUnits(1L)
                                                                            .writeCapacityUnits(2L)
                                                                            .build();
        ProvisionedThroughput provisionedThroughput2 = ProvisionedThroughput.builder()
                                                                            .readCapacityUnits(3L)
                                                                            .writeCapacityUnits(4L)
                                                                            .build();


        List<EnhancedGlobalSecondaryIndex> globalSecondaryIndexList = Arrays.asList(
                EnhancedGlobalSecondaryIndex.builder()
                        .indexName("gsi_1")
                        .projection(projection1)
                        .provisionedThroughput(provisionedThroughput1)
                        .build(),
                EnhancedGlobalSecondaryIndex.builder()
                        .indexName("gsi_2")
                        .projection(projection2)
                        .provisionedThroughput(provisionedThroughput2)
                        .build());

        CreateTableOperation<FakeItemWithIndices> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                                                                  .globalSecondaryIndices(globalSecondaryIndexList)
                                                                  .localSecondaryIndices(Collections.singletonList(
                                                                      EnhancedLocalSecondaryIndex.create("lsi_1", projection3)))
                                                                  .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithIndices.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);



        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.keySchema()).containsExactlyInAnyOrder(KeySchemaElement.builder()
                                                                           .attributeName("id")
                                                                           .keyType(HASH)
                                                                           .build(),
                                                           KeySchemaElement.builder()
                                                                           .attributeName("sort")
                                                                           .keyType(RANGE)
                                                                           .build());
        software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex expectedGsi1 =
            software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex.builder()
                                                                .indexName("gsi_1")
                                                                .keySchema(KeySchemaElement.builder()
                                                                                           .attributeName("gsi_id")
                                                                                           .keyType(HASH)
                                                                                           .build(),
                                                                           KeySchemaElement.builder()
                                                                                           .attributeName("gsi_sort")
                                                                                           .keyType(RANGE)
                                                                                           .build())
                                                                .projection(projection1)
                                                                .provisionedThroughput(provisionedThroughput1)
                                                                .build();
        software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex expectedGsi2 =
            software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex.builder()
                                                                .indexName("gsi_2")
                                                                .keySchema(KeySchemaElement.builder()
                                                                                           .attributeName("gsi_id")
                                                                                           .keyType(HASH)
                                                                                           .build())
                                                                .projection(projection2)
                                                                .provisionedThroughput(provisionedThroughput2)
                                                                .build();
        MatcherAssert.assertThat(request.globalSecondaryIndexes(), containsInAnyOrder(matchesGsi(expectedGsi1),
                                                                                matchesGsi(expectedGsi2)));
        software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex expectedLsi =
            software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex.builder()
                                                             .indexName("lsi_1")
                                                             .keySchema(KeySchemaElement.builder()
                                                                                        .attributeName("id")
                                                                                        .keyType(HASH)
                                                                                        .build(),
                                                                        KeySchemaElement.builder()
                                                                                        .attributeName("lsi_sort")
                                                                                        .keyType(RANGE)
                                                                                        .build())
                                                             .projection(projection3)
                                                             .build();
        assertThat(request.localSecondaryIndexes()).containsExactlyInAnyOrder(expectedLsi);
        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder()
                               .attributeName("id")
                               .attributeType(ScalarAttributeType.S)
                               .build(),
            AttributeDefinition.builder()
                               .attributeName("sort")
                               .attributeType(ScalarAttributeType.S)
                               .build(),
            AttributeDefinition.builder()
                               .attributeName("lsi_sort")
                               .attributeType(ScalarAttributeType.S)
                               .build(),
            AttributeDefinition.builder()
                               .attributeName("gsi_id")
                               .attributeType(ScalarAttributeType.S)
                               .build(),
            AttributeDefinition.builder()
                               .attributeName("gsi_sort")
                               .attributeType(ScalarAttributeType.S)
                               .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_invalidGsi() {
        ProvisionedThroughput provisionedThroughput = ProvisionedThroughput.builder()
                                                                           .readCapacityUnits(1L)
                                                                           .writeCapacityUnits(1L)
                                                                           .build();

        List<EnhancedGlobalSecondaryIndex> invalidGsiList = Collections.singletonList(
                EnhancedGlobalSecondaryIndex.builder()
                        .indexName("invalid")
                        .projection(p -> p.projectionType(ProjectionType.ALL))
                        .provisionedThroughput(provisionedThroughput)
                        .build());

        CreateTableOperation<FakeItem> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder().globalSecondaryIndices(invalidGsiList).build());

        operation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_invalidGsiAsLsiReference() {
        List<EnhancedLocalSecondaryIndex> invalidGsiList = Collections.singletonList(
            EnhancedLocalSecondaryIndex.create("gsi_1", Projection.builder().projectionType(ProjectionType.ALL).build()));

        CreateTableOperation<FakeItemWithIndices> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder().localSecondaryIndices(invalidGsiList).build());

        operation.generateRequest(FakeItemWithIndices.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void generateRequest_validLsiAsGsiReference() {
        List<EnhancedGlobalSecondaryIndex> validLsiList = Collections.singletonList(
                EnhancedGlobalSecondaryIndex.builder()
                        .indexName("lsi_1")
                        .projection(p -> p.projectionType(ProjectionType.ALL))
                        .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                        .build());

        CreateTableOperation<FakeItemWithIndices> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder().globalSecondaryIndices(validLsiList).build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithIndices.getTableSchema(), PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex globalSecondaryIndex =
            request.globalSecondaryIndexes().get(0);

        assertThat(globalSecondaryIndex.indexName()).isEqualTo("lsi_1");
    }

    @Test
    public void generateRequest_nonReferencedIndicesDoNotCreateExtraAttributeDefinitions() {
        CreateTableOperation<FakeItemWithIndices> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder().build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithIndices.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        AttributeDefinition attributeDefinition1 = AttributeDefinition.builder()
                                                                      .attributeName("id")
                                                                      .attributeType(ScalarAttributeType.S)
                                                                      .build();
        AttributeDefinition attributeDefinition2 = AttributeDefinition.builder()
                                                                      .attributeName("sort")
                                                                      .attributeType(ScalarAttributeType.S)
                                                                      .build();

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(attributeDefinition1, attributeDefinition2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_invalidLsi() {
        List<EnhancedLocalSecondaryIndex> invalidLsiList = Collections.singletonList(
            EnhancedLocalSecondaryIndex.create("invalid", Projection.builder().projectionType(ProjectionType.ALL).build()));

        CreateTableOperation<FakeItem> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder().localSecondaryIndices(invalidLsiList).build());

        operation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void generateRequest_withProvisionedThroughput() {
       ProvisionedThroughput provisionedThroughput = ProvisionedThroughput.builder()
                                                                          .writeCapacityUnits(1L)
                                                                          .readCapacityUnits(2L)
                                                                          .build();

        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().provisionedThroughput(provisionedThroughput).build());

        CreateTableRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.billingMode()).isEqualTo(BillingMode.PROVISIONED);
        assertThat(request.provisionedThroughput()).isEqualTo(provisionedThroughput);
    }

    @Test
    public void generateRequest_withNoProvisionedThroughput() {
        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder().build());

        CreateTableRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.billingMode()).isEqualTo(BillingMode.PAY_PER_REQUEST);
    }

    @Test
    public void generateRequest_withStreamSpecification() {
        StreamSpecification streamSpecification = StreamSpecification.builder()
                                                                     .streamEnabled(true)
                                                                     .streamViewType(StreamViewType.NEW_IMAGE)
                                                                     .build();

        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().streamSpecification(streamSpecification).build());

        CreateTableRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.streamSpecification()).isEqualTo(streamSpecification);
    }

    @Test
    public void generateRequest_withNoStreamSpecification() {
        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder().build());

        CreateTableRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.streamSpecification()).isNull();
    }


    @Test
    public void generateRequest_withNumericKey() {
        CreateTableOperation<FakeItemWithNumericSort> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                                                                                                                        .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithNumericSort.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.keySchema()).containsExactlyInAnyOrder(KeySchemaElement.builder()
                                                                           .attributeName("id")
                                                                           .keyType(HASH)
                                                                           .build(),
                                                           KeySchemaElement.builder()
                                                                           .attributeName("sort")
                                                                           .keyType(RANGE)
                                                                           .build());

        assertThat(request.globalSecondaryIndexes()).isEqualTo(DefaultSdkAutoConstructList.getInstance());
        assertThat(request.localSecondaryIndexes()).isEqualTo(DefaultSdkAutoConstructList.getInstance());

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder()
                               .attributeName("id")
                               .attributeType(ScalarAttributeType.S)
                               .build(),
            AttributeDefinition.builder()
                               .attributeName("sort")
                               .attributeType(ScalarAttributeType.N)
                               .build());
    }

    @Test
    public void generateRequest_withBinaryKey() {
        CreateTableOperation<FakeItemWithBinaryKey> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                                                                                                                      .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithBinaryKey.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.keySchema()).containsExactlyInAnyOrder(KeySchemaElement.builder()
                                                                           .attributeName("id")
                                                                           .keyType(HASH)
                                                                           .build());

        assertThat(request.globalSecondaryIndexes()).isEmpty();
        assertThat(request.localSecondaryIndexes()).isEmpty();

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder()
                               .attributeName("id")
                               .attributeType(ScalarAttributeType.B)
                               .build());
    }

    @Test
    public void generateRequest_withByteBufferKey() {
        CreateTableOperation<FakeItemWithByteBufferKey> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithByteBufferKey.getTableSchema(),
                PRIMARY_CONTEXT,
                null);

        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.keySchema()).containsExactlyInAnyOrder(KeySchemaElement.builder()
                .attributeName("id")
                .keyType(HASH)
                .build());

        assertThat(request.globalSecondaryIndexes()).isEmpty();
        assertThat(request.localSecondaryIndexes()).isEmpty();

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
                AttributeDefinition.builder()
                        .attributeName("id")
                        .attributeType(ScalarAttributeType.B)
                        .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_doesNotWorkForIndex() {
        CreateTableOperation<FakeItemWithIndices> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                                                                                                                    .build());

        operation.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null);
    }

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder().build());
        CreateTableRequest createTableRequest = CreateTableRequest.builder().build();
        CreateTableResponse expectedResponse = CreateTableResponse.builder().build();
        when(mockDynamoDbClient.createTable(any(CreateTableRequest.class))).thenReturn(expectedResponse);

        CreateTableResponse actualResponse = operation.serviceCall(mockDynamoDbClient).apply(createTableRequest);

        assertThat(actualResponse).isSameAs(expectedResponse);
        verify(mockDynamoDbClient).createTable(same(createTableRequest));
    }

    @Test
    public void transformResults_doesNothing() {
        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder().build());
        CreateTableResponse response = CreateTableResponse.builder().build();

        operation.transformResponse(response, FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void generateRequest_gsiWithSingleKeys_buildsCorrectly() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("gsi_1")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<FakeItemWithIndices> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithIndices.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("gsi_1");
        assertThat(gsi.keySchema().size()).isEqualTo(2);
    }

    @Test
    public void generateRequest_gsiWithCompositeKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("composite_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(5L).writeCapacityUnits(5L))
                .build());

        CreateTableOperation<FakeItemWithCompositeGsi> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithCompositeGsi.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);

        assertThat(gsi.indexName()).isEqualTo("composite_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(4);

        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames).containsExactlyInAnyOrder("gsi_pk1", "gsi_pk2");

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames).containsExactlyInAnyOrder("gsi_sk1", "gsi_sk2");
    }

    @Test
    public void generateRequest_gsiWithFlattenedPartitionKey() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("flatten_partition_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<FakeItemWithFlattenedGsi> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithFlattenedGsi.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("flatten_partition_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(1);
        assertThat(gsi.keySchema().get(0).attributeName()).isEqualTo("gsiPartitionKey");
        assertThat(gsi.keySchema().get(0).keyType()).isEqualTo(HASH);
    }

    @Test
    public void generateRequest_gsiWithFlattenedSortKey() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("flatten_sort_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<FakeItemWithFlattenedGsi> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithFlattenedGsi.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("flatten_sort_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(2);
        assertThat(gsi.keySchema().get(0).attributeName()).isEqualTo("id");
        assertThat(gsi.keySchema().get(0).keyType()).isEqualTo(HASH);
        assertThat(gsi.keySchema().get(1).attributeName()).isEqualTo("gsiSortKey");
        assertThat(gsi.keySchema().get(1).keyType()).isEqualTo(RANGE);
    }

    @Test
    public void generateRequest_gsiWithMixedFlattenedKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("flatten_mixed_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<FakeItemWithFlattenedGsi> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithFlattenedGsi.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("flatten_mixed_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(2);
        
        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames).containsExactlyInAnyOrder("gsiMixedPartitionKey");

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames).containsExactlyInAnyOrder("gsiMixedSortKey");
    }

    @Test
    public void generateRequest_gsiWithBothFlattenedKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("flatten_both_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<FakeItemWithFlattenedGsi> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithFlattenedGsi.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("flatten_both_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(2);
        
        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames).containsExactlyInAnyOrder("gsiBothSortKey");

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames).containsExactlyInAnyOrder("gsiBothSortKey");
    }

    @Test
    public void generateRequest_gsiWithMixedCompositePartitionKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("mixed_partition_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<FakeItemWithMixedCompositeGsi> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithMixedCompositeGsi.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("mixed_partition_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(4);
        
        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames).containsExactlyInAnyOrder("rootPartitionKey1", "rootPartitionKey2", "flattenedPartitionKey1", "flattenedPartitionKey2");
    }

    @Test
    public void generateRequest_gsiWithMixedCompositeSortKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("mixed_sort_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<FakeItemWithMixedCompositeGsi> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithMixedCompositeGsi.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("mixed_sort_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(6);

        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames).containsExactlyInAnyOrder("rootPartitionKey1", "rootPartitionKey2");

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames).containsExactlyInAnyOrder("rootSortKey1", "rootSortKey2", "flattenedSortKey1", "flattenedSortKey2");
    }

    @Test
    public void generateRequest_gsiWithFullMixedCompositeKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("full_mixed_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<FakeItemWithMixedCompositeGsi> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithMixedCompositeGsi.getTableSchema(),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("full_mixed_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(8);
        
        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames).containsExactlyInAnyOrder("rootPartitionKey1", "rootPartitionKey2", "flattenedPartitionKey1", "flattenedPartitionKey2");

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames).containsExactlyInAnyOrder("rootSortKey1", "rootSortKey2", "flattenedSortKey1", "flattenedSortKey2");
    }

    @Test
    public void generateRequest_immutableGsiWithCompositeKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("gsi1")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(5L).writeCapacityUnits(5L))
                .build());

        CreateTableOperation<CompositeMetadataImmutable> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(ImmutableTableSchema.create(CompositeMetadataImmutable.class),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("gsi1");
        assertThat(gsi.keySchema().size()).isEqualTo(4);

        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames).containsExactlyInAnyOrder("gsiPk1", "gsiPk2");

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames).containsExactlyInAnyOrder("gsiSk1", "gsiSk2");
    }

    @Test
    public void generateRequest_immutableGsiWithCrossIndexKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Arrays.asList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("gsi1")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build(),
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("gsi2")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<CrossIndexImmutable> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(ImmutableTableSchema.create(CrossIndexImmutable.class),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(2);
        
        GlobalSecondaryIndex gsi1 = request.globalSecondaryIndexes().stream()
            .filter(gsi -> "gsi1".equals(gsi.indexName()))
            .findFirst().orElse(null);
        assertThat(gsi1.keySchema().size()).isEqualTo(2);
        assertThat(gsi1.keySchema().get(0).attributeName()).isEqualTo("attr1");
        assertThat(gsi1.keySchema().get(0).keyType()).isEqualTo(HASH);
        assertThat(gsi1.keySchema().get(1).attributeName()).isEqualTo("attr2");
        assertThat(gsi1.keySchema().get(1).keyType()).isEqualTo(HASH);
        
        GlobalSecondaryIndex gsi2 = request.globalSecondaryIndexes().stream()
            .filter(gsi -> "gsi2".equals(gsi.indexName()))
            .findFirst().orElse(null);
        assertThat(gsi2.keySchema().size()).isEqualTo(2);
        assertThat(gsi2.keySchema().get(0).attributeName()).isEqualTo("attr3");
        assertThat(gsi2.keySchema().get(0).keyType()).isEqualTo(HASH);
        assertThat(gsi2.keySchema().get(1).attributeName()).isEqualTo("attr1");
        assertThat(gsi2.keySchema().get(1).keyType()).isEqualTo(RANGE);
    }

    @Test
    public void generateRequest_immutableGsiWithMixedFlattenedKeys() {
        List<EnhancedGlobalSecondaryIndex> gsiList = Collections.singletonList(
            EnhancedGlobalSecondaryIndex.builder()
                .indexName("mixed_gsi")
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .provisionedThroughput(p -> p.readCapacityUnits(1L).writeCapacityUnits(1L))
                .build());

        CreateTableOperation<MixedFlattenedImmutable> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(gsiList)
                .build());

        CreateTableRequest request = operation.generateRequest(ImmutableTableSchema.create(MixedFlattenedImmutable.class),
                                                               PRIMARY_CONTEXT, null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName()).isEqualTo("mixed_gsi");
        assertThat(gsi.keySchema().size()).isEqualTo(4);

        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames).containsExactlyInAnyOrder("rootKey1", "flatKey1");

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames).containsExactlyInAnyOrder("rootKey2", "flatKey2");
    }

    @Test
    public void generateRequest_withVectorIndex() {
        DocumentTableSchema tableSchema = DocumentTableSchema.builder()
                                                             .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                                   "id",
                                                                                   AttributeValueType.S)
                                                             .addIndexSortKey(TableMetadata.primaryIndexName(),
                                                                              "category",
                                                                              AttributeValueType.S)
                                                             .build();
        Projection projection = Projection.builder().projectionType(ProjectionType.ALL).build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("embeddings-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(1536)
                                                             .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                             .projection(projection)
                                                             .addSearchSchemaElement(b -> b.attributeName("id")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.HASH))
                                                             .addSearchSchemaElement(b -> b.attributeName("category")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.INLINE_FILTER))
                                                             .build();

        CreateTableOperation<EnhancedDocument> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        assertThat(request.vectorIndexes().size()).isEqualTo(1);
        VectorIndex sdkVectorIndex = request.vectorIndexes().get(0);
        assertThat(sdkVectorIndex.indexName()).isEqualTo("embeddings-index");
        assertThat(sdkVectorIndex.vectorAttribute().attributeName()).isEqualTo("embedding");
        assertThat(sdkVectorIndex.dimensions()).isEqualTo(1536L);
        assertThat(sdkVectorIndex.distanceFunction()).isEqualTo(VectorDistanceFunction.DOT_PRODUCT);
        assertThat(sdkVectorIndex.projection()).isEqualTo(projection);
        assertThat(sdkVectorIndex.searchSchema().size()).isEqualTo(2);
        assertThat(sdkVectorIndex.searchSchema().get(0).attributeName()).isEqualTo("id");
        assertThat(sdkVectorIndex.searchSchema().get(0).searchSchemaElementTypeAsString()).isEqualTo("HASH");
        assertThat(sdkVectorIndex.searchSchema().get(1).attributeName()).isEqualTo("category");
        assertThat(sdkVectorIndex.searchSchema().get(1).searchSchemaElementTypeAsString()).isEqualTo("INLINE_FILTER");

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("category").attributeType(ScalarAttributeType.S).build());
        assertThat(request.globalSecondaryIndexes()).isEmpty();
        assertThat(request.localSecondaryIndexes()).isEmpty();
    }

    @Test
    public void generateRequest_withVectorIndex_inlineFilterOnNonKeyAttribute() {
        StaticTableSchema<ItemWithInlineFilterAttribute> tableSchema =
            StaticTableSchema.builder(ItemWithInlineFilterAttribute.class)
                             .newItemSupplier(ItemWithInlineFilterAttribute::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ItemWithInlineFilterAttribute::getPk)
                                                               .setter(ItemWithInlineFilterAttribute::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(String.class, a -> a.name("sk")
                                                               .getter(ItemWithInlineFilterAttribute::getSk)
                                                               .setter(ItemWithInlineFilterAttribute::setSk)
                                                               .tags(primarySortKey()))
                             .addAttribute(String.class, a -> a.name("category")
                                                               .getter(ItemWithInlineFilterAttribute::getCategory)
                                                               .setter(ItemWithInlineFilterAttribute::setCategory))
                             .build();
        Projection projection = Projection.builder().projectionType(ProjectionType.ALL).build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("cosine-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(4)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(projection)
                                                             .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.HASH))
                                                             .addSearchSchemaElement(b -> b.attributeName("category")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.INLINE_FILTER))
                                                             .build();

        CreateTableOperation<ItemWithInlineFilterAttribute> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("sk").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("category").attributeType(ScalarAttributeType.S).build());
        assertThat(request.vectorIndexes().get(0).searchSchema().size()).isEqualTo(2);
        assertThat(request.vectorIndexes().get(0).searchSchema().get(1).attributeName()).isEqualTo("category");
    }

    @Test
    public void generateRequest_withDocumentTableSchema_vectorIndex_inlineFilterOnNonKeyAttribute() {
        DocumentTableSchema tableSchema = DocumentTableSchema.builder()
                                                             .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                                   "pk",
                                                                                   AttributeValueType.S)
                                                             .addIndexSortKey(TableMetadata.primaryIndexName(),
                                                                              "sk",
                                                                              AttributeValueType.S)
                                                             .build();
        Projection projection = Projection.builder().projectionType(ProjectionType.ALL).build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("cosine-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(4)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(projection)
                                                             .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.HASH))
                                                             .addSearchSchemaElement(b -> b.attributeName("category")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.INLINE_FILTER))
                                                             .build();

        CreateTableOperation<EnhancedDocument> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("sk").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("category").attributeType(ScalarAttributeType.S).build());
    }

    @Test
    public void generateRequest_withVectorIndexAndGsi() {
        Projection projection = Projection.builder().projectionType(ProjectionType.ALL).build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("embeddings-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(768)
                                                             .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                             .projection(projection)
                                                             .addSearchSchemaElement(b -> b.attributeName("id")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.HASH))
                                                             .addSearchSchemaElement(b -> b.attributeName("lsi_sort")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.INLINE_FILTER))
                                                             .build();

        CreateTableOperation<FakeItemWithIndices> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder()
                                      .globalSecondaryIndices(EnhancedGlobalSecondaryIndex.builder()
                                                                                          .indexName("gsi_1")
                                                                                          .projection(projection)
                                                                                          .provisionedThroughput(
                                                                                              ProvisionedThroughput.builder()
                                                                                                                   .readCapacityUnits(1L)
                                                                                                                   .writeCapacityUnits(1L)
                                                                                                                   .build())
                                                                                          .build())
                                      .vectorIndexes(vectorIndex)
                                      .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithIndices.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(1);
        assertThat(request.vectorIndexes().size()).isEqualTo(1);
        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("sort").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("gsi_id").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("gsi_sort").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("lsi_sort").attributeType(ScalarAttributeType.S).build());
    }

    @Test
    public void generateRequest_nullVectorIndexes_omitsFromRequest() {
        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().build());

        CreateTableRequest request = operation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);

        assertThat(request.vectorIndexes()).isEmpty();
    }

    @Test
    public void generateRequest_emptyVectorIndexes_omitsFromRequest() {
        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(Collections.emptyList()).build());

        CreateTableRequest request = operation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);

        assertThat(request.vectorIndexes()).isEmpty();
    }

    @Test
    public void generateRequest_multipleVectorIndexes() {
        DocumentTableSchema tableSchema = DocumentTableSchema.builder()
                                                             .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                                   "id",
                                                                                   AttributeValueType.S)
                                                             .build();
        EnhancedVectorIndex cosineIndex = EnhancedVectorIndex.builder()
                                                             .indexName("cosine-index")
                                                             .vectorAttributeName("embedding_a")
                                                             .dimensions(256)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                                             .build();
        EnhancedVectorIndex euclideanIndex = EnhancedVectorIndex.builder()
                                                                .indexName("euclidean-index")
                                                                .vectorAttributeName("embedding_b")
                                                                .dimensions(512)
                                                                .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                                .projection(Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build())
                                                                .build();

        CreateTableOperation<EnhancedDocument> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(cosineIndex, euclideanIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        assertThat(request.vectorIndexes().size()).isEqualTo(2);
        assertThat(request.vectorIndexes().get(0).indexName()).isEqualTo("cosine-index");
        assertThat(request.vectorIndexes().get(1).indexName()).isEqualTo("euclidean-index");
    }

    @Test
    public void generateRequest_vectorAttributeNotInAttributeDefinitions() {
        DocumentTableSchema tableSchema = DocumentTableSchema.builder()
                                                             .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                                   "id",
                                                                                   AttributeValueType.S)
                                                             .build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("vec-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(128)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                                             .build();

        CreateTableOperation<EnhancedDocument> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        Set<String> attributeNames = request.attributeDefinitions().stream()
                                            .map(AttributeDefinition::attributeName)
                                            .collect(Collectors.toSet());
        assertThat(attributeNames).doesNotContain("embedding");
    }

    @Test
    public void generateRequest_vectorIndexEmptySearchSchema() {
        DocumentTableSchema tableSchema = DocumentTableSchema.builder()
                                                             .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                                   "pk",
                                                                                   AttributeValueType.S)
                                                             .build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("no-schema-index")
                                                             .vectorAttributeName("vec")
                                                             .dimensions(64)
                                                             .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                             .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                                             .build();

        CreateTableOperation<EnhancedDocument> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        VectorIndex sdkIndex = request.vectorIndexes().get(0);
        assertThat(sdkIndex.hasSearchSchema()).isFalse();
    }

    @Test
    public void generateRequest_vectorIndexNullProjection_defaultsToAll() {
        DocumentTableSchema tableSchema = DocumentTableSchema.builder()
                                                             .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                                   "pk",
                                                                                   AttributeValueType.S)
                                                             .build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("default-proj-index")
                                                             .vectorAttributeName("vec")
                                                             .dimensions(64)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .build();

        CreateTableOperation<EnhancedDocument> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        assertThat(request.vectorIndexes().get(0).projection().projectionType()).isEqualTo(ProjectionType.ALL);
    }

    @Test
    public void generateRequest_noSearchSchema_onlyPrimaryKeysInAttributeDefinitions() {
        StaticTableSchema<ItemWithInlineFilterAttribute> tableSchema =
            StaticTableSchema.builder(ItemWithInlineFilterAttribute.class)
                             .newItemSupplier(ItemWithInlineFilterAttribute::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ItemWithInlineFilterAttribute::getPk)
                                                               .setter(ItemWithInlineFilterAttribute::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(String.class, a -> a.name("sk")
                                                               .getter(ItemWithInlineFilterAttribute::getSk)
                                                               .setter(ItemWithInlineFilterAttribute::setSk)
                                                               .tags(primarySortKey()))
                             .build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("no-schema-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(4)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                                             .build();

        CreateTableOperation<ItemWithInlineFilterAttribute> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("sk").attributeType(ScalarAttributeType.S).build());
        assertThat(request.vectorIndexes().get(0).hasSearchSchema()).isFalse();
    }

    @Test
    public void generateRequest_vectorIndexOnlyInlineFilter_mapsIndexWithoutHash() {
        StaticTableSchema<ItemWithInlineFilterAttribute> tableSchema =
            StaticTableSchema.builder(ItemWithInlineFilterAttribute.class)
                             .newItemSupplier(ItemWithInlineFilterAttribute::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ItemWithInlineFilterAttribute::getPk)
                                                               .setter(ItemWithInlineFilterAttribute::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(String.class, a -> a.name("sk")
                                                               .getter(ItemWithInlineFilterAttribute::getSk)
                                                               .setter(ItemWithInlineFilterAttribute::setSk)
                                                               .tags(primarySortKey()))
                             .addAttribute(String.class, a -> a.name("category")
                                                               .getter(ItemWithInlineFilterAttribute::getCategory)
                                                               .setter(ItemWithInlineFilterAttribute::setCategory))
                             .build();
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("inline-filter-only-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(4)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                                             .addSearchSchemaElement(b -> b.attributeName("category")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.INLINE_FILTER))
                                                             .build();

        CreateTableOperation<ItemWithInlineFilterAttribute> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        VectorIndex sdkIndex = request.vectorIndexes().get(0);
        assertThat(sdkIndex.searchSchema()).hasSize(1);
        assertThat(sdkIndex.searchSchema().get(0).attributeName()).isEqualTo("category");
        assertThat(sdkIndex.searchSchema().get(0).searchSchemaElementTypeAsString()).isEqualTo("INLINE_FILTER");
        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("sk").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("category").attributeType(ScalarAttributeType.S).build());
    }

    @Test
    public void generateRequest_keyWithNonScalarType_documentTableSchemaFallsBackToString() {
        DocumentTableSchema tableSchema = DocumentTableSchema.builder()
                                                             .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                                   "pk",
                                                                                   AttributeValueType.S)
                                                             .addIndexSortKey(TableMetadata.primaryIndexName(),
                                                                              "boolKey",
                                                                              AttributeValueType.BOOL)
                                                             .build();

        CreateTableOperation<EnhancedDocument> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().build());

        CreateTableRequest request = operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null);

        assertThat(request.attributeDefinitions()).containsExactlyInAnyOrder(
            AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName("boolKey").attributeType(ScalarAttributeType.S).build());
    }

    @Test
    public void generateRequest_vectorSearchSchema_nonScalarConverterAttribute_throwsWhenScalarTypeIsNull() {
        StaticTableSchema<ItemWithBooleanAttribute> tableSchema =
            StaticTableSchema.builder(ItemWithBooleanAttribute.class)
                             .newItemSupplier(ItemWithBooleanAttribute::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ItemWithBooleanAttribute::getPk)
                                                               .setter(ItemWithBooleanAttribute::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(Boolean.class, a -> a.name("active")
                                                                .getter(ItemWithBooleanAttribute::getActive)
                                                                .setter(ItemWithBooleanAttribute::setActive))
                             .build();

        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("test-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(4)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(Projection.builder()
                                                                                   .projectionType(ProjectionType.ALL)
                                                                                   .build())
                                                             .addSearchSchemaElement(b -> b.attributeName("active")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.INLINE_FILTER))
                                                             .build();

        CreateTableOperation<ItemWithBooleanAttribute> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        assertThatThrownBy(() -> operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attribute 'active' must be defined in the table schema as a scalar type (S, N, or B)")
            .hasMessageContaining("found: BOOL");
    }

    @Test
    public void generateRequest_vectorSearchSchema_undefinedAttribute_throwsWhenConverterIsNull() {
        StaticTableSchema<ItemWithInlineFilterAttribute> tableSchema =
            StaticTableSchema.builder(ItemWithInlineFilterAttribute.class)
                             .newItemSupplier(ItemWithInlineFilterAttribute::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(ItemWithInlineFilterAttribute::getPk)
                                                               .setter(ItemWithInlineFilterAttribute::setPk)
                                                               .tags(primaryPartitionKey()))
                             .build();

        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("test-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(4)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(Projection.builder()
                                                                                   .projectionType(ProjectionType.ALL)
                                                                                   .build())
                                                             .addSearchSchemaElement(b -> b.attributeName("unknown_attr")
                                                                                           .searchSchemaElementType(
                                                                                               SearchSchemaElementType.HASH))
                                                             .build();

        CreateTableOperation<ItemWithInlineFilterAttribute> operation = CreateTableOperation.create(
            CreateTableEnhancedRequest.builder().vectorIndexes(vectorIndex).build());

        assertThatThrownBy(() -> operation.generateRequest(tableSchema, PRIMARY_CONTEXT, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attribute 'unknown_attr' must be defined in the table schema as a scalar type (S, N, or B)")
            .hasMessageContaining("found: UNDEFINED");
    }

    static class ItemWithBooleanAttribute {
        private String pk;
        private Boolean active;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    static class ItemWithInlineFilterAttribute {
        private String pk;
        private String sk;
        private String category;

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }

        public String getSk() {
            return sk;
        }

        public void setSk(String sk) {
            this.sk = sk;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }
}
