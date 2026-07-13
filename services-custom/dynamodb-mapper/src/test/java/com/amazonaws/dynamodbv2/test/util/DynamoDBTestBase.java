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
package com.amazonaws.dynamodbv2.test.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.test.AWSTestBase;

public class DynamoDBTestBase extends AWSTestBase {

    protected static final String ENDPOINT = "http://dynamodb.us-east-1.amazonaws.com/";

    protected static AmazonDynamoDBClient dynamo;

    /**
     * Gets a map of key values for the single hash key attribute value given.
     */
    protected Map<String, AttributeValue> getMapKey(String attributeName, AttributeValue value) {
        HashMap<String, AttributeValue> map = new HashMap<String, AttributeValue>();
        map.put(attributeName, value);
        return map;
    }

    public static void setUpTestBase() {
        try {
            setUpCredentials();
        } catch (Exception e) {
            throw new AmazonClientException("Unable to load credential property file.", e);
        }
        
        dynamo = new AmazonDynamoDBClient(credentials);
        dynamo.setEndpoint(ENDPOINT);
    }

    public static AmazonDynamoDB getClient() {
        if (dynamo == null) {
            setUpTestBase();
        }
        return dynamo;
    }

    protected static void waitForTableToBecomeDeleted(String tableName) {
        waitForTableToBecomeDeleted(dynamo, tableName);
    }

    public static void waitForTableToBecomeDeleted(AmazonDynamoDB dynamo, String tableName) {
        System.out.println("Waiting for " + tableName + " to become Deleted...");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while ( System.currentTimeMillis() < endTime ) {
            try {
                Thread.sleep(1000 * 20);
            } catch ( Exception e ) {
            }
            try {
                DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
                TableDescription table = dynamo.describeTable(request).getTable();

                String tableStatus = table.getTableStatus();
                System.out.println("  - current state: " + tableStatus);
                if ( tableStatus.equals(TableStatus.DELETING.toString()) )
                    continue;
            } catch ( AmazonServiceException ase ) {
                if ( ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == true ){
                     System.out.println("successfully deleted");
                    return;
                    }
            }
        }

        throw new RuntimeException("Table " + tableName + " never went deleted");
    }

    protected static <T extends Object> void assertSetsEqual(Collection<T> expected, Collection<T> given) {
        Set<T> givenCopy = new HashSet<T>();
        givenCopy.addAll(given);
        for ( T e : expected ) {
            if ( !givenCopy.remove(e) ) {
                fail("Expected element not found: " + e);
            }
        }

        assertTrue("Unexpected elements found: " + givenCopy, givenCopy.isEmpty());
    }

    /**
     * Only valid for whole numbers
     */
    protected static void assertNumericSetsEquals(Set<? extends Number> expected, Collection<String> given) {
        Set<BigDecimal> givenCopy = new HashSet<BigDecimal>();
        for (String s : given) {
            BigDecimal bd = new BigDecimal(s);
            givenCopy.add(bd.setScale(0));
        }

        Set<BigDecimal> expectedCopy = new HashSet<BigDecimal>();
        for (Number n : expected) {
            BigDecimal bd = new BigDecimal(n.toString());
            expectedCopy.add(bd.setScale(0));
        }

        assertSetsEqual(expectedCopy, givenCopy);
    }

    protected static <T extends Object> Set<T> toSet(T... array) {
        Set<T> set = new HashSet<T>();
        for ( T t : array ) {
            set.add(t);
        }
        return set;
    }

    protected static <T extends Object> Set<T> toSet(Collection<T> collection) {
        Set<T> set = new HashSet<T>();
        for ( T t : collection ) {
            set.add(t);
        }
        return set;
    }

    protected static byte[] generateByteArray(int length) {
        byte [] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte)(i % Byte.MAX_VALUE);
        }
        return bytes;
    }

}
