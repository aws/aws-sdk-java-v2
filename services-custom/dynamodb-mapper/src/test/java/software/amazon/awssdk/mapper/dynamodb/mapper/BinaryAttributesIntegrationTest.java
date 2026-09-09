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

package software.amazon.awssdk.mapper.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.mapper.dynamodb.pojos.BinaryAttributeByteArrayClass;
import software.amazon.awssdk.mapper.dynamodb.pojos.BinaryAttributeByteBufferClass;

/**
 * Tests simple string attributes
 */
public class BinaryAttributesIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String BINARY_ATTRIBUTE = "binaryAttribute";
    private static final String BINARY_SET_ATTRIBUTE = "binarySetAttribute";
    private static final List<Map<String, AttributeValue>> attrs = new LinkedList<Map<String, AttributeValue>>();
    private static final int contentLength = 512;
    // Test data
    static {
            Map<String, AttributeValue> attr = new HashMap<String, AttributeValue>();
            attr.put(KEY_NAME, AttributeValue.builder().s("" + startKey++).build());
            attr.put(BINARY_ATTRIBUTE, AttributeValue.builder().b(SdkBytes.fromByteBuffer(ByteBuffer.wrap(generateByteArray(contentLength)))).build());
            attr.put(BINARY_SET_ATTRIBUTE, AttributeValue.builder().
            		bs(SdkBytes.fromByteBuffer(ByteBuffer.wrap(generateByteArray(contentLength))),
            				SdkBytes.fromByteBuffer(ByteBuffer.wrap(generateByteArray(contentLength + 1)))).build());
            attrs.add(attr);

    };

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBMapperIntegrationTestBase.setUp();

        // Insert the data
        for ( Map<String, AttributeValue> attr : attrs ) {
            dynamo.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(attr).build());
        }
    }

    @Test
    public void testLoad() throws Exception {
        DynamoDBMapper util = new DynamoDBMapper(dynamo);

        for ( Map<String, AttributeValue> attr : attrs ) {
        	// test BinaryAttributeClass
            BinaryAttributeByteBufferClass x = util.load(BinaryAttributeByteBufferClass.class, attr.get(KEY_NAME).s());
            assertEquals(x.getKey(), attr.get(KEY_NAME).s());
            assertEquals(x.getBinaryAttribute(), ByteBuffer.wrap(generateByteArray(contentLength)));
            assertTrue(x.getBinarySetAttribute().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
            assertTrue(x.getBinarySetAttribute().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

            // test BinaryAttributeByteArrayClass
            BinaryAttributeByteArrayClass y = util.load(BinaryAttributeByteArrayClass.class, attr.get(KEY_NAME).s());
            assertEquals(y.getKey(), attr.get(KEY_NAME).s());
            assertTrue(Arrays.equals(y.getBinaryAttribute(), (generateByteArray(contentLength))));
            assertEquals(2, y.getBinarySetAttribute().size());
            assertTrue(setContainsBytes(y.getBinarySetAttribute(), generateByteArray(contentLength)));
            assertTrue(setContainsBytes(y.getBinarySetAttribute(), generateByteArray(contentLength+1)));
        }

    }

    @Test
    public void testSave() {
    	// test BinaryAttributeClass
        List<BinaryAttributeByteBufferClass> byteBufferObjs = new ArrayList<BinaryAttributeByteBufferClass>();
        for ( int i = 0; i < 5; i++ ) {
        	BinaryAttributeByteBufferClass obj = getUniqueByteBufferObject(contentLength);
            byteBufferObjs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for (BinaryAttributeByteBufferClass obj : byteBufferObjs) {
            util.save(obj);
        }

        for (BinaryAttributeByteBufferClass obj : byteBufferObjs) {
        	BinaryAttributeByteBufferClass loaded = util.load(BinaryAttributeByteBufferClass.class, obj.getKey());
        	assertEquals(loaded.getKey(), obj.getKey());
        	assertEquals(loaded.getBinaryAttribute(), ByteBuffer.wrap(generateByteArray(contentLength)));
            assertTrue(loaded.getBinarySetAttribute().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        }

        // test BinaryAttributeByteArrayClass
        List<BinaryAttributeByteArrayClass> bytesObjs = new ArrayList<BinaryAttributeByteArrayClass>();
        for ( int i = 0; i < 5; i++ ) {
        	BinaryAttributeByteArrayClass obj = getUniqueBytesObject(contentLength);
            bytesObjs.add(obj);
        }

        for (BinaryAttributeByteArrayClass obj : bytesObjs) {
            util.save(obj);
        }

        for (BinaryAttributeByteArrayClass obj : bytesObjs) {
        	BinaryAttributeByteArrayClass loaded = util.load(BinaryAttributeByteArrayClass.class, obj.getKey());
        	 assertEquals(loaded.getKey(), obj.getKey());
             assertTrue(Arrays.equals(loaded.getBinaryAttribute(), (generateByteArray(contentLength))));
             assertEquals(1, loaded.getBinarySetAttribute().size());
             assertTrue(setContainsBytes(loaded.getBinarySetAttribute(), generateByteArray(contentLength)));
        }
    }

    /**
     * Tests saving an incomplete object into DynamoDB
     */
    @Test
    public void testIncompleteObject() {
    	// test BinaryAttributeClass
    	BinaryAttributeByteBufferClass byteBufferObj = getUniqueByteBufferObject(contentLength);
        byteBufferObj.setBinarySetAttribute(null);
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        util.save(byteBufferObj);

        BinaryAttributeByteBufferClass loadedX = util.load(BinaryAttributeByteBufferClass.class, byteBufferObj.getKey());
        assertEquals(loadedX.getKey(), byteBufferObj.getKey());
    	assertEquals(loadedX.getBinaryAttribute(), ByteBuffer.wrap(generateByteArray(contentLength)));
    	assertEquals(loadedX.getBinarySetAttribute(), null);


        // test removing an attribute
        assertNotNull(byteBufferObj.getBinaryAttribute());
        byteBufferObj.setBinaryAttribute(null);
        util.save(byteBufferObj);

        loadedX = util.load(BinaryAttributeByteBufferClass.class, byteBufferObj.getKey());
        assertEquals(loadedX.getKey(), byteBufferObj.getKey());
    	assertEquals(loadedX.getBinaryAttribute(), null);
    	assertEquals(loadedX.getBinarySetAttribute(), null);

    	// test BinaryAttributeByteArrayClass
    	BinaryAttributeByteArrayClass bytesObj = getUniqueBytesObject(contentLength);
        bytesObj.setBinarySetAttribute(null);
        util.save(bytesObj);

        BinaryAttributeByteArrayClass loadedY = util.load(BinaryAttributeByteArrayClass.class, bytesObj.getKey());
        assertEquals(loadedY.getKey(), bytesObj.getKey());
    	assertTrue(Arrays.equals(loadedY.getBinaryAttribute(), generateByteArray(contentLength)));
    	assertEquals(loadedY.getBinarySetAttribute(), null);


        // test removing an attribute
        assertNotNull(bytesObj.getBinaryAttribute());
        bytesObj.setBinaryAttribute(null);
        util.save(bytesObj);

        loadedY = util.load(BinaryAttributeByteArrayClass.class, bytesObj.getKey());
        assertEquals(loadedY.getKey(), bytesObj.getKey());
    	assertEquals(loadedY.getBinaryAttribute(), null);
    	assertEquals(loadedY.getBinarySetAttribute(), null);
    }

    @Test
    public void testUpdate() {
    	// test BinaryAttributeClass
        List<BinaryAttributeByteBufferClass> byteBufferObjs = new ArrayList<BinaryAttributeByteBufferClass>();
        for ( int i = 0; i < 5; i++ ) {
        	BinaryAttributeByteBufferClass obj = getUniqueByteBufferObject(contentLength);
            byteBufferObjs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for (BinaryAttributeByteBufferClass obj : byteBufferObjs) {
            util.save(obj);
        }

        for ( BinaryAttributeByteBufferClass obj : byteBufferObjs ) {
        	BinaryAttributeByteBufferClass replacement = getUniqueByteBufferObject(contentLength - 1);
            replacement.setKey(obj.getKey());
            util.save(replacement);

            BinaryAttributeByteBufferClass loaded = util.load(BinaryAttributeByteBufferClass.class, obj.getKey());
            assertEquals(loaded.getKey(), obj.getKey());
        	assertEquals(loaded.getBinaryAttribute(), ByteBuffer.wrap(generateByteArray(contentLength - 1)));
            assertTrue(loaded.getBinarySetAttribute().contains(ByteBuffer.wrap(generateByteArray(contentLength - 1))));

        }

        // test BinaryAttributeByteArrayClass
        List<BinaryAttributeByteArrayClass> bytesObj = new ArrayList<BinaryAttributeByteArrayClass>();
        for ( int i = 0; i < 5; i++ ) {
        	BinaryAttributeByteArrayClass obj = getUniqueBytesObject(contentLength);
            bytesObj.add(obj);
        }

        for (BinaryAttributeByteArrayClass obj : bytesObj) {
            util.save(obj);
        }

        for ( BinaryAttributeByteArrayClass obj : bytesObj ) {
        	BinaryAttributeByteArrayClass replacement = getUniqueBytesObject(contentLength - 1);
            replacement.setKey(obj.getKey());
            util.save(replacement);

             BinaryAttributeByteArrayClass loaded = util.load(BinaryAttributeByteArrayClass.class, obj.getKey());
        	 assertEquals(loaded.getKey(), obj.getKey());
             assertTrue(Arrays.equals(loaded.getBinaryAttribute(), (generateByteArray(contentLength - 1))));
             assertEquals(1, loaded.getBinarySetAttribute().size());
             assertTrue(setContainsBytes(loaded.getBinarySetAttribute(), generateByteArray(contentLength - 1)));

        }
    }

    @Test
    public void testDelete() throws Exception {
    	// test BinaryAttributeClass
    	BinaryAttributeByteBufferClass byteBufferObj = getUniqueByteBufferObject(contentLength);
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        util.save(byteBufferObj);

        util.delete(byteBufferObj);
        assertNull(util.load(BinaryAttributeByteBufferClass.class, byteBufferObj.getKey()));

        // test BinaryAttributeByteArrayClass
        BinaryAttributeByteArrayClass bytesObj = getUniqueBytesObject(contentLength);
        util.save(bytesObj);

        util.delete(bytesObj);
        assertNull(util.load(BinaryAttributeByteArrayClass.class, bytesObj.getKey()));

    }

    private BinaryAttributeByteArrayClass getUniqueBytesObject(int contentLength) {
    	BinaryAttributeByteArrayClass obj = new BinaryAttributeByteArrayClass();
        obj.setKey(String.valueOf(startKey++));
        obj.setBinaryAttribute(generateByteArray(contentLength));
        Set<byte[]> byteArray = new HashSet<byte[]>();
        byteArray.add(generateByteArray(contentLength));
        obj.setBinarySetAttribute(byteArray);
        return obj;
    }

    private boolean setContainsBytes(Set<byte[]> set, byte[] bytes) {
    	     Iterator<byte[]> iter = set.iterator();
    	     while (iter.hasNext()) {
    	    	 if (Arrays.equals(iter.next(), bytes))
    	    		 return true;
    	     }
    	return false;
    }

}
