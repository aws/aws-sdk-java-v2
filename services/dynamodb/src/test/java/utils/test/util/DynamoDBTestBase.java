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

package utils.test.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.core.AmazonClientException;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.Logger;

public class DynamoDBTestBase extends AwsTestBase {
    protected static final String ENDPOINT = "http://dynamodb.us-east-1.amazonaws.com/";

    protected static DynamoDBClient dynamo;

    private static final Logger log = Logger.loggerFor(DynamoDBTestBase.class);

    public static void setUpTestBase() {
        try {
            setUpCredentials();
        } catch (Exception e) {
            throw new AmazonClientException("Unable to load credential property file.", e);
        }

        dynamo = DynamoDBClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    public static DynamoDBClient getClient() {
        if (dynamo == null) {
            setUpTestBase();
        }
        return dynamo;
    }

    protected static void waitForTableToBecomeDeleted(String tableName) {
        waitForTableToBecomeDeleted(dynamo, tableName);
    }

    public static void waitForTableToBecomeDeleted(DynamoDBClient dynamo, String tableName) {
        log.info(() -> "Waiting for " + tableName + " to become Deleted...");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (60_000);
        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(5_000);
            } catch (Exception e) {
                // Ignored or expected.
            }
            try {
                DescribeTableRequest request = DescribeTableRequest.builder().tableName(tableName).build();
                TableDescription table = dynamo.describeTable(request).table();

                log.info(() -> "  - current state: " + table.tableStatusString());
                if (table.tableStatus() == TableStatus.DELETING) {
                    continue;
                }
            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException")) {
                    log.info(() -> "successfully deleted");
                    return;
                }
            }
        }

        throw new RuntimeException("Table " + tableName + " never went deleted");
    }

    protected static <T extends Object> void assertSetsEqual(Collection<T> expected, Collection<T> given) {
        Set<T> givenCopy = new HashSet<T>();
        givenCopy.addAll(given);
        for (T e : expected) {
            if (!givenCopy.remove(e)) {
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
        for (T t : array) {
            set.add(t);
        }
        return set;
    }

    protected static <T extends Object> Set<T> toSet(Collection<T> collection) {
        Set<T> set = new HashSet<T>();
        for (T t : collection) {
            set.add(t);
        }
        return set;
    }

    protected static byte[] generateByteArray(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i % Byte.MAX_VALUE);
        }
        return bytes;
    }

    /**
     * Gets a map of key values for the single hash key attribute value given.
     */
    protected Map<String, AttributeValue> mapKey(String attributeName, AttributeValue value) {
        HashMap<String, AttributeValue> map = new HashMap<String, AttributeValue>();
        map.put(attributeName, value);
        return map;
    }

}
