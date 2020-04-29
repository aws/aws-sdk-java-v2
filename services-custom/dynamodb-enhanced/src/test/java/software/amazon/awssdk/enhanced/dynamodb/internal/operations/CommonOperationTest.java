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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@RunWith(MockitoJUnitRunner.class)
public class CommonOperationTest {

    private static final String FAKE_REQUEST = "fake-request";
    private static final String FAKE_RESPONSE = "fake-response";
    private static final String FAKE_RESULT = "fake-result";
    private static final String FAKE_TABLE_NAME = "fake-table-name";
    private static final String FAKE_INDEX_NAME = "fake-index-name";

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Spy
    private CommonOperation<FakeItem, String, String, String> spyCommonOperation;

    @Before
    public void stubSpy() {
        when(spyCommonOperation.generateRequest(any(), any(), any())).thenReturn(FAKE_REQUEST);
        when(spyCommonOperation.serviceCall(any())).thenReturn(s -> {
            if (!FAKE_REQUEST.equals(s)) {
                throw new RuntimeException("Did not receive expected request");
            }

            return FAKE_RESPONSE;
        });
        when(spyCommonOperation.transformResponse(any(), any(), any(), any())).thenReturn(FAKE_RESULT);
    }

    @Test
    public void execute_defaultImplementation_behavesCorrectlyAndReturnsCorrectResult() {
        OperationContext operationContext = DefaultOperationContext.create(FAKE_TABLE_NAME, FAKE_INDEX_NAME);
        String result = spyCommonOperation.execute(FakeItem.getTableSchema(),
                                                   operationContext,
                                                   mockDynamoDbEnhancedClientExtension,
                                                   mockDynamoDbClient);

        assertThat(result, is(FAKE_RESULT));
        verify(spyCommonOperation).generateRequest(FakeItem.getTableSchema(), operationContext, mockDynamoDbEnhancedClientExtension);
        verify(spyCommonOperation).serviceCall(mockDynamoDbClient);
        verify(spyCommonOperation).transformResponse(FAKE_RESPONSE, FakeItem.getTableSchema(), operationContext,
                                                     mockDynamoDbEnhancedClientExtension);
    }
}
