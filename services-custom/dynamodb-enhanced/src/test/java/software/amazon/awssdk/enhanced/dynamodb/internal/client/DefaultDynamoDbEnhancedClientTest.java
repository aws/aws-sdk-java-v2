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
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbEnhancedClientTest {
    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Mock
    private TableSchema<Object> mockTableSchema;

    @InjectMocks
    private DefaultDynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Test
    public void table() {
        DefaultDynamoDbTable<Object> mappedTable = dynamoDbEnhancedClient.table("table-name", mockTableSchema);

        assertThat(mappedTable.dynamoDbClient(), is(mockDynamoDbClient));
        assertThat(mappedTable.mapperExtension(), is(mockDynamoDbEnhancedClientExtension));
        assertThat(mappedTable.tableSchema(), is(mockTableSchema));
        assertThat(mappedTable.tableName(), is("table-name"));
    }

    @Test
    public void builder_minimal() {
        DefaultDynamoDbEnhancedClient builtObject = DefaultDynamoDbEnhancedClient.builder()
                                                                                 .dynamoDbClient(mockDynamoDbClient)
                                                                                 .build();

        assertThat(builtObject.dynamoDbClient(), is(mockDynamoDbClient));
        assertThat(builtObject.mapperExtension(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        DefaultDynamoDbEnhancedClient builtObject = DefaultDynamoDbEnhancedClient.builder()
                                                                                 .dynamoDbClient(mockDynamoDbClient)
                                                                                 .extendWith(mockDynamoDbEnhancedClientExtension)
                                                                                 .build();

        assertThat(builtObject.dynamoDbClient(), is(mockDynamoDbClient));
        assertThat(builtObject.mapperExtension(), is(mockDynamoDbEnhancedClientExtension));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_missingDynamoDbClient() {
        DefaultDynamoDbEnhancedClient.builder().extendWith(mockDynamoDbEnhancedClientExtension).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_extraExtension() {
        DefaultDynamoDbEnhancedClient.builder()
                                     .dynamoDbClient(mockDynamoDbClient)
                                     .extendWith(mockDynamoDbEnhancedClientExtension)
                                     .extendWith(mock(DynamoDbEnhancedClientExtension.class))
                                     .build();
    }

    @Test
    public void toBuilder() {
        DefaultDynamoDbEnhancedClient copiedObject = dynamoDbEnhancedClient.toBuilder().build();

        assertThat(copiedObject, is(dynamoDbEnhancedClient));
    }
}
