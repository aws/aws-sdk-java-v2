package com.amazonaws.services.dynamodbv2.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.amazonaws.dynamodbv2.test.util.DynamoDBIntegrationTestBase;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBAttribute;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBSaveExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;

public class KeyOnlyPutIntegrationTest extends DynamoDBIntegrationTestBase {
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class HashAndAttribute {

        protected String key;
        protected String normalStringAttribute;

        @DynamoDBHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @DynamoDBAttribute
        public String getNormalStringAttribute() {
            return normalStringAttribute;
        }

        public void setNormalStringAttribute(String normalStringAttribute) {
            this.normalStringAttribute = normalStringAttribute;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((normalStringAttribute == null) ? 0 : normalStringAttribute.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            HashAndAttribute other = (HashAndAttribute) obj;
            if ( key == null ) {
                if ( other.key != null )
                    return false;
            } else if ( !key.equals(other.key) )
                return false;
            if ( normalStringAttribute == null ) {
                if ( other.normalStringAttribute != null )
                    return false;
            } else if ( !normalStringAttribute.equals(other.normalStringAttribute) )
                return false;
            return true;
        }
    }
    
    @Test
    public void testKeyOnlyPut() throws Exception {
        /*
         * Testing this scenario
         *     (1) An empty table with the schema:
         *
         *    "key" (HASH)
         *
         *    (2) A POJO class:
         *    "key" (HASH), "attribute" (NON-KEY)
         *
         *    (3) Save operation by some user:
         *     - item : {"key" : "some value"}
         *     - user-specified expected values : {"attribute" : {Exist : true}}
         *     - SaveBehavior : UPDATE (default)
         *
         *    (4) Expected behavior
         *    ConditionalCheckFailedException, and the table should remain empty.
         */
        List<HashAndAttribute> objs = new ArrayList<HashAndAttribute>();
        for ( int i = 0; i < 5; i++ ) {
            HashAndAttribute obj = getUniqueObject(new HashAndAttribute());
            objs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for ( HashAndAttribute obj : objs ) {
            try {
                DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
                Map<String,ExpectedAttributeValue> expected = new HashMap<String,ExpectedAttributeValue>();
                ExpectedAttributeValue expectedVersion = new ExpectedAttributeValue()
                    .withValue(new AttributeValue()
                    .withS("SomeNonExistantValue"))   
                    .withExists(true);
                expected.put("normalStringAttribute", expectedVersion);
                saveExpression.setExpected(expected);
                
                util.save(obj,saveExpression);
                fail("This should fail, expected clause should block an insert.");
            }catch(ConditionalCheckFailedException e){
                
            }
            assertNull(util.load(HashAndAttribute.class, obj.getKey()));
            
            //this should succeed without the expected clause
            obj.setNormalStringAttribute("to-be-deleted");
            util.save(obj);
            obj.setNormalStringAttribute(null);
            util.save(obj);
            Object loaded = util.load(HashAndAttribute.class, obj.getKey());
            assertEquals("Expected " + obj.toString() + ", but was " + loaded.toString(), obj, loaded);
        }
    }
    
    private <T extends HashAndAttribute> T getUniqueObject(T obj) {
        obj.setKey("" + startKey++);
        return obj;
    }
}
