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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.utils.Validate;

/**
 * A visitor across all possible types of a {@link ItemAttributeValue}.
 *
 * <p>
 * This is useful in {@link ItemAttributeValueConverter} implementations, without having to write a switch statement on the
 * {@link ItemAttributeValue#type()}.
 *
 * @see ItemAttributeValue#convert(TypeConvertingVisitor)
 */
@SdkPublicApi
public abstract class TypeConvertingVisitor<T> {
    private final Class<? extends ItemAttributeValueConverter> converterClass;
    private final Class<?> targetType;

    /**
     * Called by subclasses to provide enhanced logging when a specific type isn't handled.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     *
     * @param targetType The type to which this visitor is converting.
     */
    protected TypeConvertingVisitor(Class<?> targetType) {
        this(targetType, null);
    }

    /**
     * Called by subclasses to provide enhanced logging when a specific type isn't handled.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided type is null.</li>
     * </ol>
     *
     * @param targetType The type to which this visitor is converting.
     * @param converterClass The converter implementation that is creating this visitor. This may be null.
     */
    protected TypeConvertingVisitor(Class<?> targetType,
                                    Class<? extends ItemAttributeValueConverter> converterClass) {
        Validate.paramNotNull(targetType, "targetType");
        this.targetType = targetType;
        this.converterClass = converterClass;
    }

    /**
     * Convert the provided value into the target type.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the value cannot be converted by this visitor.</li>
     * </ol>
     */
    public final T convert(ItemAttributeValue value) {
        switch (value.type()) {
            case NULL: return convertNull();
            case MAP: return convertMap(value.asMap());
            case STRING: return convertString(value.asString());
            case NUMBER: return convertNumber(value.asNumber());
            case BYTES: return convertBytes(value.asBytes());
            case BOOLEAN: return convertBoolean(value.asBoolean());
            case SET_OF_STRINGS: return convertSetOfStrings(value.asSetOfStrings());
            case SET_OF_NUMBERS: return convertSetOfNumbers(value.asSetOfNumbers());
            case SET_OF_BYTES: return convertSetOfBytes(value.asSetOfBytes());
            case LIST_OF_ATTRIBUTE_VALUES: return convertListOfAttributeValues(value.asListOfAttributeValues());
            default: throw new IllegalStateException("Unsupported type: " + value.type());
        }
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isNull()} is true.
     */
    public T convertNull() {
        return null;
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isMap()} is true. The provided value is the
     * underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertMap(Map<String, ItemAttributeValue> value) {
        return defaultConvert(ItemAttributeValueType.MAP, value);
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isString()} is true. The provided value is the
     * underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertString(String value) {
        return defaultConvert(ItemAttributeValueType.STRING, value);
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isNumber()} is true. The provided value is the
     * underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertNumber(String value) {
        return defaultConvert(ItemAttributeValueType.NUMBER, value);
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isBytes()} is true. The provided value is the
     * underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertBytes(SdkBytes value) {
        return defaultConvert(ItemAttributeValueType.BYTES, value);
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isBoolean()} is true. The provided value is the
     * underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertBoolean(Boolean value) {
        return defaultConvert(ItemAttributeValueType.BOOLEAN, value);
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isSetOfStrings()} is true. The provided value is the
     * underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertSetOfStrings(List<String> value) {
        return defaultConvert(ItemAttributeValueType.SET_OF_STRINGS, value);
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isSetOfNumbers()} is true. The provided value is the
     * underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertSetOfNumbers(List<String> value) {
        return defaultConvert(ItemAttributeValueType.SET_OF_NUMBERS, value);
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isSetOfBytes()} is true. The provided value is the
     * underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertSetOfBytes(List<SdkBytes> value) {
        return defaultConvert(ItemAttributeValueType.SET_OF_BYTES, value);
    }

    /**
     * Invoked when visiting an attribute in which {@link ItemAttributeValue#isListOfAttributeValues()} is true. The provided
     * value is the underlying value of the {@link ItemAttributeValue} being converted.
     */
    public T convertListOfAttributeValues(Collection<ItemAttributeValue> value) {
        return defaultConvert(ItemAttributeValueType.LIST_OF_ATTRIBUTE_VALUES, value);
    }

    /**
     * This is invoked by default if a different "convert" method is not overridden. By default, this throws an exception.
     *
     * @param type The type that wasn't handled by another "convert" method.
     * @param value The value that wasn't handled by another "convert" method.
     */
    public T defaultConvert(ItemAttributeValueType type, Object value) {
        if (converterClass != null) {
            throw new IllegalStateException(converterClass.getTypeName() + " cannot convert an attribute of type " + type +
                                            " into the requested type " + targetType);
        }

        throw new IllegalStateException("Cannot convert attribute of type " + type + " into a " + targetType);
    }
}
