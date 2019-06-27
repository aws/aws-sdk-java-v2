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

/**
 * Used by {@link MapSubtypeAttributeConverter} to determine the key and value type for a map, if the key and value types are a
 * parameter on the map's type, but they aren't the first and second parameter (respectively).
 *
 * <p>
 * For example, with {@code class MyMap<T, K, V> extends HashMap<K, V>}, an {@link ItemAttributeValue} cannot be converted to a
 * {@code MyMap}, because the {@code MapSubtypeAttributeConverter} must know that the key and value types of the map are defined
 * by the second and third type parameters. In this case, {@code MyList} can implement {@code GenericConvertibleMap} to specify
 * that the second type parameter is the key type, and the third type parameter is the value type:
 * <pre>
 * class MyMap<T, K, V> extends HashMap<K, V> implements GenericConvertibleMap {
 *     @Override
 *     public int keyConversionTypeIndex() {
 *         return 1;
 *     }
 *
 *     @Override
 *     public int valueConversionTypeIndex() {
 *         return 2;
 *     }
 * }
 * </pre>
 *
 * <p>
 * {@link KeyValueTypeAwareMap} should be used if the key and value types are not a parameter on the map type at all.
 *
 * @see KeyValueTypeAwareMap
 */
@SdkPublicApi
public interface GenericConvertibleMap {
    /**
     * Return the index of the type parameter on this type that defines the key type. 0 indicates the first type parameter.
     */
    int keyConversionTypeIndex();

    /**
     * Return the index of the type parameter on this type that defines the value type. 0 indicates the first type parameter.
     */
    int valueConversionTypeIndex();
}
