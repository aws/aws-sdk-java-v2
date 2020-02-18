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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.nullAttributeValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.boolAttribute;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.integerNumberAttribute;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.stringAttribute;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attribute.AttributeSupplier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemComposedClass;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;

public class StaticTableSchemaTest {
    private static final String TABLE_TAG_KEY = "table-tag-key";
    private static final String TABLE_TAG_VALUE = "table-tag-value";
    private static final AttributeValue ATTRIBUTE_VALUE_B = AttributeValue.builder().bool(true).build();
    private static final AttributeValue ATTRIBUTE_VALUE_S = AttributeValue.builder().s("test-string").build();

    private static final StaticTableSchema<FakeDocument> FAKE_DOCUMENT_TABLE_SCHEMA =
        StaticTableSchema.builder(FakeDocument.class)
                         .newItemSupplier(FakeDocument::new)
                         .attributes(
                             stringAttribute("documentString", FakeDocument::getDocumentString, FakeDocument::setDocumentString),
                             integerNumberAttribute("documentInteger", FakeDocument::getDocumentInteger, FakeDocument::setDocumentInteger))
                         .build();

    private static final FakeMappedItem FAKE_ITEM = FakeMappedItem.builder()
                                                                  .aPrimitiveBoolean(true)
                                                                  .aBoolean(true)
                                                                  .aString("test-string")
                                                                  .build();

    private static class FakeMappedItem {
        private boolean aPrimitiveBoolean;
        private Boolean aBoolean;
        private String aString;
        private Integer anInteger;
        private int aPrimitiveInteger;
        private Byte aByte;
        private byte aPrimitiveByte;
        private Long aLong;
        private long aPrimitiveLong;
        private Short aShort;
        private short aPrimitiveShort;
        private Double aDouble;
        private double aPrimitiveDouble;
        private Float aFloat;
        private float aPrimitiveFloat;
        private ByteBuffer aBytebuffer;
        private FakeDocument aFakeDocument;
        private Set<String> aStringSet;
        private Set<Integer> anIntegerSet;
        private Set<Byte> aByteSet;
        private Set<Long> aLongSet;
        private Set<Short> aShortSet;
        private Set<Double> aDoubleSet;
        private Set<Float> aFloatSet;
        private Set<ByteBuffer> aByteBufferSet;
        private List<Integer> anIntegerList;
        private List<List<FakeDocument>> aNestedStructure;
        private Map<String, String> aStringMap;

        FakeMappedItem() {
        }

        FakeMappedItem(boolean aPrimitiveBoolean, Boolean aBoolean, String aString, Integer anInteger,
                       int aPrimitiveInteger, Byte aByte, byte aPrimitiveByte, Long aLong, long aPrimitiveLong,
                       Short aShort, short aPrimitiveShort, Double aDouble, double aPrimitiveDouble, Float aFloat,
                       float aPrimitiveFloat, ByteBuffer aBytebuffer, FakeDocument aFakeDocument,
                       Set<String> aStringSet, Set<Integer> anIntegerSet, Set<Byte> aByteSet,
                       Set<Long> aLongSet, Set<Short> aShortSet, Set<Double> aDoubleSet, Set<Float> aFloatSet,
                       Set<ByteBuffer> aByteBufferSet, List<Integer> anIntegerList,
                       List<List<FakeDocument>> aNestedStructure, Map<String, String> aStringMap) {
            this.aPrimitiveBoolean = aPrimitiveBoolean;
            this.aBoolean = aBoolean;
            this.aString = aString;
            this.anInteger = anInteger;
            this.aPrimitiveInteger = aPrimitiveInteger;
            this.aByte = aByte;
            this.aPrimitiveByte = aPrimitiveByte;
            this.aLong = aLong;
            this.aPrimitiveLong = aPrimitiveLong;
            this.aShort = aShort;
            this.aPrimitiveShort = aPrimitiveShort;
            this.aDouble = aDouble;
            this.aPrimitiveDouble = aPrimitiveDouble;
            this.aFloat = aFloat;
            this.aPrimitiveFloat = aPrimitiveFloat;
            this.aBytebuffer = aBytebuffer;
            this.aFakeDocument = aFakeDocument;
            this.aStringSet = aStringSet;
            this.anIntegerSet = anIntegerSet;
            this.aByteSet = aByteSet;
            this.aLongSet = aLongSet;
            this.aShortSet = aShortSet;
            this.aDoubleSet = aDoubleSet;
            this.aFloatSet = aFloatSet;
            this.aByteBufferSet = aByteBufferSet;
            this.anIntegerList = anIntegerList;
            this.aNestedStructure = aNestedStructure;
            this.aStringMap = aStringMap;
        }
        
        public static Builder builder() {
            return new Builder();
        }

        boolean isAPrimitiveBoolean() {
            return aPrimitiveBoolean;
        }

        void setAPrimitiveBoolean(boolean aPrimitiveBoolean) {
            this.aPrimitiveBoolean = aPrimitiveBoolean;
        }

        Boolean getABoolean() {
            return aBoolean;
        }

        void setABoolean(Boolean aBoolean) {
            this.aBoolean = aBoolean;
        }

        String getAString() {
            return aString;
        }

        void setAString(String aString) {
            this.aString = aString;
        }

        Integer getAnInteger() {
            return anInteger;
        }

        void setAnInteger(Integer anInteger) {
            this.anInteger = anInteger;
        }

        int getAPrimitiveInteger() {
            return aPrimitiveInteger;
        }

        void setAPrimitiveInteger(int aPrimitiveInteger) {
            this.aPrimitiveInteger = aPrimitiveInteger;
        }

        Byte getAByte() {
            return aByte;
        }

        void setAByte(Byte aByte) {
            this.aByte = aByte;
        }

        byte getAPrimitiveByte() {
            return aPrimitiveByte;
        }

        void setAPrimitiveByte(byte aPrimitiveByte) {
            this.aPrimitiveByte = aPrimitiveByte;
        }

        Long getALong() {
            return aLong;
        }

        void setALong(Long aLong) {
            this.aLong = aLong;
        }

        long getAPrimitiveLong() {
            return aPrimitiveLong;
        }

        void setAPrimitiveLong(long aPrimitiveLong) {
            this.aPrimitiveLong = aPrimitiveLong;
        }

        Short getAShort() {
            return aShort;
        }

        void setAShort(Short aShort) {
            this.aShort = aShort;
        }

        short getAPrimitiveShort() {
            return aPrimitiveShort;
        }

        void setAPrimitiveShort(short aPrimitiveShort) {
            this.aPrimitiveShort = aPrimitiveShort;
        }

        Double getADouble() {
            return aDouble;
        }

        void setADouble(Double aDouble) {
            this.aDouble = aDouble;
        }

        double getAPrimitiveDouble() {
            return aPrimitiveDouble;
        }

        void setAPrimitiveDouble(double aPrimitiveDouble) {
            this.aPrimitiveDouble = aPrimitiveDouble;
        }

        Float getAFloat() {
            return aFloat;
        }

        void setAFloat(Float aFloat) {
            this.aFloat = aFloat;
        }

        float getAPrimitiveFloat() {
            return aPrimitiveFloat;
        }

        void setAPrimitiveFloat(float aPrimitiveFloat) {
            this.aPrimitiveFloat = aPrimitiveFloat;
        }

        ByteBuffer getABytebuffer() {
            return aBytebuffer;
        }

        void setABytebuffer(ByteBuffer aBytebuffer) {
            this.aBytebuffer = aBytebuffer;
        }

        FakeDocument getAFakeDocument() {
            return aFakeDocument;
        }

        void setAFakeDocument(FakeDocument aFakeDocument) {
            this.aFakeDocument = aFakeDocument;
        }

        Set<String> getAStringSet() {
            return aStringSet;
        }

        void setAStringSet(Set<String> aStringSet) {
            this.aStringSet = aStringSet;
        }

        Set<Integer> getAnIntegerSet() {
            return anIntegerSet;
        }

        void setAnIntegerSet(Set<Integer> anIntegerSet) {
            this.anIntegerSet = anIntegerSet;
        }

        Set<Byte> getAByteSet() {
            return aByteSet;
        }

        void setAByteSet(Set<Byte> aByteSet) {
            this.aByteSet = aByteSet;
        }

        Set<Long> getALongSet() {
            return aLongSet;
        }

        void setALongSet(Set<Long> aLongSet) {
            this.aLongSet = aLongSet;
        }

        Set<Short> getAShortSet() {
            return aShortSet;
        }

        void setAShortSet(Set<Short> aShortSet) {
            this.aShortSet = aShortSet;
        }

        Set<Double> getADoubleSet() {
            return aDoubleSet;
        }

        void setADoubleSet(Set<Double> aDoubleSet) {
            this.aDoubleSet = aDoubleSet;
        }

        Set<Float> getAFloatSet() {
            return aFloatSet;
        }

        void setAFloatSet(Set<Float> aFloatSet) {
            this.aFloatSet = aFloatSet;
        }

        Set<ByteBuffer> getAByteBufferSet() {
            return aByteBufferSet;
        }

        void setAByteBufferSet(Set<ByteBuffer> aByteBufferSet) {
            this.aByteBufferSet = aByteBufferSet;
        }

        List<Integer> getAnIntegerList() {
            return anIntegerList;
        }

        void setAnIntegerList(List<Integer> anIntegerList) {
            this.anIntegerList = anIntegerList;
        }

        List<List<FakeDocument>> getANestedStructure() {
            return aNestedStructure;
        }

        void setANestedStructure(List<List<FakeDocument>> aNestedStructure) {
            this.aNestedStructure = aNestedStructure;
        }

        Map<String, String> getAStringMap() {
            return aStringMap;
        }

        void setAStringMap(Map<String, String> aStringMap) {
            this.aStringMap = aStringMap;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FakeMappedItem that = (FakeMappedItem) o;
            return aPrimitiveBoolean == that.aPrimitiveBoolean &&
                   aPrimitiveInteger == that.aPrimitiveInteger &&
                   aPrimitiveByte == that.aPrimitiveByte &&
                   aPrimitiveLong == that.aPrimitiveLong &&
                   aPrimitiveShort == that.aPrimitiveShort &&
                   Double.compare(that.aPrimitiveDouble, aPrimitiveDouble) == 0 &&
                   Float.compare(that.aPrimitiveFloat, aPrimitiveFloat) == 0 &&
                   Objects.equals(aBoolean, that.aBoolean) &&
                   Objects.equals(aString, that.aString) &&
                   Objects.equals(anInteger, that.anInteger) &&
                   Objects.equals(aByte, that.aByte) &&
                   Objects.equals(aLong, that.aLong) &&
                   Objects.equals(aShort, that.aShort) &&
                   Objects.equals(aDouble, that.aDouble) &&
                   Objects.equals(aFloat, that.aFloat) &&
                   Objects.equals(aBytebuffer, that.aBytebuffer) &&
                   Objects.equals(aFakeDocument, that.aFakeDocument) &&
                   Objects.equals(aStringSet, that.aStringSet) &&
                   Objects.equals(anIntegerSet, that.anIntegerSet) &&
                   Objects.equals(aByteSet, that.aByteSet) &&
                   Objects.equals(aLongSet, that.aLongSet) &&
                   Objects.equals(aShortSet, that.aShortSet) &&
                   Objects.equals(aDoubleSet, that.aDoubleSet) &&
                   Objects.equals(aFloatSet, that.aFloatSet) &&
                   Objects.equals(aByteBufferSet, that.aByteBufferSet) &&
                   Objects.equals(anIntegerList, that.anIntegerList) &&
                   Objects.equals(aNestedStructure, that.aNestedStructure) &&
                   Objects.equals(aStringMap, that.aStringMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aPrimitiveBoolean, aBoolean, aString, anInteger, aPrimitiveInteger, aByte,
                                aPrimitiveByte, aLong, aPrimitiveLong, aShort, aPrimitiveShort, aDouble,
                                aPrimitiveDouble, aFloat, aPrimitiveFloat, aBytebuffer, aFakeDocument, aStringSet,
                                anIntegerSet, aByteSet, aLongSet, aShortSet, aDoubleSet, aFloatSet, aByteBufferSet,
                                anIntegerList, aNestedStructure, aStringMap);
        }

        private static class Builder {
            private boolean aPrimitiveBoolean;
            private Boolean aBoolean;
            private String aString;
            private Integer anInteger;
            private int aPrimitiveInteger;
            private Byte aByte;
            private byte aPrimitiveByte;
            private Long aLong;
            private long aPrimitiveLong;
            private Short aShort;
            private short aPrimitiveShort;
            private Double aDouble;
            private double aPrimitiveDouble;
            private Float aFloat;
            private float aPrimitiveFloat;
            private ByteBuffer aBytebuffer;
            private FakeDocument aFakeDocument;
            private Set<String> aStringSet;
            private Set<Integer> anIntegerSet;
            private Set<Byte> aByteSet;
            private Set<Long> aLongSet;
            private Set<Short> aShortSet;
            private Set<Double> aDoubleSet;
            private Set<Float> aFloatSet;
            private Set<ByteBuffer> aByteBufferSet;
            private List<Integer> anIntegerList;
            private List<List<FakeDocument>> aNestedStructure;
            private Map<String, String> aStringMap;

            Builder aPrimitiveBoolean(boolean aPrimitiveBoolean) {
                this.aPrimitiveBoolean = aPrimitiveBoolean;
                return this;
            }

            Builder aBoolean(Boolean aBoolean) {
                this.aBoolean = aBoolean;
                return this;
            }

            Builder aString(String aString) {
                this.aString = aString;
                return this;
            }

            Builder anInteger(Integer anInteger) {
                this.anInteger = anInteger;
                return this;
            }

            Builder aPrimitiveInteger(int aPrimitiveInteger) {
                this.aPrimitiveInteger = aPrimitiveInteger;
                return this;
            }

            Builder aByte(Byte aByte) {
                this.aByte = aByte;
                return this;
            }

            Builder aPrimitiveByte(byte aPrimitiveByte) {
                this.aPrimitiveByte = aPrimitiveByte;
                return this;
            }

            Builder aLong(Long aLong) {
                this.aLong = aLong;
                return this;
            }

            Builder aPrimitiveLong(long aPrimitiveLong) {
                this.aPrimitiveLong = aPrimitiveLong;
                return this;
            }

            Builder aShort(Short aShort) {
                this.aShort = aShort;
                return this;
            }

            Builder aPrimitiveShort(short aPrimitiveShort) {
                this.aPrimitiveShort = aPrimitiveShort;
                return this;
            }

            Builder aDouble(Double aDouble) {
                this.aDouble = aDouble;
                return this;
            }

            Builder aPrimitiveDouble(double aPrimitiveDouble) {
                this.aPrimitiveDouble = aPrimitiveDouble;
                return this;
            }

            Builder aFloat(Float aFloat) {
                this.aFloat = aFloat;
                return this;
            }

            Builder aPrimitiveFloat(float aPrimitiveFloat) {
                this.aPrimitiveFloat = aPrimitiveFloat;
                return this;
            }

            Builder aBytebuffer(ByteBuffer aBytebuffer) {
                this.aBytebuffer = aBytebuffer;
                return this;
            }

            Builder aFakeDocument(FakeDocument aFakeDocument) {
                this.aFakeDocument = aFakeDocument;
                return this;
            }

            Builder aStringSet(Set<String> aStringSet) {
                this.aStringSet = aStringSet;
                return this;
            }

            Builder anIntegerSet(Set<Integer> anIntegerSet) {
                this.anIntegerSet = anIntegerSet;
                return this;
            }

            Builder aByteSet(Set<Byte> aByteSet) {
                this.aByteSet = aByteSet;
                return this;
            }

            Builder aLongSet(Set<Long> aLongSet) {
                this.aLongSet = aLongSet;
                return this;
            }

            Builder aShortSet(Set<Short> aShortSet) {
                this.aShortSet = aShortSet;
                return this;
            }

            Builder aDoubleSet(Set<Double> aDoubleSet) {
                this.aDoubleSet = aDoubleSet;
                return this;
            }

            Builder aFloatSet(Set<Float> aFloatSet) {
                this.aFloatSet = aFloatSet;
                return this;
            }

            Builder aByteBufferSet(Set<ByteBuffer> aByteBufferSet) {
                this.aByteBufferSet = aByteBufferSet;
                return this;
            }

            Builder anIntegerList(List<Integer> anIntegerList) {
                this.anIntegerList = anIntegerList;
                return this;
            }

            Builder aNestedStructure(List<List<FakeDocument>> aNestedStructure) {
                this.aNestedStructure = aNestedStructure;
                return this;
            }

            Builder aStringMap(Map<String, String> aStringMap) {
                this.aStringMap = aStringMap;
                return this;
            }
            
            public FakeMappedItem build() {
                return new FakeMappedItem(aPrimitiveBoolean, aBoolean, aString, anInteger, aPrimitiveInteger, aByte,
                                          aPrimitiveByte, aLong, aPrimitiveLong, aShort, aPrimitiveShort, aDouble,
                                          aPrimitiveDouble, aFloat, aPrimitiveFloat, aBytebuffer, aFakeDocument,
                                          aStringSet, anIntegerSet, aByteSet, aLongSet, aShortSet, aDoubleSet,
                                          aFloatSet, aByteBufferSet, anIntegerList, aNestedStructure, aStringMap);
            }
        }
    }
    
    private static class FakeDocument {
        private String documentString;
        private Integer documentInteger;

        FakeDocument() {
        }

        private FakeDocument(String documentString, Integer documentInteger) {
            this.documentString = documentString;
            this.documentInteger = documentInteger;
        }
        
        private static FakeDocument of(String documentString, Integer documentInteger) {
            return new FakeDocument(documentString, documentInteger);
        }

        String getDocumentString() {
            return documentString;
        }

        void setDocumentString(String documentString) {
            this.documentString = documentString;
        }

        Integer getDocumentInteger() {
            return documentInteger;
        }

        void setDocumentInteger(Integer documentInteger) {
            this.documentInteger = documentInteger;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FakeDocument that = (FakeDocument) o;
            return Objects.equals(documentString, that.documentString) &&
                   Objects.equals(documentInteger, that.documentInteger);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentString, documentInteger);
        }
    }
    
    private static class FakeAbstractSubclass extends FakeAbstractSuperclass {

    }

    private static class FakeBrokenClass {
        FakeAbstractSuperclass abstractObject;

        FakeAbstractSuperclass getAbstractObject() {
            return abstractObject;
        }

        void setAbstractObject(FakeAbstractSuperclass abstractObject) {
            this.abstractObject = abstractObject;
        }
    }

    private static abstract class FakeAbstractSuperclass {
        private String aString;

        String getAString() {
            return aString;
        }

        void setAString(String aString) {
            this.aString = aString;
        }
    }

    private static final Collection<AttributeSupplier<FakeMappedItem>> ATTRIBUTES = Arrays.asList(
        boolAttribute("a_primitive_boolean", FakeMappedItem::isAPrimitiveBoolean,
             FakeMappedItem::setAPrimitiveBoolean),
        boolAttribute("a_boolean", FakeMappedItem::getABoolean,
             FakeMappedItem::setABoolean),
        stringAttribute("a_string", FakeMappedItem::getAString, FakeMappedItem::setAString));

    private StaticTableSchema<FakeMappedItem> createSimpleTableSchema() {
        return StaticTableSchema.builder(FakeMappedItem.class)
                                .newItemSupplier(FakeMappedItem::new)
                                .attributes(ATTRIBUTES)
                                .build();
    }

    private static class TestTableTag extends TableTag {
        @Override
        protected Map<String, Object> customMetadata() {
            return singletonMap(TABLE_TAG_KEY, TABLE_TAG_VALUE);
        }
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void getTableMetadata_hasCorrectFields() {
        TableMetadata tableMetadata = FakeItemWithSort.getTableSchema().tableMetadata();

        assertThat(tableMetadata.primaryPartitionKey(), is("id"));
        assertThat(tableMetadata.primarySortKey(), is(Optional.of("sort")));
    }

    @Test
    public void itemToMap_returnsCorrectMapWithMultipleAttributes() {
        Map<String, AttributeValue> attributeMap = createSimpleTableSchema().itemToMap(FAKE_ITEM, false);

        assertThat(attributeMap.size(), is(3));
        assertThat(attributeMap, hasEntry("a_boolean", ATTRIBUTE_VALUE_B));
        assertThat(attributeMap, hasEntry("a_primitive_boolean", ATTRIBUTE_VALUE_B));
        assertThat(attributeMap, hasEntry("a_string", ATTRIBUTE_VALUE_S));
    }

    @Test
    public void itemToMap_omitsNullAttributes() {
        FakeMappedItem fakeMappedItemWithNulls = FakeMappedItem.builder().aPrimitiveBoolean(true).build();
        Map<String, AttributeValue> attributeMap = createSimpleTableSchema().itemToMap(fakeMappedItemWithNulls, true);

        assertThat(attributeMap.size(), is(1));
        assertThat(attributeMap, hasEntry("a_primitive_boolean", ATTRIBUTE_VALUE_B));
    }

    @Test
    public void itemToMap_filtersAttributes() {
        Map<String, AttributeValue> attributeMap = createSimpleTableSchema()
            .itemToMap(FAKE_ITEM, asList("a_boolean", "a_string"));

        assertThat(attributeMap.size(), is(2));
        assertThat(attributeMap, hasEntry("a_boolean", ATTRIBUTE_VALUE_B));
        assertThat(attributeMap, hasEntry("a_string", ATTRIBUTE_VALUE_S));
    }

    @Test(expected = IllegalArgumentException.class)
    public void itemToMap_attributeNotFound_throwsIllegalArgumentException() {
        createSimpleTableSchema().itemToMap(FAKE_ITEM, singletonList("unknown_key"));
    }

    @Test
    public void mapToItem_returnsCorrectItemWithMultipleAttributes() {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("a_boolean", ATTRIBUTE_VALUE_B);
        attributeValueMap.put("a_primitive_boolean", ATTRIBUTE_VALUE_B);
        attributeValueMap.put("a_string", ATTRIBUTE_VALUE_S);

        FakeMappedItem fakeMappedItem =
            createSimpleTableSchema().mapToItem(Collections.unmodifiableMap(attributeValueMap));

        assertThat(fakeMappedItem, is(FAKE_ITEM));
    }

    @Test
    public void mapToItem_unknownAttributes_doNotCauseErrors() {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("unknown_attribute", ATTRIBUTE_VALUE_S);

        createSimpleTableSchema().mapToItem(Collections.unmodifiableMap(attributeValueMap));
    }

    @Test
    public void mapToItem_attributesWrongType_doesNotAttemptToWriteValue() {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("a_boolean", ATTRIBUTE_VALUE_S);
        attributeValueMap.put("a_primitive_boolean", ATTRIBUTE_VALUE_S);
        attributeValueMap.put("a_string", ATTRIBUTE_VALUE_B);

        FakeMappedItem fakeMappedItem =
            createSimpleTableSchema().mapToItem(Collections.unmodifiableMap(attributeValueMap));

        FakeMappedItem expectedFakeMappedItem = FakeMappedItem.builder().build();
        assertThat(fakeMappedItem, is(expectedFakeMappedItem));
    }

    @Test
    public void mapperCanHandleDocument() {
        FakeDocument fakeDocument = FakeDocument.of("test-123", 123);

        Map<String, AttributeValue> expectedMap = new HashMap<>();
        expectedMap.put("documentInteger", AttributeValue.builder().n("123").build());
        expectedMap.put("documentString", AttributeValue.builder().s("test-123").build());

        verifyNullableAttribute(Attributes.documentMapAttribute("value",
                                                      FakeMappedItem::getAFakeDocument,
                                                      FakeMappedItem::setAFakeDocument,
                                                      FAKE_DOCUMENT_TABLE_SCHEMA),
                                FakeMappedItem.builder().aFakeDocument(fakeDocument).build(),
                                AttributeValue.builder().m(expectedMap).build());
    }

    @Test
    public void mapperCanHandleDocumentWithNullValues() {
        verifyNullAttribute(Attributes.documentMapAttribute("value",
                                                   FakeMappedItem::getAFakeDocument,
                                                   FakeMappedItem::setAFakeDocument,
                                                   FAKE_DOCUMENT_TABLE_SCHEMA),
                            FakeMappedItem.builder().build());
    }

    @Test
    public void mapperCanHandleInteger() {
        verifyNullableAttribute(Attributes.integerNumberAttribute("value", FakeMappedItem::getAnInteger,
                                                         FakeMappedItem::setAnInteger),
                                FakeMappedItem.builder().anInteger(123).build(),
                                AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandlePrimitiveInteger() {
        verifyAttribute(Attributes.integerNumberAttribute("value", FakeMappedItem::getAPrimitiveInteger,
                                                 FakeMappedItem::setAPrimitiveInteger),
                        FakeMappedItem.builder().aPrimitiveInteger(123).build(),
                        AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandleBoolean() {
        verifyNullableAttribute(Attributes.boolAttribute("value", FakeMappedItem::getABoolean, FakeMappedItem::setABoolean),
                                FakeMappedItem.builder().aBoolean(true).build(),
                                AttributeValue.builder().bool(true).build());
    }

    @Test
    public void mapperCanHandlePrimitiveBoolean() {
        verifyAttribute(Attributes.boolAttribute("value", FakeMappedItem::isAPrimitiveBoolean,
                                        FakeMappedItem::setAPrimitiveBoolean),
                        FakeMappedItem.builder().aPrimitiveBoolean(true).build(),
                        AttributeValue.builder().bool(true).build());
    }

    @Test
    public void mapperCanHandleString() {
        verifyNullableAttribute(stringAttribute("value", FakeMappedItem::getAString, FakeMappedItem::setAString),
                                FakeMappedItem.builder().aString("onetwothree").build(),
                                AttributeValue.builder().s("onetwothree").build());
    }

    @Test
    public void mapperCanHandleLong() {
        verifyNullableAttribute(Attributes.longNumberAttribute("value", FakeMappedItem::getALong, FakeMappedItem::setALong),
                                FakeMappedItem.builder().aLong(123L).build(),
                                AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandlePrimitiveLong() {
        verifyAttribute(Attributes.longNumberAttribute("value", FakeMappedItem::getAPrimitiveLong,
                                              FakeMappedItem::setAPrimitiveLong),
                        FakeMappedItem.builder().aPrimitiveLong(123L).build(),
                        AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandleShort() {
        verifyNullableAttribute(Attributes.shortNumberAttribute("value", FakeMappedItem::getAShort, FakeMappedItem::setAShort),
                                FakeMappedItem.builder().aShort((short)123).build(),
                                AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandlePrimitiveShort() {
        verifyAttribute(Attributes.shortNumberAttribute("value", FakeMappedItem::getAPrimitiveShort,
                                               FakeMappedItem::setAPrimitiveShort),
                        FakeMappedItem.builder().aPrimitiveShort((short)123).build(),
                        AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandleByte() {
        verifyNullableAttribute(Attributes.byteNumberAttribute("value", FakeMappedItem::getAByte, FakeMappedItem::setAByte),
                                FakeMappedItem.builder().aByte((byte)123).build(),
                                AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandlePrimitiveByte() {
        verifyAttribute(Attributes.byteNumberAttribute("value", FakeMappedItem::getAPrimitiveByte,
                                              FakeMappedItem::setAPrimitiveByte),
                        FakeMappedItem.builder().aPrimitiveByte((byte)123).build(),
                        AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandleDouble() {
        verifyNullableAttribute(Attributes.doubleNumberAttribute("value", FakeMappedItem::getADouble,
                                                        FakeMappedItem::setADouble),
                                FakeMappedItem.builder().aDouble(1.23).build(),
                                AttributeValue.builder().n("1.23").build());
    }

    @Test
    public void mapperCanHandlePrimitiveDouble() {
        verifyAttribute(Attributes.doubleNumberAttribute("value", FakeMappedItem::getAPrimitiveDouble,
                                                FakeMappedItem::setAPrimitiveDouble),
                        FakeMappedItem.builder().aPrimitiveDouble(1.23).build(),
                        AttributeValue.builder().n("1.23").build());
    }

    @Test
    public void mapperCanHandleFloat() {
        verifyNullableAttribute(Attributes.floatNumberAttribute("value", FakeMappedItem::getAFloat, FakeMappedItem::setAFloat),
                                FakeMappedItem.builder().aFloat(1.23f).build(),
                                AttributeValue.builder().n("1.23").build());
    }

    @Test
    public void mapperCanHandlePrimitiveFloat() {
        verifyAttribute(Attributes.floatNumberAttribute("value", FakeMappedItem::getAPrimitiveFloat,
                                               FakeMappedItem::setAPrimitiveFloat),
                        FakeMappedItem.builder().aPrimitiveFloat(1.23f).build(),
                        AttributeValue.builder().n("1.23").build());
    }


    @Test
    public void mapperCanHandleByteBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("test".getBytes(UTF_8));
        verifyNullableAttribute(Attributes.binaryAttribute("value",
                                                 FakeMappedItem::getABytebuffer,
                                                 FakeMappedItem::setABytebuffer),
                                FakeMappedItem.builder().aBytebuffer(byteBuffer).build(),
                                AttributeValue.builder().b(SdkBytes.fromByteBuffer(byteBuffer)).build());
    }

    @Test
    public void mapperCanHandleSimpleList() {
        verifyNullableAttribute(Attributes.listAttribute("value",
                                                FakeMappedItem::getAnIntegerList,
                                                FakeMappedItem::setAnIntegerList,
                                                AttributeTypes.integerNumberType()),
                                FakeMappedItem.builder().anIntegerList(asList(1, 2, 3)).build(),
                                AttributeValue.builder().l(asList(AttributeValue.builder().n("1").build(),
                                                                  AttributeValue.builder().n("2").build(),
                                                                  AttributeValue.builder().n("3").build())).build());
    }

    @Test
    public void mapperCanHandleNestedLists() {
        FakeMappedItem fakeMappedItem =
            FakeMappedItem.builder()
                          .aNestedStructure(singletonList(singletonList(FakeDocument.of("nested", null))))
                          .build();

        Map<String, AttributeValue> documentMap = new HashMap<>();
        documentMap.put("documentString", AttributeValue.builder().s("nested").build());
        documentMap.put("documentInteger", AttributeValue.builder().nul(true).build());

        AttributeValue attributeValue =
            AttributeValue.builder()
                          .l(singletonList(AttributeValue.builder()
                                                         .l(AttributeValue.builder().m(documentMap).build())
                                                         .build()))
                          .build();

        verifyNullableAttribute(Attributes.listAttribute("value",
                                                FakeMappedItem::getANestedStructure,
                                                FakeMappedItem::setANestedStructure,
                                                AttributeTypes.listType(
                                                    AttributeTypes.documentMapType(FAKE_DOCUMENT_TABLE_SCHEMA))),
                                fakeMappedItem,
                                attributeValue);
    }

    @Test
    public void mapperCanHandleIntegerSet() {
        Set<Integer> valueSet = new HashSet<>(asList(1, 2, 3));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());

        verifyNullableAttribute(Attributes.integerSetAttribute("value",
                                                      FakeMappedItem::getAnIntegerSet,
                                                      FakeMappedItem::setAnIntegerSet),
                                FakeMappedItem.builder().anIntegerSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleStringSet() {
        Set<String> valueSet = new HashSet<>(asList("one", "two", "three"));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());

        verifyNullableAttribute(Attributes.stringSetAttribute("value", FakeMappedItem::getAStringSet,
                                                              FakeMappedItem::setAStringSet),
                                FakeMappedItem.builder().aStringSet(valueSet).build(),
                                AttributeValue.builder().ss(expectedList).build());
    }

    @Test
    public void mapperCanHandleLongSet() {
        Set<Long> valueSet = new HashSet<>(asList(1L, 2L, 3L));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());

        verifyNullableAttribute(Attributes.longSetAttribute("value", FakeMappedItem::getALongSet, FakeMappedItem::setALongSet),
                                FakeMappedItem.builder().aLongSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleShortSet() {
        Set<Short> valueSet = new HashSet<>(asList((short)1, (short)2, (short)3));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());

        verifyNullableAttribute(Attributes.shortSetAttribute("value", FakeMappedItem::getAShortSet,
                                                    FakeMappedItem::setAShortSet),
                                FakeMappedItem.builder().aShortSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleByteSet() {
        Set<Byte> valueSet = new HashSet<>(asList((byte)1, (byte)2, (byte)3));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());

        verifyNullableAttribute(Attributes.byteSetAttribute("value", FakeMappedItem::getAByteSet, FakeMappedItem::setAByteSet),
                                FakeMappedItem.builder().aByteSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleDoubleSet() {
        Set<Double> valueSet = new HashSet<>(asList(1.2, 3.4, 5.6));
        List<String> expectedList = valueSet.stream().map(Object::toString).collect(toList());

        verifyNullableAttribute(Attributes.doubleSetAttribute("value", FakeMappedItem::getADoubleSet,
                                                     FakeMappedItem::setADoubleSet),
                                FakeMappedItem.builder().aDoubleSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleFloatSet() {
        Set<Float> valueSet = new HashSet<>(asList(1.2f, 3.4f, 5.6f));
        List<String> expectedList = valueSet.stream().map(Object::toString).collect(toList());

        verifyNullableAttribute(Attributes.floatSetAttribute("value", FakeMappedItem::getAFloatSet,
                                                    FakeMappedItem::setAFloatSet),
                                FakeMappedItem.builder().aFloatSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleByteBufferSet() {
        ByteBuffer byteBuffer1 = ByteBuffer.wrap("one".getBytes(UTF_8));
        ByteBuffer byteBuffer2 = ByteBuffer.wrap("two".getBytes(UTF_8));
        ByteBuffer byteBuffer3 = ByteBuffer.wrap("three".getBytes(UTF_8));
        Set<ByteBuffer> byteBuffer = new HashSet<>(asList(byteBuffer1, byteBuffer2, byteBuffer3));
        List<SdkBytes> sdkBytes = byteBuffer.stream().map(SdkBytes::fromByteBuffer).collect(toList());

        verifyNullableAttribute(Attributes.binarySetAttribute("value",
                                                              FakeMappedItem::getAByteBufferSet,
                                                              FakeMappedItem::setAByteBufferSet),
                                FakeMappedItem.builder().aByteBufferSet(byteBuffer).build(),
                                AttributeValue.builder().bs(sdkBytes).build());
    }

    @Test
    public void mapperCanHandleGenericMap() {
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("one", "two");
        stringMap.put("three", "four");

        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("one", AttributeValue.builder().s("two").build());
        attributeValueMap.put("three", AttributeValue.builder().s("four").build());

        verifyNullableAttribute(Attributes.mapAttribute("value",
                                               FakeMappedItem::getAStringMap,
                                               FakeMappedItem::setAStringMap,
                                               AttributeTypes.stringType()),
                                FakeMappedItem.builder().aStringMap(stringMap).build(),
                                AttributeValue.builder().m(attributeValueMap).build());
    }

    @Test
    public void getAttributeValue_correctlyMapsSuperclassAttributes() {
        FakeItem fakeItem = FakeItem.builder().id("id-value").build();
        fakeItem.setSubclassAttribute("subclass-value");

        AttributeValue attributeValue = FakeItem.getTableSchema().attributeValue(fakeItem, "subclass_attribute");

        assertThat(attributeValue, is(AttributeValue.builder().s("subclass-value").build()));
    }

    @Test
    public void getAttributeValue_correctlyMapsComposedClassAttributes() {
        FakeItem fakeItem = FakeItem.builder().id("id-value")
            .composedObject(FakeItemComposedClass.builder().composedAttribute("composed-value").build())
            .build();

        AttributeValue attributeValue = FakeItem.getTableSchema().attributeValue(fakeItem, "composed_attribute");

        assertThat(attributeValue, is(AttributeValue.builder().s("composed-value").build()));
    }

    @Test
    public void mapToItem_correctlyConstructsComposedClass() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", AttributeValue.builder().s("id-value").build());
        itemMap.put("composed_attribute", AttributeValue.builder().s("composed-value").build());

        FakeItem fakeItem = FakeItem.getTableSchema().mapToItem(itemMap);

        assertThat(fakeItem,
                   is(FakeItem.builder()
                              .id("id-value")
                              .composedObject(FakeItemComposedClass.builder()
                                                                   .composedAttribute("composed-value")
                                                                   .build())
                              .build()));
    }

    @Test
    public void buildAbstractTableSchema() {
        StaticTableSchema<FakeMappedItem> tableSchema = StaticTableSchema.builder(FakeMappedItem.class)
                                                                         .attributes(stringAttribute("aString",
                                                                                     FakeMappedItem::getAString,
                                                                                     FakeMappedItem::setAString))
                                                                         .build();

        assertThat(tableSchema.itemToMap(FAKE_ITEM, false), is(singletonMap("aString", stringValue("test-string"))));

        exception.expect(UnsupportedOperationException.class);
        exception.expectMessage("abstract");
        tableSchema.mapToItem(singletonMap("aString", stringValue("test-string")));
    }

    @Test
    public void buildAbstractWithFlatten() {
        StaticTableSchema<FakeMappedItem> tableSchema =
            StaticTableSchema.builder(FakeMappedItem.class)
                             .flatten(FAKE_DOCUMENT_TABLE_SCHEMA,
                                      FakeMappedItem::getAFakeDocument,
                                      FakeMappedItem::setAFakeDocument)
                             .build();

        FakeDocument document = FakeDocument.of("test-string", null);
        FakeMappedItem item = FakeMappedItem.builder().aFakeDocument(document).build();

        assertThat(tableSchema.itemToMap(item, true),
                   is(singletonMap("documentString", AttributeValue.builder().s("test-string").build())));
    }

    @Test
    public void buildAbstractExtends() {
        StaticTableSchema<FakeAbstractSuperclass> superclassTableSchema =
            StaticTableSchema.builder(FakeAbstractSuperclass.class)
                             .attributes(Attributes.stringAttribute("aString",
                                                           FakeAbstractSuperclass::getAString,
                                                           FakeAbstractSuperclass::setAString))
                             .build();

        StaticTableSchema<FakeAbstractSubclass> subclassTableSchema =
            StaticTableSchema.builder(FakeAbstractSubclass.class)
                             .<FakeAbstractSubclass>extend(superclassTableSchema)
                             .build();

        FakeAbstractSubclass item = new FakeAbstractSubclass();
        item.setAString("test-string");

        assertThat(subclassTableSchema.itemToMap(item, true),
                   is(singletonMap("aString", AttributeValue.builder().s("test-string").build())));
    }

    @Test
    public void buildAbstractTagWith() {

        StaticTableSchema<FakeDocument> abstractTableSchema =
            StaticTableSchema
                .builder(FakeDocument.class)
                .tagWith(new TestTableTag())
                .build();

        assertThat(abstractTableSchema.tableMetadata().customMetadataObject(TABLE_TAG_KEY, String.class),
                   is(Optional.of(TABLE_TAG_VALUE)));
    }

    @Test
    public void buildConcreteTagWith() {

        StaticTableSchema<FakeDocument> concreteTableSchema =
            StaticTableSchema
                .builder(FakeDocument.class)
                .newItemSupplier(FakeDocument::new)
                .tagWith(new TestTableTag())
                .build();

        assertThat(concreteTableSchema.tableMetadata().customMetadataObject(TABLE_TAG_KEY, String.class),
                   is(Optional.of(TABLE_TAG_VALUE)));
    }

    @Test
    public void instantiateFlattenedAbstractClassShouldThrowException() {
        StaticTableSchema<FakeAbstractSuperclass> superclassTableSchema =
            StaticTableSchema.builder(FakeAbstractSuperclass.class)
                             .attributes(Attributes.stringAttribute("aString",
                                                           FakeAbstractSuperclass::getAString,
                                                           FakeAbstractSuperclass::setAString))
                             .build();

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("abstract");
        StaticTableSchema.builder(FakeBrokenClass.class)
                         .newItemSupplier(FakeBrokenClass::new)
                         .flatten(superclassTableSchema,
                                  FakeBrokenClass::getAbstractObject,
                                  FakeBrokenClass::setAbstractObject);
    }

    private void verifyAttribute(AttributeSupplier<FakeMappedItem> mappedAttribute,
                                 FakeMappedItem fakeMappedItem,
                                 AttributeValue attributeValue) {

        StaticTableSchema<FakeMappedItem> tableSchema = StaticTableSchema.builder(FakeMappedItem.class)
                                                                         .newItemSupplier(FakeMappedItem::new)
                                                                         .attributes(mappedAttribute)
                                                                         .build();
        Map<String, AttributeValue> expectedMap = singletonMap("value", attributeValue);

        Map<String, AttributeValue> resultMap = tableSchema.itemToMap(fakeMappedItem, false);
        assertThat(resultMap, is(expectedMap));

        FakeMappedItem resultItem = tableSchema.mapToItem(expectedMap);
        assertThat(resultItem, is(fakeMappedItem));
    }

    private void verifyNullAttribute(AttributeSupplier<FakeMappedItem> mappedAttribute,
                                     FakeMappedItem fakeMappedItem) {

        StaticTableSchema<FakeMappedItem> tableSchema = StaticTableSchema.builder(FakeMappedItem.class)
                                                                         .newItemSupplier(FakeMappedItem::new)
                                                                         .attributes(mappedAttribute)
                                                                         .build();
        Map<String, AttributeValue> expectedMap = singletonMap("value", nullAttributeValue());

        Map<String, AttributeValue> resultMap = tableSchema.itemToMap(fakeMappedItem, false);
        assertThat(resultMap, is(expectedMap));

        FakeMappedItem resultItem = tableSchema.mapToItem(expectedMap);
        assertThat(resultItem, is(nullValue()));
    }

    private void verifyNullableAttribute(AttributeSupplier<FakeMappedItem> mappedAttribute,
                                         FakeMappedItem fakeMappedItem,
                                         AttributeValue attributeValue) {

        verifyAttribute(mappedAttribute, fakeMappedItem, attributeValue);
        verifyNullAttribute(mappedAttribute, FakeMappedItem.builder().build());
    }
}
