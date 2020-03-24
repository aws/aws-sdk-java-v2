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
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbIndexTest {
    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void keyFrom_secondaryIndex_partitionAndSort() {
        FakeItemWithIndices item = FakeItemWithIndices.createUniqueFakeItemWithIndices();
        DefaultDynamoDbIndex<FakeItemWithIndices> dynamoDbMappedIndex =
            new DefaultDynamoDbIndex<>(mockDynamoDbClient,
                                       mockDynamoDbEnhancedClientExtension,
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
        DefaultDynamoDbIndex<FakeItemWithIndices> dynamoDbMappedIndex =
            new DefaultDynamoDbIndex<>(mockDynamoDbClient,
                                       mockDynamoDbEnhancedClientExtension,
                                       FakeItemWithIndices.getTableSchema(),
                                       "test_table",
                                       "gsi_2");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getGsiId())));
        assertThat(key.sortKeyValue(), is(Optional.empty()));
    }
}
