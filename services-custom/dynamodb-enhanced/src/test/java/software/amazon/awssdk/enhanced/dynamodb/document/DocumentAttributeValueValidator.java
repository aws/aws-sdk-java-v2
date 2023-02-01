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

package software.amazon.awssdk.enhanced.dynamodb.document;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DocumentAttributeValueValidator {

    public static boolean validateSpecificGetter(AttributeValue value, DefaultEnhancedDocument enhancedDocument, String key) {

        EnhancedAttributeValue enhancedAttributeValue = EnhancedAttributeValue.fromAttributeValue(value);
        if (enhancedAttributeValue.isNull()) {
            return enhancedDocument.isNull(key);
        }
        if (enhancedAttributeValue.isString()) {
            return enhancedAttributeValue.asString().equals(enhancedDocument.getString(key));
        }
        if (enhancedAttributeValue.isNumber()) {
            return enhancedAttributeValue.asNumber().equals(enhancedDocument.getSdkNumber(key).stringValue());
        }
        if (enhancedAttributeValue.isBytes()) {
            return enhancedAttributeValue.asBytes().equals(enhancedDocument.getSdkBytes(key));
        }
        if (enhancedAttributeValue.isBoolean()) {
            return enhancedAttributeValue.asBoolean().equals(enhancedDocument.getBoolean(key));
        }
        if (enhancedAttributeValue.isNull()) {
            return enhancedDocument.isNull(key);
        }
        if (enhancedAttributeValue.isMap()) {
            return validateMapAsDocument(enhancedAttributeValue.asMap(), enhancedDocument.getMapAsDocument(key))
                   && validateGenericMap(enhancedAttributeValue.asMap(), enhancedDocument.getRawMap(key));
        }
        if (enhancedAttributeValue.isSetOfBytes()) {
            return enhancedAttributeValue.asSetOfBytes().containsAll(enhancedDocument.getSdkBytesSet(key))
                   && enhancedDocument.getSdkBytesSet(key).containsAll(enhancedAttributeValue.asSetOfBytes());
        }
        if (enhancedAttributeValue.isSetOfNumbers()) {
            List<SdkNumber> strings =
                enhancedAttributeValue.asSetOfNumbers().stream()
                                      .map(stringNumber -> SdkNumber.fromString(stringNumber))
                                      .collect(Collectors.toList());
            return strings.containsAll(enhancedDocument.getNumberSet(key))
                   && enhancedDocument.getNumberSet(key).containsAll(strings);
        }
        if (enhancedAttributeValue.isSetOfStrings()) {
            return enhancedAttributeValue.asSetOfStrings().containsAll(enhancedDocument.getStringSet(key))
                   && enhancedDocument.getStringSet(key).containsAll(enhancedAttributeValue.asSetOfStrings());
        }
        if (enhancedAttributeValue.isListOfAttributeValues()) {
            return validateGenericList(enhancedAttributeValue.asListOfAttributeValues(), enhancedDocument.getList(key));
        }
        throw new IllegalStateException("enhancedAttributeValue type not found " + enhancedAttributeValue.type());

    }

    private static boolean validateGenericObjects(AttributeValue attributeValue, Object object) {

        EnhancedAttributeValue enhancedAttributeValue = EnhancedAttributeValue.fromAttributeValue(attributeValue);
        if (enhancedAttributeValue.isNull()) {
            return object == null;
        }
        if (enhancedAttributeValue.isString()) {
            return enhancedAttributeValue.asString().equals(object);
        }
        if (enhancedAttributeValue.isNumber()) {
            return SdkNumber.fromString(enhancedAttributeValue.asNumber()).equals(object);
        }
        if (enhancedAttributeValue.isBytes()) {
            return enhancedAttributeValue.asBytes().equals(object);
        }
        if (enhancedAttributeValue.isBoolean()) {
            return enhancedAttributeValue.asBoolean().equals(object);
        }
        if (enhancedAttributeValue.isSetOfStrings()) {
            return enhancedAttributeValue.asSetOfStrings().equals(object);
        }
        if (enhancedAttributeValue.isSetOfNumbers()) {
            return enhancedAttributeValue.asSetOfNumbers().stream().map(string -> SdkNumber.fromString(string))
                                         .collect(Collectors.toList()).equals(object);
        }
        if (enhancedAttributeValue.isSetOfBytes()) {
            return enhancedAttributeValue.asSetOfBytes().stream().map(byteValue -> SdkBytes.fromByteArray(byteValue.asByteArray()))
                                         .collect(Collectors.toList()).equals(object);
        }
        if (enhancedAttributeValue.isListOfAttributeValues()) {
            return validateGenericList(enhancedAttributeValue.asListOfAttributeValues(), (List) object);
        }
        if (enhancedAttributeValue.isMap()) {
            return validateGenericMap(enhancedAttributeValue.asMap(), (Map<String, Object>) object);
        }
        throw new IllegalStateException("Cannot identify type ");

    }

    private static boolean validateGenericList(List<AttributeValue> asListOfAttributeValues, List<?> list) {

        if (asListOfAttributeValues.size() != list.size()) {
            return false;
        }
        int index = 0;
        for (AttributeValue attributeValue : asListOfAttributeValues) {
            if (!validateGenericObjects(attributeValue, list.get(index))) {
                return false;
            }
            index++;
        }
        return true;
    }

    public static boolean validateGenericMap(Map<String, AttributeValue> attributeValueMap, Map<String, Object> rawMap) {
        if (attributeValueMap.size() != rawMap.size()) {
            return false;
        }
        return attributeValueMap.entrySet().stream().allMatch(
            entry -> rawMap.containsKey(entry.getKey())
                     && validateGenericObjects(entry.getValue(), rawMap.get(entry.getKey())));
    }

    private static boolean validateMapAsDocument(Map<String, AttributeValue> attributeValueMap, EnhancedDocument mapAsDocument) {
        if (attributeValueMap.size() != mapAsDocument.asMap().size()) {
            return false;
        }
        return attributeValueMap.entrySet().stream()
                                .allMatch(entry -> mapAsDocument.isPresent(entry.getKey())
                                                   && validateGenericObjects(entry.getValue(),
                                                                             mapAsDocument.get(entry.getKey())));
    }
}
