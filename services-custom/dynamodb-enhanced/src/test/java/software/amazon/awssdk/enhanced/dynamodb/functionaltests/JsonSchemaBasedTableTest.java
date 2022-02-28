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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.model.JsonItem;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;


public class JsonSchemaBasedTableTest extends LocalDynamoDbSyncTestBase {


    ObjectMapper mapper = new ObjectMapper();
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<JsonItem> mappedTable1;

    @Before
    public void createTable() {

        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(getDynamoDbClient())
                                               .build();

        mappedTable1 = enhancedClient.table(getConcreteTableName("table-name-1"),
                                            TableSchema.withDocumentSchema(
                                                StaticTableMetadata.builder().addIndexPartitionKey(
                                                    TableMetadata.primaryIndexName(), "hash-key", AttributeValueType.S).build()));

        mappedTable1.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @Test
    public void putObject_And_getSameObjectAsJson() throws JsonProcessingException {

        String INPUT_JSON_STRING = "{\"hash-key\":\"1234\",\"binarySet\":[\"BQY=\",\"Bwg=\"],"
                                   + "\"booleanTrue\":true,\"booleanFalse\":false,\"intAttr\":1234}";
        mappedTable1.putItem(JsonItem.fromJson(INPUT_JSON_STRING));

        JsonItem item = mappedTable1.getItem(Key.builder().partitionValue("1234").build());
        assertEquals(mapper.readTree(INPUT_JSON_STRING), mapper.readTree(item.toJson()));
    }

    @Test
    public void updateItem_that_AlreadyExist() throws JsonProcessingException, InterruptedException {

        String INPUT_JSON_STRING = "{\"hash-key\":\"1234\",\"updateDone\":false}";
        mappedTable1.putItem(JsonItem.fromJson(INPUT_JSON_STRING));

        String UPDATE_INPUT_JSON_STRING = "{\"hash-key\":\"1234\",\"updateDone\":true}";
        JsonItem jsonItem = mappedTable1.updateItem(item -> item.item(JsonItem.fromJson(UPDATE_INPUT_JSON_STRING)));
        assertEquals(mapper.readTree(UPDATE_INPUT_JSON_STRING), mapper.readTree(jsonItem.toJson()));

        JsonItem item = mappedTable1.getItem(Key.builder().partitionValue("1234").build());
        assertEquals(mapper.readTree(UPDATE_INPUT_JSON_STRING), mapper.readTree(item.toJson()));
    }

    @Test
    public void deleteItem() throws JsonProcessingException, InterruptedException {

        String INPUT_JSON_STRING = "{\"hash-key\":\"1234\",\"updateDone\":false}";
        mappedTable1.putItem(JsonItem.fromJson(INPUT_JSON_STRING));

        JsonItem item = mappedTable1.getItem(Key.builder().partitionValue("1234").build());
        assertEquals(mapper.readTree(INPUT_JSON_STRING), mapper.readTree(item.toJson()));

        JsonItem deletedItem = mappedTable1.deleteItem(Key.builder().partitionValue("1234").build());
        assertEquals(mapper.readTree(INPUT_JSON_STRING), mapper.readTree(deletedItem.toJson()));

        JsonItem getDeletedItem = mappedTable1.getItem(Key.builder().partitionValue("1234").build());
        assertNull(getDeletedItem);
    }

    @Test
    public void keyFromJsonItem() throws JsonProcessingException, InterruptedException {

        String INPUT_JSON_STRING = "{\"hash-key\":\"1234\",\"updateDone\":false}";
        mappedTable1.putItem(JsonItem.fromJson(INPUT_JSON_STRING));
        Key key = mappedTable1.keyFrom(JsonItem.fromJson(INPUT_JSON_STRING));
        assertEquals(key.partitionKeyValue(), AttributeValue.builder().s("1234").build());
    }


    @Test
    public void scanRequest() throws JsonProcessingException {

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#value >= :min_value AND #value <= :max_value")
                                          .expressionValues(expressionValues)
                                          .expressionNames(Collections.singletonMap("#value", "value"))
                                          .build();

        String INPUT_ONE_JSON_STRING = "{\"hash-key\":\"0001\",\"name\":\"one\",\"value\":4}";
        String INPUT_TWO_JSON_STRING = "{\"hash-key\":\"0002\",\"name\":\"two\",\"value\":5}";
        String INPUT_ONE_DUPE_JSON_STRING = "{\"hash-key\":\"0003\",\"name\":\"three\",\"value\":9}";
        mappedTable1.putItem(JsonItem.fromJson(INPUT_ONE_JSON_STRING));
        mappedTable1.putItem(r -> r.item(JsonItem.fromJson(INPUT_TWO_JSON_STRING)));
        mappedTable1.putItem(PutItemEnhancedRequest.builder(JsonItem.class).item(JsonItem.fromJson(INPUT_ONE_DUPE_JSON_STRING)).build());

        PageIterable<JsonItem> iterable =
            mappedTable1.scan(s -> s.filterExpression(expression));
        assertEquals(iterable.items().stream().count(), 2);

        List<JsonItem> jsonItemList = iterable.items().stream().collect(Collectors.toList());
        System.out.println(jsonItemList);
        assertEquals(mapper.readTree(jsonItemList.get(0).toJson()), mapper.readTree(INPUT_TWO_JSON_STRING));
        assertEquals(mapper.readTree(jsonItemList.get(1).toJson()), mapper.readTree(INPUT_ONE_JSON_STRING));
;
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name-1"))
                                                          .build());
    }
}
