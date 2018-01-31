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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.util.ImmutableMapParameter;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.ConversionSchema;
import software.amazon.awssdk.services.dynamodb.datamodeling.ConversionSchemas;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperFieldModel;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbNativeBoolean;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTyped;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/**
 * The default converters should be able to unmarshall both V1 booleans (numeric 0/1) and native Dynamo booleans. When using the
 * standard converters booleans should be marshalled as numeric attribute values.
 */
@RunWith(MockitoJUnitRunner.class)
public class V2CompatibleBooleansTest {

    private static final String HASH_KEY = "1234";

    @Mock
    private DynamoDBClient ddb;

    /**
     * Mapper with default config.
     */
    private DynamoDbMapper defaultMapper;

    /**
     * Mapper explicitly using {@link ConversionSchemas#V2_COMPATIBLE}
     */
    private DynamoDbMapper v2CompatMapper;

    /**
     * Mapper explicitly using {@link ConversionSchemas#V1}
     */
    private DynamoDbMapper v1Mapper;

    /**
     * Mapper explicitly using {@link ConversionSchemas#V2}
     */
    private DynamoDbMapper v2Mapper;

    @Before
    public void setup() {
        defaultMapper = new DynamoDbMapper(ddb);
        v2CompatMapper = buildMapper(ConversionSchemas.V2_COMPATIBLE);
        v1Mapper = buildMapper(ConversionSchemas.V1);
        v2Mapper = buildMapper(ConversionSchemas.V2);
        // Just stub dummy response for all save related tests
        when(ddb.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());
    }

    private DynamoDbMapper buildMapper(ConversionSchema schema) {
        return new DynamoDbMapper(ddb, DynamoDbMapperConfig.builder()
                .withConversionSchema(schema)
                .build());
    }

    /**
     * Without coercion from an annotation the default mapping should marshall booleans as a number.
     */
    @Test
    public void saveBooleanUsingDefaultConverters_MarshallsIntoNumber() {
        defaultMapper.save(new UnitTestPojo().setHashKey(HASH_KEY).setBooleanAttr(true));
        verifyAttributeUpdatedWithValue("booleanAttr", AttributeValue.builder().n("1").build());
    }

    @Test
    public void saveBooleanUsingV1Schema_MarshallsIntoNumber() {
        v1Mapper.save(new UnitTestPojo().setHashKey(HASH_KEY).setBooleanAttr(true));
        verifyAttributeUpdatedWithValue("booleanAttr", AttributeValue.builder().n("1").build());
    }

    @Test
    public void saveBooleanUsingV2Compat_MarshallsIntoNumber() {
        v2CompatMapper.save(new UnitTestPojo().setHashKey(HASH_KEY).setBooleanAttr(true));
        verifyAttributeUpdatedWithValue("booleanAttr", AttributeValue.builder().n("1").build());
    }

    @Test
    public void saveBooleanUsingV2Schema_MarshallsIntoNativeBool() {
        v2Mapper.save(new UnitTestPojo().setHashKey(HASH_KEY).setBooleanAttr(true));
        verifyAttributeUpdatedWithValue("booleanAttr", AttributeValue.builder().bool(true).build());
    }

    /**
     * {@link DynamoDbNativeBoolean} or {@link DynamoDbTyped} can force
     * native
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

    private void saveCoercedNativeBoolean_MarshallsIntoNativeBoolean(DynamoDbMapper mapper) {
        mapper.save(new UnitTestPojo().setNativeBoolean(true).setHashKey(HASH_KEY));
        verifyAttributeUpdatedWithValue("nativeBoolean", AttributeValue.builder().bool(true).build());
    }

    /**
     * {@link DynamoDbTyped} can force numeric boolean conversion even when using V2 schema.
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

    private void saveCoercedNumericBoolean_MarshallsIntoNumericBoolean(DynamoDbMapper mapper) {
        mapper.save(new UnitTestPojo().setNumericBoolean(true).setHashKey(HASH_KEY));
        verifyAttributeUpdatedWithValue("numericBoolean", AttributeValue.builder().n("1").build());
    }

    @Test
    public void saveBooleanListUsingDefaultConverters_MarshallsIntoListOfNumbers() {
        defaultMapper.save(new UnitTestPojoWithList()
                .setBooleanList(Arrays.asList(Boolean.FALSE, Boolean.TRUE))
                .setHashKey(HASH_KEY));
        verifyAttributeUpdatedWithValue("booleanList", AttributeValue.builder()
                .l(
                        AttributeValue.builder().n("0").build(),
                        AttributeValue.builder().n("1").build())
                .build());
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
        assertEquals(expected, updateItemRequestCaptor.getValue().attributeUpdates().get(attributeName).value());
    }

    @Test
    public void loadNumericBooleanUsingDefaultConverters_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", AttributeValue.builder().n("1").build());
        final UnitTestPojo pojo = loadPojo(defaultMapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNumericBooleanUsingV1Schema_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", AttributeValue.builder().n("1").build());
        final UnitTestPojo pojo = loadPojo(v1Mapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNumericBooleanUsingV2CompatSchema_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", AttributeValue.builder().n("1").build());
        final UnitTestPojo pojo = loadPojo(v2CompatMapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNumericBooleanUsingV2_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", AttributeValue.builder().n("1").build());
        final UnitTestPojo pojo = loadPojo(v2Mapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNativeBooleanUsingDefaultConverters_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", AttributeValue.builder().bool(true).build());
        final UnitTestPojo pojo = loadPojo(defaultMapper);
        assertTrue(pojo.getBooleanAttr());
    }
//
    /**
     * V1 schema does not handle native bool types by default
     */
    @Test(expected = DynamoDbMappingException.class)
    public void loadNativeBooleanUsingV1Schema_FailsToUnmarshall() {
        stubGetItemRequest("booleanAttr", AttributeValue.builder().bool(true).build());
        loadPojo(v1Mapper);
    }

    /**
     * Native bool support can be forced in V1 schema with @{@link DynamoDbTyped}.
     */
    @Test
    public void loadCoercedNativeBooleanUsingV1Schema_UnmarshallsCorrectly() {
        stubGetItemRequest("nativeBoolean", AttributeValue.builder().bool(true).build());
        final UnitTestPojo pojo = loadPojo(v1Mapper);
        assertTrue(pojo.getNativeBoolean());
    }

    @Test
    public void loadNativeBooleanUsingV2CompatSchema_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", AttributeValue.builder().bool(true).build());
        final UnitTestPojo pojo = loadPojo(v2CompatMapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNativeBooleanUsingV2_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanAttr", AttributeValue.builder().bool(true).build());
        final UnitTestPojo pojo = loadPojo(v2Mapper);
        assertTrue(pojo.getBooleanAttr());
    }

    @Test
    public void loadNativeBooleanListUsingDefaultConverters_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanList", AttributeValue.builder()
                .l(
                        AttributeValue.builder().bool(true).build(),
                        AttributeValue.builder().bool(false).build()
                ).build());
        final UnitTestPojoWithList pojo = loadListPojo(defaultMapper);

        assertTrue(pojo.getBooleanList().get(0));
        assertFalse(pojo.getBooleanList().get(1));
    }

    @Test
    public void loadNumericBooleanListUsingDefaultConverters_UnmarshallsCorrectly() {
        stubGetItemRequest("booleanList", AttributeValue.builder()
                .l(
                        AttributeValue.builder().n("1").build(),
                        AttributeValue.builder().n("0").build()
                ).build());
        final UnitTestPojoWithList pojo = loadListPojo(defaultMapper);

        assertTrue(pojo.getBooleanList().get(0));
        assertFalse(pojo.getBooleanList().get(1));
    }

    private UnitTestPojoWithList loadListPojo(DynamoDbMapper mapper) {
        UnitTestPojoWithList pojo = new UnitTestPojoWithList();
        pojo.setHashKey(HASH_KEY);
        return mapper.load(pojo);
    }

    private UnitTestPojo loadPojo(DynamoDbMapper mapper) {
        return mapper.load(new UnitTestPojo().setHashKey(HASH_KEY));
    }

    /**
     * Stub a call to getItem to return a result with the given attribute value in the item.
     *
     * @param attributeName  Attribute name to return in result (in addition to hash key)
     * @param attributeValue Attribute value to return in result (in addition to hash key)
     */
    private void stubGetItemRequest(String attributeName, AttributeValue attributeValue) {
        when(ddb.getItem(any(GetItemRequest.class))).thenReturn(createGetItemResponse(attributeName, attributeValue));
    }

    /**
     * Create a {@link GetItemResponse} with the hash key value ({@value #HASH_KEY} and the additional attribute.
     *
     * @param attributeName  Additional attribute to include in created {@link GetItemResponse}.
     * @param attributeValue Value of additional attribute.
     */
    private GetItemResponse createGetItemResponse(String attributeName, AttributeValue attributeValue) {
        return GetItemResponse.builder().item(
                ImmutableMapParameter.of("hashKey", AttributeValue.builder().s(HASH_KEY).build(),
                        attributeName, attributeValue)).build();
    }

    @DynamoDbTable(tableName = "UnitTestTable")
    public static class UnitTestPojo {

        @DynamoDbHashKey
        private String hashKey;

        @DynamoDbAttribute
        private Boolean booleanAttr;

        @DynamoDbTyped(DynamoDbMapperFieldModel.DynamoDbAttributeType.BOOL)
        private Boolean nativeBoolean;

        @DynamoDbTyped(DynamoDbMapperFieldModel.DynamoDbAttributeType.N)
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

        @DynamoDbAttribute
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
