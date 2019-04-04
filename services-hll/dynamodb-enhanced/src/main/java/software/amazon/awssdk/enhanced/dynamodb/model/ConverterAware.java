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

import java.util.Collection;
import java.util.List;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.converter.DefaultConverterChain;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;

/**
 * An interface applied to all objects that wish to expose their underlying {@link ItemAttributeValueConverter}s.
 *
 * See {@link ItemAttributeValueConverter} for a detailed explanation of how the enhanced client converts between Java types
 * and DynamoDB types.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface ConverterAware {
    /**
     * Retrieve all converters that were directly configured on this object.
     */
    List<ItemAttributeValueConverter> converters();

    /**
     * An interface applied to all objects that can be configured with {@link ItemAttributeValueConverter}s.
     *
     * See {@link ItemAttributeValueConverter} for a detailed explanation of how the enhanced client converts between Java types
     * and DynamoDB types.
     */
    @NotThreadSafe
    interface Builder {
        /**
         * Add all of the provided converters to this builder, in the order of the provided collection.
         *
         * Converters earlier in the provided list take precedence over the ones later in the list, even if the later ones
         * refer to a more specific type. Converters should be added in an order from most-specific to least-specific.
         *
         * Converters configured in {@link RequestItem.Builder} always take precedence over the ones configured in
         * {@link DynamoDbEnhancedClient.Builder}.
         *
         * Converters configured in {@link DynamoDbEnhancedClient.Builder} always take precedence over the ones provided by the
         * {@link DefaultConverterChain}.
         *
         * @see ItemAttributeValueConverter
         */
        Builder addConverters(Collection<? extends ItemAttributeValueConverter> converters);

        /**
         * Add a converter to this builder.
         *
         * Converters added earlier take precedence over the ones added later, even if the later ones refer to
         * a more specific type. Converters should be added in an order from most-specific to least-specific.
         *
         * Converters configured in {@link RequestItem.Builder} always take precedence over the ones configured in
         * {@link DynamoDbEnhancedClient.Builder}.
         *
         * Converters configured in {@link DynamoDbEnhancedClient.Builder} always take precedence over the ones provided by the
         * {@link DefaultConverterChain}.
         */
        Builder addConverter(ItemAttributeValueConverter converter);

        /**
         * Reset the converters that were previously added with {@link #addConverters(Collection)} or
         * {@link #addConverter(ItemAttributeValueConverter)}.
         *
         * This <b>does not</b> reset converters configured elsewhere. Converters configured in other locations, such as in the
         * {@link DefaultConverterChain}, will still be used.
         */
        Builder clearConverters();
    }
}
