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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

@RunWith(MockitoJUnitRunner.class)
public class EmptyBinaryTest {
    private static final String TABLE_NAME = "TEST_TABLE";
    private static final SdkBytes EMPTY_BYTES = SdkBytes.fromUtf8String("");
    private static final AttributeValue EMPTY_BINARY = AttributeValue.builder().b(EMPTY_BYTES).build();

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private DynamoDbTable<TestBean> dynamoDbTable;

    @DynamoDbBean
    public static class TestBean {
        private String id;
        private SdkBytes b;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SdkBytes getB() {
            return b;
        }

        public void setB(SdkBytes b) {
            this.b = b;
        }
    }

    private static final TableSchema<TestBean> TABLE_SCHEMA = TableSchema.fromClass(TestBean.class);

    @Before
    public void initializeTable() {
        DynamoDbEnhancedClient dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder()
                                                                              .dynamoDbClient(mockDynamoDbClient)
                                                                              .build();

        this.dynamoDbTable = dynamoDbEnhancedClient.table(TABLE_NAME, TABLE_SCHEMA);
    }

    @Test
    public void putEmptyBytes() {
        TestBean testBean = new TestBean();
        testBean.setId("id123");
        testBean.setB(EMPTY_BYTES);

        dynamoDbTable.putItem(testBean);

        Map<String, AttributeValue> expectedItemMap = new HashMap<>();
        expectedItemMap.put("id", AttributeValue.builder().s("id123").build());
        expectedItemMap.put("b", EMPTY_BINARY);

        PutItemRequest expectedRequest = PutItemRequest.builder()
                                                       .tableName(TABLE_NAME)
                                                       .item(expectedItemMap)
                                                       .build();

        verify(mockDynamoDbClient).putItem(expectedRequest);
    }

    @Test
    public void getEmptyBytes() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", AttributeValue.builder().s("id123").build());
        itemMap.put("b", EMPTY_BINARY);

        GetItemResponse response = GetItemResponse.builder()
                                                  .item(itemMap)
                                                  .build();

        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);

        TestBean result = dynamoDbTable.getItem(r -> r.key(k -> k.partitionValue("id123")));

        assertThat(result.getId()).isEqualTo("id123");
        assertThat(result.getB()).isEqualTo(EMPTY_BYTES);
    }

    @Test
    public void updateEmptyBytesWithCondition() {
        Map<String, AttributeValue> expectedItemMap = new HashMap<>();
        expectedItemMap.put("id", AttributeValue.builder().s("id123").build());
        expectedItemMap.put("b", EMPTY_BINARY);
        TestBean testBean = new TestBean();
        testBean.setId("id123");
        testBean.setB(EMPTY_BYTES);

        UpdateItemResponse response = UpdateItemResponse.builder()
                                                        .attributes(expectedItemMap)
                                                        .build();
        when(mockDynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(response);

        Expression conditionExpression = Expression.builder()
                                                     .expression("#attr = :val")
                                                     .expressionNames(singletonMap("#attr", "b"))
                                                     .expressionValues(singletonMap(":val", EMPTY_BINARY))
                                                     .build();

        TestBean result = dynamoDbTable.updateItem(r -> r.item(testBean).conditionExpression(conditionExpression));

        Map<String, String> expectedExpressionAttributeNames = new HashMap<>();
        expectedExpressionAttributeNames.put("#AMZN_MAPPED_b", "b");
        expectedExpressionAttributeNames.put("#attr", "b");
        Map<String, AttributeValue> expectedExpressionAttributeValues = new HashMap<>();
        expectedExpressionAttributeValues.put(":AMZN_MAPPED_b", EMPTY_BINARY);
        expectedExpressionAttributeValues.put(":val", EMPTY_BINARY);
        Map<String, AttributeValue> expectedKeyMap = new HashMap<>();
        expectedKeyMap.put("id", AttributeValue.builder().s("id123").build());

        UpdateItemRequest expectedRequest =
            UpdateItemRequest.builder()
                             .tableName(TABLE_NAME)
                             .key(expectedKeyMap)
                             .returnValues(ReturnValue.ALL_NEW)
                             .updateExpression("SET #AMZN_MAPPED_b = :AMZN_MAPPED_b")
                             .conditionExpression("#attr = :val")
                             .expressionAttributeNames(expectedExpressionAttributeNames)
                             .expressionAttributeValues(expectedExpressionAttributeValues)
                             .build();

        verify(mockDynamoDbClient).updateItem(expectedRequest);
        assertThat(result.getId()).isEqualTo("id123");
        assertThat(result.getB()).isEqualTo(EMPTY_BYTES);
    }
}
