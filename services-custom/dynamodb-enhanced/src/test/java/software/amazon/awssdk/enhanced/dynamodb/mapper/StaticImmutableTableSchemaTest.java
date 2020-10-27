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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.nullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.math.BigDecimal;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemComposedClass;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class StaticImmutableTableSchemaTest {
    private static final String TABLE_TAG_KEY = "table-tag-key";
    private static final String TABLE_TAG_VALUE = "table-tag-value";
    private static final AttributeValue ATTRIBUTE_VALUE_B = AttributeValue.builder().bool(true).build();
    private static final AttributeValue ATTRIBUTE_VALUE_S = AttributeValue.builder().s("test-string").build();

    private static final StaticTableSchema<FakeDocument> FAKE_DOCUMENT_TABLE_SCHEMA =
        StaticTableSchema.builder(FakeDocument.class)
                         .newItemSupplier(FakeDocument::new)
                         .addAttribute(String.class, a -> a.name("documentString")
                                                           .getter(FakeDocument::getDocumentString)
                                                           .setter(FakeDocument::setDocumentString))
                         .addAttribute(Integer.class, a -> a.name("documentInteger")
                                                            .getter(FakeDocument::getDocumentInteger)
                                                            .setter(FakeDocument::setDocumentInteger))
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
        private BigDecimal aBigDecimal;
        private SdkBytes aBinaryValue;
        private FakeDocument aFakeDocument;
        private Set<String> aStringSet;
        private Set<Integer> anIntegerSet;
        private Set<Byte> aByteSet;
        private Set<Long> aLongSet;
        private Set<Short> aShortSet;
        private Set<Double> aDoubleSet;
        private Set<Float> aFloatSet;
        private Set<SdkBytes> aBinarySet;
        private List<Integer> anIntegerList;
        private List<List<FakeDocument>> aNestedStructure;
        private Map<String, String> aStringMap;
        private Map<Integer, Double> aIntDoubleMap;
        private TestEnum testEnum;

        FakeMappedItem() {
        }

        FakeMappedItem(boolean aPrimitiveBoolean, Boolean aBoolean, String aString, Integer anInteger,
                       int aPrimitiveInteger, Byte aByte, byte aPrimitiveByte, Long aLong, long aPrimitiveLong,
                       Short aShort, short aPrimitiveShort, Double aDouble, double aPrimitiveDouble, Float aFloat,
                       float aPrimitiveFloat, BigDecimal aBigDecimal, SdkBytes aBinaryValue, FakeDocument aFakeDocument,
                       Set<String> aStringSet, Set<Integer> anIntegerSet, Set<Byte> aByteSet,
                       Set<Long> aLongSet, Set<Short> aShortSet, Set<Double> aDoubleSet, Set<Float> aFloatSet,
                       Set<SdkBytes> aBinarySet, List<Integer> anIntegerList,
                       List<List<FakeDocument>> aNestedStructure, Map<String, String> aStringMap,
                       Map<Integer, Double> aIntDoubleMap, TestEnum testEnum) {
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
            this.aBigDecimal = aBigDecimal;
            this.aBinaryValue = aBinaryValue;
            this.aFakeDocument = aFakeDocument;
            this.aStringSet = aStringSet;
            this.anIntegerSet = anIntegerSet;
            this.aByteSet = aByteSet;
            this.aLongSet = aLongSet;
            this.aShortSet = aShortSet;
            this.aDoubleSet = aDoubleSet;
            this.aFloatSet = aFloatSet;
            this.aBinarySet = aBinarySet;
            this.anIntegerList = anIntegerList;
            this.aNestedStructure = aNestedStructure;
            this.aStringMap = aStringMap;
            this.aIntDoubleMap = aIntDoubleMap;
            this.testEnum = testEnum;
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

        BigDecimal aBigDecimal() {
            return aBigDecimal;
        }

        void setABigDecimal(BigDecimal aBigDecimal) {
            this.aBigDecimal = aBigDecimal;
        }

        float getAPrimitiveFloat() {
            return aPrimitiveFloat;
        }

        void setAPrimitiveFloat(float aPrimitiveFloat) {
            this.aPrimitiveFloat = aPrimitiveFloat;
        }

        SdkBytes getABinaryValue() {
            return aBinaryValue;
        }

        void setABinaryValue(SdkBytes aBinaryValue) {
            this.aBinaryValue = aBinaryValue;
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

        Set<SdkBytes> getABinarySet() {
            return aBinarySet;
        }

        void setABinarySet(Set<SdkBytes> aBinarySet) {
            this.aBinarySet = aBinarySet;
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

        Map<Integer, Double> getAIntDoubleMap() {
            return aIntDoubleMap;
        }

        void setAIntDoubleMap(Map<Integer, Double> aIntDoubleMap) {
            this.aIntDoubleMap = aIntDoubleMap;
        }

        TestEnum getTestEnum() {
            return testEnum;
        }

        void setTestEnum(TestEnum testEnum) {
            this.testEnum = testEnum;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
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
                   Objects.equals(aBinaryValue, that.aBinaryValue) &&
                   Objects.equals(aFakeDocument, that.aFakeDocument) &&
                   Objects.equals(aStringSet, that.aStringSet) &&
                   Objects.equals(anIntegerSet, that.anIntegerSet) &&
                   Objects.equals(aByteSet, that.aByteSet) &&
                   Objects.equals(aLongSet, that.aLongSet) &&
                   Objects.equals(aShortSet, that.aShortSet) &&
                   Objects.equals(aDoubleSet, that.aDoubleSet) &&
                   Objects.equals(aFloatSet, that.aFloatSet) &&
                   Objects.equals(aBinarySet, that.aBinarySet) &&
                   Objects.equals(anIntegerList, that.anIntegerList) &&
                   Objects.equals(aNestedStructure, that.aNestedStructure) &&
                   Objects.equals(aStringMap, that.aStringMap) &&
                   Objects.equals(aIntDoubleMap, that.aIntDoubleMap) &&
                   Objects.equals(testEnum, that.testEnum);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aPrimitiveBoolean, aBoolean, aString, anInteger, aPrimitiveInteger, aByte,
                                aPrimitiveByte, aLong, aPrimitiveLong, aShort, aPrimitiveShort, aDouble,
                                aPrimitiveDouble, aFloat, aPrimitiveFloat, aBinaryValue, aFakeDocument, aStringSet,
                                anIntegerSet, aByteSet, aLongSet, aShortSet, aDoubleSet, aFloatSet, aBinarySet,
                                anIntegerList, aNestedStructure, aStringMap, aIntDoubleMap, testEnum);
        }

        public enum TestEnum {
            ONE,
            TWO,
            THREE;
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
            private BigDecimal aBigDecimal;
            private SdkBytes aBinaryValue;
            private FakeDocument aFakeDocument;
            private Set<String> aStringSet;
            private Set<Integer> anIntegerSet;
            private Set<Byte> aByteSet;
            private Set<Long> aLongSet;
            private Set<Short> aShortSet;
            private Set<Double> aDoubleSet;
            private Set<Float> aFloatSet;
            private Set<SdkBytes> aBinarySet;
            private List<Integer> anIntegerList;
            private List<List<FakeDocument>> aNestedStructure;
            private Map<String, String> aStringMap;
            private Map<Integer, Double> aIntDoubleMap;
            private TestEnum testEnum;

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

            Builder aBigDecimal(BigDecimal aBigDecimal) {
                this.aBigDecimal = aBigDecimal;
                return this;
            }

            Builder aBinaryValue(SdkBytes aBinaryValue) {
                this.aBinaryValue = aBinaryValue;
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

            Builder aBinarySet(Set<SdkBytes> aBinarySet) {
                this.aBinarySet = aBinarySet;
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

            Builder aIntDoubleMap(Map<Integer, Double> aIntDoubleMap) {
                this.aIntDoubleMap = aIntDoubleMap;
                return this;
            }

            Builder testEnum(TestEnum testEnum) {
                this.testEnum = testEnum;
                return this;
            }

            public StaticImmutableTableSchemaTest.FakeMappedItem build() {
                return new StaticImmutableTableSchemaTest.FakeMappedItem(aPrimitiveBoolean, aBoolean, aString, anInteger, aPrimitiveInteger, aByte,
                                                                         aPrimitiveByte, aLong, aPrimitiveLong, aShort, aPrimitiveShort, aDouble,
                                                                         aPrimitiveDouble, aFloat, aPrimitiveFloat, aBigDecimal, aBinaryValue, aFakeDocument,
                                                                         aStringSet, anIntegerSet, aByteSet, aLongSet, aShortSet, aDoubleSet,
                                                                         aFloatSet, aBinarySet, anIntegerList, aNestedStructure, aStringMap, aIntDoubleMap,
                                                                         testEnum);
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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
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
    
    private static final Collection<StaticAttribute<FakeMappedItem, ?>> ATTRIBUTES = Arrays.asList(
        StaticAttribute.builder(FakeMappedItem.class, Boolean.class)
                       .name("a_primitive_boolean")
                       .getter(FakeMappedItem::isAPrimitiveBoolean)
                       .setter(FakeMappedItem::setAPrimitiveBoolean)
                       .build(),
        StaticAttribute.builder(FakeMappedItem.class, Boolean.class)
                       .name("a_boolean")
                       .getter(FakeMappedItem::getABoolean)
                       .setter(FakeMappedItem::setABoolean)
                       .build(),
        StaticAttribute.builder(FakeMappedItem.class, String.class)
                       .name("a_string")
                       .getter(FakeMappedItem::getAString)
                       .setter(FakeMappedItem::setAString)
                       .build()
    );

    private StaticTableSchema<FakeMappedItem> createSimpleTableSchema() {
        return StaticTableSchema.builder(FakeMappedItem.class)
                                .newItemSupplier(FakeMappedItem::new)
                                .attributes(ATTRIBUTES)
                                .build();
    }

    private static class TestStaticTableTag implements StaticTableTag {
        @Override
        public Consumer<StaticTableMetadata.Builder> modifyMetadata() {
            return metadata -> metadata.addCustomMetadataObject(TABLE_TAG_KEY, TABLE_TAG_VALUE);
        }
    }

    @Mock
    private AttributeConverterProvider provider1;

    @Mock
    private AttributeConverterProvider provider2;

    @Mock
    private AttributeConverter<String> attributeConverter1;

    @Mock
    private AttributeConverter<String> attributeConverter2;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void itemType_returnsCorrectClass() {
        assertThat(FakeItem.getTableSchema().itemType(), is(equalTo(EnhancedType.of(FakeItem.class))));
    }

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

    @Test(expected = IllegalArgumentException.class)
    public void mapToItem_attributesWrongType_throwsException() {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("a_boolean", ATTRIBUTE_VALUE_S);
        attributeValueMap.put("a_primitive_boolean", ATTRIBUTE_VALUE_S);
        attributeValueMap.put("a_string", ATTRIBUTE_VALUE_B);

        createSimpleTableSchema().mapToItem(Collections.unmodifiableMap(attributeValueMap));
    }

    @Test
    public void mapperCanHandleEnum() {
        verifyNullableAttribute(EnhancedType.of(FakeMappedItem.TestEnum.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getTestEnum)
                                      .setter(FakeMappedItem::setTestEnum),
                                FakeMappedItem.builder().testEnum(FakeMappedItem.TestEnum.ONE).build(),
                                AttributeValue.builder().s("ONE").build());
    }

    @Test
    public void mapperCanHandleDocument() {
        FakeDocument fakeDocument = FakeDocument.of("test-123", 123);

        Map<String, AttributeValue> expectedMap = new HashMap<>();
        expectedMap.put("documentInteger", AttributeValue.builder().n("123").build());
        expectedMap.put("documentString", AttributeValue.builder().s("test-123").build());
        
        verifyNullableAttribute(EnhancedType.documentOf(FakeDocument.class, FAKE_DOCUMENT_TABLE_SCHEMA),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAFakeDocument)
                                      .setter(FakeMappedItem::setAFakeDocument),
                                FakeMappedItem.builder().aFakeDocument(fakeDocument).build(),
                                AttributeValue.builder().m(expectedMap).build());
    }

    @Test
    public void mapperCanHandleDocumentWithNullValues() {
        verifyNullAttribute(EnhancedType.documentOf(FakeDocument.class, FAKE_DOCUMENT_TABLE_SCHEMA),
                            a -> a.name("value")
                                  .getter(FakeMappedItem::getAFakeDocument)
                                  .setter(FakeMappedItem::setAFakeDocument),
                            FakeMappedItem.builder().build());
    }

    @Test
    public void mapperCanHandleInteger() {
        verifyNullableAttribute(EnhancedType.of(Integer.class), a -> a.name("value")
                                                                   .getter(FakeMappedItem::getAnInteger)
                                                                   .setter(FakeMappedItem::setAnInteger),
                                FakeMappedItem.builder().anInteger(123).build(),
                                AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandlePrimitiveInteger() {
        verifyAttribute(EnhancedType.of(int.class),
                        a -> a.name("value")
                              .getter(FakeMappedItem::getAPrimitiveInteger)
                              .setter(FakeMappedItem::setAPrimitiveInteger),
                        FakeMappedItem.builder().aPrimitiveInteger(123).build(),
                        AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandleBoolean() {
        verifyNullableAttribute(EnhancedType.of(Boolean.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getABoolean)
                                      .setter(FakeMappedItem::setABoolean),
                                FakeMappedItem.builder().aBoolean(true).build(),
                                AttributeValue.builder().bool(true).build());
    }

    @Test
    public void mapperCanHandlePrimitiveBoolean() {
        verifyAttribute(EnhancedType.of(boolean.class),
                        a -> a.name("value")
                              .getter(FakeMappedItem::isAPrimitiveBoolean)
                              .setter(FakeMappedItem::setAPrimitiveBoolean),
                        FakeMappedItem.builder().aPrimitiveBoolean(true).build(),
                        AttributeValue.builder().bool(true).build());
    }

    @Test
    public void mapperCanHandleString() {
        verifyNullableAttribute(EnhancedType.of(String.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAString)
                                      .setter(FakeMappedItem::setAString),
                                FakeMappedItem.builder().aString("onetwothree").build(),
                                AttributeValue.builder().s("onetwothree").build());
    }

    @Test
    public void mapperCanHandleLong() {
        verifyNullableAttribute(EnhancedType.of(Long.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getALong)
                                      .setter(FakeMappedItem::setALong),
                                FakeMappedItem.builder().aLong(123L).build(),
                                AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandlePrimitiveLong() {
        verifyAttribute(EnhancedType.of(long.class),
                        a -> a.name("value")
                              .getter(FakeMappedItem::getAPrimitiveLong)
                              .setter(FakeMappedItem::setAPrimitiveLong),
                        FakeMappedItem.builder().aPrimitiveLong(123L).build(),
                        AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandleShort() {
        verifyNullableAttribute(EnhancedType.of(Short.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAShort)
                                      .setter(FakeMappedItem::setAShort),
                                FakeMappedItem.builder().aShort((short)123).build(),
                                AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandlePrimitiveShort() {
        verifyAttribute(EnhancedType.of(short.class),
                        a -> a.name("value")
                              .getter(FakeMappedItem::getAPrimitiveShort)
                              .setter(FakeMappedItem::setAPrimitiveShort),
                        FakeMappedItem.builder().aPrimitiveShort((short)123).build(),
                        AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandleByte() {
        verifyNullableAttribute(EnhancedType.of(Byte.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAByte)
                                      .setter(FakeMappedItem::setAByte),
                                FakeMappedItem.builder().aByte((byte)123).build(),
                                AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandlePrimitiveByte() {
        verifyAttribute(EnhancedType.of(byte.class),
                        a -> a.name("value")
                              .getter(FakeMappedItem::getAPrimitiveByte)
                              .setter(FakeMappedItem::setAPrimitiveByte),
                        FakeMappedItem.builder().aPrimitiveByte((byte)123).build(),
                        AttributeValue.builder().n("123").build());
    }

    @Test
    public void mapperCanHandleDouble() {
        verifyNullableAttribute(EnhancedType.of(Double.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getADouble)
                                      .setter(FakeMappedItem::setADouble),
                                FakeMappedItem.builder().aDouble(1.23).build(),
                                AttributeValue.builder().n("1.23").build());
    }

    @Test
    public void mapperCanHandlePrimitiveDouble() {
        verifyAttribute(EnhancedType.of(double.class),
                        a -> a.name("value")
                              .getter(FakeMappedItem::getAPrimitiveDouble)
                              .setter(FakeMappedItem::setAPrimitiveDouble),
                        FakeMappedItem.builder().aPrimitiveDouble(1.23).build(),
                        AttributeValue.builder().n("1.23").build());
    }

    @Test
    public void mapperCanHandleFloat() {
        verifyNullableAttribute(EnhancedType.of(Float.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAFloat)
                                      .setter(FakeMappedItem::setAFloat),
                                FakeMappedItem.builder().aFloat(1.23f).build(),
                                AttributeValue.builder().n("1.23").build());
    }

    @Test
    public void mapperCanHandlePrimitiveFloat() {
        verifyAttribute(EnhancedType.of(float.class),
                        a -> a.name("value")
                              .getter(FakeMappedItem::getAPrimitiveFloat)
                              .setter(FakeMappedItem::setAPrimitiveFloat),
                        FakeMappedItem.builder().aPrimitiveFloat(1.23f).build(),
                        AttributeValue.builder().n("1.23").build());
    }


    @Test
    public void mapperCanHandleBinary() {
        SdkBytes sdkBytes = SdkBytes.fromString("test", UTF_8);
        verifyNullableAttribute(EnhancedType.of(SdkBytes.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getABinaryValue)
                                      .setter(FakeMappedItem::setABinaryValue),
                                FakeMappedItem.builder().aBinaryValue(sdkBytes).build(),
                                AttributeValue.builder().b(sdkBytes).build());
    }

    @Test
    public void mapperCanHandleSimpleList() {
        verifyNullableAttribute(EnhancedType.listOf(Integer.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAnIntegerList)
                                      .setter(FakeMappedItem::setAnIntegerList),
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

        verifyNullableAttribute(
            EnhancedType.listOf(EnhancedType.listOf(EnhancedType.documentOf(FakeDocument.class, FAKE_DOCUMENT_TABLE_SCHEMA))),
            a -> a.name("value")
                  .getter(FakeMappedItem::getANestedStructure)
                  .setter(FakeMappedItem::setANestedStructure),
            fakeMappedItem,
            attributeValue);
    }

    @Test
    public void mapperCanHandleIntegerSet() {
        Set<Integer> valueSet = new HashSet<>(asList(1, 2, 3));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());
        
        verifyNullableAttribute(EnhancedType.setOf(Integer.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAnIntegerSet)
                                      .setter(FakeMappedItem::setAnIntegerSet),
                                FakeMappedItem.builder().anIntegerSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleStringSet() {
        Set<String> valueSet = new HashSet<>(asList("one", "two", "three"));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());
        
        verifyNullableAttribute(EnhancedType.setOf(String.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAStringSet)
                                      .setter(FakeMappedItem::setAStringSet),
                                FakeMappedItem.builder().aStringSet(valueSet).build(),
                                AttributeValue.builder().ss(expectedList).build());
    }

    @Test
    public void mapperCanHandleLongSet() {
        Set<Long> valueSet = new HashSet<>(asList(1L, 2L, 3L));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());
        
        verifyNullableAttribute(EnhancedType.setOf(Long.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getALongSet)
                                      .setter(FakeMappedItem::setALongSet),
                                FakeMappedItem.builder().aLongSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleShortSet() {
        Set<Short> valueSet = new HashSet<>(asList((short) 1, (short) 2, (short) 3));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());
        
        verifyNullableAttribute(EnhancedType.setOf(Short.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAShortSet)
                                      .setter(FakeMappedItem::setAShortSet),
                                FakeMappedItem.builder().aShortSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleByteSet() {
        Set<Byte> valueSet = new HashSet<>(asList((byte) 1, (byte) 2, (byte) 3));
        List<String> expectedList = valueSet.stream().map(Objects::toString).collect(toList());
        
        verifyNullableAttribute(EnhancedType.setOf(Byte.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAByteSet)
                                      .setter(FakeMappedItem::setAByteSet),
                                FakeMappedItem.builder().aByteSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleDoubleSet() {
        Set<Double> valueSet = new HashSet<>(asList(1.2, 3.4, 5.6));
        List<String> expectedList = valueSet.stream().map(Object::toString).collect(toList());
        
        verifyNullableAttribute(EnhancedType.setOf(Double.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getADoubleSet)
                                      .setter(FakeMappedItem::setADoubleSet),
                                FakeMappedItem.builder().aDoubleSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleFloatSet() {
        Set<Float> valueSet = new HashSet<>(asList(1.2f, 3.4f, 5.6f));
        List<String> expectedList = valueSet.stream().map(Object::toString).collect(toList());
        
        verifyNullableAttribute(EnhancedType.setOf(Float.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAFloatSet)
                                      .setter(FakeMappedItem::setAFloatSet),
                                FakeMappedItem.builder().aFloatSet(valueSet).build(),
                                AttributeValue.builder().ns(expectedList).build());
    }

    @Test
    public void mapperCanHandleGenericMap() {
        Map<String, String> stringMap = new ConcurrentHashMap<>();
        stringMap.put("one", "two");
        stringMap.put("three", "four");

        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("one", AttributeValue.builder().s("two").build());
        attributeValueMap.put("three", AttributeValue.builder().s("four").build());
        
        verifyNullableAttribute(EnhancedType.mapOf(String.class, String.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAStringMap)
                                      .setter(FakeMappedItem::setAStringMap),
                                FakeMappedItem.builder().aStringMap(stringMap).build(),
                                AttributeValue.builder().m(attributeValueMap).build());
    }

    @Test
    public void mapperCanHandleIntDoubleMap() {
        Map<Integer, Double> intDoubleMap = new ConcurrentHashMap<>();
        intDoubleMap.put(1, 1.0);
        intDoubleMap.put(2, 3.0);

        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("1", AttributeValue.builder().n("1.0").build());
        attributeValueMap.put("2", AttributeValue.builder().n("3.0").build());
        
        verifyNullableAttribute(EnhancedType.mapOf(Integer.class, Double.class),
                                a -> a.name("value")
                                      .getter(FakeMappedItem::getAIntDoubleMap)
                                      .setter(FakeMappedItem::setAIntDoubleMap),
                                FakeMappedItem.builder().aIntDoubleMap(intDoubleMap).build(),
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
        StaticTableSchema<FakeMappedItem> tableSchema =
            StaticTableSchema.builder(FakeMappedItem.class)
                             .addAttribute(String.class, a -> a.name("aString")
                                                               .getter(FakeMappedItem::getAString)
                                                               .setter(FakeMappedItem::setAString))
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
                             .addAttribute(String.class, a -> a.name("aString")
                                                               .getter(FakeAbstractSuperclass::getAString)
                                                               .setter(FakeAbstractSuperclass::setAString))
                             .build();

        StaticTableSchema<FakeAbstractSubclass> subclassTableSchema =
            StaticTableSchema.builder(FakeAbstractSubclass.class)
                             .extend(superclassTableSchema)
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
                .tags(new TestStaticTableTag())
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
                .tags(new TestStaticTableTag())
                .build();

        assertThat(concreteTableSchema.tableMetadata().customMetadataObject(TABLE_TAG_KEY, String.class),
                   is(Optional.of(TABLE_TAG_VALUE)));
    }

    @Test
    public void instantiateFlattenedAbstractClassShouldThrowException() {
        StaticTableSchema<FakeAbstractSuperclass> superclassTableSchema =
            StaticTableSchema.builder(FakeAbstractSuperclass.class)
                             .addAttribute(String.class, a -> a.name("aString")
                                                               .getter(FakeAbstractSuperclass::getAString)
                                                               .setter(FakeAbstractSuperclass::setAString))
                             .build();

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("abstract");
        StaticTableSchema.builder(FakeBrokenClass.class)
                         .newItemSupplier(FakeBrokenClass::new)
                         .flatten(superclassTableSchema,
                                  FakeBrokenClass::getAbstractObject,
                                  FakeBrokenClass::setAbstractObject);
    }

    @Test
    public void addSingleAttributeConverterProvider() {
        when(provider1.converterFor(EnhancedType.of(String.class))).thenReturn(attributeConverter1);

        StaticTableSchema<FakeMappedItem> tableSchema =
            StaticTableSchema.builder(FakeMappedItem.class)
                             .newItemSupplier(FakeMappedItem::new)
                             .addAttribute(String.class, a -> a.name("aString")
                                                               .getter(FakeMappedItem::getAString)
                                                               .setter(FakeMappedItem::setAString))
                             .attributeConverterProviders(provider1)
                             .build();

        assertThat(tableSchema.attributeConverterProvider(), is(provider1));
    }

    @Test
    public void usesCustomAttributeConverterProvider() {
        String originalString = "test-string";
        String expectedString = "test-string-custom";

        when(provider1.converterFor(EnhancedType.of(String.class))).thenReturn(attributeConverter1);
        when(attributeConverter1.transformFrom(any())).thenReturn(AttributeValue.builder().s(expectedString).build());

        StaticTableSchema<FakeMappedItem> tableSchema =
            StaticTableSchema.builder(FakeMappedItem.class)
                             .newItemSupplier(FakeMappedItem::new)
                             .addAttribute(String.class, a -> a.name("aString")
                                                               .getter(FakeMappedItem::getAString)
                                                               .setter(FakeMappedItem::setAString))
                             .attributeConverterProviders(provider1)
                             .build();

        Map<String, AttributeValue> resultMap =
                tableSchema.itemToMap(FakeMappedItem.builder().aString(originalString).build(), false);
        assertThat(resultMap.get("aString").s(), is(expectedString));
    }

    @Test
    public void usesCustomAttributeConverterProviders() {
        String originalString = "test-string";
        String expectedString = "test-string-custom";

        when(provider2.converterFor(EnhancedType.of(String.class))).thenReturn(attributeConverter2);
        when(attributeConverter2.transformFrom(any())).thenReturn(AttributeValue.builder().s(expectedString).build());

        StaticTableSchema<FakeMappedItem> tableSchema =
            StaticTableSchema.builder(FakeMappedItem.class)
                             .newItemSupplier(FakeMappedItem::new)
                             .addAttribute(String.class, a -> a.name("aString")
                                                               .getter(FakeMappedItem::getAString)
                                                               .setter(FakeMappedItem::setAString))
                             .attributeConverterProviders(provider1, provider2)
                             .build();

        Map<String, AttributeValue> resultMap =
                tableSchema.itemToMap(FakeMappedItem.builder().aString(originalString).build(), false);
        assertThat(resultMap.get("aString").s(), is(expectedString));
    }

    @Test
    public void noConverterProvider_throwsException_whenMissingAttributeConverters() {
        exception.expect(NullPointerException.class);

        StaticTableSchema<FakeMappedItem> tableSchema =
                StaticTableSchema.builder(FakeMappedItem.class)
                        .newItemSupplier(FakeMappedItem::new)
                        .addAttribute(String.class, a -> a.name("aString")
                                .getter(FakeMappedItem::getAString)
                                .setter(FakeMappedItem::setAString))
                        .attributeConverterProviders(Collections.emptyList())
                        .build();
    }

    @Test
    public void noConverterProvider_handlesCorrectly_whenAttributeConvertersAreSupplied() {
        String originalString = "test-string";
        String expectedString = "test-string-custom";

        when(attributeConverter1.transformFrom(any())).thenReturn(AttributeValue.builder().s(expectedString).build());

        StaticTableSchema<FakeMappedItem> tableSchema =
                StaticTableSchema.builder(FakeMappedItem.class)
                        .newItemSupplier(FakeMappedItem::new)
                        .addAttribute(String.class, a -> a.name("aString")
                                .getter(FakeMappedItem::getAString)
                                .setter(FakeMappedItem::setAString)
                                .attributeConverter(attributeConverter1))
                        .attributeConverterProviders(Collections.emptyList())
                        .build();

        Map<String, AttributeValue> resultMap = tableSchema.itemToMap(FakeMappedItem.builder().aString(originalString).build(),
                                                                      false);
        assertThat(resultMap.get("aString").s(), is(expectedString));
    }

    private <R> void verifyAttribute(EnhancedType<R> attributeType,
                                     Consumer<StaticAttribute.Builder<FakeMappedItem, R>> staticAttribute,
                                     FakeMappedItem fakeMappedItem,
                                     AttributeValue attributeValue) {

        StaticTableSchema<FakeMappedItem> tableSchema = StaticTableSchema.builder(FakeMappedItem.class)
                                                                         .newItemSupplier(FakeMappedItem::new)
                                                                         .addAttribute(attributeType, staticAttribute)
                                                                         .build();
        Map<String, AttributeValue> expectedMap = singletonMap("value", attributeValue);

        Map<String, AttributeValue> resultMap = tableSchema.itemToMap(fakeMappedItem, false);
        assertThat(resultMap, is(expectedMap));

        FakeMappedItem resultItem = tableSchema.mapToItem(expectedMap);
        assertThat(resultItem, is(fakeMappedItem));
    }

    private <R> void verifyNullAttribute(EnhancedType<R> attributeType,
                                         Consumer<StaticAttribute.Builder<FakeMappedItem, R>> staticAttribute,
                                         FakeMappedItem fakeMappedItem) {

        StaticTableSchema<FakeMappedItem> tableSchema = StaticTableSchema.builder(FakeMappedItem.class)
                                                                         .newItemSupplier(FakeMappedItem::new)
                                                                         .addAttribute(attributeType, staticAttribute)
                                                                         .build();
        Map<String, AttributeValue> expectedMap = singletonMap("value", nullAttributeValue());

        Map<String, AttributeValue> resultMap = tableSchema.itemToMap(fakeMappedItem, false);
        assertThat(resultMap, is(expectedMap));

        FakeMappedItem resultItem = tableSchema.mapToItem(expectedMap);
        assertThat(resultItem, is(nullValue()));
    }

    private <R> void verifyNullableAttribute(EnhancedType<R> attributeType,
                                             Consumer<StaticAttribute.Builder<FakeMappedItem, R>> staticAttribute,
                                             FakeMappedItem fakeMappedItem,
                                             AttributeValue attributeValue) {

        verifyAttribute(attributeType, staticAttribute, fakeMappedItem, attributeValue);
        verifyNullAttribute(attributeType, staticAttribute, FakeMappedItem.builder().build());
    }
}

