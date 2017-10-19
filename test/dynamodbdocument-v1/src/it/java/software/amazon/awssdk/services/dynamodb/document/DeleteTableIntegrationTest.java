/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document;

import org.junit.Test;

public class DeleteTableIntegrationTest extends IntegrationTestBase {
    @Test
    public void placeholder() {
    }

    //    @Test
    public void testDeleteHashTable() {
        DynamoDb[] ddbs = {dynamo, dynamoOld};
        for (DynamoDb ddb : ddbs) {
            Table table = ddb.getTable(HASH_ONLY_TABLE_NAME);
            System.out.println(table.delete());
        }
    }

    //    @Test
    public void testDeleteRangeTable() {
        DynamoDb[] ddbs = {dynamo, dynamoOld};
        for (DynamoDb ddb : ddbs) {
            Table table = ddb.getTable(RANGE_TABLE_NAME);
            System.out.println(table.delete());
        }
    }
}
