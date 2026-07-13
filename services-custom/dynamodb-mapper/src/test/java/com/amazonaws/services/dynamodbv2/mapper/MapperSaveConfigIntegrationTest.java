package com.amazonaws.services.dynamodbv2.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.pojos.TestItem;
import com.amazonaws.util.ImmutableMapParameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;

/**
 * Tests the behavior of save method of DynamoDBMapper under different
 * SaveBehavior configurations.
 */
public class MapperSaveConfigIntegrationTest extends MapperSaveConfigTestBase {

    @AfterClass
    public static void teatDown() throws Exception {
        try {
//            dynamo.deleteTable(new DeleteTableRequest(tableName));
        } catch (Exception e) {
        }
    }

    /*********************************************
     **          UPDATE (default)               **
     *********************************************/
    
    /**
     * Tests that a key-only object could be saved with
     * UPDATE configuration, even when the key has already existed in the table.
     */
    @Test
    public void testDefaultWithOnlyKeyAttributesSpecifiedRecordInTable()
            throws Exception {

        /* First put a new item (with non-key attribute)*/
        TestItem testItem = putRandomUniqueItem("foo", null);
        
        /* Put an key-only object with the same key */
        testItem.setNonKeyAttribute(null);
        
        dynamoMapper.save(testItem, defaultConfig);
        
        /* The non-key attribute should be nulled out. */
        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);
        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
    }

    /**
     * Tests an edge case that we have fixed according a forum bug report. If
     * the object is only specified with key attributes, and such key is not
     * present in the table, we should add this object by a key-only put
     * request even if it is using UPDATE configuration.
     */
    @Test
    public void testDefaultWithOnlyKeyAttributesSpecifiedRecordNotInTable()
            throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());

        dynamoMapper.save(testItem, defaultConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
    }

    /**
     * Update an existing item in the table.
     */
    @Test
    public void testDefaultWithKeyAndNonKeyAttributesSpecifiedRecordInTable()
            throws Exception {

        /* First put a new item (without non-key attribute)*/
        TestItem testItem = putRandomUniqueItem(null, null);
        String hashKeyValue = testItem.getHashKey();
        Long rangeKeyValue = testItem.getRangeKey();

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(hashKeyValue, returnedObject.getHashKey());
        assertEquals(rangeKeyValue, returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
        
        /* Put an updated object with the same key and an additional non-key attribute. */
        testItem.setHashKey(hashKeyValue);
        testItem.setRangeKey(rangeKeyValue);
        testItem.setNonKeyAttribute("update");

        dynamoMapper.save(testItem, defaultConfig);
        returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(testItem.getNonKeyAttribute(), returnedObject.getNonKeyAttribute());
    }

    /**
     * Use UPDATE to put a new item in the table.
     */
    @Test
    public void testDefaultWithKeyAndNonKeyAttributesSpecifiedRecordNotInTable()
            throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());
        testItem.setNonKeyAttribute("new item");

        dynamoMapper.save(testItem, defaultConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(testItem.getNonKeyAttribute(), returnedObject.getNonKeyAttribute());
    }
    
    /*********************************************
     **      UPDATE_SKIP_NULL_ATTRIBUTES        **
     *********************************************/
    
    /**
     * When using UPDATE_SKIP_NULL_ATTRIBUTES, key-only update on existing item
     * should not affect the item at all, since all the null-valued non-key
     * attributes are ignored.
     */
    @Test
    public void testUpdateSkipNullWithOnlyKeyAttributesSpecifiedRecordInTable()
            throws Exception {

        /* First put a new item (with non-key attribute)*/
        TestItem testItem = putRandomUniqueItem("foo", null);
        
        /* Put an key-only object with the same key */
        testItem.setNonKeyAttribute(null);
        
        dynamoMapper.save(testItem, updateSkipNullConfig);
        
        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        /* The non-key attribute should not be removed */
        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals("foo", returnedObject.getNonKeyAttribute());
    }

    /**
     * The behavior should be the same as UPDATE.
     */
    @Test
    public void testUpdateSkipNullWithOnlyKeyAttributesSpecifiedRecordNotInTable()
            throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());

        dynamoMapper.save(testItem, updateSkipNullConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
    }

    /**
     * Use UPDATE_SKIP_NULL_ATTRIBUTES to update an existing item in the table.
     */
    @Test
    public void testUpdateSkipNullWithKeyAndNonKeyAttributesSpecifiedRecordInTable()
            throws Exception {

        /* First put a new item (without non-key attribute)*/
        TestItem testItem = putRandomUniqueItem(null, null);
        String hashKeyValue = testItem.getHashKey();
        Long rangeKeyValue = testItem.getRangeKey();

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(hashKeyValue, returnedObject.getHashKey());
        assertEquals(rangeKeyValue, returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
        
        /* Put an updated object with the same key and an additional non-key attribute. */
        String nonKeyAttributeValue = "update";
        testItem.setHashKey(hashKeyValue);
        testItem.setRangeKey(rangeKeyValue);
        testItem.setNonKeyAttribute(nonKeyAttributeValue);

        dynamoMapper.save(testItem, updateSkipNullConfig);
        returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(testItem.getNonKeyAttribute(), returnedObject.getNonKeyAttribute());
        
        /* At last, save the object again, but with non-key attribute set as null.
         * This should not change the existing item.
         */
        testItem.setNonKeyAttribute(null);
        dynamoMapper.save(testItem, updateSkipNullConfig);
        returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(nonKeyAttributeValue, returnedObject.getNonKeyAttribute());
    }

    /**
     * Use UPDATE_SKIP_NULL_ATTRIBUTES to put a new item in the table.
     */
    @Test
    public void testUpdateSkipNullWithKeyAndNonKeyAttributesSpecifiedRecordNotInTable()
            throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());
        testItem.setNonKeyAttribute("new item");

        dynamoMapper.save(testItem, updateSkipNullConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(testItem.getNonKeyAttribute(), returnedObject.getNonKeyAttribute());
    }
    
    /*********************************************
     **               APPEND_SET                **
     *********************************************/
    
    /**
     * The behavior should be the same as UPDATE_SKIP_NULL_ATTRIBUTES.
     */
    @Test
    public void testAppendSetWithOnlyKeyAttributesSpecifiedRecordInTable()
            throws Exception {

        /* First put a new item (with non-key attributes)*/
        Set<String> randomSet = generateRandomStringSet(3);
        TestItem testItem = putRandomUniqueItem("foo", randomSet);
        
        /* Put an key-only object with the same key */
        testItem.setNonKeyAttribute(null);
        testItem.setStringSetAttribute(null);
        
        dynamoMapper.save(testItem, appendSetConfig);
        
        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        /* The non-key attribute should not be removed */
        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals("foo", returnedObject.getNonKeyAttribute());
        assertTrue(assertSetEquals(randomSet, returnedObject.getStringSetAttribute()));
    }

    /**
     * The behavior should be the same as UPDATE and UPDATE_SKIP_NULL_ATTRIBUTES.
     */
    @Test
    public void testAppendSetWithOnlyKeyAttributesSpecifiedRecordNotInTable()
            throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());

        dynamoMapper.save(testItem, appendSetConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
        assertNull(returnedObject.getStringSetAttribute());
    }

    /**
     * Use APPEND_SET to update an existing item in the table.
     */
    @Test
    public void testAppendSetWithKeyAndNonKeyAttributesSpecifiedRecordInTable()
            throws Exception {

        /* First put a new item (without non-key attribute)*/
        TestItem testItem = putRandomUniqueItem(null, null);
        String hashKeyValue = testItem.getHashKey();
        Long rangeKeyValue = testItem.getRangeKey();

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(hashKeyValue, returnedObject.getHashKey());
        assertEquals(rangeKeyValue, returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
        assertNull(returnedObject.getStringSetAttribute());
        
        /* Put an updated object with the same key and an additional non-key attribute. */
        String nonKeyAttributeValue = "update";
        Set<String> stringSetAttributeValue = generateRandomStringSet(3);
        testItem.setHashKey(hashKeyValue);
        testItem.setRangeKey(rangeKeyValue);
        testItem.setNonKeyAttribute(nonKeyAttributeValue);
        testItem.setStringSetAttribute(stringSetAttributeValue);

        dynamoMapper.save(testItem, appendSetConfig);
        returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(testItem.getNonKeyAttribute(), returnedObject.getNonKeyAttribute());
        assertTrue(assertSetEquals(testItem.getStringSetAttribute(), returnedObject.getStringSetAttribute()));
        
        /* Override nonKeyAttribute and append stringSetAttribute */
        testItem.setNonKeyAttribute("blabla");
        Set<String> appendSetAttribute = generateRandomStringSet(3);
        testItem.setStringSetAttribute(appendSetAttribute);
        dynamoMapper.save(testItem, appendSetConfig);
        returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals("blabla", returnedObject.getNonKeyAttribute());
        // expected set after the append
        stringSetAttributeValue.addAll(appendSetAttribute);
        assertTrue(assertSetEquals(stringSetAttributeValue, returnedObject.getStringSetAttribute()));
        
        /* Append on an existing scalar attribute would result in an exception */
        TestAppendToScalarItem testAppendToScalarItem = new TestAppendToScalarItem();
        testAppendToScalarItem.setHashKey(testItem.getHashKey());
        testAppendToScalarItem.setRangeKey(testItem.getRangeKey());
        // this fake set attribute actually points to a scalar attribute
        testAppendToScalarItem.setFakeStringSetAttribute(generateRandomStringSet(1));
        try {
            dynamoMapper.save(testAppendToScalarItem, appendSetConfig);
            fail("Should have thrown a 'Type mismatch' service exception.");
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
        }
    }

    /**
     * Use APPEND_SET to put a new item in the table.
     */
    @Test
    public void testAppendSetWithKeyAndNonKeyAttributesSpecifiedRecordNotInTable()
            throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());
        testItem.setNonKeyAttribute("new item");
        testItem.setStringSetAttribute(generateRandomStringSet(3));

        dynamoMapper.save(testItem, appendSetConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(testItem.getNonKeyAttribute(), returnedObject.getNonKeyAttribute());
        assertEquals(testItem.getStringSetAttribute(), returnedObject.getStringSetAttribute());

    }

    /*********************************************
     **                 CLOBBER                 **
     *********************************************/
    
    /**
     * Use CLOBBER to override the existing item by saving a key-only object.
     */
    @Test
    public void testClobberWithOnlyKeyAttributesSpecifiedRecordInTable()
            throws Exception {
        /* Put the item with non-key attribute */
        TestItem testItem = putRandomUniqueItem("foo", null);

        /* Override the item by saving a key-only object. */
        testItem.setNonKeyAttribute(null);
        dynamoMapper.save(testItem, clobberConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
    }

    /**
     * Use CLOBBER to put a new item with only key attributes.
     */
    @Test
    public void testClobberWithOnlyKeyAttributesSpecifiedRecordNotInTable()
            throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());

        dynamoMapper.save(testItem, clobberConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertNull(returnedObject.getNonKeyAttribute());
    }

    /**
     * Use CLOBBER to override the existing item.
     */
    @Test
    public void testClobberWithKeyAndNonKeyAttributesSpecifiedRecordInTable()
            throws Exception {
        /* Put the item with non-key attribute */
        TestItem testItem = putRandomUniqueItem("foo", null);

        /* Override the item. */
        testItem.setNonKeyAttribute("not foo");
        dynamoMapper.save(testItem, clobberConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(testItem.getNonKeyAttribute(), returnedObject.getNonKeyAttribute());
    }

    /**
     * Use CLOBBER to put a new item.
     */
    @Test
    public void testClobberWithKeyAndNonKeyAttributesSpecifiedRecordNotInTable()
            throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());
        testItem.setNonKeyAttribute("new item");
        
        dynamoMapper.save(testItem, clobberConfig);

        TestItem returnedObject = (TestItem) dynamoMapper.load(testItem);

        assertNotNull(returnedObject);
        assertEquals(testItem.getHashKey(), returnedObject.getHashKey());
        assertEquals(testItem.getRangeKey(), returnedObject.getRangeKey());
        assertEquals(testItem.getNonKeyAttribute(), returnedObject.getNonKeyAttribute());
    }

    @Test
    public void testClobberWithVersion_SucceedsEvenWhenVersionDoesNotMatch() throws Exception {
        TestItemWithVersion testItem = putRandomUniqueItemWithVersion(10L);
        testItem.setVersion(1L);
        testItem.setNonKeyAttribute("new item");

        dynamoMapper.save(testItem, clobberConfig);

        GetItemResult item = rawGetItem(testItem);
        Map<String, AttributeValue> expected = ImmutableMapParameter.of(
            hashKeyName, new AttributeValue(testItem.getHashKey()),
            rangeKeyName, new AttributeValue().withN(testItem.getRangeKey().toString()),
            nonKeyAttributeName, new AttributeValue(testItem.getNonKeyAttribute()),
            versionAttributeName, new AttributeValue().withN(Long.toString(2L)));

        assertEquals(expected, item.getItem());
    }

    /**
     * Use CLOBBER to override the existing item by saving a key-only object.
     */
    @Test
    public void testPutWithOnlyKeyAttributesSpecifiedRecordInTable()
        throws Exception {
        /* Put the item with non-key attribute */
        TestItem testItem = putRandomUniqueItem("foo", null);

        /* Override the item by saving a key-only object. */
        testItem.setNonKeyAttribute(null);
        dynamoMapper.save(testItem, putConfig);

        GetItemResult item = rawGetItem(testItem);
        Map<String, AttributeValue> expected = ImmutableMapParameter.of(
            hashKeyName, new AttributeValue(testItem.getHashKey()),
            rangeKeyName, new AttributeValue().withN(testItem.getRangeKey().toString()));

        assertEquals(expected, item.getItem());
    }

    @Test
    public void testPutWithKeyAndNonKeyAttributesSpecifiedRecordInTable() throws Exception {
        /* Put the item with non-key attribute */
        TestItem testItem = putRandomUniqueItem("foo", null);

        /* Override the item. */
        testItem.setNonKeyAttribute("not foo");
        dynamoMapper.save(testItem, putConfig);

        GetItemResult item = rawGetItem(testItem);
        Map<String, AttributeValue> expected = ImmutableMapParameter.of(
            hashKeyName, new AttributeValue(testItem.getHashKey()),
            rangeKeyName, new AttributeValue().withN(testItem.getRangeKey().toString()),
            nonKeyAttributeName, new AttributeValue(testItem.getNonKeyAttribute()));

        assertEquals(expected, item.getItem());
    }

    @Test
    public void testPutWithKeyAndNonKeyAttributesSpecifiedRecordNotInTable()
        throws Exception {
        TestItem testItem = new TestItem();
        testItem.setHashKey(UUID.randomUUID().toString());
        testItem.setRangeKey(System.currentTimeMillis());
        testItem.setNonKeyAttribute("new item");

        dynamoMapper.save(testItem, putConfig);

        GetItemResult item = rawGetItem(testItem);
        Map<String, AttributeValue> expected = ImmutableMapParameter.of(
            hashKeyName, new AttributeValue(testItem.getHashKey()),
            rangeKeyName, new AttributeValue().withN(testItem.getRangeKey().toString()),
            nonKeyAttributeName, new AttributeValue(testItem.getNonKeyAttribute()));

        assertEquals(expected, item.getItem());
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void testPutWithVersion_FailsWhenVerisonDoesNotMatch() throws Exception {
        TestItemWithVersion testItem = putRandomUniqueItemWithVersion(10L);
        testItem.setVersion(1L);
        testItem.setNonKeyAttribute("new item");

        dynamoMapper.save(testItem, putConfig);
    }

    @Test
    public void testPutWithVersion_SucceedsWhenVersionMatches() throws Exception {
        Long version = 10L;
        TestItemWithVersion testItem = putRandomUniqueItemWithVersion(version);
        testItem.setNonKeyAttribute("new item");

        dynamoMapper.save(testItem, putConfig);

        GetItemResult item = rawGetItem(testItem);
        Map<String, AttributeValue> expected = ImmutableMapParameter.of(
            hashKeyName, new AttributeValue(testItem.getHashKey()),
            rangeKeyName, new AttributeValue().withN(testItem.getRangeKey().toString()),
            nonKeyAttributeName, new AttributeValue(testItem.getNonKeyAttribute()),
            versionAttributeName, new AttributeValue().withN(Long.toString(version + 1)));

        assertEquals(expected, item.getItem());
    }

    private GetItemResult rawGetItem(TestItem testItem) {
        return dynamo.getItem(
            new GetItemRequest()
                .withTableName(tableName)
                .withKey(ImmutableMapParameter.of(hashKeyName, new AttributeValue(testItem.getHashKey()),
                                                  rangeKeyName, new AttributeValue().withN(testItem.getRangeKey().toString()))));
    }

    private static TestItem putRandomUniqueItem(String nonKeyAttributeValue, Set<String> stringSetAttributeValue) {
        String hashKeyValue = UUID.randomUUID().toString();
        Long rangeKeyValue = System.currentTimeMillis();
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(hashKeyName, new AttributeValue().withS(hashKeyValue));
        item.put(rangeKeyName, new AttributeValue().withN(rangeKeyValue.toString()));
        if (null != nonKeyAttributeValue) {
            item.put(nonKeyAttributeName, new AttributeValue().withS(nonKeyAttributeValue));
        }
        if (null != stringSetAttributeValue) {
            item.put(stringSetAttributeName, new AttributeValue().withSS(stringSetAttributeValue));
        }
        dynamo.putItem(new PutItemRequest().withTableName(tableName).withItem(item));
        
        /* Returns the item as a modeled object. */
        TestItem testItem = new TestItem();
        testItem.setHashKey(hashKeyValue);
        testItem.setRangeKey(rangeKeyValue);
        testItem.setNonKeyAttribute(nonKeyAttributeValue);
        testItem.setStringSetAttribute(stringSetAttributeValue);
        return testItem;
    }

    private static TestItemWithVersion putRandomUniqueItemWithVersion(Long version) {
        String hashKeyValue = UUID.randomUUID().toString();
        Long rangeKeyValue = System.currentTimeMillis();
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(hashKeyName, new AttributeValue().withS(hashKeyValue));
        item.put(rangeKeyName, new AttributeValue().withN(rangeKeyValue.toString()));
        item.put(versionAttributeName, new AttributeValue().withN(version.toString()));
        dynamo.putItem(new PutItemRequest().withTableName(tableName).withItem(item));
        TestItemWithVersion testItem = new TestItemWithVersion();
        testItem.setHashKey(hashKeyValue);
        testItem.setRangeKey(rangeKeyValue);
        testItem.setVersion(version);
        return testItem;
    }

    private static Set<String> generateRandomStringSet(int size) {
        Set<String> result = new HashSet<String>();
        for (int i = 0; i < size; i++) {
            result.add(UUID.randomUUID().toString());
        }
        return result;
    }

    private static boolean assertSetEquals(Set<?> expected, Set<?> actual) {
        if (expected == null || actual == null) {
            return (expected == null && actual == null);
        }
        if (expected.size() != actual.size()) {
            return false;
        }
        for (Object item : expected) {
            if ( !actual.contains(item) ) {
                return false;
            }
        }
        return true;
    }
} 
