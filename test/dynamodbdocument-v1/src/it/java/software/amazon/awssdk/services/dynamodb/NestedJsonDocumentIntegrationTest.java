/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * DynamoDB supports nested attributes up to 32 levels deep.
 * http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html
 */
public class NestedJsonDocumentIntegrationTest extends AwsTestBase {

    private static final String TABLE = "java-sdk-nested-json-document-" + System.currentTimeMillis();
    private static final String HASH = "hash";
    private static final String JSON_MAP_ATTRIBUTE = "json";
    private static final String JSON_MAP_NESTED_KEY = "key";
    /*
     * DynamoDB supports nested attributes up to 32 levels deep.
     * http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html
     */
    private static final int MAX_JSON_PATH_DEPTH = 32;
    private static DynamoDBClient ddb;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        ddb = DynamoDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        ddb.createTable(CreateTableRequest.builder()
                .tableName(TABLE)
                .keySchema(KeySchemaElement.builder().attributeName(HASH).keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName(HASH).attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build()).build());

        TableUtils.waitUntilActive(ddb, TABLE);
    }

    @AfterClass
    public static void tearDown() {
        ddb.deleteTable(DeleteTableRequest.builder().tableName(TABLE).build());
    }

    @Test
    public void testMaxNestedDepth() {
        // minus 1 to account for the top-level attribute
        int MAX_MAP_DEPTH = MAX_JSON_PATH_DEPTH - 1;

        AttributeValue nestedJson = buildNestedMapAttribute(MAX_MAP_DEPTH);

        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(HASH, AttributeValue.builder().s("foo").build());
        item.put(JSON_MAP_ATTRIBUTE, nestedJson);

        ddb.putItem(PutItemRequest.builder()
                .tableName(TABLE)
                .item(item)
                .build());

        // Make sure we can read the max-depth item
        GetItemResponse itemResult = ddb.getItem(GetItemRequest.builder()
                .tableName(TABLE)
                .key(Collections.singletonMap(HASH,
                        AttributeValue.builder().s("foo").build()))
                .build());
        int mapDepth = computeDepthOfNestedMapAttribute(
                itemResult.item().get(JSON_MAP_ATTRIBUTE));
        Assert.assertEquals(MAX_MAP_DEPTH, mapDepth);


        // Attempt to put a JSON document with over-limit depth
        AttributeValue nestedJson_OverLimit = buildNestedMapAttribute(MAX_MAP_DEPTH + 1);

        Map<String, AttributeValue> item_OverLimit = new HashMap<String, AttributeValue>();
        item_OverLimit.put(HASH, AttributeValue.builder().s("foo").build());
        item_OverLimit.put("json", nestedJson_OverLimit);

        try {
            ddb.putItem(PutItemRequest.builder()
                    .tableName(TABLE)
                    .item(item_OverLimit).build());
            Assert.fail("ValidationException is expected, since the depth exceeds the service limit.");
        } catch (SdkServiceException expected) {
            // Ignored or expected.
        }
    }

    private AttributeValue buildNestedMapAttribute(int depth) {
        AttributeValue value = AttributeValue.builder().s("foo").build();
        while (depth-- > 0) {
            value = AttributeValue.builder().m(Collections.singletonMap(JSON_MAP_NESTED_KEY, value)).build();
        }
        return value;
    }

    private int computeDepthOfNestedMapAttribute(AttributeValue mapAttr) {
        int depth = 0;
        while (mapAttr != null && mapAttr.m() != null) {
            depth++;
            mapAttr = mapAttr.m().get(JSON_MAP_NESTED_KEY);
        }
        return depth;
    }
}
