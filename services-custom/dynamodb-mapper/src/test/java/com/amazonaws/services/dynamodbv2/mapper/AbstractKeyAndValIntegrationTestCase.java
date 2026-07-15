/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.ConsistentReads.CONSISTENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.amazonaws.services.dynamodbv2.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.pojos.KeyAndVal;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

/**
 * Tests updating component attribute fields correctly.
 */
public abstract class AbstractKeyAndValIntegrationTestCase extends DynamoDBMapperIntegrationTestBase {

    /**
     * The DynamoDBMapper instance.
     */
    protected DynamoDBMapper util;

    /**
     * Sets up the test case.
     */
    protected final void setUpTest(final SaveBehavior saveBehavior) {
        this.util = new DynamoDBMapper(dynamo, new DynamoDBMapperConfig.Builder().withSaveBehavior(saveBehavior)
                                                                                 .withConsistentReads(CONSISTENT)
                                                                                 .build());
    }

    /**
     * Sets up the test case.
     */
    @Before
    public void setUpTest() {
        setUpTest(DynamoDBMapperConfig.DEFAULT.getSaveBehavior());
    }

    /**
     * Tears down the test case.
     */
    @After
    public void tearDownTest() {
        this.util = null;
    }

    /**
     * Assert that the object updated appropriately.
     * @param changeExpected True if a change is expected.
     * @param objects The objects.
     */
    protected final <K,V> void assertBeforeAndAfterChange(final boolean changeExpected, final List<? extends KeyAndVal<K,V>> objects) {
        final List<V> befores = new ArrayList<V>(objects.size());
        for (final KeyAndVal<K,V> object : objects) {
            befores.add(object.getVal());
        }
        this.util.batchSave(objects);
        for (int i = 0, its = objects.size(); i < its; i++) {
            assertBeforeAndAfterChange(changeExpected, befores.get(i), objects.get(i).getVal());
        }
    }

    /**
     * Assert that the object updated appropriately.
     * @param changeExpected True if a change is expected.
     * @param object The object.
     * @return The value if more assertions are required.
     */
    protected final <K,V> V assertBeforeAndAfterChange(final Boolean changeExpected, final KeyAndVal<K,V> object) {
        final V before = object.getVal();
        this.util.save(object);
        final V after = object.getVal();
        if (changeExpected != null) {
            assertBeforeAndAfterChange(changeExpected, before, after);
        }
        final KeyAndVal<K,V> reload = this.util.load(object.getClass(), object.getKey());
        assertNotNull(reload);
        if (changeExpected != null) {
            assertBeforeAndAfterChange(false, after, reload.getVal());
            assertBeforeAndAfterChange(changeExpected, before, reload.getVal());
        }
        return reload.getVal();
    }

    /**
     * Assert that the object updated appropriately.
     * @param changeExpected True if a change is expected.
     * @param before The before value.
     * @param after The after value.
     */
    protected final <V> void assertBeforeAndAfterChange(final boolean changeExpected, final V before, final V after) {
        if (!changeExpected) {
            assertEquals(String.format("Expected before[%s] and after[%s] to be equal", before, after), before, after);
        } else if (before == null) {
            assertNotNull(String.format("Expected after[%s] to not be null", after), after);
        } else {
            assertFalse(String.format("Expected before[%s] and after[%s] to not be equal", before, after), before.equals(after));
        }
    }

}
