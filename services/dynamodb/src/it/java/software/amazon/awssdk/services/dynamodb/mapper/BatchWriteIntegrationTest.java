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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper.FailedBatch;
import software.amazon.awssdk.services.dynamodb.pojos.BinaryAttributeByteBufferClass;
import software.amazon.awssdk.services.dynamodb.pojos.RangeKeyClass;

/**
 * Tests batch write calls
 */
public class BatchWriteIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    // We don't start with the current system millis like other tests because
    // it's out of the range of some data types
    private static int start = 1;
    private static int byteStart = 1;
    private static int startKeyDebug = 1;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTableWithRangeAttribute();
    }

    @Test
    public void testBatchSave() throws Exception {
        List<NumberSetAttributeClass> objs = new ArrayList<NumberSetAttributeClass>();
        for (int i = 0; i < 40; i++) {
            NumberSetAttributeClass obj = getUniqueNumericObject();
            objs.add(obj);
        }

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        List<FailedBatch> failedBatches = mapper.batchSave(objs);

        assertEquals(0, failedBatches.size());

        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass loaded = mapper.load(NumberSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }

    @Test
    public void testBatchSaveAsArray() throws Exception {
        List<NumberSetAttributeClass> objs = new ArrayList<NumberSetAttributeClass>();
        for (int i = 0; i < 40; i++) {
            NumberSetAttributeClass obj = getUniqueNumericObject();
            objs.add(obj);
        }

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        NumberSetAttributeClass[] objsArray = objs.toArray(new NumberSetAttributeClass[objs.size()]);
        mapper.batchSave((Object[]) objsArray);

        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass loaded = mapper.load(NumberSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }

    @Test
    public void testBatchSaveAsListFromArray() throws Exception {
        List<NumberSetAttributeClass> objs = new ArrayList<NumberSetAttributeClass>();
        for (int i = 0; i < 40; i++) {
            NumberSetAttributeClass obj = getUniqueNumericObject();
            objs.add(obj);
        }

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        NumberSetAttributeClass[] objsArray = objs.toArray(new NumberSetAttributeClass[objs.size()]);
        mapper.batchSave(Arrays.asList(objsArray));

        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass loaded = mapper.load(NumberSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }

    @Test
    public void testBatchDelete() throws Exception {
        List<NumberSetAttributeClass> objs = new ArrayList<NumberSetAttributeClass>();
        for (int i = 0; i < 40; i++) {
            NumberSetAttributeClass obj = getUniqueNumericObject();
            objs.add(obj);
        }

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        mapper.batchSave(objs);

        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass loaded = mapper.load(NumberSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }

        // Delete the odd ones
        int i = 0;
        List<NumberSetAttributeClass> toDelete = new LinkedList<NumberSetAttributeClass>();
        for (NumberSetAttributeClass obj : objs) {
            if (i++ % 2 == 0) {
                toDelete.add(obj);
            }
        }

        mapper.batchDelete(toDelete);

        i = 0;
        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass loaded = mapper.load(NumberSetAttributeClass.class, obj.getKey());
            if (i++ % 2 == 0) {
                assertNull(loaded);
            } else {
                assertEquals(obj, loaded);
            }
        }
    }

    @Test
    public void testBatchSaveAndDelete() throws Exception {
        List<NumberSetAttributeClass> objs = new ArrayList<NumberSetAttributeClass>();
        for (int i = 0; i < 40; i++) {
            NumberSetAttributeClass obj = getUniqueNumericObject();
            objs.add(obj);
        }

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        mapper.batchSave(objs);

        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass loaded = mapper.load(NumberSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }

        // Delete the odd ones
        int i = 0;
        List<NumberSetAttributeClass> toDelete = new LinkedList<NumberSetAttributeClass>();
        for (NumberSetAttributeClass obj : objs) {
            if (i++ % 2 == 0) {
                toDelete.add(obj);
            }
        }

        // And add a bunch of new ones
        List<NumberSetAttributeClass> toSave = new LinkedList<NumberSetAttributeClass>();
        for (i = 0; i < 50; i++) {
            NumberSetAttributeClass obj = getUniqueNumericObject();
            toSave.add(obj);
        }

        mapper.batchWrite(toSave, toDelete);

        i = 0;
        for (NumberSetAttributeClass obj : objs) {
            NumberSetAttributeClass loaded = mapper.load(NumberSetAttributeClass.class, obj.getKey());
            if (i++ % 2 == 0) {
                assertNull(loaded);
            } else {
                assertEquals(obj, loaded);
            }
        }

        for (NumberSetAttributeClass obj : toSave) {
            NumberSetAttributeClass loaded = mapper.load(NumberSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }

    @Test
    public void testMultipleTables() throws Exception {

        List<Object> objs = new ArrayList<Object>();
        int numItems = 10;
        for (int i = 0; i < numItems; i++) {
            NumberSetAttributeClass obj = getUniqueNumericObject();
            objs.add(obj);
        }
        for (int i = 0; i < numItems; i++) {
            RangeKeyClass obj = getUniqueRangeKeyObject();
            objs.add(obj);
        }
        Collections.shuffle(objs);

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        List<FailedBatch> failedBatches = mapper.batchSave(objs);
        assertEquals(failedBatches.size(), 0);

        for (Object obj : objs) {
            Object loaded = null;
            if (obj instanceof NumberSetAttributeClass) {
                loaded = mapper.load(NumberSetAttributeClass.class, ((NumberSetAttributeClass) obj).getKey());
            } else if (obj instanceof RangeKeyClass) {
                loaded = mapper.load(RangeKeyClass.class, ((RangeKeyClass) obj).getKey(),
                                     ((RangeKeyClass) obj).getRangeKey());
            } else {
                fail();
            }
            assertEquals(obj, loaded);
        }

        // Delete the odd ones
        int i = 0;
        List<Object> toDelete = new LinkedList<Object>();
        for (Object obj : objs) {
            if (i++ % 2 == 0) {
                toDelete.add(obj);
            }
        }

        // And add a bunch of new ones
        List<Object> toSave = new LinkedList<Object>();
        for (i = 0; i < numItems; i++) {
            if (i % 2 == 0) {
                toSave.add(getUniqueNumericObject());
            } else {
                toSave.add(getUniqueRangeKeyObject());
            }
        }

        failedBatches = mapper.batchWrite(toSave, toDelete);
        assertEquals(0, failedBatches.size());

        i = 0;
        for (Object obj : objs) {
            Object loaded = null;
            if (obj instanceof NumberSetAttributeClass) {
                loaded = mapper.load(NumberSetAttributeClass.class, ((NumberSetAttributeClass) obj).getKey());
            } else if (obj instanceof RangeKeyClass) {
                loaded = mapper.load(RangeKeyClass.class, ((RangeKeyClass) obj).getKey(),
                                     ((RangeKeyClass) obj).getRangeKey());
            } else {
                fail();
            }

            if (i++ % 2 == 0) {
                assertNull(loaded);
            } else {
                assertEquals(obj, loaded);
            }
        }

        for (Object obj : toSave) {
            Object loaded = null;
            if (obj instanceof NumberSetAttributeClass) {
                loaded = mapper.load(NumberSetAttributeClass.class, ((NumberSetAttributeClass) obj).getKey());
            } else if (obj instanceof RangeKeyClass) {
                loaded = mapper.load(RangeKeyClass.class, ((RangeKeyClass) obj).getKey(),
                                     ((RangeKeyClass) obj).getRangeKey());
            } else {
                fail();
            }
            assertEquals(obj, loaded);
        }
    }

    /**
     * Test whether it finish processing all the items even if the first batch is failed.
     */
    @Test
    public void testErrorHandling() {

        List<Object> objs = new ArrayList<Object>();
        int numItems = 25;

        for (int i = 0; i < numItems; i++) {
            NoSuchTableClass obj = getuniqueBadObject();
            objs.add(obj);
        }

        for (int i = 0; i < numItems; i++) {
            RangeKeyClass obj = getUniqueRangeKeyObject();
            objs.add(obj);
        }

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);

        // The failed batch
        List<FailedBatch> failedBatches = mapper.batchSave(objs);
        assertEquals(1, failedBatches.size());
        assertEquals(numItems, failedBatches.get(0).getUnprocessedItems().get("tableNotExist").size());

        // The second batch succeeds, get them back
        for (Object obj : objs.subList(25, 50)) {
            RangeKeyClass loaded = mapper
                    .load(RangeKeyClass.class, ((RangeKeyClass) obj).getKey(), ((RangeKeyClass) obj).getRangeKey());
            assertEquals(obj, loaded);
        }
    }

    /**
     * Test whether we can split large batch request into small pieces.
     */
    // DynamoDB changed their error for requests that are too large from a
    // 413 (RequestEntityTooLarge) to a generic 400 (ValidationException), so
    // the mapper's batch-splitting logic is broken. Not sure there's a good
    // fix client-side without the service changing back to 413 so we can
    // distinguish this case from other ValidationExceptions.
    // @Test
    public void testLargeRequestEntity() {

        // The total batch size is beyond 1M, test whether our client can split
        // the batch correctly
        List<BinaryAttributeByteBufferClass> objs = new ArrayList<BinaryAttributeByteBufferClass>();

        int numItems = 25;
        final int CONTENT_LENGTH = 1024 * 25;

        for (int i = 0; i < numItems; i++) {
            BinaryAttributeByteBufferClass obj = getUniqueByteBufferObject(CONTENT_LENGTH);
            objs.add(obj);
        }

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        List<FailedBatch> failedBatches = mapper.batchSave(objs);
        assertEquals(0, failedBatches.size());

        // Get these objects back
        for (BinaryAttributeByteBufferClass obj : objs) {
            BinaryAttributeByteBufferClass loaded = mapper.load(BinaryAttributeByteBufferClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }

        // There are three super large item together with some small ones, test
        // whether we can successfully
        // save these small items.
        objs.clear();
        numItems = 10;
        List<BinaryAttributeByteBufferClass> largeObjs = new ArrayList<BinaryAttributeByteBufferClass>();

        // Put three super large item(beyond 64k)
        largeObjs.add(getUniqueByteBufferObject(CONTENT_LENGTH * 30));
        largeObjs.add(getUniqueByteBufferObject(CONTENT_LENGTH * 30));
        largeObjs.add(getUniqueByteBufferObject(CONTENT_LENGTH * 30));
        for (int i = 0; i < numItems - 3; i++) {
            BinaryAttributeByteBufferClass obj = getUniqueByteBufferObject(CONTENT_LENGTH / 25);
            objs.add(obj);
        }

        objs.addAll(largeObjs);

        failedBatches = mapper.batchSave(objs);
        assertEquals(3, failedBatches.size());
        objs.removeAll(largeObjs);

        // Get these small objects back
        for (BinaryAttributeByteBufferClass obj : objs) {
            BinaryAttributeByteBufferClass loaded = mapper.load(BinaryAttributeByteBufferClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }

        // The whole batch is super large objects, none of them will be
        // processed
        largeObjs.clear();
        for (int i = 0; i < 5; i++) {
            BinaryAttributeByteBufferClass obj = getUniqueByteBufferObject(CONTENT_LENGTH * 30);
            largeObjs.add(obj);
        }
        failedBatches = mapper.batchSave(largeObjs);
        assertEquals(5, failedBatches.size());
    }


    private NoSuchTableClass getuniqueBadObject() {
        NoSuchTableClass obj = new NoSuchTableClass();
        obj.setKey(String.valueOf(startKeyDebug++));
        return obj;
    }

    private NumberSetAttributeClass getUniqueNumericObject() {
        NumberSetAttributeClass obj = new NumberSetAttributeClass();
        obj.setKey(String.valueOf(startKeyDebug++));
        obj.setBigDecimalAttribute(toSet(new BigDecimal(startKey++), new BigDecimal(startKey++), new BigDecimal(startKey++)));
        obj.setBigIntegerAttribute(
                toSet(new BigInteger("" + startKey++), new BigInteger("" + startKey++), new BigInteger("" + startKey++)));
        obj.setByteObjectAttribute(toSet(new Byte(nextByte()), new Byte(nextByte()), new Byte(nextByte())));
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

    private RangeKeyClass getUniqueRangeKeyObject() {
        RangeKeyClass obj = new RangeKeyClass();
        obj.setKey(startKey++);
        obj.setIntegerAttribute(toSet(start++, start++, start++));
        obj.setBigDecimalAttribute(new BigDecimal(startKey++));
        obj.setRangeKey(start++);
        obj.setStringAttribute("" + startKey++);
        obj.setStringSetAttribute(toSet("" + startKey++, "" + startKey++, "" + startKey++));
        return obj;
    }

    private String nextByte() {
        return "" + byteStart++ % Byte.MAX_VALUE;
    }
}
