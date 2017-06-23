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

import java.math.BigDecimal;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.dynamodb.document.spec.DeleteItemSpec;
import software.amazon.awssdk.services.dynamodb.document.utils.ValueMap;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

public class DeleteItemIntegrationTest extends IntegrationTestBase {

    private static Table table;

    @BeforeClass
    public static void setup() throws InterruptedException {
        IntegrationTestBase.setup();
        table = dynamo.getTable(RANGE_TABLE_NAME);
    }

    @After
    public void putItemAfterDelete() {
        Item item = new Item()
                .withPrimaryKey(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0)
                .withBinary("binary", new byte[] {1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[] {5, 6}, new byte[] {7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        table.putItem(item);
        item = table.getItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0);
        System.out.println(item);
        Assert.assertNotNull(item);
        Assert.assertTrue(item.numberOfAttributes() > 0);
    }

    @Test
    public void testDelete_0() {
        table.deleteItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0);
        Item item = table.getItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0);
        Assert.assertNull(item);
    }

    @Test
    public void testDelete_1() {
        table.deleteItem(new KeyAttribute(HASH_KEY_NAME, HASH_KEY_NAME),
                         new KeyAttribute(RANGE_KEY_NAME, 0));
        Item item = table.getItem(new KeyAttribute(HASH_KEY_NAME, HASH_KEY_NAME),
                                  new KeyAttribute(RANGE_KEY_NAME, 0));
        Assert.assertNull(item);
    }

    @Test
    public void testDelete_2() {
        table.deleteItem(new PrimaryKey()
                                 .addComponent(HASH_KEY_NAME, "deleteTest")
                                 .addComponent(RANGE_KEY_NAME, 0));
        Item item = table.getItem(new PrimaryKey()
                                          .addComponent(HASH_KEY_NAME, "deleteTest")
                                          .addComponent(RANGE_KEY_NAME, 0));
        Assert.assertNull(item);
    }

    @Test
    public void testDelete_WithExpected_0() {
        try {
            table.deleteItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0,
                             new Expected("stringAttr").eq("not bla"));
            Assert.fail();
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }

        table.deleteItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0,
                         new Expected("stringAttr").eq("bla"));
        Item item = table.getItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0);
        Assert.assertNull(item);
    }

    @Test
    public void testDelete_WithExpected_1() {
        try {
            table.deleteItem(new PrimaryKey()
                                     .addComponent(HASH_KEY_NAME, "deleteTest")
                                     .addComponent(RANGE_KEY_NAME, 0),
                             new Expected("stringAttr").in("not bla"));
            Assert.fail();
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }

        table.deleteItem(new PrimaryKey()
                                 .addComponent(HASH_KEY_NAME, "deleteTest")
                                 .addComponent(RANGE_KEY_NAME, 0),
                         new Expected("stringAttr").in("bla"));
        Item item = table.getItem(new PrimaryKey()
                                          .addComponent(HASH_KEY_NAME, "deleteTest")
                                          .addComponent(RANGE_KEY_NAME, 0));
        Assert.assertNull(item);
    }

    @Test
    public void testDelete_WithConditionExpression() {
        try {
            table.deleteItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0,
                             "stringAttr = :bla", null, new ValueMap().withString(":bla", "not bla"));
            Assert.fail();
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }

        table.deleteItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0,
                         "stringAttr = :bla", null, new ValueMap().withString(":bla", "bla"));
        Item item = table.getItem(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0);
        Assert.assertNull(item);
    }

    @Test
    public void testDeleteOutcome() {
        DeleteItemOutcome outcome = table.deleteItem(new DeleteItemSpec()
                                                             .withPrimaryKey(HASH_KEY_NAME, "deleteTest", RANGE_KEY_NAME, 0)
                                                             .withReturnValues(ReturnValue.ALL_OLD));

        Assert.assertNotNull(outcome);
        Assert.assertNotNull(outcome.getDeleteItemResponse());
        Assert.assertNotNull(outcome.getItem());

        Item deletedItem = outcome.getItem();
        Map<String, Object> attributes = deletedItem.asMap();
        Assert.assertEquals("deleteTest", attributes.get(HASH_KEY_NAME));
        Assert.assertEquals(new BigDecimal(0), attributes.get(RANGE_KEY_NAME));
    }
}
