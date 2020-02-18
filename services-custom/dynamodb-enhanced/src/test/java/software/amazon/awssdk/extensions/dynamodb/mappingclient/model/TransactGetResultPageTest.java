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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbEnhancedClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ReadModification;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class TransactGetResultPageTest {
    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private MapperExtension mockMapperExtension;

    private DynamoDbTable<FakeItem> createMappedTable(MapperExtension mapperExtension) {
        return DynamoDbEnhancedClient.builder()
                                     .dynamoDbClient(mockDynamoDbClient)
                                     .extendWith(mapperExtension)
                                     .build()
                                     .table(TABLE_NAME, FakeItem.getTableSchema());
    }

    @Test
    public void noExtension_mapsToItem() {
        FakeItem fakeItem = FakeItem.createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        TransactGetResultPage transactGetResultPage = TransactGetResultPage.create(fakeItemMap);

        assertThat(transactGetResultPage.getItem(createMappedTable(null)), is(fakeItem));
    }

    @Test
    public void extension_mapsToItem() {
        FakeItem fakeItem = FakeItem.createUniqueFakeItem();
        FakeItem fakeItem2 = FakeItem.createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        Map<String, AttributeValue> fakeItemMap2 = FakeItem.getTableSchema().itemToMap(fakeItem2, true);
        when(mockMapperExtension.afterRead(anyMap(), any(), any()))
            .thenReturn(ReadModification.builder().transformedItem(fakeItemMap2).build());

        TransactGetResultPage transactGetResultPage = TransactGetResultPage.create(fakeItemMap);

        DynamoDbTable<FakeItem> mappedTable = createMappedTable(mockMapperExtension);
        assertThat(transactGetResultPage.getItem(mappedTable), is(fakeItem2));
        verify(mockMapperExtension).afterRead(fakeItemMap,
                                              OperationContext.create(mappedTable.tableName()),
                                              FakeItem.getTableMetadata());
    }

    @Test
    public void nullMapReturnsNullItem() {
        TransactGetResultPage transactGetResultPage = TransactGetResultPage.create(null);

        assertThat(transactGetResultPage.getItem(createMappedTable(null)), is(nullValue()));
    }

    @Test
    public void emptyMapReturnsNullItem() {
        TransactGetResultPage transactGetResultPage = TransactGetResultPage.create(emptyMap());

        assertThat(transactGetResultPage.getItem(createMappedTable(null)), is(nullValue()));
    }

}
