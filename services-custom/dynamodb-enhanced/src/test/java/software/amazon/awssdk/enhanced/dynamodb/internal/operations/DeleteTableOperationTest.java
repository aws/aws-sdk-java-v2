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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DeleteTableOperationTest {

    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
            DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final OperationContext GSI_1_CONTEXT =
            DefaultOperationContext.create(TABLE_NAME, "gsi_1");

    @Mock
    private DynamoDbClient mockDynamoDbClient;


    @Test
    public void getServiceCall_makesTheRightCall() {
        DeleteTableOperation<FakeItem> operation = DeleteTableOperation.create();
        DeleteTableRequest deleteTableRequest = DeleteTableRequest.builder().build();
        operation.serviceCall(mockDynamoDbClient).apply(deleteTableRequest);
        verify(mockDynamoDbClient).deleteTable(same(deleteTableRequest));
    }


    @Test
    public void generateRequest_from_deleteTableOperation() {
        DeleteTableOperation<FakeItemWithSort> deleteTableOperation = DeleteTableOperation.create();
        final DeleteTableRequest deleteTableRequest = deleteTableOperation
                .generateRequest(FakeItemWithSort.getTableSchema(),
                        PRIMARY_CONTEXT,
                        null);
        assertThat(deleteTableRequest, is(DeleteTableRequest.builder().tableName(TABLE_NAME).build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_doesNotWorkForIndex() {
        DeleteTableOperation<FakeItemWithIndices> operation = DeleteTableOperation.create();
        operation.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null);
    }

}
