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
package software.amazon.awssdk.mapper.dynamodb.mapper;

import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.ConsistentReads.CONSISTENT;
import static org.junit.Assert.assertEquals;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBAttribute;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMappingException;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;

/**
 * Tests inheritance behavior in DynamoDB mapper.
 */
public class InheritanceIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class BaseClass {

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
            BaseClass other = (BaseClass) obj;
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

    public static class SubClass extends BaseClass {

        private String subField;

        public String getSubField() {
            return subField;
        }

        public void setSubField(String subField) {
            this.subField = subField;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((subField == null) ? 0 : subField.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if ( this == obj )
                return true;
            if ( !super.equals(obj) )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            SubClass other = (SubClass) obj;
            if ( subField == null ) {
                if ( other.subField != null )
                    return false;
            } else if ( !subField.equals(other.subField) )
                return false;
            return true;
        }

    }

    public static class SubSubClass extends SubClass {

        private String subSubField;

        public String getSubSubField() {
            return subSubField;
        }

        public void setSubSubField(String subSubField) {
            this.subSubField = subSubField;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((subSubField == null) ? 0 : subSubField.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj )
                return true;
            if ( !super.equals(obj) )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            SubSubClass other = (SubSubClass) obj;
            if ( subSubField == null ) {
                if ( other.subSubField != null )
                    return false;
            } else if ( !subSubField.equals(other.subSubField) )
                return false;
            return true;
        }
    }

    @Test
    public void testSubClass() throws Exception {
        List<Object> objs = new ArrayList<Object>();
        for ( int i = 0; i < 5; i++ ) {
            SubClass obj = getUniqueObject(new SubClass());
            obj.setSubField("" + startKey++);
            objs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for ( Object obj : objs ) {
            util.save(obj);
            assertEquals(util.load(SubClass.class, ((SubClass)obj).getKey()), obj);
        }
    }

    @Test
    public void testSubSubClass() throws Exception {
        List<SubSubClass> objs = new ArrayList<SubSubClass>();
        for ( int i = 0; i < 5; i++ ) {
            SubSubClass obj = getUniqueObject(new SubSubClass());
            obj.setSubField("" + startKey++);
            obj.setSubSubField("" + startKey++);
            objs.add(obj);
        }

        DynamoDBMapper util = new DynamoDBMapper(dynamo, DynamoDBMapperConfig.builder()
                                                                             .withConsistentReads(CONSISTENT)
                                                                             .build());
        for ( SubSubClass obj : objs ) {
            util.save(obj);
            assertEquals(util.load(SubSubClass.class, obj.getKey()), obj);
        }
    }

    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static interface Interface {

        @DynamoDBHashKey
        public String getKey();

        public void setKey(String key);

        @DynamoDBAttribute
        public String getAttribute();

        public void setAttribute(String attribute);
    }

    public static class Implementation implements Interface {

        private String key;
        private String attribute;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
            result = prime * result + ((key == null) ? 0 : key.hashCode());
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
            Implementation other = (Implementation) obj;
            if ( attribute == null ) {
                if ( other.attribute != null )
                    return false;
            } else if ( !attribute.equals(other.attribute) )
                return false;
            if ( key == null ) {
                if ( other.key != null )
                    return false;
            } else if ( !key.equals(other.key) )
                return false;
            return true;
        }
    }
    
    @Test(expected = DynamoDBMappingException.class)
    public void testImplementation() throws Exception {
        List<Implementation> objs = new ArrayList<Implementation>();
        for ( int i = 0; i < 5; i++ ) {
            Implementation obj = new Implementation();            
            obj.setKey("" + startKey++);
            obj.setAttribute("" + startKey++);
            objs.add(obj);
        }

        // Saving new objects with a null version field should populate it
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for ( Interface obj : objs ) {
            util.save(obj);
            assertEquals(util.load(Implementation.class, obj.getKey()), obj);
        }
    }

    private <T extends BaseClass> T getUniqueObject(T obj) {
        obj.setKey("" + startKey++);
        obj.setNormalStringAttribute("" + startKey++);
        return obj;
    }
}
