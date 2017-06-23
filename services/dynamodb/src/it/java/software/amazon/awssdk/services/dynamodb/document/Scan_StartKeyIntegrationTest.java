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

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.document.spec.ScanSpec;
import software.amazon.awssdk.services.dynamodb.document.utils.ValueMap;

public class Scan_StartKeyIntegrationTest extends IntegrationTestBase {

    @Test
    public void testItemIteration_New() {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withString(HASH_KEY_NAME, "allDataTypes")
                .withBinary("binary", new byte[] {1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[] {5, 6}, new byte[] {7, 8})
                .withBoolean("booleanTrue", true)
                .withBoolean("booleanFalse", false)
                .withInt("intAttr", 1234)
                .withList("listAtr", "abc", "123")
                .withMap("mapAttr",
                         new ValueMap()
                                 .withString("key1", "value1")
                                 .withInt("key2", 999))
                .withNull("nullAttr")
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = 0; i < 15; i++) {
            table.putItem(item.withNumber(RANGE_KEY_NAME, i));
        }

        ItemCollection<?> col = table.scan(new ScanSpec().withScanFilters(
                new ScanFilter(HASH_KEY_NAME).eq("allDataTypes"),
                new ScanFilter(RANGE_KEY_NAME).between(1, 10)
                                                                         ).withExclusiveStartKey(
                "hashkeyAttr", "allDataTypes",
                "rangekeyAttr", 2)
                                          );
        int resultCount = 0;
        for (Item it : col) {
            resultCount++;
            System.out.println(it);
        }
        Assert.assertEquals(8, resultCount);
    }
}
