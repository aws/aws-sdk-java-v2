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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.HASH;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithBinaryKey;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithByteBufferKey;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithCompositeGsi;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithFlattenedGsi;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithMixedCompositeGsi;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithNumericSort;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CompositeMetadataImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.CrossIndexImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.MixedFlattenedImmutable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
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



        assertThat(request.tableName(), is(TABLE_NAME));
        assertThat(request.keySchema(), containsInAnyOrder(KeySchemaElement.builder()
                                                                           .attributeName("id")
                                                                           .keyType(HASH)
                                                                           .build(),
                                                           KeySchemaElement.builder()
                                                                           .attributeName("sort")
                                                                           .keyType(RANGE)
                                                                           .build()));
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
        assertThat(request.globalSecondaryIndexes(), containsInAnyOrder(matchesGsi(expectedGsi1),
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
        assertThat(request.localSecondaryIndexes(), containsInAnyOrder(expectedLsi));
        assertThat(request.attributeDefinitions(), containsInAnyOrder(
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
                               .build()));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex globalSecondaryIndex =
            request.globalSecondaryIndexes().get(0);

        assertThat(globalSecondaryIndex.indexName(), is("lsi_1"));
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

        assertThat(request.attributeDefinitions(), containsInAnyOrder(attributeDefinition1, attributeDefinition2));
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

        assertThat(request.billingMode(), is(BillingMode.PROVISIONED));
        assertThat(request.provisionedThroughput(), is(provisionedThroughput));
    }

    @Test
    public void generateRequest_withNoProvisionedThroughput() {
        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder().build());

        CreateTableRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.billingMode(), is(BillingMode.PAY_PER_REQUEST));
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

        assertThat(request.streamSpecification(), is(streamSpecification));
    }

    @Test
    public void generateRequest_withNoStreamSpecification() {
        CreateTableOperation<FakeItem> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder().build());

        CreateTableRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.streamSpecification(), is(nullValue()));
    }


    @Test
    public void generateRequest_withNumericKey() {
        CreateTableOperation<FakeItemWithNumericSort> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                                                                                                                        .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithNumericSort.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.tableName(), is(TABLE_NAME));
        assertThat(request.keySchema(), containsInAnyOrder(KeySchemaElement.builder()
                                                                           .attributeName("id")
                                                                           .keyType(HASH)
                                                                           .build(),
                                                           KeySchemaElement.builder()
                                                                           .attributeName("sort")
                                                                           .keyType(RANGE)
                                                                           .build()));

        assertThat(request.globalSecondaryIndexes(), is(DefaultSdkAutoConstructList.getInstance()));
        assertThat(request.localSecondaryIndexes(), is(DefaultSdkAutoConstructList.getInstance()));

        assertThat(request.attributeDefinitions(), containsInAnyOrder(
            AttributeDefinition.builder()
                               .attributeName("id")
                               .attributeType(ScalarAttributeType.S)
                               .build(),
            AttributeDefinition.builder()
                               .attributeName("sort")
                               .attributeType(ScalarAttributeType.N)
                               .build()));
    }

    @Test
    public void generateRequest_withBinaryKey() {
        CreateTableOperation<FakeItemWithBinaryKey> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                                                                                                                      .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithBinaryKey.getTableSchema(),
                                                               PRIMARY_CONTEXT,
                                                               null);

        assertThat(request.tableName(), is(TABLE_NAME));
        assertThat(request.keySchema(), containsInAnyOrder(KeySchemaElement.builder()
                                                                           .attributeName("id")
                                                                           .keyType(HASH)
                                                                           .build()));

        assertThat(request.globalSecondaryIndexes(), is(empty()));
        assertThat(request.localSecondaryIndexes(), is(empty()));

        assertThat(request.attributeDefinitions(), containsInAnyOrder(
            AttributeDefinition.builder()
                               .attributeName("id")
                               .attributeType(ScalarAttributeType.B)
                               .build()));
    }

    @Test
    public void generateRequest_withByteBufferKey() {
        CreateTableOperation<FakeItemWithByteBufferKey> operation = CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                .build());

        CreateTableRequest request = operation.generateRequest(FakeItemWithByteBufferKey.getTableSchema(),
                PRIMARY_CONTEXT,
                null);

        assertThat(request.tableName(), is(TABLE_NAME));
        assertThat(request.keySchema(), containsInAnyOrder(KeySchemaElement.builder()
                .attributeName("id")
                .keyType(HASH)
                .build()));

        assertThat(request.globalSecondaryIndexes(), is(empty()));
        assertThat(request.localSecondaryIndexes(), is(empty()));

        assertThat(request.attributeDefinitions(), containsInAnyOrder(
                AttributeDefinition.builder()
                        .attributeName("id")
                        .attributeType(ScalarAttributeType.B)
                        .build()));
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

        assertThat(actualResponse, sameInstance(expectedResponse));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("gsi_1"));
        assertThat(gsi.keySchema().size(), is(2));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);

        assertThat(gsi.indexName(), is("composite_gsi"));
        assertThat(gsi.keySchema().size(), is(4));

        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames, containsInAnyOrder("gsi_pk1", "gsi_pk2"));

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames, containsInAnyOrder("gsi_sk1", "gsi_sk2"));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("flatten_partition_gsi"));
        assertThat(gsi.keySchema().size(), is(1));
        assertThat(gsi.keySchema().get(0).attributeName(), is("gsiPartitionKey"));
        assertThat(gsi.keySchema().get(0).keyType(), is(HASH));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("flatten_sort_gsi"));
        assertThat(gsi.keySchema().size(), is(2));
        assertThat(gsi.keySchema().get(0).attributeName(), is("id"));
        assertThat(gsi.keySchema().get(0).keyType(), is(HASH));
        assertThat(gsi.keySchema().get(1).attributeName(), is("gsiSortKey"));
        assertThat(gsi.keySchema().get(1).keyType(), is(RANGE));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("flatten_mixed_gsi"));
        assertThat(gsi.keySchema().size(), is(2));
        
        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames, containsInAnyOrder("gsiMixedPartitionKey"));

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames, containsInAnyOrder("gsiMixedSortKey"));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("flatten_both_gsi"));
        assertThat(gsi.keySchema().size(), is(2));
        
        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames, containsInAnyOrder("gsiBothSortKey"));

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames, containsInAnyOrder("gsiBothSortKey"));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("mixed_partition_gsi"));
        assertThat(gsi.keySchema().size(), is(4));
        
        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames, containsInAnyOrder("rootPartitionKey1", "rootPartitionKey2", "flattenedPartitionKey1", "flattenedPartitionKey2"));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("mixed_sort_gsi"));
        assertThat(gsi.keySchema().size(), is(6));

        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames, containsInAnyOrder("rootPartitionKey1", "rootPartitionKey2"));

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames, containsInAnyOrder("rootSortKey1", "rootSortKey2", "flattenedSortKey1", "flattenedSortKey2"));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("full_mixed_gsi"));
        assertThat(gsi.keySchema().size(), is(8));
        
        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames, containsInAnyOrder("rootPartitionKey1", "rootPartitionKey2", "flattenedPartitionKey1", "flattenedPartitionKey2"));

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames, containsInAnyOrder("rootSortKey1", "rootSortKey2", "flattenedSortKey1", "flattenedSortKey2"));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("gsi1"));
        assertThat(gsi.keySchema().size(), is(4));

        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames, containsInAnyOrder("gsiPk1", "gsiPk2"));

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames, containsInAnyOrder("gsiSk1", "gsiSk2"));
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

        assertThat(request.globalSecondaryIndexes().size(), is(2));
        
        GlobalSecondaryIndex gsi1 = request.globalSecondaryIndexes().stream()
            .filter(gsi -> "gsi1".equals(gsi.indexName()))
            .findFirst().orElse(null);
        assertThat(gsi1.keySchema().size(), is(2));
        assertThat(gsi1.keySchema().get(0).attributeName(), is("attr1"));
        assertThat(gsi1.keySchema().get(0).keyType(), is(HASH));
        assertThat(gsi1.keySchema().get(1).attributeName(), is("attr2"));
        assertThat(gsi1.keySchema().get(1).keyType(), is(HASH));
        
        GlobalSecondaryIndex gsi2 = request.globalSecondaryIndexes().stream()
            .filter(gsi -> "gsi2".equals(gsi.indexName()))
            .findFirst().orElse(null);
        assertThat(gsi2.keySchema().size(), is(2));
        assertThat(gsi2.keySchema().get(0).attributeName(), is("attr3"));
        assertThat(gsi2.keySchema().get(0).keyType(), is(HASH));
        assertThat(gsi2.keySchema().get(1).attributeName(), is("attr1"));
        assertThat(gsi2.keySchema().get(1).keyType(), is(RANGE));
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

        assertThat(request.globalSecondaryIndexes().size(), is(1));
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);
        assertThat(gsi.indexName(), is("mixed_gsi"));
        assertThat(gsi.keySchema().size(), is(4));

        Set<String> partitionKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == HASH)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(partitionKeyNames, containsInAnyOrder("rootKey1", "flatKey1"));

        Set<String> sortKeyNames = gsi.keySchema().stream()
            .filter(key -> key.keyType() == RANGE)
            .map(KeySchemaElement::attributeName)
            .collect(Collectors.toSet());
        assertThat(sortKeyNames, containsInAnyOrder("rootKey2", "flatKey2"));
    }
}
