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

package software.amazon.awssdk.enhanced.dynamodb.converter;

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.DefaultConversionContext;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The context associated with a single {@link ItemAttributeValueConverter#fromAttributeValue} or
 * {@link ItemAttributeValueConverter#toAttributeValue} call.
 *
 * This includes helpful information that the converter can use for debugging purposes or in its implementation of the
 * converter itself. Not all converters will require information from the context.
 *
 * @see ItemAttributeValueConverter
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface ConversionContext extends ToCopyableBuilder<ConversionContext.Builder, ConversionContext> {
    /**
     * Create a builder that can be used for defining and creating a {@link ConversionContext}.
     */
    static Builder builder() {
        return DefaultConversionContext.builder();
    }

    /**
     * Retrieve the name of the attribute being converted.
     *
     * This is primarily useful for debugging purposes.
     *
     * <ul>
     *     <li>For root items, this will be empty.</li>
     *     <li>For list members, this will be the name of the list.</li>
     *     <li>For map keys and values, this will be the name of the map.</li>
     * </ul>
     */
    Optional<String> attributeName();

    /**
     * The conversion chain associated with the item being converted.
     *
     * This is useful for implementing container types, where the original conversion chain is needed to convert sub-members.
     * For example, a list converter would likely use this converter for each of the members in the list.
     */
    ItemAttributeValueConverter converter();

    /**
     * A builder for defining and creating a {@link ConversionContext}. This can be created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    interface Builder extends CopyableBuilder<ConversionContext.Builder, ConversionContext> {
        /**
         * Specify the name of the attribute being converted. This value is not required.
         *
         * @see #attributeName()
         */
        Builder attributeName(String attributeName);

        /**
         * Specify the conversion chain associated with the item being converted.
         *
         * @see #converter()
         */
        Builder converter(ItemAttributeValueConverter converter);
    }
}
