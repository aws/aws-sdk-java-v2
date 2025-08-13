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

package software.amazon.awssdk.enhanced.dynamodb.internal.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbIndexTest {
    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void keyFrom_secondaryIndex_partitionAndSort() {
        DefaultDynamoDbTable<FakeItemWithIndices> table = createTable();

        FakeItemWithIndices item = FakeItemWithIndices.createUniqueFakeItemWithIndices();
        DefaultDynamoDbIndex<FakeItemWithIndices> dynamoDbMappedIndex =
            new DefaultDynamoDbIndex<>(table, "gsi_1");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getGsiId())));
        assertThat(key.sortKeyValue(), is(Optional.of(stringValue(item.getGsiSort()))));
    }

    @Test
    public void keyFrom_secondaryIndex_partitionOnly() {
        DefaultDynamoDbTable<FakeItemWithIndices> table = createTable();

        FakeItemWithIndices item = FakeItemWithIndices.createUniqueFakeItemWithIndices();
        DefaultDynamoDbIndex<FakeItemWithIndices> dynamoDbMappedIndex =
            new DefaultDynamoDbIndex<>(table, "gsi_2");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getGsiId())));
        assertThat(key.sortKeyValue(), is(Optional.empty()));
    }

    @Test
    public void query_consistentReadNotSetOnTableOrRequest_shouldDefaultToNull() {
        DefaultDynamoDbTable<FakeItemWithIndices> table = createTable();
        DefaultDynamoDbIndex<FakeItemWithIndices> dynamoDbMappedIndex = new DefaultDynamoDbIndex<>(table, "gsi_2");
        dynamoDbMappedIndex.query(q -> q.queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue("val").build())));

        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(mockDynamoDbClient).queryPaginator(captor.capture());

        QueryRequest actualRequest = captor.getValue();
        assertThat(actualRequest.consistentRead(), is(nullValue()));
    }

    @Test
    public void scan_consistentReadSetOnTableNotSetOnRequest_shouldUseTableValue() {
        DefaultDynamoDbTable<FakeItemWithIndices> table =  new DefaultDynamoDbTable<>(mockDynamoDbClient,
                                                                                      mockDynamoDbEnhancedClientExtension,
                                                                                      FakeItemWithIndices.getTableSchema(),
                                                                                      "test-table",
                                                                                      true);
        DefaultDynamoDbIndex<FakeItemWithIndices> dynamoDbMappedIndex = new DefaultDynamoDbIndex<>(table, "gsi_2");
        dynamoDbMappedIndex.scan();

        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(mockDynamoDbClient).scanPaginator(captor.capture());

        ScanRequest actualRequest = captor.getValue();
        assertThat(actualRequest.consistentRead(), is(true));
    }

    @Test
    public void scan_consistentReadSetOnTableAndRequest_shouldUseRequestValue() {
        DefaultDynamoDbTable<FakeItemWithIndices> table =  new DefaultDynamoDbTable<>(mockDynamoDbClient,
                                                                                      mockDynamoDbEnhancedClientExtension,
                                                                                      FakeItemWithIndices.getTableSchema(),
                                                                                      "test-table",
                                                                                      true);
        DefaultDynamoDbIndex<FakeItemWithIndices> dynamoDbMappedIndex = new DefaultDynamoDbIndex<>(table, "gsi_2");
        dynamoDbMappedIndex.scan(s -> s.consistentRead(false));

        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(mockDynamoDbClient).scanPaginator(captor.capture());

        ScanRequest actualRequest = captor.getValue();
        assertThat(actualRequest.consistentRead(), is(false));
    }

    private DefaultDynamoDbTable<FakeItemWithIndices> createTable() {
        return new DefaultDynamoDbTable<>(mockDynamoDbClient,
                                          mockDynamoDbEnhancedClientExtension,
                                          FakeItemWithIndices.getTableSchema(), "test-table");
    }
}
