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

package software.amazon.awssdk.enhanced.dynamodb.internal.update;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithUpdateBehaviors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UpdateExpressionBuilderTest {

    private static final TableSchema<RecordWithUpdateBehaviors> TABLE_SCHEMA =
        TableSchema.fromClass(RecordWithUpdateBehaviors.class);

    @Test
    public void generateUpdateExpression_Simple() {
        // RecordWithUpdateBehaviors item = new RecordWithUpdateBehaviors();
        // item.setId("id123");
        //
        // Map<String, AttributeValue> itemMap = TABLE_SCHEMA.itemToMap(item, true);
        //
        // Expression expectedExpression = Expression.builder()
        //                                           .expression("SET #AMZN_MAPPED_id = :AMZN_MAPPED_id")
        //                                           .expressionNames(Collections.singletonMap("#AMZN_MAPPED_id", "id"))
        //                                           .expressionValues(Collections.singletonMap(":AMZN_MAPPED_id",
        //                                                                                      AttributeValue.builder().s(item.getId()).build()))
        //                                           .build();
        //
        // Expression updateExpression = UpdateExpressionUtils.generateUpdateExpression(itemMap, TABLE_SCHEMA.tableMetadata());
        // assertThat(updateExpression, is(expectedExpression));
    }

    @Test
    public void generateUpdateExpression_WithCounter() {
        // RecordWithUpdateBehaviors item = new RecordWithUpdateBehaviors();
        // item.setId("id123");
        // item.setVersion(3L);
        // item.setCounter(5L);
        //
        // Map<String, AttributeValue> itemMap = TABLE_SCHEMA.itemToMap(item, true);
        //
        // String expectedExpression = "SET "
        //                             + "#AMZN_MAPPED_counter = if_not_exists(#AMZN_MAPPED_counter, :AMZN_MAPPED_counter_Start) + "
        //                                 + ":AMZN_MAPPED_counter_Delta, "
        //                             + "#AMZN_MAPPED_id = :AMZN_MAPPED_id, "
        //                             + "#AMZN_MAPPED_version = :AMZN_MAPPED_version";
        //
        // Map<String, String> expectedNames = new HashMap<>();
        // expectedNames.put("#AMZN_MAPPED_id", "id");
        // expectedNames.put("#AMZN_MAPPED_version", "version");
        // expectedNames.put("#AMZN_MAPPED_counter", "counter");
        //
        // Map<String, AttributeValue> expectedValues = new HashMap<>();
        // expectedValues.put(":AMZN_MAPPED_id", AttributeValue.builder().s(item.getId()).build());
        // expectedValues.put(":AMZN_MAPPED_version", AttributeValue.builder().n(item.getVersion().toString()).build());
        // expectedValues.put(":AMZN_MAPPED_counter_Start", AttributeValue.builder().n("-1").build());
        // expectedValues.put(":AMZN_MAPPED_counter_Delta", AttributeValue.builder().n("1").build());
        //
        // Expression expectedUpdateExpression = Expression.builder()
        //                                           .expression(expectedExpression)
        //                                           .expressionNames(expectedNames)
        //                                           .expressionValues(expectedValues)
        //                                           .build();
        //
        // Expression updateExpression = UpdateExpressionUtils.generateUpdateExpression(itemMap, TABLE_SCHEMA.tableMetadata());
        // assertThat(updateExpression, is(expectedUpdateExpression));
    }

}