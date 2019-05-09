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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.Table;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link ItemAttributeValue} that can be converted into any Java type.
 *
 * <p>
 * This is usually returned by the SDK from methods that load data from DynamoDB, like {@link Table#getItem(RequestItem)}'s
 * {@link ResponseItem} result.
 *
 * <p>
 * Multiple categories of methods are exposed:
 * <ol>
 *     <li>{@code asType()} methods like {@link #asString()} and {@link #asInteger()} that can be used to retrieve the attribute
 *     value as a type that is definitely supported by the SDK by default. These types will always be supported by the SDK,
 *     unless the converters have been overridden or the value in DynamoDB cannot be converted to the requested type.</li>
 *     <li>{@code as()} methods like {@link #as(Class)} and {@link #as(TypeToken)} that can be used to retrieve the attribute
 *     value as any Java type. These types may be supported by the SDK or custom converters.</li>
 *     <li>{@link #attributeValue()}, which returns the {@link ItemAttributeValue} exactly as it was returned by DynamoDB.</li>
 * </ol>
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface ConvertableItemAttributeValue {
    /**
     * Retrieve the {@link ItemAttributeValue} exactly as it was returned by DynamoDB.
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     */
    ItemAttributeValue attributeValue();

    /**
     * Convert this attribute value into the provided type, using the {@link ItemAttributeValueConverter}s configured in the SDK.
     *
     * <p>
     * For parameterized types, use {@link #as(TypeToken)}.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>The provided class is null.</li>
     *     <li>A converter does not exist for the requested type.</li>
     *     <li>This attribute value cannot be converted to the requested type.</li>
     * </ol>
     */
    <T> T as(Class<T> type);

    /**
     * Convert this attribute value into a type that matches the provided type token.
     *
     * <p>
     * This is useful for parameterized types. Non-parameterized types should use {@link #as(Class)}. Lists should use
     * {@link #asList(Class)}. Maps should use {@link #asMap(Class, Class)}.
     *
     * <p>
     * When creating a {@link TypeToken}, you must create an anonymous sub-class, e.g.
     * {@code new TypeToken<Collection<String>>()&#123;&#125;} (note the extra {}).
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>The provided token is null.</li>
     *     <li>A converter does not exist for the provided type.</li>
     *     <li>This attribute value cannot be converted to the requested type.</li>
     * </ol>
     */
    <T> T as(TypeToken<T> type);

    /**
     * Convert this attribute value into a {@link String} using the {@link ItemAttributeValueConverter}s configured in the SDK.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>This attribute value cannot be converted to a String.</li>
     * </ol>
     */
    default String asString() {
        return as(String.class);
    }

    /**
     * Convert this attribute value into an {@link Integer} using the {@link ItemAttributeValueConverter}s configured in the SDK.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>This attribute value cannot be converted to an Integer.</li>
     * </ol>
     */
    default Integer asInteger() {
        return as(Integer.class);
    }

    /**
     * Convert this attribute value into an {@link Integer} using the {@link ItemAttributeValueConverter}s configured in the SDK.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>This attribute value cannot be converted to an Instant.</li>
     * </ol>
     */
    default Instant asInstant() {
        return as(Instant.class);
    }

    /**
     * Convert this attribute value into a {@link List}, parameterized with the provided class. This uses the
     * {@link ItemAttributeValueConverter}s configured in the SDK.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>The provided type is null.</li>
     *     <li>This attribute value cannot be converted to a List of the requested type.</li>
     * </ol>
     */
    default <T> List<T> asList(Class<T> listParameterType) {
        Validate.paramNotNull(listParameterType, "listParameterType");
        return as(TypeToken.listOf(listParameterType));
    }

    /**
     * Convert this attribute value into a {@link Map}, parameterized with the provided classes. This uses the
     * {@link ItemAttributeValueConverter}s configured in the SDK.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>The provided key or value type is null.</li>
     *     <li>This attribute value cannot be converted to a Map of the requested types.</li>
     * </ol>
     */
    default <K, V> Map<K, V> asMap(Class<K> keyType, Class<V> valueType) {
        Validate.paramNotNull(keyType, "keyType");
        Validate.paramNotNull(valueType, "valueType");
        return as(TypeToken.mapOf(keyType, valueType));
    }
}
