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

package software.amazon.awssdk.benchmark.coldstart;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * The request used by every cold-start arm, so the arms differ only in whether {@code SdkWarmUp.prime} ran.
 *
 * <p>The item shape matches {@code fixtures/json-protocol/putitem-response.json} and the item built by
 * {@code apicall.protocol.V2JsonRoundtripBenchmark}, covering the scalar, set, map and list attribute types so the
 * marshaller and unmarshaller do a representative amount of first-call work.
 */
final class ColdStartRequests {

    static final String FIXTURE = "json-protocol/putitem-response.json";
    static final String CONTENT_TYPE = "application/x-amz-json-1.0";

    private ColdStartRequests() {
    }

    static PutItemRequest putItemRequest() {
        return PutItemRequest.builder()
                             .tableName("benchmark-table")
                             .item(itemMap())
                             .build();
    }

    private static Map<String, AttributeValue> itemMap() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.fromS("benchmark-key"));
        item.put("sk", AttributeValue.fromN("100"));
        item.put("stringField", AttributeValue.fromS("test-value"));
        item.put("numberField", AttributeValue.fromN("123.456"));
        item.put("binaryField", AttributeValue.fromB(SdkBytes.fromUtf8String("hello world")));
        item.put("stringSetField", AttributeValue.builder().ss("value1", "value2", "value3").build());
        item.put("numberSetField", AttributeValue.builder().ns("1.1", "2.2", "3.3").build());
        item.put("boolField", AttributeValue.fromBool(false));
        item.put("nullField", AttributeValue.builder().nul(true).build());
        Map<String, AttributeValue> deep = new HashMap<>();
        deep.put("level2", AttributeValue.fromN("999"));
        Map<String, AttributeValue> nested = new HashMap<>();
        nested.put("nested", AttributeValue.fromS("nested-value"));
        nested.put("deepNested", AttributeValue.fromM(deep));
        item.put("mapField", AttributeValue.fromM(nested));
        item.put("listField", AttributeValue.builder().l(
            AttributeValue.fromS("item1"),
            AttributeValue.fromN("42"),
            AttributeValue.fromBool(true),
            AttributeValue.builder().nul(true).build()).build());
        return item;
    }
}
