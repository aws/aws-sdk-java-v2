/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A simpler, and more user-friendly version of the generated {@link AttributeValue}.
 *
 * This is a union type of the types exposed by DynamoDB, exactly as they're exposed by DynamoDB.
 *
 * An instance of {@link ItemAttributeValue} represents exactly one DynamoDB type, like String (s), Number (n) or Bytes (b). This
 * type can be determined with the {@link #type()} method or the {@code is*} methods like {@link #isString()} or
 * {@link #isNumber()}. Once the type is known, the value can be extracted with {@code as*} methods like {@link #asString()}
 * or {@link #asNumber()}.
 *
 * When converting an {@link ItemAttributeValue} into a concrete Java type, it can be tedious to use the {@link #type()} or
 * {@code is*} methods. For this reason, a {@link #convert(TypeConvertingVisitor)} method is provided that exposes a polymorphic
 * way of converting a value into another type.
 *
 * An instance of {@link ItemAttributeValue} is created with the {@code from*} methods, like {@link #fromString(String)} or
 * {@link #fromNumber(String)}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ItemAttributeValue {
    private final ItemAttributeValueType type;
    private final boolean isNull;
    private final Map<String, ItemAttributeValue> mapValue;
    private final String stringValue;
    private final String numberValue;
    private final SdkBytes bytesValue;
    private final Boolean booleanValue;
    private final List<String> setOfStringsValue;
    private final List<String> setOfNumbersValue;
    private final List<SdkBytes> setOfBytesValue;
    private final List<ItemAttributeValue> listOfAttributeValuesValue;

    private ItemAttributeValue(InternalBuilder builder) {
        this.type = builder.type;
        this.isNull = builder.isNull;
        this.stringValue = builder.stringValue;
        this.numberValue = builder.numberValue;
        this.bytesValue = builder.bytesValue;
        this.booleanValue = builder.booleanValue;

        this.mapValue = builder.mapValue == null
                        ? null
                        : Collections.unmodifiableMap(new LinkedHashMap<>(builder.mapValue));
        this.setOfStringsValue = builder.setOfStringsValue == null
                                 ? null
                                 : Collections.unmodifiableList(new ArrayList<>(builder.setOfStringsValue));
        this.setOfNumbersValue = builder.setOfNumbersValue == null
                                 ? null
                                 : Collections.unmodifiableList(new ArrayList<>(builder.setOfNumbersValue));
        this.setOfBytesValue = builder.setOfBytesValue == null
                               ? null
                               : Collections.unmodifiableList(new ArrayList<>(builder.setOfBytesValue));
        this.listOfAttributeValuesValue = builder.listOfAttributeValuesValue == null
                                          ? null
                                          : Collections.unmodifiableList(new ArrayList<>(builder.listOfAttributeValuesValue));
    }

    /**
     * Create an {@link ItemAttributeValue} for the null DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().nul(true).build())}
     */
    public static ItemAttributeValue nullValue() {
        return new InternalBuilder().isNull().build();
    }

    /**
     * Create an {@link ItemAttributeValue} for a map (m) DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().m(...).build())}
     */
    public static ItemAttributeValue fromMap(Map<String, ItemAttributeValue> mapValue) {
        return new InternalBuilder().mapValue(mapValue).build();
    }

    /**
     * Create an {@link ItemAttributeValue} for a string (s) DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().s(...).build())}
     */
    public static ItemAttributeValue fromString(String stringValue) {
        return new InternalBuilder().stringValue(stringValue).build();
    }

    /**
     * Create an {@link ItemAttributeValue} for a number (n) DynamoDB type.
     *
     * This is a String, because it matches the underlying DynamoDB representation.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().n(...).build())}
     */
    public static ItemAttributeValue fromNumber(String numberValue) {
        return new InternalBuilder().numberValue(numberValue).build();
    }

    /**
     * Create an {@link ItemAttributeValue} for a bytes (b) DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().b(...).build())}
     */
    public static ItemAttributeValue fromBytes(SdkBytes bytesValue) {
        return new InternalBuilder().bytesValue(bytesValue).build();
    }


    /**
     * Create an {@link ItemAttributeValue} for a boolean (bool) DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().bool(...).build())}
     */
    public static ItemAttributeValue fromBoolean(Boolean booleanValue) {
        return new InternalBuilder().booleanValue(booleanValue).build();
    }

    /**
     * Create an {@link ItemAttributeValue} for a set-of-strings (ss) DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().ss(...).build())}
     */
    public static ItemAttributeValue fromSetOfStrings(Collection<String> setOfStringsValue) {
        return new InternalBuilder().setOfStringsValue(setOfStringsValue).build();
    }

    /**
     * Create an {@link ItemAttributeValue} for a set-of-numbers (ns) DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().ns(...).build())}
     */
    public static ItemAttributeValue fromSetOfNumbers(Collection<String> setOfNumbersValue) {
        return new InternalBuilder().setOfNumbersValue(setOfNumbersValue).build();
    }

    /**
     * Create an {@link ItemAttributeValue} for a set-of-bytes (bs) DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().bs(...).build())}
     */
    public static ItemAttributeValue fromSetOfBytes(Collection<SdkBytes> setOfBytesValue) {
        return new InternalBuilder().setOfBytesValue(setOfBytesValue).build();
    }

    /**
     * Create an {@link ItemAttributeValue} for a list-of-attributes (l) DynamoDB type.
     *
     * Equivalent to: {@code ItemAttributeValue.fromGeneratedAttributeValue(AttributeValue.builder().l(...).build())}
     */
    public static ItemAttributeValue fromListOfAttributeValues(List<ItemAttributeValue> listOfAttributeValuesValue) {
        return new InternalBuilder().listOfAttributeValuesValue(listOfAttributeValuesValue).build();
    }

    /**
     * Create an {@link ItemAttributeValue} from a generated {@code Map<String, AttributeValue>}.
     */
    public static ItemAttributeValue fromGeneratedItem(Map<String, AttributeValue> attributeValues) {
        Map<String, ItemAttributeValue> result = new LinkedHashMap<>();
        attributeValues.forEach((k, v) -> result.put(k, fromGeneratedAttributeValue(v)));
        return ItemAttributeValue.fromMap(result);
    }

    /**
     * Create an {@link ItemAttributeValue} from a generated {@link AttributeValue}.
     */
    public static ItemAttributeValue fromGeneratedAttributeValue(AttributeValue attributeValue) {
        if (attributeValue.s() != null) {
            return ItemAttributeValue.fromString(attributeValue.s());
        }
        if (attributeValue.n() != null) {
            return ItemAttributeValue.fromNumber(attributeValue.n());
        }
        if (attributeValue.bool() != null) {
            return ItemAttributeValue.fromBoolean(attributeValue.bool());
        }
        if (Boolean.TRUE.equals(attributeValue.nul())) {
            return ItemAttributeValue.nullValue();
        }
        if (attributeValue.b() != null) {
            return ItemAttributeValue.fromBytes(attributeValue.b());
        }
        if (attributeValue.m() != null && !(attributeValue.m() instanceof SdkAutoConstructMap)) {
            Map<String, ItemAttributeValue> map = new LinkedHashMap<>();
            attributeValue.m().forEach((k, v) -> map.put(k, ItemAttributeValue.fromGeneratedAttributeValue(v)));
            return ItemAttributeValue.fromMap(map);
        }
        if (attributeValue.l() != null && !(attributeValue.l() instanceof SdkAutoConstructList)) {
            List<ItemAttributeValue> list =
                    attributeValue.l().stream().map(ItemAttributeValue::fromGeneratedAttributeValue).collect(toList());
            return ItemAttributeValue.fromListOfAttributeValues(list);
        }
        if (attributeValue.bs() != null && !(attributeValue.bs() instanceof SdkAutoConstructList)) {
            return ItemAttributeValue.fromSetOfBytes(attributeValue.bs());
        }
        if (attributeValue.ss() != null && !(attributeValue.ss() instanceof SdkAutoConstructList)) {
            return ItemAttributeValue.fromSetOfStrings(attributeValue.ss());
        }
        if (attributeValue.ns() != null && !(attributeValue.ns() instanceof SdkAutoConstructList)) {
            return ItemAttributeValue.fromSetOfNumbers(attributeValue.ns());
        }

        throw new IllegalStateException("Unable to convert attribute value: " + attributeValue);
    }

    /**
     * Retrieve the underlying DynamoDB type of this value, such as String (s) or Number (n).
     */
    public ItemAttributeValueType type() {
        return type;
    }

    /**
     * Apply the provided visitor to this item attribute value, converting it into a specific type. This is useful in
     * {@link ItemAttributeValueConverter} implementations, without having to write a switch statement on the {@link #type()}.
     */
    public <T> T convert(TypeConvertingVisitor<T> convertingVisitor) {
        return convertingVisitor.convert(this);
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is a Map (m).
     */
    public boolean isMap() {
        return mapValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is a String (s).
     */
    public boolean isString() {
        return stringValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is a Number (n).
     */
    public boolean isNumber() {
        return numberValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is Bytes (b).
     */
    public boolean isBytes() {
        return bytesValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is a Boolean (bool).
     */
    public boolean isBoolean() {
        return booleanValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is a Set of Strings (ss).
     */
    public boolean isSetOfStrings() {
        return setOfStringsValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is a Set of Numbers (ns).
     */
    public boolean isSetOfNumbers() {
        return setOfNumbersValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is a Set of Bytes (bs).
     */
    public boolean isSetOfBytes() {
        return setOfBytesValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is a List of AttributeValues (l).
     */
    public boolean isListOfAttributeValues() {
        return listOfAttributeValuesValue != null;
    }

    /**
     * Returns true if the underlying DynamoDB type of this value is Null (null).
     */
    public boolean isNull() {
        return isNull;
    }

    /**
     * Retrieve this value as a map. This will throw an exception if {@link #isMap()} is false.
     */
    public Map<String, ItemAttributeValue> asMap() {
        Validate.isTrue(isMap(), "Value is not a map.");
        return mapValue;
    }

    /**
     * Retrieve this value as a string. This will throw an exception if {@link #isString()} is false.
     */
    public String asString() {
        Validate.isTrue(isString(), "Value is not a string.");
        return stringValue;
    }

    /**
     * Retrieve this value as a number. This will throw an exception if {@link #isNumber()} is false.
     *
     * Note: This returns a {@code String} (instead of a {@code Number}), because that's the generated type from
     * DynamoDB: {@link AttributeValue#n()}.
     */
    public String asNumber() {
        Validate.isTrue(isNumber(), "Value is not a number.");
        return numberValue;
    }

    /**
     * Retrieve this value as bytes. This will throw an exception if {@link #isBytes()} is false.
     */
    public SdkBytes asBytes() {
        Validate.isTrue(isBytes(), "Value is not bytes.");
        return bytesValue;
    }

    /**
     * Retrieve this value as a boolean. This will throw an exception if {@link #isBoolean()} is false.
     */
    public Boolean asBoolean() {
        Validate.isTrue(isBoolean(), "Value is not a boolean.");
        return booleanValue;
    }

    /**
     * Retrieve this value as a set of strings. This will throw an exception if {@link #isSetOfStrings()} is false.
     *
     * Note: This returns a {@code List} (instead of a {@code Set}), because that's the generated type from
     * DynamoDB: {@link AttributeValue#ss()}.
     */
    public List<String> asSetOfStrings() {
        Validate.isTrue(isSetOfStrings(), "Value is not a list of strings.");
        return setOfStringsValue;
    }

    /**
     * Retrieve this value as a set of numbers. This will throw an exception if {@link #isSetOfNumbers()} is false.
     *
     * Note: This returns a {@code List<String>} (instead of a {@code Set<Number>}), because that's the generated type from
     * DynamoDB: {@link AttributeValue#ns()}.
     */
    public List<String> asSetOfNumbers() {
        Validate.isTrue(isSetOfNumbers(), "Value is not a list of numbers.");
        return setOfNumbersValue;
    }

    /**
     * Retrieve this value as a set of bytes. This will throw an exception if {@link #isSetOfBytes()} is false.
     *
     * Note: This returns a {@code List} (instead of a {@code Set}), because that's the generated type from
     * DynamoDB: {@link AttributeValue#bs()}.
     */
    public List<SdkBytes> asSetOfBytes() {
        Validate.isTrue(isSetOfBytes(), "Value is not a list of bytes.");
        return setOfBytesValue;
    }

    /**
     * Retrieve this value as a list of attribute values. This will throw an exception if {@link #isListOfAttributeValues()} is
     * false.
     */
    public List<ItemAttributeValue> asListOfAttributeValues() {
        Validate.isTrue(isListOfAttributeValues(), "Value is not a list of attribute values.");
        return listOfAttributeValuesValue;
    }

    /**
     * Convert this {@link ItemAttributeValue} into a generated {@code Map<String, AttributeValue>}. This will throw an exception
     * if {@link #isMap()} is false.
     */
    public Map<String, AttributeValue> toGeneratedItem() {
        Validate.validState(isMap(), "Cannot convert an attribute value of type %s to a generated item. Must be %s.",
                            type(), ItemAttributeValueType.MAP);

        AttributeValue generatedAttributeValue = toGeneratedAttributeValue();

        Validate.validState(generatedAttributeValue.m() != null && !(generatedAttributeValue.m() instanceof SdkAutoConstructMap),
                            "Map ItemAttributeValue was not converted into a Map AttributeValue.");
        return generatedAttributeValue.m();
    }

    /**
     * Convert this {@link ItemAttributeValue} into a generated {@link AttributeValue}.
     */
    public AttributeValue toGeneratedAttributeValue() {
        return convert(ToGeneratedAttributeValueVisitor.INSTANCE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ItemAttributeValue that = (ItemAttributeValue) o;

        if (isNull != that.isNull) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        if (mapValue != null ? !mapValue.equals(that.mapValue) : that.mapValue != null) {
            return false;
        }
        if (stringValue != null ? !stringValue.equals(that.stringValue) : that.stringValue != null) {
            return false;
        }
        if (numberValue != null ? !numberValue.equals(that.numberValue) : that.numberValue != null) {
            return false;
        }
        if (bytesValue != null ? !bytesValue.equals(that.bytesValue) : that.bytesValue != null) {
            return false;
        }
        if (booleanValue != null ? !booleanValue.equals(that.booleanValue) : that.booleanValue != null) {
            return false;
        }
        if (setOfStringsValue != null ? !setOfStringsValue.equals(that.setOfStringsValue) : that.setOfStringsValue != null) {
            return false;
        }
        if (setOfNumbersValue != null ? !setOfNumbersValue.equals(that.setOfNumbersValue) : that.setOfNumbersValue != null) {
            return false;
        }
        if (setOfBytesValue != null ? !setOfBytesValue.equals(that.setOfBytesValue) : that.setOfBytesValue != null) {
            return false;
        }
        return listOfAttributeValuesValue != null ? listOfAttributeValuesValue.equals(that.listOfAttributeValuesValue)
                                                  : that.listOfAttributeValuesValue == null;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (isNull ? 1 : 0);
        result = 31 * result + (mapValue != null ? mapValue.hashCode() : 0);
        result = 31 * result + (stringValue != null ? stringValue.hashCode() : 0);
        result = 31 * result + (numberValue != null ? numberValue.hashCode() : 0);
        result = 31 * result + (bytesValue != null ? bytesValue.hashCode() : 0);
        result = 31 * result + (booleanValue != null ? booleanValue.hashCode() : 0);
        result = 31 * result + (setOfStringsValue != null ? setOfStringsValue.hashCode() : 0);
        result = 31 * result + (setOfNumbersValue != null ? setOfNumbersValue.hashCode() : 0);
        result = 31 * result + (setOfBytesValue != null ? setOfBytesValue.hashCode() : 0);
        result = 31 * result + (listOfAttributeValuesValue != null ? listOfAttributeValuesValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        Object value = convert(ToStringVisitor.INSTANCE);
        return ToString.builder("ItemAttributeValue")
                       .add("type", type)
                       .add("value", value)
                       .build();
    }

    private static class ToGeneratedAttributeValueVisitor extends TypeConvertingVisitor<AttributeValue> {
        private static final ToGeneratedAttributeValueVisitor INSTANCE = new ToGeneratedAttributeValueVisitor();

        private ToGeneratedAttributeValueVisitor() {
            super(AttributeValue.class);
        }

        @Override
        public AttributeValue convertNull() {
            return AttributeValue.builder().nul(true).build();
        }

        @Override
        public AttributeValue convertMap(Map<String, ItemAttributeValue> value) {
            Map<String, AttributeValue> map = new LinkedHashMap<>();
            value.forEach((k, v) -> map.put(k, v.toGeneratedAttributeValue()));
            return AttributeValue.builder().m(map).build();
        }

        @Override
        public AttributeValue convertString(String value) {
            return AttributeValue.builder().s(value).build();
        }

        @Override
        public AttributeValue convertNumber(String value) {
            return AttributeValue.builder().n(value).build();
        }

        @Override
        public AttributeValue convertBytes(SdkBytes value) {
            return AttributeValue.builder().b(value).build();
        }

        @Override
        public AttributeValue convertBoolean(Boolean value) {
            return AttributeValue.builder().bool(value).build();
        }

        @Override
        public AttributeValue convertSetOfStrings(List<String> value) {
            return AttributeValue.builder().ss(value).build();
        }

        @Override
        public AttributeValue convertSetOfNumbers(List<String> value) {
            return AttributeValue.builder().ns(value).build();
        }

        @Override
        public AttributeValue convertSetOfBytes(List<SdkBytes> value) {
            return AttributeValue.builder().bs(value).build();
        }

        @Override
        public AttributeValue convertListOfAttributeValues(Collection<ItemAttributeValue> value) {
            return AttributeValue.builder()
                                 .l(value.stream().map(ItemAttributeValue::toGeneratedAttributeValue).collect(toList()))
                                 .build();
        }
    }

    private static class ToStringVisitor extends TypeConvertingVisitor<Object> {
        private static final ToStringVisitor INSTANCE = new ToStringVisitor();

        private ToStringVisitor() {
            super(Object.class);
        }

        @Override
        public Object convertNull() {
            return "null";
        }

        @Override
        public Object convertMap(Map<String, ItemAttributeValue> value) {
            return value;
        }

        @Override
        public Object convertString(String value) {
            return value;
        }

        @Override
        public Object convertNumber(String value) {
            return value;
        }

        @Override
        public Object convertBytes(SdkBytes value) {
            return value;
        }

        @Override
        public Object convertBoolean(Boolean value) {
            return value;
        }

        @Override
        public Object convertSetOfStrings(List<String> value) {
            return value;
        }

        @Override
        public Object convertSetOfNumbers(List<String> value) {
            return value;
        }

        @Override
        public Object convertSetOfBytes(List<SdkBytes> value) {
            return value;
        }

        @Override
        public Object convertListOfAttributeValues(Collection<ItemAttributeValue> value) {
            return value;
        }
    }

    private static class InternalBuilder {
        private ItemAttributeValueType type;
        private boolean isNull = false;
        private Map<String, ItemAttributeValue> mapValue;
        private String stringValue;
        private String numberValue;
        private SdkBytes bytesValue;
        private Boolean booleanValue;
        private Collection<String> setOfStringsValue;
        private Collection<String> setOfNumbersValue;
        private Collection<SdkBytes> setOfBytesValue;
        private Collection<ItemAttributeValue> listOfAttributeValuesValue;

        public InternalBuilder isNull() {
            this.type = ItemAttributeValueType.NULL;
            this.isNull = true;
            return this;
        }

        private InternalBuilder mapValue(Map<String, ItemAttributeValue> mapValue) {
            this.type = ItemAttributeValueType.MAP;
            this.mapValue = mapValue;
            return this;
        }

        private InternalBuilder stringValue(String stringValue) {
            this.type = ItemAttributeValueType.STRING;
            this.stringValue = stringValue;
            return this;
        }

        private InternalBuilder numberValue(String numberValue) {
            this.type = ItemAttributeValueType.NUMBER;
            this.numberValue = numberValue;
            return this;
        }

        private InternalBuilder bytesValue(SdkBytes bytesValue) {
            this.type = ItemAttributeValueType.BYTES;
            this.bytesValue = bytesValue;
            return this;
        }

        private InternalBuilder booleanValue(Boolean booleanValue) {
            this.type = ItemAttributeValueType.BOOLEAN;
            this.booleanValue = booleanValue;
            return this;
        }

        private InternalBuilder setOfStringsValue(Collection<String> setOfStringsValue) {
            this.type = ItemAttributeValueType.SET_OF_STRINGS;
            this.setOfStringsValue = setOfStringsValue;
            return this;
        }

        private InternalBuilder setOfNumbersValue(Collection<String> setOfNumbersValue) {
            this.type = ItemAttributeValueType.SET_OF_NUMBERS;
            this.setOfNumbersValue = setOfNumbersValue;
            return this;
        }

        private InternalBuilder setOfBytesValue(Collection<SdkBytes> setOfBytesValue) {
            this.type = ItemAttributeValueType.SET_OF_BYTES;
            this.setOfBytesValue = setOfBytesValue;
            return this;
        }

        private InternalBuilder listOfAttributeValuesValue(Collection<ItemAttributeValue> listOfAttributeValuesValue) {
            this.type = ItemAttributeValueType.LIST_OF_ATTRIBUTE_VALUES;
            this.listOfAttributeValuesValue = listOfAttributeValuesValue;
            return this;
        }

        private ItemAttributeValue build() {
            return new ItemAttributeValue(this);
        }
    }
}
