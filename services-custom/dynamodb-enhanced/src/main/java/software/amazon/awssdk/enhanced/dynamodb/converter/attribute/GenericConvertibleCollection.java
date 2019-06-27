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
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.CollectionSubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;

/**
 * Used by {@link CollectionSubtypeAttributeConverter} to determine the type of value stored in a collection, if the collection
 * type is parameterized with more than one type.
 *
 * <p>
 * For example, with {@code class MyList<T, U> extends ArrayList<U>}, an {@link ItemAttributeValue} cannot be converted to a
 * {@code MyList}, because the {@code CollectionSubtypeAttributeConverter} must know that the contents of the list are defined by
 * the second type parameter. In this case, {@code MyList} can implement {@code GenericConvertibleCollection} to specify that the
 * second type parameter is the element type:
 * <pre>
 * class MyList<T, U> extends ArrayList<U> implements GenericConvertibleCollection {
 *     @Override
 *     public int elementConversionTypeIndex() {
 *         return 1;
 *     }
 * }
 * </pre>
 *
 * <p>
 * {@link ElementTypeAwareCollection} should be used if the element type is not a parameter on the collection type at all.
 *
 * @see ElementTypeAwareCollection
 */
@SdkPublicApi
public interface GenericConvertibleCollection {
    /**
     * Return the index of the type parameter on this type that defines the element type. 0 indicates the first type parameter.
     */
    int elementConversionTypeIndex();
}
