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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.MapSubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * Used by {@link MapSubtypeAttributeConverter} to determine the type of the key and value in a map, if the type isn't
 * parameterized in the map's type.
 *
 * <p>
 * For example, with {@code class MyMap extends HashMap<String, String>}, an {@link ItemAttributeValue} cannot be converted to a
 * {@code MyMap}, because the {@code MapSubtypeAttributeConverter} must know that the key and values contents of the list are a
 * {@code String}. In this case, {@code MyMap} can implement {@code KeyValueTypeAwareMap} to specify that the key and value types
 * type are a {@code String}:
 * <pre>
 * class MyMap extends HashMap<String, String> implements KeyValueTypeAwareMap {
 *     @Override
 *     public TypeToken<String> keyConversionType() {
 *         return TypeToken.of(String.class);
 *     }
 *
 *     @Override
 *     public TypeToken<String> valueConversionType() {
 *         return TypeToken.of(String.class);
 *     }
 * }
 * </pre>
 *
 * <p>
 * {@link GenericConvertibleMap} should be used if the key and value types are a parameter on the map type, but they aren't the
 * first and second parameter (respectively).
 *
 * @see GenericConvertibleMap
 */
@SdkPublicApi
public interface KeyValueTypeAwareMap {
    /**
     * Return the specific key type in this map.
     */
    TypeToken<?> keyConversionType();

    /**
     * Return the specific value type in this map.
     */
    TypeToken<?> valueConversionType();
}
