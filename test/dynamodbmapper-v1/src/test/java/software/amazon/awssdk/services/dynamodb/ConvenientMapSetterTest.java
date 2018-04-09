/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb;

import org.junit.Test;

/**
 * Tests on using convenient map setters.
 */
public class ConvenientMapSetterTest {

    /** Test on using map entry adder method. */
    @Test
    public void testMapEntryAdderMethod() {
// NOTE(dongie): Convenience setters are not generated
//        PutItemRequest putItemRequest = new PutItemRequest()
//                .addItemEntry("hash-key", AttributeValue.builder().withS("1"))
//                .addItemEntry("range-key", AttributeValue.builder().withS("2"))
//                .addItemEntry("attribute", AttributeValue.builder().withS("3"));
//
//        Map<String, AttributeValue> item = putItemRequest.getItem();
//        assertEquals(3, item.size());
//        assertEquals("1", item.get("hash-key").s());
//        assertEquals("2", item.get("range-key").s());
//        assertEquals("3", item.get("attribute").s());
//
//        putItemRequest.clearItemEntries();
//        assertNull(putItemRequest.getItem());
//    }
//
//    /** Test on using predefined map entry setter to provide map parameter. */
//    @Test
//    public void testPredefinedMapEntryMethod() {
//        ScanRequest scanRequest = new ScanRequest().withExclusiveStartKey(
//                new AbstractMap.SimpleEntry<String, AttributeValue>("hash-key", AttributeValue.builder().withS("1")),
//                new AbstractMap.SimpleEntry<String, AttributeValue>("range-key", AttributeValue.builder().withS("2")));
//
//        Map<String, AttributeValue> item = scanRequest.getExclusiveStartKey();
//        assertEquals(2, item.size());
//        assertEquals("1", item.get("hash-key").s());
//        assertEquals("2", item.get("range-key").s());
//    }
//
//    /** Test on IllegalArgumentException when providing duplicated keys. */
//    @Test(expected = IllegalArgumentException.class)
//    public void testDuplicatedKeysException() {
//        new PutItemRequest()
//                .addItemEntry("hash-key", AttributeValue.builder().withS("1"))
//                .addItemEntry("hash-key", AttributeValue.builder().withS("2"));
//    }
//
//    /** Test on handling null entry objects. */
//    @Test
//    public void testNullEntryException() {
//        // hashKey is set as not nullable, and rangeKey is nullable
//        // so this call should be fine.
//        ScanRequest scanRequest = new ScanRequest().withExclusiveStartKey(
//                new AbstractMap.SimpleEntry<String, AttributeValue>("hash-key", AttributeValue.builder().withS("1")),
//                null);
//
//        // but this call should throw IllegalArgumentException.
//        try {
//            scanRequest.withExclusiveStartKey(
//                    null,
//                    new AbstractMap.SimpleEntry<String, AttributeValue>("hash-key", AttributeValue.builder().withS("1")));
//            fail("Should throw IllegalArgumentException.");
//        } catch (IllegalArgumentException iae) {
//            // Ignored or expected.
//        }
    }
}
