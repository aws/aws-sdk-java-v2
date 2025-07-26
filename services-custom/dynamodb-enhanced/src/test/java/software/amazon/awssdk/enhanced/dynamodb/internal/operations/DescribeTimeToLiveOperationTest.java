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
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveRequest;

@RunWith(MockitoJUnitRunner.class)
public class DescribeTimeToLiveOperationTest {

    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Test
    public void getServiceCall_makesTheRightCall() {
        DescribeTimeToLiveOperation<FakeItem> operation = DescribeTimeToLiveOperation.create();
        DescribeTimeToLiveRequest describeTimeToLiveRequest = DescribeTimeToLiveRequest.builder().build();
        operation.serviceCall(mockDynamoDbClient).apply(describeTimeToLiveRequest);
        verify(mockDynamoDbClient).describeTimeToLive(same(describeTimeToLiveRequest));
    }


    @Test
    public void generateRequest_from_DescribeTimeToLiveOperation() {
        DescribeTimeToLiveOperation<FakeItemWithSort> describeTimeToLiveOperation = DescribeTimeToLiveOperation.create();
        DescribeTimeToLiveRequest describeTimeToLiveRequest = describeTimeToLiveOperation
            .generateRequest(FakeItemWithSort.getTableSchema(),
                             PRIMARY_CONTEXT,
                             null);
        assertThat(describeTimeToLiveRequest, is(DescribeTimeToLiveRequest.builder().tableName(TABLE_NAME).build()));
    }
}
