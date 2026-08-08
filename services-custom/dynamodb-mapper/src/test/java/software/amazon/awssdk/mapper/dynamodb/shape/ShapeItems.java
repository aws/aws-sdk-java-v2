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
package software.amazon.awssdk.mapper.dynamodb.shape;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBAttribute;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBDocument;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBFlattened;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBNativeBoolean;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBRangeKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConvertedEnum;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBVersionAttribute;

// Mapper-annotated test POJOs shared by ShapeRequestTest and ShapeResponseTest.
final class ShapeItems {

    private ShapeItems() {
    }

    // Hash key plus one string attribute; the grammar workhorse.
    @DynamoDBTable(tableName = "M_String")
    public static class StringItem {
        private String id;
        private String value;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBAttribute
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    // Hash + range key for composite-key and range-condition cases.
    @DynamoDBTable(tableName = "M_Range")
    public static class RangeItem {
        private String id;
        private String range;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBRangeKey
        public String getRange() {
            return range;
        }

        public void setRange(String range) {
            this.range = range;
        }
    }

    // Version-attributed, for the auto-generated version guard.
    @DynamoDBTable(tableName = "M_Versioned")
    public static class VersionedItem {
        private String id;
        private Long version;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBVersionAttribute
        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }
    }

    // Nested @DynamoDBDocument rendered as an M.
    @DynamoDBDocument
    public static class Address {
        private String city;
        private Long zip;

        @DynamoDBAttribute
        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        @DynamoDBAttribute
        public Long getZip() {
            return zip;
        }

        public void setZip(Long zip) {
            this.zip = zip;
        }
    }

    public enum Color {
        RED, GREEN, BLUE
    }

    // One field per value encoding; a case sets one field and saves with PUT so each fixture isolates a single encoding.
    @DynamoDBTable(tableName = "M_AllTypes")
    public static class AllTypesItem {
        private String id;
        private Long number;
        private Double doubleValue;
        private Boolean numericBool;
        private Boolean nativeBool;
        private Set<String> stringSet;
        private Set<Long> numberSet;
        private List<String> list;
        private Map<String, String> map;
        private Address document;
        private Color enumValue;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public AllTypesItem withId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDBAttribute
        public Long getNumber() {
            return number;
        }

        public void setNumber(Long number) {
            this.number = number;
        }

        public AllTypesItem withNumber(Long number) {
            this.number = number;
            return this;
        }

        @DynamoDBAttribute(attributeName = "double")
        public Double getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(Double doubleValue) {
            this.doubleValue = doubleValue;
        }

        public AllTypesItem withDouble(Double doubleValue) {
            this.doubleValue = doubleValue;
            return this;
        }

        // Plain Boolean, encoded as N 0/1.
        @DynamoDBAttribute
        public Boolean getNumericBool() {
            return numericBool;
        }

        public void setNumericBool(Boolean numericBool) {
            this.numericBool = numericBool;
        }

        public AllTypesItem withNumericBool(Boolean numericBool) {
            this.numericBool = numericBool;
            return this;
        }

        // Encoded as a native BOOL.
        @DynamoDBNativeBoolean
        @DynamoDBAttribute
        public Boolean getNativeBool() {
            return nativeBool;
        }

        public void setNativeBool(Boolean nativeBool) {
            this.nativeBool = nativeBool;
        }

        public AllTypesItem withNativeBool(Boolean nativeBool) {
            this.nativeBool = nativeBool;
            return this;
        }

        @DynamoDBAttribute
        public Set<String> getStringSet() {
            return stringSet;
        }

        public void setStringSet(Set<String> stringSet) {
            this.stringSet = stringSet;
        }

        public AllTypesItem withStringSet(Set<String> stringSet) {
            this.stringSet = stringSet;
            return this;
        }

        @DynamoDBAttribute
        public Set<Long> getNumberSet() {
            return numberSet;
        }

        public void setNumberSet(Set<Long> numberSet) {
            this.numberSet = numberSet;
        }

        public AllTypesItem withNumberSet(Set<Long> numberSet) {
            this.numberSet = numberSet;
            return this;
        }

        @DynamoDBAttribute
        public List<String> getList() {
            return list;
        }

        public void setList(List<String> list) {
            this.list = list;
        }

        public AllTypesItem withList(List<String> list) {
            this.list = list;
            return this;
        }

        @DynamoDBAttribute
        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }

        public AllTypesItem withMap(Map<String, String> map) {
            this.map = map;
            return this;
        }

        @DynamoDBAttribute
        public Address getDocument() {
            return document;
        }

        public void setDocument(Address document) {
            this.document = document;
        }

        public AllTypesItem withDocument(Address document) {
            this.document = document;
            return this;
        }

        @DynamoDBTypeConvertedEnum
        @DynamoDBAttribute(attributeName = "enum")
        public Color getEnumValue() {
            return enumValue;
        }

        public void setEnumValue(Color enumValue) {
            this.enumValue = enumValue;
        }

        public AllTypesItem withEnumValue(Color enumValue) {
            this.enumValue = enumValue;
            return this;
        }
    }

    // Default Date maps to an ISO-8601 S; the epoch and pattern variants have their own annotations.
    @DynamoDBTable(tableName = "M_Dated")
    public static class DatedItem {
        private String id;
        private Date createdAt;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBAttribute
        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }

    // A nested bean whose single field is flattened into the parent item as a sibling attribute.
    @DynamoDBDocument
    public static class Name {
        private String first;

        @DynamoDBAttribute
        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }
    }

    @DynamoDBTable(tableName = "M_Flattened")
    public static class FlattenedItem {
        private String id;
        private Name name;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBFlattened(attributes = @DynamoDBAttribute(mappedBy = "first", attributeName = "firstName"))
        public Name getName() {
            return name;
        }

        public void setName(Name name) {
            this.name = name;
        }
    }

    // Hash key and table declared on the base; the subclass inherits both and only adds one attribute.
    @DynamoDBTable(tableName = "M_Inherited")
    public static class BaseKeyed {
        private String id;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class InheritedItem extends BaseKeyed {
        private String label;

        @DynamoDBAttribute
        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
