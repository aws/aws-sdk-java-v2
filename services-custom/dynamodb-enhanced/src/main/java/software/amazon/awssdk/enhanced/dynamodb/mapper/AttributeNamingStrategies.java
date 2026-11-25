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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BuiltInAttributeNamingStrategy;

/**
 * Built-in {@link AttributeNamingStrategy} implementations.
 *
 * <p>
 * A customer-defined strategy implements {@link AttributeNamingStrategy} directly. For example, a strategy can
 * lowercase every source name with {@code attributeName.toLowerCase(Locale.ROOT)}.
 */
// CHECKSTYLE:OFF: PluralEnumNames - The design names this type AttributeNamingStrategies.
@SdkPublicApi
public final class AttributeNamingStrategies {
    // CHECKSTYLE:ON: PluralEnumNames
    /**
     * Joins the lowercase words with {@code _}. {@code customerId} is stored as {@code customer_id}.
     */
    public static final AttributeNamingStrategy LOWER_SNAKE_CASE = BuiltInAttributeNamingStrategy.LOWER_SNAKE_CASE;

    /**
     * Uppercases the words and joins them with {@code _}. {@code customerId} is stored as {@code CUSTOMER_ID}.
     */
    public static final AttributeNamingStrategy UPPER_SNAKE_CASE = BuiltInAttributeNamingStrategy.UPPER_SNAKE_CASE;

    /**
     * Capitalizes every word after the first and joins the words without a separator. {@code customer_id} is stored as
     * {@code customerId}.
     */
    public static final AttributeNamingStrategy LOWER_CAMEL_CASE = BuiltInAttributeNamingStrategy.LOWER_CAMEL_CASE;

    /**
     * Capitalizes every word and joins the words without a separator. {@code customer_id} is stored as
     * {@code CustomerId}.
     */
    public static final AttributeNamingStrategy UPPER_CAMEL_CASE = BuiltInAttributeNamingStrategy.UPPER_CAMEL_CASE;

    private AttributeNamingStrategies() {
    }
}
