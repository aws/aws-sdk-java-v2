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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import java.nio.ByteBuffer;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * This static helper class contains some literal {@link AttributeValue} constants and converters. Primarily these
 * will be used if constructing a literal key object or for use in a custom filter expression. Eg:
 *
 * {@code Key<?> myKey = Key.of(stringValue("id123"), numberValue(4.23));
 * Expression filterExpression = Expression.of("id = :filter_id", singletonMap(":filter_id", stringValue("id123")); }
 */
@SdkPublicApi
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
        return AttributeTypes.stringType().objectToAttributeValue(value);
    }

    /**
     * Creates a literal numeric {@link AttributeValue} from any type of Java number.
     * @param value A number to create the literal from.
     * @return An {@link AttributeValue} of type n that represents the numeric literal.
     */
    public static AttributeValue numberValue(Number value) {
        return AttributeTypes.numberType(null).objectToAttributeValue(value);
    }

    /**
     * Creates a literal binary {@link AttributeValue} from a Java {@link ByteBuffer}.
     * @param value A {@link ByteBuffer} to create the literal from.
     * @return An {@link AttributeValue} of type B that represents the binary literal.
     */
    public static AttributeValue binaryValue(ByteBuffer value) {
        return AttributeTypes.binaryType().objectToAttributeValue(value);
    }

    /**
     * A helper method to test if an {@link AttributeValue} is a 'null' constant. This will not test if the
     * AttributeValue object is null itself, and in fact will throw a NullPointerException if you pass in null.
     * @param attributeValue An {@link AttributeValue} to test for null.
     * @return true if the supplied AttributeValue represents a null value, or false if it does not.
     */
    public static boolean isNullAttributeValue(AttributeValue attributeValue) {
        return attributeValue.nul() != null && attributeValue.nul();
    }
}
