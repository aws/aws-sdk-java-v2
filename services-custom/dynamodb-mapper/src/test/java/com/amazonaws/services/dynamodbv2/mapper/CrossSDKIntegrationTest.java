/*
 * Copyright 2013 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.services.dynamodbv2.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.DynamoDBMapperIntegrationTestBase;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.pojos.CrossSDKVerificationClass;
import com.amazonaws.services.dynamodbv2.util.TableUtils;


/**
 * Cross-SDK acceptance test. More of a smoke test, verifies that the formats
 * used by each program's ORM can be read by the others.
 */
public class CrossSDKIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String TABLE_NAME = "aws-xsdk";

    private static final String HASH_KEY = "3530a51a-0760-47d2-bfcb-158320d6188a";
    private static final String RANGE_KEY = "61cdf81e-792f-4dd8-a812-a16185bfbf60";

    private static int start = 1;

    // @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        dynamo = new AmazonDynamoDBClient(credentials);

        // Create a table
        String keyName = DynamoDBMapperIntegrationTestBase.KEY_NAME;
        String rangeKey = "rangeKey";
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(TABLE_NAME)
                .withKeySchema(new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH),
                        new KeySchemaElement().withAttributeName(rangeKey).withKeyType(KeyType.RANGE))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(keyName).withAttributeType(
                                ScalarAttributeType.S),
                        new AttributeDefinition().withAttributeName(rangeKey).withAttributeType(
                                ScalarAttributeType.S));
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(10L)
                .withWriteCapacityUnits(10L));

        if (TableUtils.createTableIfNotExists(dynamo, createTableRequest)) {
            TableUtils.waitUntilActive(dynamo, TABLE_NAME);
        }
    }

    @Test
    public void disabled() {
    }

    // This record written by the .NET mapper no longer exists, so this test
    // NPEs. If we want to add back something similar we should generate some
    // items using the .NET mapper and check a serialized form of them into
    // this package so this can be run as a unit test.
    // @Test
    public void testLoad() throws Exception {
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);

        CrossSDKVerificationClass obj = mapper.load(CrossSDKVerificationClass.class, HASH_KEY, RANGE_KEY);

        Long originalVersion = obj.getVersion();

        assertNotNull(obj);
        assertNotNull(obj.getKey());
        assertEquals(obj.getKey(), HASH_KEY);
        assertNotNull(obj.getRangeKey());
        assertEquals(obj.getRangeKey(), RANGE_KEY);
        assertNotNull(originalVersion);
        assertNotNull(obj.getBigDecimalAttribute());
        assertNotNull(obj.getBigDecimalSetAttribute());
        assertEquals(3, obj.getBigDecimalSetAttribute().size());
        assertNotNull(obj.getBigIntegerAttribute());
        assertNotNull(obj.getBigIntegerSetAttribute());
        assertEquals(3, obj.getBigIntegerSetAttribute().size());
        assertNotNull(obj.getBooleanAttribute());
        assertNotNull(obj.getBooleanSetAttribute());
        assertEquals(2, obj.getBooleanSetAttribute().size());
        assertNotNull(obj.getByteAttribute());
        assertNotNull(obj.getByteSetAttribute());
        assertEquals(3, obj.getByteSetAttribute().size());
        assertNotNull(obj.getCalendarAttribute());
        assertNotNull(obj.getCalendarSetAttribute());
        assertEquals(3, obj.getCalendarSetAttribute().size());
        assertNotNull(obj.getDateAttribute());
        assertNotNull(obj.getDateSetAttribute());
        assertEquals(3, obj.getDateSetAttribute().size());
        assertNotNull(obj.getDoubleAttribute());
        assertNotNull(obj.getDoubleSetAttribute());
        assertEquals(3, obj.getDoubleSetAttribute().size());
        assertNotNull(obj.getFloatAttribute());
        assertNotNull(obj.getFloatSetAttribute());
        assertEquals(3, obj.getFloatSetAttribute().size());
        assertNotNull(obj.getIntegerAttribute());
        assertNotNull(obj.getIntegerSetAttribute());
        assertEquals(3, obj.getIntegerSetAttribute().size());
        assertNotNull(obj.getLongAttribute());
        assertNotNull(obj.getLongSetAttribute());
        assertEquals(3, obj.getLongSetAttribute().size());
        assertNotNull(obj.getStringSetAttribute());
        assertEquals(3, obj.getStringSetAttribute().size());

        updateObjectValues(obj);

        mapper.save(obj);
        assertFalse(originalVersion.equals(obj.getVersion()));

        CrossSDKVerificationClass loaded = mapper.load(CrossSDKVerificationClass.class, HASH_KEY, RANGE_KEY);
        assertEquals(loaded, obj);
    }

    /**
     * Updates all values in the object (except for the keys and version)
     */
    private void updateObjectValues(CrossSDKVerificationClass obj) {
        obj.setBigDecimalAttribute(obj.getBigDecimalAttribute().add(BigDecimal.ONE));
        Set<BigDecimal> bigDecimals = new HashSet<BigDecimal>();
        for (BigDecimal d : obj.getBigDecimalSetAttribute()) {
            bigDecimals.add(d.add(BigDecimal.ONE));
        }
        obj.setBigDecimalSetAttribute(bigDecimals);

        obj.setBigIntegerAttribute(obj.getBigIntegerAttribute().add(BigInteger.ONE));
        Set<BigInteger> bigInts = new HashSet<BigInteger>();
        for (BigInteger d : obj.getBigIntegerSetAttribute()) {
            bigInts.add(d.add(BigInteger.ONE));
        }
        obj.setBigIntegerSetAttribute(bigInts);

        obj.setBooleanAttribute(!obj.getBooleanAttribute());

        obj.setByteAttribute((byte) ((obj.getByteAttribute() +1) % Byte.MAX_VALUE));
        Set<Byte> bytes = new HashSet<Byte>();
        for (Byte b : obj.getByteSetAttribute()) {
            bytes.add((byte) ((b +1) % Byte.MAX_VALUE));
        }

        obj.getCalendarAttribute().setTime(new Date(obj.getCalendarAttribute().getTimeInMillis() + 1000));
        for (Calendar c : obj.getCalendarSetAttribute()) {
            c.setTime(new Date(c.getTimeInMillis() + 1000));
        }

        obj.getDateAttribute().setTime(obj.getDateAttribute().getTime() + 1000);
        for (Date d : obj.getDateSetAttribute()) {
            d.setTime(d.getTime() + 1000);
        }

        obj.setDoubleAttribute(obj.getDoubleAttribute() + 1.0);
        Set<Double> doubleSet = new HashSet<Double>();
        for (Double d : obj.getDoubleSetAttribute()) {
            doubleSet.add(d + 1.0);
        }
        obj.setDoubleSetAttribute(doubleSet);

        obj.setFloatAttribute((float) (obj.getFloatAttribute() + 1.0));
        Set<Float> floatSet = new HashSet<Float>();
        for (Float f : obj.getFloatSetAttribute()) {
            floatSet.add(f + 1.0f);
        }
        obj.setFloatSetAttribute(floatSet);

        obj.setIntegerAttribute(obj.getIntegerAttribute() + 1);
        Set<Integer> intSet = new HashSet<Integer>();
        for (Integer i : obj.getIntegerSetAttribute()) {
            intSet.add(i + 1);
        }
        obj.setIntegerSetAttribute(intSet);

        obj.setLastUpdater("java-sdk");

        obj.setLongAttribute(obj.getLongAttribute() + 1);
        Set<Long> longSet = new HashSet<Long>();
        for (Long l : obj.getLongSetAttribute()) {
            longSet.add(l + 1);
        }
        obj.setLongSetAttribute(longSet);

        obj.setStringSetAttribute(toSet(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    }

    /**
     * Used to set up the original object, no longer used.
     */
    @SuppressWarnings("unused")
    private CrossSDKVerificationClass getUniqueObject() {
        CrossSDKVerificationClass obj = new CrossSDKVerificationClass();
        obj.setKey(HASH_KEY);
        obj.setRangeKey(RANGE_KEY);
        obj.setBigDecimalAttribute(new BigDecimal(start++));
        obj.setBigDecimalSetAttribute(toSet(new BigDecimal(start++), new BigDecimal(start++), new BigDecimal(start++)));
        obj.setBigIntegerAttribute(new BigInteger("" + start++));
        obj.setBigIntegerSetAttribute(toSet(new BigInteger("" + start++), new BigInteger("" + start++), new BigInteger("" + start++)));
        obj.setBooleanAttribute(start++ % 2 == 0);
        obj.setBooleanSetAttribute(toSet(true, false));
        obj.setByteAttribute((byte) start++);
        obj.setByteSetAttribute(toSet((byte) start++, (byte) start++, (byte) start++));
        obj.setCalendarAttribute(getUniqueCalendar());
        obj.setCalendarSetAttribute(toSet(getUniqueCalendar(), getUniqueCalendar(), getUniqueCalendar()));
        obj.setDateAttribute(new Date(start++));
        obj.setDateSetAttribute(toSet(new Date(start++), new Date(start++), new Date(start++)));
        obj.setDoubleAttribute((double) start++);
        obj.setDoubleSetAttribute(toSet((double) start++, (double) start++, (double) start++));
        obj.setFloatAttribute((float) start++);
        obj.setFloatSetAttribute(toSet((float) start++, (float) start++, (float) start++));
        obj.setIntegerAttribute(start++);
        obj.setIntegerSetAttribute(toSet(start++, start++, start++));
        obj.setLongAttribute((long) start++);
        obj.setLongSetAttribute(toSet((long) start++, (long) start++, (long) start++));
        obj.setStringSetAttribute(toSet("" + start++, "" + start++, "" + start++));
        return obj;
    }

    private Calendar getUniqueCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(start++));
        return cal;
    }

}
