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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;

public class ExpectedTest {

    private static Entry<String, ExpectedAttributeValue> toExpectedAttributeValue(Expected expected) {
        Map<String, ExpectedAttributeValue> map = InternalUtils
                .toExpectedAttributeValueMap(Arrays.asList(expected));
        Assert.assertEquals(1, map.size());

        Iterator<Entry<String, ExpectedAttributeValue>> iter = map.entrySet().iterator();
        return iter.next();
    }

    @Test
    public void testExpected_EQ() {
        Expected expected = new Expected("foo").eq("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.EQ, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());

        expected = new Expected("foo").eq(null);
        ddbExpected = toExpectedAttributeValue(expected);
        ddbExpected_attrName = ddbExpected.getKey();
        ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.EQ, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals(true, ddbExpected_value.attributeValueList().get(0).nul());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_NE() {
        Expected expected = new Expected("foo").ne("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.NE, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_EXISTS() {
        Expected expected = new Expected("foo").exists();
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.NOT_NULL, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(null, ddbExpected_value.attributeValueList());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_NOTEXISTS() {
        Expected expected = new Expected("foo").notExist();
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.NULL, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(null, ddbExpected_value.attributeValueList());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_CONTAINS() {
        Expected expected = new Expected("foo").contains("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.CONTAINS, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_NOTCONTAINS() {
        Expected expected = new Expected("foo").notContains("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.NOT_CONTAINS, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_BEGINSWITH() {
        Expected expected = new Expected("foo").beginsWith("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.BEGINS_WITH, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_IN() {
        // Single value
        Expected expected = new Expected("foo").in("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.IN, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());

        // Multi-value
        expected = new Expected("foo").in("bar", "charlie", null);
        ddbExpected = toExpectedAttributeValue(expected);
        ddbExpected_attrName = ddbExpected.getKey();
        ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(3, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals("charlie", ddbExpected_value.attributeValueList().get(1).s());
        Assert.assertEquals(true, ddbExpected_value.attributeValueList().get(2).nul());
        Assert.assertEquals(ComparisonOperator.IN, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());

        // Null values
        try {
            expected = new Expected("foo").in((Object[]) null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // Ignored or expected.
        }

        // Empty values
        try {
            expected = new Expected("foo").in();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // Ignored or expected.
        }
    }

    @Test
    public void testExpected_BETWEEN() {
        Expected expected = new Expected("foo").between(0, 100);
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(2, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("0", ddbExpected_value.attributeValueList().get(0).n());
        Assert.assertEquals("100", ddbExpected_value.attributeValueList().get(1).n());
        Assert.assertEquals(ComparisonOperator.BETWEEN, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_GE() {
        Expected expected = new Expected("foo").ge("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.GE, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_GT() {
        Expected expected = new Expected("foo").gt("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.GT, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_LE() {
        Expected expected = new Expected("foo").le("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.LE, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_LT() {
        Expected expected = new Expected("foo").lt("bar");
        Entry<String, ExpectedAttributeValue> ddbExpected = toExpectedAttributeValue(expected);
        String ddbExpected_attrName = ddbExpected.getKey();
        ExpectedAttributeValue ddbExpected_value = ddbExpected.getValue();

        Assert.assertEquals("foo", ddbExpected_attrName);
        Assert.assertEquals(ComparisonOperator.LT, ddbExpected_value.comparisonOperator());
        Assert.assertEquals(1, ddbExpected_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbExpected_value.attributeValueList().get(0).s());
        Assert.assertEquals(null, ddbExpected_value.value());
        Assert.assertEquals(null, ddbExpected_value.exists());
    }

    @Test
    public void testExpected_EmptyAttributeName() {
        try {
            new Expected(null);
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            // Ignored or expected.
        }

        try {
            new Expected("");
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            // Ignored or expected.
        }
    }

    @Test
    public void testExpected_DuplicateAttribute() {
        Table fakeTable = new Table(DynamoDbClient.builder().region(Region.US_WEST_2).build(), "fake-table");
        try {
            fakeTable.putItem(new Item(),
                              new Expected("foo").eq("bar"),
                              new Expected("foo").eq("charlie"));
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            // Ignored or expected.
        }
    }
}
