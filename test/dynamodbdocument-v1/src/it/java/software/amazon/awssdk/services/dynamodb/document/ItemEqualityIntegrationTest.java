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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.document.spec.GetItemSpec;

public class ItemEqualityIntegrationTest extends IntegrationTestBase {

    @Test
    public void test_equalMapAndJSON() {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "user123")
                .withJson(
                        "Details",
                        "{ \"UserID1\": 0}");
        table.putItem(item);
        Item itemGet = table.getItem(new GetItemSpec().withPrimaryKey(new
                                                                              KeyAttribute(HASH_KEY_NAME, "user123"))
                                                      .withConsistentRead(true));
        assertEquals(item.asMap(), itemGet.asMap());
        assertEquals(Item.fromJson(item.toJson()), Item.fromJson(itemGet.toJson()));
    }

    @Test
    public void test_equalMap() {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "user123")
                .withString("DateTime", "1357306017")
                .withJson(
                        "Details",
                        "{ \"UserID1\": 0, \"UserID2\": 0, \"Message\": \"my message\", \"DateTime\": 0}");
        table.putItem(item);
        Item itemGet = table.getItem(new GetItemSpec().withPrimaryKey(new
                                                                              KeyAttribute(HASH_KEY_NAME, "user123"))
                                                      .withConsistentRead(true));
        assertEquals(item.asMap(), itemGet.asMap());
    }
}
