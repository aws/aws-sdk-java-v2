/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the behavior of the {@link CapacityManager}
 */
public class CapacityManagerTest {

    /**
     * Tests that capacity can be acquired when available and can not be
     * once exhausted.
     */
    @Test
    public void acquire() {
        CapacityManager mgr = new CapacityManager(10);
        Assertions.assertTrue(mgr.acquire());
        Assertions.assertEquals(mgr.availableCapacity(), 9);
        Assertions.assertEquals(mgr.consumedCapacity(), 1);
        Assertions.assertTrue(mgr.acquire(9));
        Assertions.assertEquals(mgr.availableCapacity(), 0);
        Assertions.assertEquals(mgr.consumedCapacity(), 10);
        Assertions.assertFalse(mgr.acquire(1));
    }

    /**
     * Tests that capacity can be properly released, making additional capacity
     * available to be acquired.
     */
    @Test
    public void release() {
        CapacityManager mgr = new CapacityManager(10);
        mgr.acquire(10);
        mgr.release();
        Assertions.assertEquals(mgr.availableCapacity(), 1);
        Assertions.assertEquals(mgr.consumedCapacity(), 9);
        mgr.release(50);
        Assertions.assertEquals(mgr.availableCapacity(), 10);
        Assertions.assertEquals(mgr.consumedCapacity(), 0);
    }

    /**
     * Tests that, if created with negative capacity, CapacityManager effectively operates
     * in a no-op mode.
     */
    @Test
    public void noOp() {
        CapacityManager mgr = new CapacityManager(-1);
        Assertions.assertTrue(mgr.acquire());
        mgr.release();
        Assertions.assertTrue(mgr.acquire());
        Assertions.assertEquals(mgr.availableCapacity(), -1);
        Assertions.assertEquals(mgr.consumedCapacity(), 0);
    }

}
