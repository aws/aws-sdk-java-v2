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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Tests string set attributes
 */
public class NumericSetAttributesIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String INTEGER_ATTRIBUTE = "integerAttribute";
    private static final String FLOAT_OBJECT_ATTRIBUTE = "floatObjectAttribute";
    private static final String DOUBLE_OBJECT_ATTRIBUTE = "doubleObjectAttribute";
    private static final String BIG_INTEGER_ATTRIBUTE = "bigIntegerAttribute";
    private static final String BIG_DECIMAL_ATTRIBUTE = "bigDecimalAttribute";
    private static final String LONG_OBJECT_ATTRIBUTE = "longObjectAttribute";
    private static final String BYTE_OBJECT_ATTRIBUTE = "byteObjectAttribute";
    private static final String BOOLEAN_ATTRIBUTE = "booleanAttribute";
    private static final List<Map<String, AttributeValue>> attrs = new LinkedList<Map<String, AttributeValue>>();
    // We don't start with the current system millis like other tests because
    // it's out of the range of some data types
    private static int start = 1;
    private static int byteStart = 1;

    // Test data
    static {
        for (int i = 0; i < 5; i++) {
            Map<String, AttributeValue> attr = new HashMap<String, AttributeValue>();
            attr.put(KEY_NAME, AttributeValue.builder().s("" + start++).build());
            attr.put(INTEGER_ATTRIBUTE, AttributeValue.builder().ns("" + start++, "" + start++, "" + start++).build());
            attr.put(FLOAT_OBJECT_ATTRIBUTE, AttributeValue.builder().ns("" + start++, "" + start++, "" + start++).build());
            attr.put(DOUBLE_OBJECT_ATTRIBUTE, AttributeValue.builder().ns("" + start++, "" + start++, "" + start++).build());
            attr.put(BIG_INTEGER_ATTRIBUTE, AttributeValue.builder().ns("" + start++, "" + start++, "" + start++).build());
            attr.put(BIG_DECIMAL_ATTRIBUTE, AttributeValue.builder().ns("" + start++, "" + start++, "" + start++).build());
            attr.put(LONG_OBJECT_ATTRIBUTE, AttributeValue.builder().ns("" + start++, "" + start++, "" + start++).build());
            attr.put(BYTE_OBJECT_ATTRIBUTE, AttributeValue.builder().ns("" + byteStart++, "" + byteStart++, "" + byteStart++).build());
            attr.put(BOOLEAN_ATTRIBUTE, AttributeValue.builder().ns("0", "1").build());
            attrs.add(attr);
        }
    }

    ;

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBMapperIntegrationTestBase.setUp();

        // Insert the data
        for (Map<String, AttributeValue> attr : attrs) {
            dynamo.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(attr).build());
        }
    }

    @Test
    public void testLoad() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        for (Map<String, AttributeValue> attr : attrs) {
            NumberSetAttributeClass x = util.load(NumberSetAttributeClass.class, attr.get(KEY_NAME).s());
            assertEquals(x.getKey(), attr.get(KEY_NAME).s());

            // Convert all numbers to the most inclusive type for easy comparison
            assertNumericSetsEquals(x.getBigDecimalAttribute(), attr.get(BIG_DECIMAL_ATTRIBUTE).ns());
            assertNumericSetsEquals(x.getBigIntegerAttribute(), attr.get(BIG_INTEGER_ATTRIBUTE).ns());
            assertNumericSetsEquals(x.getFloatObjectAttribute(), attr.get(FLOAT_OBJECT_ATTRIBUTE).ns());
            assertNumericSetsEquals(x.getDoubleObjectAttribute(), attr.get(DOUBLE_OBJECT_ATTRIBUTE).ns());
            assertNumericSetsEquals(x.getIntegerAttribute(), attr.get(INTEGER_ATTRIBUTE).ns());
            assertNumericSetsEquals(x.getLongObjectAttribute(), attr.get(LONG_OBJECT_ATTRIBUTE).ns());
            assertNumericSetsEquals(x.getByteObjectAttribute(), attr.get(BYTE_OBJECT_ATTRIBUTE).ns());
            assertSetsEqual(toSet("0", "1"), attr.get(BOOLEAN_ATTRIBUTE).ns());
        }
    }

    @Test
    public void testSave() throws Exception {
        List<NumberSetAttributeClass> objs = new ArrayList<NumberSetAttributeClass>();
        for (int i = 0; i < 5; i++) {
            NumberSetAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (NumberSetAttributeClass obj : objs) {
            util.save(obj);
        }

        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass loaded = util.load(NumberSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        List<NumberSetAttributeClass> objs = new ArrayList<NumberSetAttributeClass>();
        for (int i = 0; i < 5; i++) {
            NumberSetAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (NumberSetAttributeClass obj : objs) {
            util.save(obj);
        }

        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass replacement = getUniqueObject();
            replacement.setKey(obj.getKey());
            util.save(replacement);
            assertEquals(replacement, util.load(NumberSetAttributeClass.class, obj.getKey()));
        }
    }

    private NumberSetAttributeClass getUniqueObject() {
        NumberSetAttributeClass obj = new NumberSetAttributeClass();
        obj.setKey(String.valueOf(startKey++));
        obj.setBigDecimalAttribute(toSet(new BigDecimal(startKey++), new BigDecimal(startKey++), new BigDecimal(startKey++)));
        obj.setBigIntegerAttribute(
                toSet(new BigInteger("" + startKey++), new BigInteger("" + startKey++), new BigInteger("" + startKey++)));
        obj.setByteObjectAttribute(toSet(new Byte("" + byteStart++), new Byte("" + byteStart++), new Byte("" + byteStart++)));
        obj.setDoubleObjectAttribute(toSet(new Double("" + start++), new Double("" + start++), new Double("" + start++)));
        obj.setFloatObjectAttribute(toSet(new Float("" + start++), new Float("" + start++), new Float("" + start++)));
        obj.setIntegerAttribute(toSet(new Integer("" + start++), new Integer("" + start++), new Integer("" + start++)));
        obj.setLongObjectAttribute(toSet(new Long("" + start++), new Long("" + start++), new Long("" + start++)));
        obj.setBooleanAttribute(toSet(true, false));
        obj.setDateAttribute(toSet(new Date(startKey++), new Date(startKey++), new Date(startKey++)));
        Set<Calendar> cals = new HashSet<Calendar>();
        for (Date d : obj.getDateAttribute()) {
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(d);
            cals.add(cal);
        }
        obj.setCalendarAttribute(toSet(cals));
        return obj;
    }
}
