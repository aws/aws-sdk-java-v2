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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.ChainExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbEnhancedAsyncClientTest {
    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;
    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;
    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension2;
    @Mock
    private TableSchema<Object> mockTableSchema;

    private DefaultDynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Before
    public void initializeClient() {
        this.dynamoDbEnhancedAsyncClient =
            DefaultDynamoDbEnhancedAsyncClient.builder()
                                              .dynamoDbClient(mockDynamoDbAsyncClient)
                                              .extensions(mockDynamoDbEnhancedClientExtension)
                                              .build();
    }

    @Test
    public void table() {
        DefaultDynamoDbAsyncTable<Object> mappedTable = dynamoDbEnhancedAsyncClient.table("table-name", mockTableSchema);

        assertThat(mappedTable.dynamoDbClient(), is(mockDynamoDbAsyncClient));
        assertThat(mappedTable.mapperExtension(), is(mockDynamoDbEnhancedClientExtension));
        assertThat(mappedTable.tableSchema(), is(mockTableSchema));
        assertThat(mappedTable.tableName(), is("table-name"));
    }

    @Test
    public void builder_minimal() {
        DefaultDynamoDbEnhancedAsyncClient builtObject =
            DefaultDynamoDbEnhancedAsyncClient.builder()
                                              .dynamoDbClient(mockDynamoDbAsyncClient)
                                              .build();

        assertThat(builtObject.dynamoDbAsyncClient(), is(mockDynamoDbAsyncClient));
        assertThat(builtObject.mapperExtension(), instanceOf(VersionedRecordExtension.class));
    }

    @Test
    public void builder_maximal() {
        DefaultDynamoDbEnhancedAsyncClient builtObject =
            DefaultDynamoDbEnhancedAsyncClient.builder()
                                              .dynamoDbClient(mockDynamoDbAsyncClient)
                                              .extensions(mockDynamoDbEnhancedClientExtension)
                                              .build();

        assertThat(builtObject.dynamoDbAsyncClient(), is(mockDynamoDbAsyncClient));
        assertThat(builtObject.mapperExtension(), is(mockDynamoDbEnhancedClientExtension));
    }

    @Test
    public void builder_multipleExtensions_varargs() {
        DefaultDynamoDbEnhancedAsyncClient builtObject =
            DefaultDynamoDbEnhancedAsyncClient.builder()
                                         .dynamoDbClient(mockDynamoDbAsyncClient)
                                         .extensions(mockDynamoDbEnhancedClientExtension, mockDynamoDbEnhancedClientExtension2)
                                         .build();

        assertThat(builtObject.dynamoDbAsyncClient(), is(mockDynamoDbAsyncClient));
        assertThat(builtObject.mapperExtension(), instanceOf(ChainExtension.class));
    }

    @Test
    public void builder_multipleExtensions_list() {
        DefaultDynamoDbEnhancedAsyncClient builtObject =
            DefaultDynamoDbEnhancedAsyncClient.builder()
                                         .dynamoDbClient(mockDynamoDbAsyncClient)
                                         .extensions(Arrays.asList(mockDynamoDbEnhancedClientExtension, mockDynamoDbEnhancedClientExtension2))
                                         .build();

        assertThat(builtObject.dynamoDbAsyncClient(), is(mockDynamoDbAsyncClient));
        assertThat(builtObject.mapperExtension(), instanceOf(ChainExtension.class));
    }

    @Test
    public void toBuilder() {
        DefaultDynamoDbEnhancedAsyncClient copiedObject = dynamoDbEnhancedAsyncClient.toBuilder().build();

        assertThat(copiedObject, is(dynamoDbEnhancedAsyncClient));
    }
}
