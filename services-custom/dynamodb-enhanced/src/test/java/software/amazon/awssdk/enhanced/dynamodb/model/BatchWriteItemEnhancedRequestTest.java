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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

@RunWith(MockitoJUnitRunner.class)
public class BatchWriteItemEnhancedRequestTest {

    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<FakeItem> fakeItemMappedTable;


    @Before
    public void setupMappedTables() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(mockDynamoDbClient).build();
        fakeItemMappedTable = enhancedClient.table(TABLE_NAME, FakeItem.getTableSchema());
    }


    @Test
    public void builder_minimal() {
        BatchWriteItemEnhancedRequest builtObject = BatchWriteItemEnhancedRequest.builder().build();

        assertThat(builtObject.writeBatches(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        WriteBatch writeBatch = WriteBatch.builder(FakeItem.class)
                                          .mappedTableResource(fakeItemMappedTable)
                                          .addDeleteItem(r -> r.key(k -> k.partitionValue("key")))
                                          .build();

        BatchWriteItemEnhancedRequest builtObject = BatchWriteItemEnhancedRequest.builder()
                                                                                 .writeBatches(writeBatch)
                                                                                 .build();

        assertThat(builtObject.writeBatches(), is(Collections.singletonList(writeBatch)));
    }

    @Test
    public void builder_add_single() {
        WriteBatch writeBatch = WriteBatch.builder(FakeItem.class)
                                          .mappedTableResource(fakeItemMappedTable)
                                          .addDeleteItem(r -> r.key(k -> k.partitionValue("key")))
                                          .build();

        BatchWriteItemEnhancedRequest builtObject = BatchWriteItemEnhancedRequest.builder()
                                                                                 .addWriteBatch(writeBatch)
                                                                                 .build();

        assertThat(builtObject.writeBatches(), is(Collections.singletonList(writeBatch)));
    }

    @Test
    public void test_equalsAndHashCode_when_returnConsumedCapacityIsDifferent() {
        WriteBatch writeBatch = WriteBatch.builder(FakeItem.class)
                                          .mappedTableResource(fakeItemMappedTable)
                                          .addDeleteItem(r -> r.key(k -> k.partitionValue("key")))
                                          .build();

        BatchWriteItemEnhancedRequest builtObject1 = BatchWriteItemEnhancedRequest.builder()
                                                                                  .writeBatches(writeBatch)
                                                                                  .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                                  .build();

        BatchWriteItemEnhancedRequest builtObject2 = BatchWriteItemEnhancedRequest.builder()
                                                                                  .writeBatches(writeBatch)
                                                                                  .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                                                                  .build();


        assertThat(builtObject1, not(equalTo(builtObject2)));
        assertThat(builtObject1.hashCode(), not(equalTo(builtObject2.hashCode())));
    }

    @Test
    public void test_returnConsumedCapacity_unknownToSdkVersion() {
        String newValue = UUID.randomUUID().toString();
        WriteBatch writeBatch = WriteBatch.builder(FakeItem.class)
                                          .mappedTableResource(fakeItemMappedTable)
                                          .addDeleteItem(r -> r.key(k -> k.partitionValue("key")))
                                          .build();

        BatchWriteItemEnhancedRequest builtObject = BatchWriteItemEnhancedRequest.builder()
                                                                                 .writeBatches(writeBatch)
                                                                                 .returnConsumedCapacity(newValue)
                                                                                 .build();

        // Assert that new value resolves to correct enum value
        assertThat(builtObject.returnConsumedCapacity(), equalTo(ReturnConsumedCapacity.UNKNOWN_TO_SDK_VERSION));
    }

    @Test
    public void toBuilder() {
        BatchWriteItemEnhancedRequest builtObject = BatchWriteItemEnhancedRequest.builder().build();

        BatchWriteItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

}
