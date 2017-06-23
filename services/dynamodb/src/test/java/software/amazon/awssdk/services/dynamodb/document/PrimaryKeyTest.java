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

package software.amazon.awssdk.services.dynamodb.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class PrimaryKeyTest {

    @Test
    public void ctor_KeyAttributes() {
        new PrimaryKey();
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_nullKeyAttributes() {
        new PrimaryKey((KeyAttribute) null);
    }

    @Test
    public void ctor_nullValue() {
        new PrimaryKey("name", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_nullName() {
        new PrimaryKey(null, "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_emptyName() {
        new PrimaryKey("  ", "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_sameHashRangeKeyNames() {
        new PrimaryKey("key", "val1", "key", "val2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_badHashKeyName() {
        new PrimaryKey("", "val1", "key", "val2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_badRangeKeyName() {
        new PrimaryKey("key1", "val1", "", "val2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addComponents_nullElement() {
        new PrimaryKey().addComponents((KeyAttribute) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addComponent_nullName() {
        new PrimaryKey().addComponent(null, "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addComponent_emptyName() {
        new PrimaryKey().addComponent("  ", "val");
    }

    @Test
    public void addComponent_nullVal() {
        new PrimaryKey().addComponent("key", null);
    }

    @Test
    public void addComponents_null() {
        new PrimaryKey().addComponents((KeyAttribute[]) null);
    }

    @Test
    public void ctor_nullHashRangeKeys() {
        new PrimaryKey("hashkey", null, "rangekey", null);
    }

    @Test
    public void testEquals() {
        assertEquals(new PrimaryKey("hashkey", null, "rangekey", null),
                     new PrimaryKey("hashkey", null, "rangekey", null));

        assertEquals(new PrimaryKey("k1", "v1", "k2", "v2"),
                     new PrimaryKey("k1", "v1", "k2", "v2"));
        assertFalse(new PrimaryKey("k1", "v1").equals(new Attribute("k1", "v1")));
        assertFalse(new PrimaryKey("k1", "v1").equals(null));

        Set<PrimaryKey> set = new HashSet<PrimaryKey>();
        set.add(new PrimaryKey("k1", "v1", "k2", "v2"));
        set.add(new PrimaryKey("k1", "v1", "k2", "v2"));
        assertTrue(set.size() == 1);

        set.add(new PrimaryKey("k1", "v1"));
        assertTrue(set.size() == 2);
    }

    @Test
    public void hasComponent() {
        assertTrue(new PrimaryKey("hashkey", null, "rangekey", null).hasComponent("hashkey"));
        assertTrue(new PrimaryKey("hashkey", null, "rangekey", null).hasComponent("rangekey"));
        assertFalse(new PrimaryKey("hashkey", null, "rangekey", null).hasComponent("notExist"));
        assertTrue(new PrimaryKey("k1", "v1", "k2", "v2").hasComponent("k1"));
        assertTrue(new PrimaryKey("k1", "v1", "k2", "v2").hasComponent("k2"));
        assertFalse(new PrimaryKey("k1", "v1", "k2", "v2").hasComponent("notExist"));
    }
}
