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

import static com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads.CONSISTENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.amazonaws.services.dynamodbv2.DynamoDBMapperIntegrationTestBase;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDeleteExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.util.ImmutableMapParameter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Tests updating version fields correctly
 */
public class VersionAttributeUpdateIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class VersionFieldBaseClass {

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
            VersionFieldBaseClass other = (VersionFieldBaseClass) obj;
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

    public static class StringVersionField extends VersionFieldBaseClass {

        private String version;

        @DynamoDBVersionAttribute
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((version == null) ? 0 : version.hashCode());
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
            StringVersionField other = (StringVersionField) obj;
            if ( version == null ) {
                if ( other.version != null )
                    return false;
            } else if ( !version.equals(other.version) )
                return false;
            return true;
        }
    }

    @Test(expected = DynamoDBMappingException.class)
    public void testStringVersion() throws Exception {
        List<StringVersionField> objs = new ArrayList<StringVersionField>();
        for ( int i = 0; i < 5; i++ ) {
            StringVersionField obj = getUniqueObject(new StringVersionField());
            objs.add(obj);
        }

        // Saving new objects with a null version field should populate it
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for ( StringVersionField obj : objs ) {
            assertNull(obj.getVersion());
            util.save(obj);
            assertNotNull(obj.getVersion());
            assertEquals(obj, util.load(StringVersionField.class, obj.getKey()));
        }
    }

    public static class BigIntegerVersionField extends VersionFieldBaseClass {

        private BigInteger version;

        @DynamoDBVersionAttribute
        public BigInteger getVersion() {
            return version;
        }

        public void setVersion(BigInteger version) {
            this.version = version;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((version == null) ? 0 : version.hashCode());
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
            BigIntegerVersionField other = (BigIntegerVersionField) obj;
            if ( version == null ) {
                if ( other.version != null )
                    return false;
            } else if ( !version.equals(other.version) )
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "BigIntegerVersionField [version=" + version + ", key=" + key + ", normalStringAttribute="
                    + normalStringAttribute + "]";
        }
    }

    @Test
    public void testBigIntegerVersion() {
        List<BigIntegerVersionField> objs = new ArrayList<BigIntegerVersionField>();
        for ( int i = 0; i < 5; i++ ) {
            BigIntegerVersionField obj = getUniqueObject(new BigIntegerVersionField());
            objs.add(obj);
        }

        // Saving new objects with a null version field should populate it
        DynamoDBMapper util = new DynamoDBMapper(dynamo, DynamoDBMapperConfig.builder()
                                                                             .withConsistentReads(CONSISTENT)
                                                                             .build());
        for ( BigIntegerVersionField obj : objs ) {
            assertNull(obj.getVersion());
            util.save(obj);
            assertNotNull(obj.getVersion());

            assertEquals(obj, util.load(BigIntegerVersionField.class, obj.getKey()));
        }

        for ( BigIntegerVersionField obj : objs ) {
            BigIntegerVersionField replacement = getUniqueObject(new BigIntegerVersionField());
            replacement.setKey(obj.getKey());
            replacement.setVersion(obj.getVersion());

            util.save(replacement);
            // The version field should have changed in memory
            assertFalse(obj.getVersion().equals(replacement.getVersion()));

            BigIntegerVersionField loadedObject = util.load(BigIntegerVersionField.class, obj.getKey());
            assertEquals(replacement, loadedObject);

            // Trying to update the object again should trigger a concurrency
            // exception
            try {
                util.save(obj);
                fail("Should have thrown an exception");
            } catch ( Exception expected ) {
            }

            // Now try again overlaying the correct version number by using a saveExpression
            // this should not throw the conditional check failed exception
            try {
            	DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
            	Map<String,ExpectedAttributeValue> expected = new HashMap<String,ExpectedAttributeValue>();
            	ExpectedAttributeValue expectedVersion = new ExpectedAttributeValue()
                    .withValue(new AttributeValue()
                    .withN(obj.getVersion().add(BigInteger.valueOf(1)).toString()));
            	expected.put("version", expectedVersion);
            	saveExpression.setExpected(expected);
                util.save(obj, saveExpression);
            } catch ( Exception expected ) {
            	fail("This should succeed, version was updated.");
            }
        }
    }

    public static final class IntegerVersionField extends VersionFieldBaseClass {

        private Integer notCalledVersion;

        // Making sure that we can substitute attribute names as necessary
        @DynamoDBVersionAttribute(attributeName = "version")
        public Integer getNotCalledVersion() {
            return notCalledVersion;
        }

        public void setNotCalledVersion(Integer notCalledVersion) {
            this.notCalledVersion = notCalledVersion;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((notCalledVersion == null) ? 0 : notCalledVersion.hashCode());
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
            IntegerVersionField other = (IntegerVersionField) obj;
            if ( notCalledVersion == null ) {
                if ( other.notCalledVersion != null )
                    return false;
            } else if ( !notCalledVersion.equals(other.notCalledVersion) )
                return false;
            return true;
        }
    }

    @Test
    public void testIntegerVersion() {
        List<IntegerVersionField> objs = new ArrayList<IntegerVersionField>();
        for ( int i = 0; i < 5; i++ ) {
            IntegerVersionField obj = getUniqueObject(new IntegerVersionField());
            objs.add(obj);
        }

        // Saving new objects with a null version field should populate it
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for ( IntegerVersionField obj : objs ) {
            assertNull(obj.getNotCalledVersion());
            util.save(obj);
            assertNotNull(obj.getNotCalledVersion());

            assertEquals(obj, util.load(IntegerVersionField.class, obj.getKey()));
        }

        for ( IntegerVersionField obj : objs ) {
            IntegerVersionField replacement = getUniqueObject(new IntegerVersionField());
            replacement.setKey(obj.getKey());
            replacement.setNotCalledVersion(obj.getNotCalledVersion());

            util.save(replacement);
            // The version field should have changed in memory
            assertFalse(obj.getNotCalledVersion().equals(replacement.getNotCalledVersion()));

            IntegerVersionField loadedObject = util.load(IntegerVersionField.class, obj.getKey());
            assertEquals(replacement, loadedObject);

            // Trying to update the object again should trigger a concurrency
            // exception
            try {
                util.save(obj);
                fail("Should have thrown an exception");
            } catch ( Exception expected ) {
            }

            // Trying to delete the object should also fail
            try {
                util.delete(obj);
                fail("Should have thrown an exception");
            } catch ( Exception expected ) {
            }

            // But specifying CLOBBER will allow deletion
            util.save(obj, new DynamoDBMapperConfig(SaveBehavior.CLOBBER));

            // Trying to delete with the wrong version should fail
            try {
                //version is now 2 in db, set object version to 3.
                obj.setNotCalledVersion(3);
                util.delete(obj);
                fail("Should have thrown an exception");
            } catch ( Exception expected ) {
            }

            // Now try deleting again overlaying the correct version number by using a deleteExpression
            // this should not throw the conditional check failed exception
            try {
                DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression();
                Map<String,ExpectedAttributeValue> expected = new HashMap<String,ExpectedAttributeValue>();
                ExpectedAttributeValue expectedVersion = new ExpectedAttributeValue()
                    .withValue(new AttributeValue()
                    .withN("2"));  //version is still 2 in db
                expected.put("version", expectedVersion);
                deleteExpression.setExpected(expected);
                util.delete(obj, deleteExpression);
            } catch ( Exception expected ) {
                fail("This should succeed, version was updated.");
            }
        }
    }

    /**
     * Tests providing additional expected conditions when saving and deleting
     * item with versioned fields.
     */
    @Test
    public void testVersionedAttributeWithUserProvidedExpectedConditions() {
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);
        IntegerVersionField versionedObject = getUniqueObject(new IntegerVersionField());
        assertNull(versionedObject.getNotCalledVersion());

        // Add additional expected conditions via DynamoDBSaveExpression.
        // Expected conditions joined by AND are compatible with the conditions
        // for auto-generated keys.
        DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression()
                .withExpected(Collections.singletonMap(
                        "otherAttribute", new ExpectedAttributeValue(false)))
                .withConditionalOperator(ConditionalOperator.AND);
        // The save should succeed since the user provided conditions are joined by AND.
        mapper.save(versionedObject, saveExpression);
        // The version field should be populated
        assertNotNull(versionedObject.getNotCalledVersion());
        IntegerVersionField other = mapper.load(IntegerVersionField.class, versionedObject.getKey());
        assertEquals(other, versionedObject);

        // delete should also work
        DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression()
                .withExpected(Collections.singletonMap(
                        "otherAttribute", new ExpectedAttributeValue(false)))
                .withConditionalOperator(ConditionalOperator.AND);
        mapper.delete(versionedObject, deleteExpression);

        // Change the conditional operator to OR.
        // IllegalArgumentException is expected since the additional expected
        // conditions cannot be joined with the conditions for auto-generated
        // keys.
        saveExpression.setConditionalOperator(ConditionalOperator.OR);
        deleteExpression.setConditionalOperator(ConditionalOperator.OR);
        try {
            mapper.save(getUniqueObject(new IntegerVersionField()), saveExpression);
        } catch (IllegalArgumentException expected) {}
        try {
            mapper.delete(getUniqueObject(new IntegerVersionField()), deleteExpression);
        } catch (IllegalArgumentException expected) {}

        // User-provided OR conditions should work if they completely override
        // the generated conditions for the version field.
        Map<String, ExpectedAttributeValue> goodConditions =
                ImmutableMapParameter.of(
                    "otherAttribute", new ExpectedAttributeValue(false),
                    "version",        new ExpectedAttributeValue(false)
                );
        Map<String, ExpectedAttributeValue> badConditions =
                ImmutableMapParameter.of(
                        "otherAttribute", new ExpectedAttributeValue(new AttributeValue("non-existent-value")),
                        "version",        new ExpectedAttributeValue(new AttributeValue().withN("-1"))
                );

        IntegerVersionField newObj = getUniqueObject(new IntegerVersionField());
        saveExpression.setExpected(badConditions);
        try {
            mapper.save(newObj, saveExpression);
        } catch (ConditionalCheckFailedException expected) {}

        saveExpression.setExpected(goodConditions);
        mapper.save(newObj, saveExpression);

        deleteExpression.setExpected(badConditions);
        try {
            mapper.delete(newObj, deleteExpression);
        } catch (ConditionalCheckFailedException expected) {}

        deleteExpression.setExpected(goodConditions);
        mapper.delete(newObj, deleteExpression);
    }

    public static final class ByteVersionField extends VersionFieldBaseClass {

        private Byte version;

        @DynamoDBVersionAttribute
        public Byte getVersion() {
            return version;
        }

        public void setVersion(Byte version) {
            this.version = version;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((version == null) ? 0 : version.hashCode());
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
            ByteVersionField other = (ByteVersionField) obj;
            if ( version == null ) {
                if ( other.version != null )
                    return false;
            } else if ( !version.equals(other.version) )
                return false;
            return true;
        }
    }

    @Test
    public void testByteVersion() {
        List<ByteVersionField> objs = new ArrayList<ByteVersionField>();
        for ( int i = 0; i < 5; i++ ) {
            ByteVersionField obj = getUniqueObject(new ByteVersionField());
            objs.add(obj);
        }

        // Saving new objects with a null version field should populate it
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for ( ByteVersionField obj : objs ) {
            assertNull(obj.getVersion());
            util.save(obj);
            assertNotNull(obj.getVersion());

            assertEquals(obj, util.load(ByteVersionField.class, obj.getKey()));
        }

        for ( ByteVersionField obj : objs ) {
            ByteVersionField replacement = getUniqueObject(new ByteVersionField());
            replacement.setKey(obj.getKey());
            replacement.setVersion(obj.getVersion());

            util.save(replacement);
            // The version field should have changed in memory
            assertFalse(obj.getVersion().equals(replacement.getVersion()));

            ByteVersionField loadedObject = util.load(ByteVersionField.class, obj.getKey());
            assertEquals(replacement, loadedObject);

            // Trying to update the object again should trigger a concurrency
            // exception
            try {
                util.save(obj);
                fail("Should have thrown an exception");
            } catch ( Exception expected ) {
            }
        }
    }

    public static final class LongVersionField extends VersionFieldBaseClass {

        private Long version;

        @DynamoDBVersionAttribute
        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((version == null) ? 0 : version.hashCode());
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
            LongVersionField other = (LongVersionField) obj;
            if ( version == null ) {
                if ( other.version != null )
                    return false;
            } else if ( !version.equals(other.version) )
                return false;
            return true;
        }
    }

    @Test
    public void testLongVersion() {
        List<LongVersionField> objs = new ArrayList<LongVersionField>();
        for ( int i = 0; i < 5; i++ ) {
            LongVersionField obj = getUniqueObject(new LongVersionField());
            objs.add(obj);
        }

        // Saving new objects with a null version field should populate it
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for ( LongVersionField obj : objs ) {
            assertNull(obj.getVersion());
            util.save(obj);
            assertNotNull(obj.getVersion());

            assertEquals(obj, util.load(LongVersionField.class, obj.getKey()));
        }

        for ( LongVersionField obj : objs ) {
            LongVersionField replacement = getUniqueObject(new LongVersionField());
            replacement.setKey(obj.getKey());
            replacement.setVersion(obj.getVersion());

            util.save(replacement);
            // The version field should have changed in memory
            assertFalse(obj.getVersion().equals(replacement.getVersion()));

            LongVersionField loadedObject = util.load(LongVersionField.class, obj.getKey());
            assertEquals(replacement, loadedObject);

            // Trying to update the object again should trigger a concurrency
            // exception
            try {
                util.save(obj);
                fail("Should have thrown an exception");
            } catch ( Exception expected ) {
            }
        }
    }

    private <T extends VersionFieldBaseClass> T getUniqueObject(T obj) {
        obj.setKey("" + startKey++);
        obj.setNormalStringAttribute("" + startKey++);
        return obj;
    }
}
