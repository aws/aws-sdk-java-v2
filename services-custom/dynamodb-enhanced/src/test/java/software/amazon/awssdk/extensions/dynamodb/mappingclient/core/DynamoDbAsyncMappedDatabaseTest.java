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
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.DatabaseOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@RunWith(MockitoJUnitRunner.class)
public class DynamoDbAsyncMappedDatabaseTest {
    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;
    @Mock
    private MapperExtension mockMapperExtension;
    @Mock
    private DatabaseOperation<?, ?, String> mockDatabaseOperation;
    @Mock
    private TableSchema<Object> mockTableSchema;

    @InjectMocks
    private DynamoDbAsyncMappedDatabase dynamoDbAsyncMappedDatabase;

    @Test
    public void execute() {
        when(mockDatabaseOperation.executeAsync(any(), any())).thenReturn(CompletableFuture.completedFuture("test"));

        String result = dynamoDbAsyncMappedDatabase.execute(mockDatabaseOperation).join();

        assertThat(result, is("test"));
        verify(mockDatabaseOperation).executeAsync(mockDynamoDbAsyncClient, mockMapperExtension);
    }

    @Test
    public void table() {
        DynamoDbAsyncMappedTable<Object> mappedTable = dynamoDbAsyncMappedDatabase.table("table-name", mockTableSchema);

        assertThat(mappedTable.dynamoDbClient(), is(mockDynamoDbAsyncClient));
        assertThat(mappedTable.mapperExtension(), is(mockMapperExtension));
        assertThat(mappedTable.tableSchema(), is(mockTableSchema));
        assertThat(mappedTable.tableName(), is("table-name"));
    }

    @Test
    public void builder_minimal() {
        DynamoDbAsyncMappedDatabase builtObject =
            DynamoDbAsyncMappedDatabase.builder()
                                       .dynamoDbClient(mockDynamoDbAsyncClient)
                                       .build();

        assertThat(builtObject.dynamoDbAsyncClient(), is(mockDynamoDbAsyncClient));
        assertThat(builtObject.mapperExtension(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        DynamoDbAsyncMappedDatabase builtObject =
            DynamoDbAsyncMappedDatabase.builder()
                                       .dynamoDbClient(mockDynamoDbAsyncClient)
                                       .extendWith(mockMapperExtension)
                                       .build();

        assertThat(builtObject.dynamoDbAsyncClient(), is(mockDynamoDbAsyncClient));
        assertThat(builtObject.mapperExtension(), is(mockMapperExtension));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_missingDynamoDbClient() {
        DynamoDbAsyncMappedDatabase.builder().extendWith(mockMapperExtension).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_extraExtension() {
        DynamoDbAsyncMappedDatabase.builder()
                                   .dynamoDbClient(mockDynamoDbAsyncClient)
                                   .extendWith(mockMapperExtension)
                                   .extendWith(mock(MapperExtension.class))
                                   .build();
    }

    @Test
    public void toBuilder() {
        DynamoDbAsyncMappedDatabase copiedObject = dynamoDbAsyncMappedDatabase.toBuilder().build();

        assertThat(copiedObject, is(dynamoDbAsyncMappedDatabase));
    }
}