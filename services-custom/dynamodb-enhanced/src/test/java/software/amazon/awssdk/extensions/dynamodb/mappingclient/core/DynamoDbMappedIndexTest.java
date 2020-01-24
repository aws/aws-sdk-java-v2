/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.IndexOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithIndices;

@RunWith(MockitoJUnitRunner.class)
public class DynamoDbMappedIndexTest {
    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private IndexOperation<FakeItem, Object, Object, FakeItem> mockIndexOperation;

    @Mock
    private MapperExtension mockMapperExtension;

    @Test
    public void execute() {
        FakeItem expectedOutput = FakeItem.createUniqueFakeItem();
        DynamoDbMappedIndex<FakeItem> dynamoDbMappedIndex = new DynamoDbMappedIndex<>(mockDynamoDbClient,
                                                                                      mockMapperExtension,
                                                                                      FakeItem.getTableSchema(),
                                                                                      "test_table",
                                                                                      "test_index");
        when(mockIndexOperation.executeOnSecondaryIndex(any(), any(), any(), any(), any())).thenReturn(expectedOutput);

        FakeItem actualOutput = dynamoDbMappedIndex.execute(mockIndexOperation);

        assertThat(actualOutput, is(expectedOutput));
        verify(mockIndexOperation).executeOnSecondaryIndex(FakeItem.getTableSchema(),
                                                           "test_table",
                                                           "test_index",
                                                           mockMapperExtension,
                                                           mockDynamoDbClient);
    }

    @Test
    public void keyFrom_secondaryIndex_partitionAndSort() {
        FakeItemWithIndices item = FakeItemWithIndices.createUniqueFakeItemWithIndices();
        DynamoDbMappedIndex<FakeItemWithIndices> dynamoDbMappedIndex =
            new DynamoDbMappedIndex<>(mockDynamoDbClient,
                                      mockMapperExtension,
                                      FakeItemWithIndices.getTableSchema(),
                                      "test_table",
                                      "gsi_1");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getGsiId())));
        assertThat(key.sortKeyValue(), is(Optional.of(stringValue(item.getGsiSort()))));
    }

    @Test
    public void keyFrom_secondaryIndex_partitionOnly() {
        FakeItemWithIndices item = FakeItemWithIndices.createUniqueFakeItemWithIndices();
        DynamoDbMappedIndex<FakeItemWithIndices> dynamoDbMappedIndex =
            new DynamoDbMappedIndex<>(mockDynamoDbClient,
                                      mockMapperExtension,
                                      FakeItemWithIndices.getTableSchema(),
                                      "test_table",
                                      "gsi_2");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getGsiId())));
        assertThat(key.sortKeyValue(), is(Optional.empty()));
    }
}
