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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class AttributeTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullAttributeName() {
        new Attribute(null, "invalid attribute name");
    }

    @Test
    public void nullAttributeValue() {
        Attribute a = new Attribute("null attribute value is fine", null);
        assertTrue(a.hashCode() != 0);
    }

    @Test
    public void testHashCode() {
        Attribute a1 = new Attribute("name", null);
        Attribute a2 = new Attribute("name", "a2");
        Attribute a3 = new Attribute("name", "a3");
        Attribute a4 = new Attribute("name4", "a3");
        Set<Integer> checkUniqueness = new HashSet<Integer>();
        checkUniqueness.add(a1.hashCode());
        checkUniqueness.add(a2.hashCode());
        checkUniqueness.add(a3.hashCode());
        checkUniqueness.add(a4.hashCode());
        assertTrue(checkUniqueness.size() == 4);
    }

    @Test
    public void testEquals() {
        Attribute a1 = new Attribute("name", null);
        Attribute a2 = new Attribute("name", "a2");
        Attribute a3 = new Attribute("name", "a3");
        Attribute a4 = new Attribute("name4", "a3");
        Set<Attribute> checkUniqueness = new HashSet<Attribute>();
        checkUniqueness.add(a1);
        checkUniqueness.add(a2);
        checkUniqueness.add(a3);
        checkUniqueness.add(a4);
        assertTrue(checkUniqueness.size() == 4);

        assertTrue(checkUniqueness.contains(new Attribute("name", null)));
        assertTrue(checkUniqueness.contains(new Attribute("name", "a2")));
        assertTrue(checkUniqueness.contains(new Attribute("name", "a3")));
        assertTrue(checkUniqueness.contains(new Attribute("name4", "a3")));

        assertFalse(checkUniqueness.contains(new Attribute("not", "exist")));
        assertFalse(a1.equals("name"));
        assertFalse(a1.equals(null));
    }
}
