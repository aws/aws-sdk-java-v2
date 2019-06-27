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
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * Used by {@link CollectionSubtypeAttributeConverter} to determine the type of value stored in a collection, if the type isn't
 * parameterized in the collection's type.
 *
 * <p>
 * For example, with {@code class MyList extends ArrayList<Integer>}, an {@link ItemAttributeValue} cannot be converted to a
 * {@code MyList}, because the {@code CollectionSubtypeAttributeConverter} must know that the contents of the list are an
 * {@code Integer}. In this case, {@code MyList} can implement {@code ElementTypeAwareCollection} to specify that the element
 * type is an {@code Integer}:
 * <pre>
 * class MyList extends ArrayList<Integer> implements ElementTypeAwareCollection {
 *     @Override
 *     public TypeToken<Integer> elementConversionType() {
 *         return TypeToken.of(Integer.class);
 *     }
 * }
 * </pre>
 *
 * <p>
 * {@link GenericConvertibleCollection} should be used if the element type is a parameter on the collection type and there are
 * multiple type parameters.
 *
 * @see GenericConvertibleCollection
 */
@SdkPublicApi
public interface ElementTypeAwareCollection {
    /**
     * Return the specific element type stored in this collection.
     */
    TypeToken<?> elementConversionType();
}
