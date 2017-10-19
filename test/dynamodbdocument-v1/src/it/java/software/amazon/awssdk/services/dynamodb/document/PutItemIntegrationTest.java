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
import software.amazon.awssdk.services.dynamodb.document.spec.GetItemSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.PutItemSpec;
import software.amazon.awssdk.services.dynamodb.document.utils.NameMap;
import software.amazon.awssdk.services.dynamodb.document.utils.ValueMap;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

public class PutItemIntegrationTest extends IntegrationTestBase {

    @Test
    public void testEmptyRecord() {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        PutItemOutcome out = table.putItem(new Item().with(HASH_KEY_NAME, "emptyRecord"));
        System.out.println(out);
    }

    @Test
    public void testHashOnlyAllDataTypes() {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);
        PutItemOutcome out = table.putItem(
                new Item().with(HASH_KEY_NAME, "allDataTypes")
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
                        .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz")
        );
        System.out.println(out);
    }

    @Test
    public void testHashOnlyPriorDataTypes() {
        Table table = dynamoOld.getTable(HASH_ONLY_TABLE_NAME);
        PutItemOutcome out = table.putItem(
                new Item().with(HASH_KEY_NAME, "priorDataTypes")
                        .withBinary("binary", new byte[] {1, 2, 3, 4})
                        .withBinarySet("binarySet", new byte[] {5, 6}, new byte[] {7, 8})
                        .withInt("intAttr", 1234)
                        .withNumber("numberAttr", 999.1234)
                        .withString("stringAttr", "bla")
                        .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz")
        );
        System.out.println(out);
    }

    @Test
    public void testHashOnlyAllDataTypesViaSpec() {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);

        Item oldItem = new Item()
                .with(HASH_KEY_NAME, "allDataTypesViaSpec")
                .withInt("intAttr", 1234);
        table.putItem(oldItem);

        Item newItem = new Item()
                .with(HASH_KEY_NAME, "allDataTypesViaSpec")
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

        PutItemOutcome out = table.putItem(
                new PutItemSpec()
                        .withItem(newItem)
                        .withExpected(
                                new Expected(HASH_KEY_NAME).notContains("xyz"),
                                new Expected("intAttr").between(1, 9999))
                        .withReturnValues(ReturnValue.ALL_OLD)
        );

        Assert.assertTrue(ItemTestUtils.equalsItem(oldItem, out.getItem()));

        Item getNewItem = table.getItem(new GetItemSpec()
                .withPrimaryKey(HASH_KEY_NAME, "allDataTypesViaSpec")
                .withConsistentRead(true));
        Assert.assertTrue(ItemTestUtils.equalsItem(newItem, getNewItem));

    }

    @Test
    public void testConditionalExpression() {
        Table table = dynamo.getTable(HASH_ONLY_TABLE_NAME);

        final String hashKeyVal = "testConditionalExpression";
        Item oldItem = new Item()
                .with(HASH_KEY_NAME, hashKeyVal)
                .withInt("intAttr", 1234);
        table.putItem(oldItem);

        Item newItem = new Item()
                .with(HASH_KEY_NAME, hashKeyVal)
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
        PutItemOutcome out = table.putItem(newItem,
                "NOT (contains (#pk, :hashkeyAttr)) AND (intAttr BETWEEN :lo AND :hi)",
                new NameMap().with("#pk", "hashkeyAttr"),
                new ValueMap()
                        .withString(":hashkeyAttr", "xyz")
                        .withInt(":lo", 1)
                        .withInt(":hi", 9999));

        // By default PutItem returns no attributes
        Assert.assertNull(out.getItem());

        Item getNewItem = table.getItem(HASH_KEY_NAME, hashKeyVal);
        Assert.assertTrue(ItemTestUtils.equalsItem(newItem, getNewItem));
    }

}
