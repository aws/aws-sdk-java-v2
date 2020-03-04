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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithBinaryKey;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithNumericSort;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@RunWith(MockitoJUnitRunner.class)
public class CreateTableOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        OperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        OperationContext.create(TABLE_NAME, "gsi_1");

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


        List<GlobalSecondaryIndex> globalSecondaryIndexList = Arrays.asList(
            GlobalSecondaryIndex.create("gsi_1", projection1, provisionedThroughput1),
            GlobalSecondaryIndex.create("gsi_2", projection2, provisionedThroughput2));

        CreateTableOperation<FakeItemWithIndices> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder()
                                                                  .globalSecondaryIndices(globalSecondaryIndexList)
                                                                  .localSecondaryIndices(Collections.singletonList(
                                                                      LocalSecondaryIndex.create("lsi_1", projection3)))
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

        List<GlobalSecondaryIndex> invalidGsiList = Collections.singletonList(
            GlobalSecondaryIndex.create("invalid",
                                        Projection.builder().projectionType(ProjectionType.ALL).build(),
                                        provisionedThroughput));

        CreateTableOperation<FakeItem> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder().globalSecondaryIndices(invalidGsiList).build());

        operation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_invalidGsiAsLsiReference() {
        List<LocalSecondaryIndex> invalidGsiList = Collections.singletonList(
            LocalSecondaryIndex.create("gsi_1", Projection.builder().projectionType(ProjectionType.ALL).build()));

        CreateTableOperation<FakeItemWithIndices> operation =
            CreateTableOperation.create(CreateTableEnhancedRequest.builder().localSecondaryIndices(invalidGsiList).build());

        operation.generateRequest(FakeItemWithIndices.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void generateRequest_validLsiAsGsiReference() {
        List<GlobalSecondaryIndex> validLsiList = Collections.singletonList(
            GlobalSecondaryIndex.create("lsi_1",
                                        Projection.builder().projectionType(ProjectionType.ALL).build(),
                                        ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build()));

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
        List<LocalSecondaryIndex> invalidLsiList = Collections.singletonList(
            LocalSecondaryIndex.create("invalid", Projection.builder().projectionType(ProjectionType.ALL).build()));

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
}
