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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * This static helper class contains some literal {@link AttributeValue} constants and converters. Primarily these
 * will be used if constructing a literal key object or for use in a custom filter expression. Eg:
 *
 * {@code Key<?> myKey = Key.create(stringValue("id123"), numberValue(4.23));
 * Expression filterExpression = Expression.of("id = :filter_id", singletonMap(":filter_id", stringValue("id123")); }
 */
@SdkInternalApi
public final class AttributeValues {
    private static final AttributeValue NULL_ATTRIBUTE_VALUE = AttributeValue.builder().nul(true).build();

    private AttributeValues() {
    }

    /**
     * The constant that represents a 'null' in a DynamoDb record.
     * @return An {@link AttributeValue} of type NUL that represents 'null'.
     */
    public static AttributeValue nullAttributeValue() {
        return NULL_ATTRIBUTE_VALUE;
    }

    /**
     * Creates a literal string {@link AttributeValue}.
     * @param value A string to create the literal from.
     * @return An {@link AttributeValue} of type S that represents the string literal.
     */
    public static AttributeValue stringValue(String value) {
        return AttributeValue.builder().s(value).build();
    }

    /**
     * Creates a literal numeric {@link AttributeValue} from any type of Java number.
     * @param value A number to create the literal from.
     * @return An {@link AttributeValue} of type n that represents the numeric literal.
     */
    public static AttributeValue numberValue(Number value) {
        return AttributeValue.builder().n(value.toString()).build();
    }

    /**
     * Creates a literal binary {@link AttributeValue} from raw bytes.
     * @param value bytes to create the literal from.
     * @return An {@link AttributeValue} of type B that represents the binary literal.
     */
    public static AttributeValue binaryValue(SdkBytes value) {
        return AttributeValue.builder().b(value).build();
    }
}
