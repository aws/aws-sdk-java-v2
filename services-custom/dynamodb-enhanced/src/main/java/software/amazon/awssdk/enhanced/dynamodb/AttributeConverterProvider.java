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

package software.amazon.awssdk.enhanced.dynamodb;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterProviderResolver;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Interface for determining the {@link AttributeConverter} to use for
 * converting a given {@link EnhancedType}.
 */
@SdkPublicApi
public interface AttributeConverterProvider {

    /**
     * Finds a {@link AttributeConverter} for converting an object with a type
     * specified by a {@link EnhancedType} to a {@link AttributeValue} and back.
     *
     * @param enhancedType The type of the object to be converted
     * @return {@link AttributeConverter} for converting the given type.
     */
    <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType);

    /**
     * Returns a default implementation of AttributeConverterProvider with all
     * standard Java type converters included.
     */
    static AttributeConverterProvider defaultProvider() {
        return ConverterProviderResolver.defaultConverterProvider();
    }
}
