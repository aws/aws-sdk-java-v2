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
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.pojos.StringAttributeClass;

/**
 * Tests simple string attributes
 */
public class SimpleStringAttributesIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String ORIGINAL_NAME_ATTRIBUTE = "originalName";
    private static final String STRING_ATTRIBUTE = "stringAttribute";
    private static final List<Map<String, AttributeValue>> attrs = new LinkedList<Map<String, AttributeValue>>();

    // Test data
    static {
        for ( int i = 0; i < 5; i++ ) {
            Map<String, AttributeValue> attr = new HashMap<String, AttributeValue>();
            attr.put(KEY_NAME, new AttributeValue().withS("" + startKey++));
            attr.put(STRING_ATTRIBUTE, new AttributeValue().withS("" + startKey++));
            attr.put(ORIGINAL_NAME_ATTRIBUTE, new AttributeValue().withS("" + startKey++));
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
            StringAttributeClass x = util.load(StringAttributeClass.class, attr.get(KEY_NAME).getS());
            assertEquals(x.getKey(), attr.get(KEY_NAME).getS());
            assertEquals(x.getStringAttribute(), attr.get(STRING_ATTRIBUTE).getS());
            assertEquals(x.getRenamedAttribute(), attr.get(ORIGINAL_NAME_ATTRIBUTE).getS());
        }

    }

    @Test
    public void testSave() {
        List<StringAttributeClass> objs = new ArrayList<StringAttributeClass>();
        for ( int i = 0; i < 5; i++ ) {
            StringAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for (StringAttributeClass obj : objs) {
            util.save(obj);
        }

        for (StringAttributeClass obj : objs) {
            StringAttributeClass loaded = util.load(StringAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }
    
    /**
     * Tests saving an incomplete object into DynamoDB
     */
    @Test
    public void testIncompleteObject() {
        StringAttributeClass obj = getUniqueObject();
        obj.setStringAttribute(null);
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        util.save(obj);
        
        assertEquals(obj, util.load(StringAttributeClass.class, obj.getKey()));
        
        // test removing an attribute
        assertNotNull(obj.getRenamedAttribute());
        obj.setRenamedAttribute(null);
        util.save(obj);
        assertEquals(obj, util.load(StringAttributeClass.class, obj.getKey()));        
    }
    
    @Test
    public void testUpdate() {
        List<StringAttributeClass> objs = new ArrayList<StringAttributeClass>();
        for ( int i = 0; i < 5; i++ ) {
            StringAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for (StringAttributeClass obj : objs) {
            util.save(obj);
        }

        for ( StringAttributeClass obj : objs ) {
            StringAttributeClass replacement = getUniqueObject();
            replacement.setKey(obj.getKey());
            util.save(replacement);
            
            assertEquals(replacement, util.load(StringAttributeClass.class, obj.getKey()));
        }
    }
    
    @Test
    public void testSaveOnlyKey() {
        KeyOnly obj = new KeyOnly();
        obj.setKey("" + startKey++);
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);
        mapper.save(obj);
        
        KeyOnly loaded = mapper.load(KeyOnly.class, obj.getKey(), new DynamoDBMapperConfig(ConsistentReads.CONSISTENT));
        assertEquals(obj, loaded);
        
        // saving again shouldn't be an error
        mapper.save(obj);
    }
    
    @Test
    public void testSaveOnlyKeyClobber() {
        KeyOnly obj = new KeyOnly();
        obj.setKey("" + startKey++);
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);
        mapper.save(obj, new DynamoDBMapperConfig(SaveBehavior.CLOBBER));
        
        KeyOnly loaded = mapper.load(KeyOnly.class, obj.getKey(), new DynamoDBMapperConfig(ConsistentReads.CONSISTENT));
        assertEquals(obj, loaded);
        
        // saving again shouldn't be an error
        mapper.save(obj, new DynamoDBMapperConfig(SaveBehavior.CLOBBER));
    }
    
    @DynamoDBTable(tableName="aws-java-sdk-util")
    public static final class KeyOnly {
        private String key;

        @DynamoDBHashKey        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            KeyOnly other = (KeyOnly) obj;
            if ( key == null ) {
                if ( other.key != null )
                    return false;
            } else if ( !key.equals(other.key) )
                return false;
            return true;
        }
    }

    private StringAttributeClass getUniqueObject() {
        StringAttributeClass obj = new StringAttributeClass();
        obj.setKey(String.valueOf(startKey++));
        obj.setRenamedAttribute(String.valueOf(startKey++));
        obj.setStringAttribute(String.valueOf(startKey++));
        return obj;
    }

}
