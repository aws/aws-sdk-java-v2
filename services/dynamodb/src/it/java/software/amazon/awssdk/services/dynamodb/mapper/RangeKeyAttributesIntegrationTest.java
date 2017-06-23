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
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.pojos.RangeKeyClass;

/**
 * Tests range and hash key combination
 */
public class RangeKeyAttributesIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String RANGE_KEY = "rangeKey";
    private static final String INTEGER_ATTRIBUTE = "integerSetAttribute";
    private static final String BIG_DECIMAL_ATTRIBUTE = "bigDecimalAttribute";
    private static final String STRING_SET_ATTRIBUTE = "stringSetAttribute";
    private static final String STRING_ATTRIBUTE = "stringAttribute";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final List<Map<String, AttributeValue>> attrs = new LinkedList<Map<String, AttributeValue>>();
    // We don't start with the current system millis like other tests because
    // it's out of the range of some data types
    private static int start = 1;

    // Test data
    static {
        for (int i = 0; i < 5; i++) {
            Map<String, AttributeValue> attr = new HashMap<String, AttributeValue>();
            attr.put(KEY_NAME, AttributeValue.builder().n("" + startKey++).build());
            attr.put(RANGE_KEY, AttributeValue.builder().n("" + start++).build());
            attr.put(INTEGER_ATTRIBUTE, AttributeValue.builder().ns("" + start++, "" + start++, "" + start++).build());
            attr.put(BIG_DECIMAL_ATTRIBUTE, AttributeValue.builder().n("" + start++).build());
            attr.put(STRING_ATTRIBUTE, AttributeValue.builder().s("" + start++).build());
            attr.put(STRING_SET_ATTRIBUTE, AttributeValue.builder().ss("" + start++, "" + start++, "" + start++).build());
            attr.put(VERSION_ATTRIBUTE, AttributeValue.builder().n("1").build());

            attrs.add(attr);
        }
    }

    ;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTableWithRangeAttribute();

        // Insert the data
        for (Map<String, AttributeValue> attr : attrs) {
            dynamo.putItem(PutItemRequest.builder().tableName(TABLE_WITH_RANGE_ATTRIBUTE).item(attr).build());
        }
    }

    @Test
    public void testLoad() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        for (Map<String, AttributeValue> attr : attrs) {
            RangeKeyClass x = util.load(newRangeKey(Long.parseLong(attr.get(KEY_NAME).n()),
                                                    Double.parseDouble(attr.get(RANGE_KEY).n())));

            // Convert all numbers to the most inclusive type for easy
            // comparison
            assertEquals(new BigDecimal(x.getKey()), new BigDecimal(attr.get(KEY_NAME).n()));
            assertEquals(new BigDecimal(x.getRangeKey()), new BigDecimal(attr.get(RANGE_KEY).n()));
            assertEquals(new BigDecimal(x.getVersion()), new BigDecimal(attr.get(VERSION_ATTRIBUTE).n()));
            assertEquals(x.getBigDecimalAttribute(), new BigDecimal(attr.get(BIG_DECIMAL_ATTRIBUTE).n()));
            assertNumericSetsEquals(x.getIntegerAttribute(), attr.get(INTEGER_ATTRIBUTE).ns());
            assertEquals(x.getStringAttribute(), attr.get(STRING_ATTRIBUTE).s());
            assertSetsEqual(x.getStringSetAttribute(), toSet(attr.get(STRING_SET_ATTRIBUTE).ss()));
        }
    }

    private RangeKeyClass newRangeKey(long hashKey, double rangeKey) {
        RangeKeyClass obj = new RangeKeyClass();
        obj.setKey(hashKey);
        obj.setRangeKey(rangeKey);
        return obj;
    }

    @Test
    public void testSave() throws Exception {
        List<RangeKeyClass> objs = new ArrayList<RangeKeyClass>();
        for (int i = 0; i < 5; i++) {
            RangeKeyClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (RangeKeyClass obj : objs) {
            util.save(obj);
        }

        for (RangeKeyClass obj : objs) {
            RangeKeyClass loaded = util.load(RangeKeyClass.class, obj.getKey(), obj.getRangeKey());
            assertEquals(obj, loaded);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        List<RangeKeyClass> objs = new ArrayList<RangeKeyClass>();
        for (int i = 0; i < 5; i++) {
            RangeKeyClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (RangeKeyClass obj : objs) {
            util.save(obj);
        }

        for (RangeKeyClass obj : objs) {
            RangeKeyClass replacement = getUniqueObject();
            replacement.setKey(obj.getKey());
            replacement.setRangeKey(obj.getRangeKey());
            replacement.setVersion(obj.getVersion());
            util.save(replacement);

            RangeKeyClass loadedObject = util.load(RangeKeyClass.class, obj.getKey(), obj.getRangeKey());
            assertEquals(replacement, loadedObject);

            // If we try to update the old version, we should get an error
            replacement.setVersion(replacement.getVersion() - 1);
            try {
                util.save(replacement);
                fail("Should have thrown an exception");
            } catch (Exception expected) {
                // Ignored or expected.
            }
        }
    }

    private RangeKeyClass getUniqueObject() {
        RangeKeyClass obj = new RangeKeyClass();
        obj.setKey(startKey++);
        obj.setIntegerAttribute(toSet(start++, start++, start++));
        obj.setBigDecimalAttribute(new BigDecimal(startKey++));
        obj.setRangeKey(start++);
        obj.setStringAttribute("" + startKey++);
        obj.setStringSetAttribute(toSet("" + startKey++, "" + startKey++, "" + startKey++));
        return obj;
    }
}
