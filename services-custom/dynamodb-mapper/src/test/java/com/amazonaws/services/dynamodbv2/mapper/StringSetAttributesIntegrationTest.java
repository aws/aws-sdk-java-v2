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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.DynamoDBMapperIntegrationTestBase;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.pojos.StringSetAttributeClass;


/**
 * Tests string set attributes
 */
public class StringSetAttributesIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String ORIGINAL_NAME_ATTRIBUTE = "originalName";
    private static final String STRING_SET_ATTRIBUTE = "stringSetAttribute";
    private static final String EXTRA_ATTRIBUTE = "extra";
    private static final List<Map<String, AttributeValue>> attrs = new LinkedList<Map<String, AttributeValue>>();    

    // Test data
    static {
        for ( int i = 0; i < 5; i++ ) {
            Map<String, AttributeValue> attr = new HashMap<String, AttributeValue>();
            attr.put(KEY_NAME, new AttributeValue().withS("" + startKey++));
            attr.put(STRING_SET_ATTRIBUTE, new AttributeValue().withSS("" + ++startKey, "" + ++startKey, "" + ++startKey));
            attr.put(ORIGINAL_NAME_ATTRIBUTE, new AttributeValue().withSS("" + ++startKey, "" + ++startKey, "" + ++startKey));
            attr.put(EXTRA_ATTRIBUTE, new AttributeValue().withSS("" + ++startKey, "" + ++startKey, "" + ++startKey));
            attrs.add(attr);
        }
    };
    
    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBMapperIntegrationTestBase.setUp();
        
        // Insert the data
        for ( Map<String, AttributeValue> attr : attrs ) {
            dynamo.putItem(new PutItemRequest(TABLE_NAME, attr));            
        }
    }
    
    @Test
    public void testLoad() throws Exception {
        DynamoDBMapper util = new DynamoDBMapper(dynamo);        
        
        for ( Map<String, AttributeValue> attr : attrs ) {
            StringSetAttributeClass x = util.load(StringSetAttributeClass.class, attr.get(KEY_NAME).getS());
            assertEquals(x.getKey(), attr.get(KEY_NAME).getS());
            assertSetsEqual(x.getStringSetAttribute(), toSet(attr.get(STRING_SET_ATTRIBUTE).getSS()));
            assertSetsEqual(x.getStringSetAttributeRenamed(), toSet(attr.get(ORIGINAL_NAME_ATTRIBUTE).getSS()));
        }        
    }
 
    /**
     * Tests saving only some attributes of an object.
     */
    @Test
    public void testIncompleteObject() {
        DynamoDBMapper util = new DynamoDBMapper(dynamo);        

        StringSetAttributeClass obj = getUniqueObject();
        obj.setStringSetAttribute(null);
        util.save(obj);
        
        assertEquals(obj, util.load(StringSetAttributeClass.class, obj.getKey()));
        
        obj.setStringSetAttributeRenamed(null);
        util.save(obj);
        assertEquals(obj, util.load(StringSetAttributeClass.class, obj.getKey()));        
    }
    
    @Test
    public void testSave() throws Exception {
        List<StringSetAttributeClass> objs = new ArrayList<StringSetAttributeClass>();
        for ( int i = 0; i < 5; i++ ) {
            StringSetAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for (StringSetAttributeClass obj : objs) {
            util.save(obj);
        }

        for (StringSetAttributeClass obj : objs) {
            StringSetAttributeClass loaded = util.load(StringSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }
    
    @Test
    public void testUpdate() throws Exception {
        List<StringSetAttributeClass> objs = new ArrayList<StringSetAttributeClass>();
        for ( int i = 0; i < 5; i++ ) {
            StringSetAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for (StringSetAttributeClass obj : objs) {
            util.save(obj);
        }

        for ( StringSetAttributeClass obj : objs ) {
            StringSetAttributeClass replacement = getUniqueObject();
            replacement.setKey(obj.getKey());
            util.save(replacement);
            
            assertEquals(replacement, util.load(StringSetAttributeClass.class, obj.getKey()));
        }
    }

    private StringSetAttributeClass getUniqueObject() {
        StringSetAttributeClass obj = new StringSetAttributeClass();
        obj.setKey(String.valueOf(startKey++));
        obj.setStringSetAttribute(toSet(String.valueOf(startKey++), String.valueOf(startKey++), String.valueOf(startKey++)));
        obj.setStringSetAttributeRenamed(toSet(String.valueOf(startKey++), String.valueOf(startKey++), String.valueOf(startKey++)));
        return obj;
    }

}
