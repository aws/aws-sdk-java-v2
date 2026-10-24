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
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithTTL;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveResponse;

@RunWith(MockitoJUnitRunner.class)
public class UpdateTimeToLiveOperationTest {

    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final TableSchema<RecordWithTTL> TABLE_SCHEMA = TableSchema.fromClass(RecordWithTTL.class);

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        UpdateTimeToLiveOperation<RecordWithTTL> operation = UpdateTimeToLiveOperation.create(true);
        UpdateTimeToLiveRequest updateTimeToLiveRequest = UpdateTimeToLiveRequest.builder().build();
        UpdateTimeToLiveResponse expectedResponse = UpdateTimeToLiveResponse.builder().build();
        when(mockDynamoDbClient.updateTimeToLive(any(UpdateTimeToLiveRequest.class))).thenReturn(expectedResponse);

        UpdateTimeToLiveResponse response = operation.serviceCall(mockDynamoDbClient).apply(updateTimeToLiveRequest);

        assertThat(response, sameInstance(expectedResponse));
        verify(mockDynamoDbClient).updateTimeToLive(same(updateTimeToLiveRequest));
    }

    @Test
    public void generateRequest_from_UpdateTimeToLiveOperation() {
        UpdateTimeToLiveOperation<RecordWithTTL> updateTimeToLiveOperation = UpdateTimeToLiveOperation.create(true);
        UpdateTimeToLiveRequest updateTimeToLiveRequest = updateTimeToLiveOperation.generateRequest(TABLE_SCHEMA,
                                                                                                    PRIMARY_CONTEXT,
                                                                                                    null);
        assertThat(updateTimeToLiveRequest, is(UpdateTimeToLiveRequest.builder()
                                                                      .tableName(TABLE_NAME)
                                                                      .timeToLiveSpecification(TimeToLiveSpecification.builder()
                                                                                                                      .enabled(true)
                                                                                                                      .attributeName(
                                                                                                                          "expirationDate")
                                                                                                                      .build())
                                                                      .build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_withoutTtlAnnotation_throwsIllegalArgumentException() {
        UpdateTimeToLiveOperation<FakeItem> updateTimeToLiveOperation = UpdateTimeToLiveOperation.create(true);

        updateTimeToLiveOperation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }
}
