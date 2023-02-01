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

package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Utilities for working with {@link AttributeValue} and {@link EnhancedDocument} types.
 */
@SdkInternalApi
public final class DocumentUtils {
    public static final AttributeValue NULL_ATTRIBUTE_VALUE = AttributeValue.fromNul(true);

    private DocumentUtils() {
    }

    /**
     * Converts AttributeValue to simple Java Objects like String, SdkNumber, SdkByte.
     */
    public static Object toSimpleValue(AttributeValue value) {
        EnhancedAttributeValue attributeValue = EnhancedAttributeValue.fromAttributeValue(value);
        if (attributeValue.isNull()) {
            return null;
        }
        if (Boolean.FALSE.equals(value.nul())) {
            throw new UnsupportedOperationException("False-NULL is not supported in DynamoDB");
        }
        if (attributeValue.isBoolean()) {
            return attributeValue.asBoolean();
        }
        if (attributeValue.isString()) {
            return attributeValue.asString();
        }
        if (attributeValue.isNumber()) {
            return SdkNumber.fromString(attributeValue.asNumber());
        }
        if (attributeValue.isBytes()) {
            return attributeValue.asBytes();
        }
        if (attributeValue.isSetOfStrings()) {
            return attributeValue.asSetOfStrings();
        }
        if (attributeValue.isSetOfNumbers()) {
            return attributeValue.asSetOfNumbers().stream().map(SdkNumber::fromString).collect(Collectors.toList());
        }
        if (value.hasBs()) {
            return value.bs();
        }
        if (attributeValue.isListOfAttributeValues()) {
            return toSimpleList(attributeValue.asListOfAttributeValues());
        }
        if (attributeValue.isMap()) {
            return toSimpleMapValue(attributeValue.asMap());
        }
        throw new IllegalArgumentException("Attribute value must not be empty: " + value);
    }

    /**
     * Converts a List of attributeValues to list of simple java objects.
     */
    public static List<Object> toSimpleList(List<AttributeValue> attrValues) {
        if (attrValues == null) {
            return null;
        }
        return attrValues.stream()
                         .map(DocumentUtils::toSimpleValue)
                         .collect(Collectors.toCollection(() -> new ArrayList<>(attrValues.size())));
    }

    /**
     * Converts a Map of string-attributeValues  key value pair to  Map of string-simple java objects key value pair..
     */
    public static Map<String, Object> toSimpleMapValue(Map<String, AttributeValue> values) {
        if (values == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>(values.size());
        for (Map.Entry<String, AttributeValue> entry : values.entrySet()) {
            result.put(entry.getKey(), toSimpleValue(entry.getValue()));
        }
        return result;
    }

    private static AttributeValue convertSetToAttributeValue(Set<?> objects,
                                                             AttributeConverterProvider attributeConverterProvider) {

        if (!objects.isEmpty()) {
            Iterator<?> iterator = objects.iterator();
            Object firstNonNullElement = null;
            while (iterator.hasNext() && firstNonNullElement == null) {
                firstNonNullElement = iterator.next();
            }
            if (firstNonNullElement != null) {
                return attributeConverterProvider.converterFor(EnhancedType.setOf(firstNonNullElement.getClass()))
                                                 .transformFrom((Set) objects);
            }
        }
        // If Set is empty or if all elements are null then default to empty string set.
        return AttributeValue.fromSs(new ArrayList<>());
    }


    /**
     * Converts sourceObject to AttributeValue based on provided AttributeConverterProvider.
     */
    public static AttributeValue convert(Object sourceObject, AttributeConverterProvider attributeConverterProvider) {
        if (sourceObject == null) {
            return NULL_ATTRIBUTE_VALUE;
        }
        if (sourceObject instanceof List) {
            return convertListToAttributeValue((Collection) sourceObject, attributeConverterProvider);
        }
        if (sourceObject instanceof Set) {
            return convertSetToAttributeValue((Set<?>) sourceObject, attributeConverterProvider);
        }
        if (sourceObject instanceof Map) {
            return convertMapToAttributeValue((Map<?, ?>) sourceObject, attributeConverterProvider);
        }
        AttributeConverter attributeConverter = attributeConverterProvider.converterFor(EnhancedType.of(sourceObject.getClass()));
        return attributeConverter.transformFrom(sourceObject);
    }


    /**
     * Coverts AttributeValue to simple java objects like String, SdkNumber, Boolean, List, Set, SdkBytes or Maps.
     */
    public static Object convertAttributeValueToObject(AttributeValue attributeValue) {
        if (attributeValue.hasL()) {
            return toSimpleList(attributeValue.l());
        }
        if (attributeValue.hasM()) {
            return toSimpleMapValue(attributeValue.m());
        }
        return toSimpleValue(attributeValue);
    }

    /**
     * Iterators Collection of objects and converts each element.
     */
    private static AttributeValue convertListToAttributeValue(Collection<?> objects,
                                                              AttributeConverterProvider attributeConverterProvider) {
        return AttributeValue.fromL(objects.stream()
                                           .map(obj -> convert(obj, attributeConverterProvider))
                                           .collect(Collectors.toList()));
    }

    private static AttributeValue convertMapToAttributeValue(Map<?, ?> objects,
                                                             AttributeConverterProvider attributeConverterProvider) {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        objects.forEach((key, value) -> attributeValueMap.put(String.valueOf(key), convert(value, attributeConverterProvider)));
        return AttributeValue.fromM(attributeValueMap);
    }

}
