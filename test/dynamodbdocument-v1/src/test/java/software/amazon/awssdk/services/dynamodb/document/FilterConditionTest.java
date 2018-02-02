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
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;

/**
 * Covers ScanFilter, which shares the same underlying implementation as QueryFilter.
 */
public class FilterConditionTest {

    private static Entry<String, Condition> toAttributeCondition(ScanFilter ScanFilter) {
        Map<String, Condition> map = InternalUtils
                .toAttributeConditionMap(Arrays.asList(ScanFilter));
        Assert.assertEquals(1, map.size());

        Iterator<Entry<String, Condition>> iter = map.entrySet().iterator();
        return iter.next();
    }

    @Test
    public void testScanFilter_EQ() {
        ScanFilter ScanFilter = new ScanFilter("foo").eq("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.EQ, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());

        ScanFilter = new ScanFilter("foo").eq(null);
        ddbscanFilter = toAttributeCondition(ScanFilter);
        ddbscanFilter_attrName = ddbscanFilter.getKey();
        ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.EQ, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals(true, ddbscanFilter_value.attributeValueList().get(0).nul());
    }

    @Test
    public void testScanFilter_NE() {
        ScanFilter ScanFilter = new ScanFilter("foo").ne("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.NE, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
    }

    @Test
    public void testScanFilter_EXISTS() {
        ScanFilter ScanFilter = new ScanFilter("foo").exists();
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.NOT_NULL, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(null, ddbscanFilter_value.attributeValueList());
    }

    @Test
    public void testScanFilter_NOTEXISTS() {
        ScanFilter ScanFilter = new ScanFilter("foo").notExist();
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.NULL, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(null, ddbscanFilter_value.attributeValueList());
    }

    @Test
    public void testScanFilter_CONTAINS() {
        ScanFilter ScanFilter = new ScanFilter("foo").contains("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.CONTAINS, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
    }

    @Test
    public void testScanFilter_NOTCONTAINS() {
        ScanFilter ScanFilter = new ScanFilter("foo").notContains("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.NOT_CONTAINS, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
    }

    @Test
    public void testScanFilter_BEGINSWITH() {
        ScanFilter ScanFilter = new ScanFilter("foo").beginsWith("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.BEGINS_WITH, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
    }

    @Test
    public void testScanFilter_IN() {
        // Single value
        ScanFilter ScanFilter = new ScanFilter("foo").in("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.IN, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());

        // Multi-value
        ScanFilter = new ScanFilter("foo").in("bar", "charlie", null);
        ddbscanFilter = toAttributeCondition(ScanFilter);
        ddbscanFilter_attrName = ddbscanFilter.getKey();
        ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(3, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
        Assert.assertEquals("charlie", ddbscanFilter_value.attributeValueList().get(1).s());
        Assert.assertEquals(true, ddbscanFilter_value.attributeValueList().get(2).nul());
        Assert.assertEquals(ComparisonOperator.IN, ddbscanFilter_value.comparisonOperator());

        // Null values
        try {
            ScanFilter = new ScanFilter("foo").in((Object[]) null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // Ignored or expected.
        }

        // Empty values
        try {
            ScanFilter = new ScanFilter("foo").in();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // Ignored or expected.
        }
    }

    @Test
    public void testScanFilter_BETWEEN() {
        ScanFilter ScanFilter = new ScanFilter("foo").between(0, 100);
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(2, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("0", ddbscanFilter_value.attributeValueList().get(0).n());
        Assert.assertEquals("100", ddbscanFilter_value.attributeValueList().get(1).n());
        Assert.assertEquals(ComparisonOperator.BETWEEN, ddbscanFilter_value.comparisonOperator());
    }

    @Test
    public void testScanFilter_GE() {
        ScanFilter ScanFilter = new ScanFilter("foo").ge("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.GE, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
    }

    @Test
    public void testScanFilter_GT() {
        ScanFilter ScanFilter = new ScanFilter("foo").gt("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.GT, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
    }

    @Test
    public void testScanFilter_LE() {
        ScanFilter ScanFilter = new ScanFilter("foo").le("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.LE, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
    }

    @Test
    public void testScanFilter_LT() {
        ScanFilter ScanFilter = new ScanFilter("foo").lt("bar");
        Entry<String, Condition> ddbscanFilter = toAttributeCondition(ScanFilter);
        String ddbscanFilter_attrName = ddbscanFilter.getKey();
        Condition ddbscanFilter_value = ddbscanFilter.getValue();

        Assert.assertEquals("foo", ddbscanFilter_attrName);
        Assert.assertEquals(ComparisonOperator.LT, ddbscanFilter_value.comparisonOperator());
        Assert.assertEquals(1, ddbscanFilter_value.attributeValueList().size());
        Assert.assertEquals("bar", ddbscanFilter_value.attributeValueList().get(0).s());
    }

    @Test
    public void testScanFilter_EmptyAttributeName() {
        try {
            new ScanFilter(null);
            Assert.fail();
        } catch (IllegalArgumentException ScanFilter) {
            // Ignored or expected.
        }

        try {
            new ScanFilter("");
            Assert.fail();
        } catch (IllegalArgumentException ScanFilter) {
            // Ignored or expected.
        }
    }

    @Test
    @Ignore // FIXME: fails with "region cannot be null"
    public void testScanFilter_DuplicateAttribute() {
        Table fakeTable = new Table(DynamoDBClient.builder().region(Region.US_WEST_2).build(), "fake-table");
        try {
            fakeTable.scan(
                    new ScanFilter("foo").eq("bar"),
                    new ScanFilter("foo").eq("charlie"));
            Assert.fail();
        } catch (IllegalArgumentException ScanFilter) {
            // Ignored or expected.
        }
    }
}
