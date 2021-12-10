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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;

@RunWith(MockitoJUnitRunner.class)
public class DescribeTableOperationTest {

    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
            DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final OperationContext GSI_1_CONTEXT =
            DefaultOperationContext.create(TABLE_NAME, "gsi_1");

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Test
    public void getServiceCall_makesTheRightCall() {
        DescribeTableOperation<FakeItem> operation = DescribeTableOperation.create();
        DescribeTableRequest describeTableRequest = DescribeTableRequest.builder().build();
        operation.serviceCall(mockDynamoDbClient).apply(describeTableRequest);
        verify(mockDynamoDbClient).describeTable(same(describeTableRequest));
    }


    @Test
    public void generateRequest_from_DescribeTableOperation() {
        DescribeTableOperation<FakeItemWithSort> describeTableOperation = DescribeTableOperation.create();
        DescribeTableRequest describeTableRequest = describeTableOperation
                .generateRequest(FakeItemWithSort.getTableSchema(),
                        PRIMARY_CONTEXT,
                        null);
        assertThat(describeTableRequest, is(describeTableRequest.builder().tableName(TABLE_NAME).build()));
    }

    @Test
    public void generateRequest_doesNotWorkForIndex() {
        DescribeTableOperation<FakeItemWithIndices> operation = DescribeTableOperation.create();
        assertThatThrownBy(() -> operation.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null))
            .hasMessageContaining("cannot be executed against a secondary index");
    }

}
