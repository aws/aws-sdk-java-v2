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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.pojos.KeyAndVal;

/**
 * Tests updating component attribute fields correctly.
 */
public abstract class AbstractKeyAndValIntegrationTestCase extends DynamoDBMapperIntegrationTestBase {

    /**
     * The DynamoDBMapper instance.
     */
    protected DynamoDbMapper util;

    /**
     * Sets up the test case.
     */
    protected final void setUpTest(final DynamoDbMapperConfig.SaveBehavior saveBehavior) {
        this.util = new DynamoDbMapper(dynamo, new DynamoDbMapperConfig.Builder().withSaveBehavior(saveBehavior).build());
    }

    /**
     * Sets up the test case.
     */
    @Before
    public void setUpTest() {
        setUpTest(DynamoDbMapperConfig.DEFAULT.saveBehavior());
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
     *
     * @param changeExpected True if a change is expected.
     * @param objects        The objects.
     */
    protected final <K, V> void assertBeforeAndAfterChange(final boolean changeExpected,
                                                           final List<? extends KeyAndVal<K, V>> objects) {
        final List<V> befores = new ArrayList<V>(objects.size());
        for (final KeyAndVal<K, V> object : objects) {
            befores.add(object.getVal());
        }
        this.util.batchSave(objects);
        for (int i = 0, its = objects.size(); i < its; i++) {
            assertBeforeAndAfterChange(changeExpected, befores.get(i), objects.get(i).getVal());
        }
    }

    /**
     * Assert that the object updated appropriately.
     *
     * @param changeExpected True if a change is expected.
     * @param object         The object.
     * @return The value if more assertions are required.
     */
    protected final <K, V> V assertBeforeAndAfterChange(final Boolean changeExpected, final KeyAndVal<K, V> object) {
        final V before = object.getVal();
        this.util.save(object);
        final V after = object.getVal();
        if (changeExpected != null) {
            assertBeforeAndAfterChange(changeExpected, before, after);
        }
        final KeyAndVal<K, V> reload = this.util.load(object.getClass(), object.getKey());
        assertNotNull(reload);
        if (changeExpected != null) {
            assertBeforeAndAfterChange(false, after, reload.getVal());
            assertBeforeAndAfterChange(changeExpected, before, reload.getVal());
        }
        return reload.getVal();
    }

    /**
     * Assert that the object updated appropriately.
     *
     * @param changeExpected True if a change is expected.
     * @param before         The before value.
     * @param after          The after value.
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
