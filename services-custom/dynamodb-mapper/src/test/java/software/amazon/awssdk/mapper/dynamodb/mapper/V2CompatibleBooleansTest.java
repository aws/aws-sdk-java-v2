/*
 * Copyright 2011-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import software.amazon.awssdk.mapper.dynamodb.ConversionSchema;
import software.amazon.awssdk.mapper.dynamodb.ConversionSchemas;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBAttribute;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperFieldModel;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMappingException;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTyped;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.util.ImmutableMapParameter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The default converters should be able to unmarshall both V1 booleans (numeric 0/1) and native Dynamo booleans. When using the
 * standard converters booleans should be marshalled as numeric attribute values.
 */
@RunWith(MockitoJUnitRunner.class)
public class V2CompatibleBooleansTest {

    private static final String HASH_KEY = "1234";

    @Mock
    private AmazonDynamoDB ddb;

    /**
     * Mapper with default config.
     */
    private DynamoDBMapper defaultMapper;

    /**
     * Mapper explicitly using {@link ConversionSchemas#V2_COMPATIBLE}
     */
    private DynamoDBMapper v2CompatMapper;

    /**
     * Mapper explicitly using {@link ConversionSchemas#V1}
     */
    private DynamoDBMapper v1Mapper;

    /**
     * Mapper explicitly using {@link ConversionSchemas#V2}
     */
    private DynamoDBMapper v2Mapper;

    @Before
    public void setup() {
        defaultMapper = new DynamoDBMapper(ddb);
        v2CompatMapper = buildMapper(ConversionSchemas.V2_COMPATIBLE);
        v1Mapper = buildMapper(ConversionSchemas.V1);
        v2Mapper = buildMapper(ConversionSchemas.V2);
        // Just stub dummy response for all save related tests
        when(ddb.updateItem(any(UpdateItemRequest.class))).thenReturn(new UpdateItemResult());
    }

    private DynamoDBMapper buildMapper(ConversionSchema schema) {
        return new DynamoDBMapper(ddb, DynamoDBMapperConfig.builder()
                .withConversionSchema(schema)
                .build());
    }

    /**
     * Without coercion from an annotation the default mapping should marshall booleans as a number.
     */
    @Test
    public void saveBooleanUsingDefaultConverters_MarshallsIntoNumber() {
        defaultMapper.save(new UnitTestPojo().setHashKey(HASH_KEY).setBooleanAttr(true));
        verifyAttributeUpdatedWithValue("booleanAttr", new AttributeValue().withN("1"));
    }

    @Test
    public void saveBooleanUsingV1Schema_MarshallsIntoNumber() {
        v1Mapper.save(new UnitTestPojo().setHashKey(HASH_KEY).setBooleanAttr(true));
        verifyAttributeUpdatedWithValue("booleanAttr", new AttributeValue().withN("1"));
    }

    @Test
    public void saveBooleanUsingV2Compat_MarshallsIntoNumber() {
        v2CompatMapper.save(new UnitTestPojo().setHashKey(HASH_KEY).setBooleanAttr(true));
        verifyAttributeUpdatedWithValue("booleanAttr", new AttributeValue().withN("1"));
    }

    @Test
    public void saveBooleanUsingV2Schema_MarshallsIntoNativeBool() {
        v2Mapper.save(new UnitTestPojo().setHashKey(HASH_KEY).setBooleanAttr(true));
        verifyAttributeUpdatedWithValue("booleanAttr", new AttributeValue().withBOOL(true));
    }

    /**
     * {@link software.amazon.awssdk.mapper.dynamodb.DynamoDBNativeBoolean} or {@link DynamoDBTyped} can force native
     * boolean marshalling.
     */
    @Test
    public void saveCoercedNativeBooleanUsingDefaultConverters_MarshallsIntoNativeBool() {
        saveCoercedNativeBoolean_MarshallsIntoNativeBoolean(defaultMapper);
    }

    @Test
    public void saveCoercedNativeBooleanUsingV1Schema_MarshallsIntoNativeBool() {
        saveCoercedNativeBoolean_MarshallsIntoNativeBoolean(v1Mapper);
    }

    @Test
    public void saveCoercedNativeBooleanUsingV2CompatSchema_MarshallsIntoNativeBool() {
        saveCoercedNativeBoolean_MarshallsIntoNativeBoolean(v2CompatMapper);
    }

    @Test
    public void saveCoercedNativeBooleanUsingV2_MarshallsIntoNativeBool() {
        saveCoercedNativeBoolean_MarshallsIntoNativeBoolean(v2Mapper);
    }

    private void saveCoercedNativeBoolean_MarshallsIntoNativeBoolean(DynamoDBMapper mapper) {
        mapper.save(new UnitTestPojo().setNativeBoolean(true).setHashKey(HASH_KEY));
        verifyAttributeUpdatedWithValue("nativeBoolean", new AttributeValue().withBOOL(true));
    }

    /**
     * {@link DynamoDBTyped} can force numeric boolean conversion even when using V2 schema.
     */
    @Test
    public void saveCoercedNumericBooleanUsingDefaultConverters_MarshallsIntoNumericBool() {
        saveCoercedNumericBoolean_MarshallsIntoNumericBoolean(defaultMapper);
    }

    @Test
    public void saveCoercedNumericBooleanUsingV1Schema_MarshallsIntoNumericBool() {
        saveCoercedNumericBoolean_MarshallsIntoNumericBoolean(v1Mapper);
    }

    @Test
    public void saveCoercedNumericBooleanUsingV2CompatSchema_MarshallsIntoNumericBool() {
        saveCoercedNumericBoolean_MarshallsIntoNumericBoolean(v2CompatMapper);
    }

    @Test
    public void saveCoercedNumericBooleanUsingV2_MarshallsIntoNumericBool() {
        saveCoercedNumericBoolean_MarshallsIntoNumericBoolean(v2Mapper);
    }

    private void saveCoercedNumericBoolean_MarshallsIntoNumericBoolean(DynamoDBMapper mapper) {
        mapper.save(new UnitTestPojo().setNumericBoolean(true).setHashKey(HASH_KEY));
        verifyAttributeUpdatedWithValue("numericBoolean", new AttributeValue().withN("1"));
    }

    @Test
    public void saveBooleanListUsingDefaultConverters_MarshallsIntoListOfNumbers() {
        defaultMapper.save(new UnitTestPojoWithList()
                                   .setBooleanList(Arrays.asList(Boolean.FALSE, Boolean.TRUE))
                                   .setHashKey(HASH_KEY));
        verifyAttributeUpdatedWithValue("booleanList", new AttributeValue().withL(new AttributeValue().withN("0"),
                                                                                  new AttributeValue().withN("1")));
    }

    /**
     * Verifies the mapper results in an update item call that has an update for the appropriate attribute.
     *
     * @param attributeName Attribute expected to be updated.
     * @param expected      Expected value of update action.
     */
    private void verifyAttributeUpdatedWithValue(String attributeName, AttributeValue expected) {
        ArgumentCaptor<UpdateItemRequest> updateItemRequestCaptor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(ddb).updateItem(updateItemRequestCaptor.capture());
        assertEquals(expected, updateItemRequestCaptor.getValue().getAttributeUpdates().get(attributeName).getValue());
    }

    @Test
    public void loadNumericBooleanUsingDefaultConverters_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", new AttributeValue().withN("1"));
        final UnitTestPojo pojo = loadPojo(defaultMapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNumericBooleanUsingV1Schema_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", new AttributeValue().withN("1"));
        final UnitTestPojo pojo = loadPojo(v1Mapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNumericBooleanUsingV2CompatSchema_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", new AttributeValue().withN("1"));
        final UnitTestPojo pojo = loadPojo(v2CompatMapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNumericBooleanUsingV2_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", new AttributeValue().withN("1"));
        final UnitTestPojo pojo = loadPojo(v2Mapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNativeBooleanUsingDefaultConverters_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", new AttributeValue().withBOOL(true));
        final UnitTestPojo pojo = loadPojo(defaultMapper);
        assertTrue(pojo.getBooleanAttr());
    }

    /**
     * V1 schema does not handle native bool types by default
     */
    @Test(expected = DynamoDBMappingException.class)
    public void loadNativeBooleanUsingV1Schema_FailsToUnmarshall() {
        stubGetItemRequest("booleanAttr", new AttributeValue().withBOOL(true));
        loadPojo(v1Mapper);
    }

    /**
     * Native bool support can be forced in V1 schema with @{@link DynamoDBTyped}.
     */
    @Test
    public void loadCoercedNativeBooleanUsingV1Schema_UnmarshallsCorrectly() {
        stubGetItemRequest("nativeBoolean", new AttributeValue().withBOOL(true));
        final UnitTestPojo pojo = loadPojo(v1Mapper);
        assertTrue(pojo.getNativeBoolean());
    }

    @Test
    public void loadNativeBooleanUsingV2CompatSchema_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", new AttributeValue().withBOOL(true));
        final UnitTestPojo pojo = loadPojo(v2CompatMapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNativeBooleanUsingV2_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", new AttributeValue().withBOOL(true));
        final UnitTestPojo pojo = loadPojo(v2Mapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNativeBooleanListUsingDefaultConverters_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanList", new AttributeValue()
                .withL(new AttributeValue().withBOOL(true),
                       new AttributeValue().withBOOL(false)));
        final UnitTestPojoWithList pojo = loadListPojo(defaultMapper);

        assertTrue(pojo.getBooleanList().get(0));
        assertFalse(pojo.getBooleanList().get(1));
    }

    @Test
    public void loadNumericBooleanListUsingDefaultConverters_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanList", new AttributeValue()
                .withL(new AttributeValue().withN("1"),
                       new AttributeValue().withN("0")));
        final UnitTestPojoWithList pojo = loadListPojo(defaultMapper);

        assertTrue(pojo.getBooleanList().get(0));
        assertFalse(pojo.getBooleanList().get(1));
    }

    private UnitTestPojoWithList loadListPojo(DynamoDBMapper mapper) {
        UnitTestPojoWithList pojo = new UnitTestPojoWithList();
        pojo.setHashKey(HASH_KEY);
        return mapper.load(pojo);
    }

    private UnitTestPojo loadPojo(DynamoDBMapper mapper) {
        return mapper.load(new UnitTestPojo().setHashKey(HASH_KEY));
    }

    /**
     * Stub a call to getItem to return a result with the given attribute value in the item.
     *
     * @param attributeName  Attribute name to return in result (in addition to hash key)
     * @param attributeValue Attribute value to return in result (in addition to hash key)
     */
    private void stubGetItemRequest(String attributeName, AttributeValue attributeValue) {
        when(ddb.getItem(any(GetItemRequest.class))).thenReturn(createGetItemResult(attributeName, attributeValue));
    }

    /**
     * Create a {@link GetItemResult} with the hash key value ({@value #HASH_KEY} and the additional attribute.
     *
     * @param attributeName  Additional attribute to include in created {@link GetItemResult}.
     * @param attributeValue Value of additional attribute.
     */
    private GetItemResult createGetItemResult(String attributeName, AttributeValue attributeValue) {
        return new GetItemResult().withItem(
                ImmutableMapParameter.of("hashKey", new AttributeValue(HASH_KEY),
                                         attributeName, attributeValue));
    }

    @DynamoDBTable(tableName = "UnitTestTable")
    public static class UnitTestPojo {

        @DynamoDBHashKey
        private String hashKey;

        @DynamoDBAttribute
        private Boolean booleanAttr;

        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.BOOL)
        private Boolean nativeBoolean;

        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.N)
        private Boolean numericBoolean;


        public String getHashKey() {
            return hashKey;
        }

        public UnitTestPojo setHashKey(String hashKey) {
            this.hashKey = hashKey;
            return this;
        }

        public Boolean getBooleanAttr() {
            return booleanAttr;
        }

        public UnitTestPojo setBooleanAttr(Boolean booleanAttr) {
            this.booleanAttr = booleanAttr;
            return this;
        }

        public Boolean getNativeBoolean() {
            return nativeBoolean;
        }

        public UnitTestPojo setNativeBoolean(Boolean nativeBoolean) {
            this.nativeBoolean = nativeBoolean;
            return this;
        }

        public Boolean getNumericBoolean() {
            return numericBoolean;
        }

        public UnitTestPojo setNumericBoolean(Boolean numericBoolean) {
            this.numericBoolean = numericBoolean;
            return this;
        }

    }

    public static class UnitTestPojoWithList extends UnitTestPojo {

        @DynamoDBAttribute
        private List<Boolean> booleanList;

        public List<Boolean> getBooleanList() {
            return booleanList;
        }

        public UnitTestPojo setBooleanList(List<Boolean> booleanList) {
            this.booleanList = booleanList;
            return this;
        }
    }

}
