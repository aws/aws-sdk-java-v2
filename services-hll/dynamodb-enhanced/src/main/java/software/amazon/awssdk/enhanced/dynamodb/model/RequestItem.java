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
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Table;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.model.DefaultRequestItem;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An item that can be sent to DynamoDB. An item is a single, unique entry in a DynamoDB table.
 *
 * A {@code RequestItem} is a {@code Map<String, Object>} that can be converted into a {@code Map<String, ItemAttributeValue>}
 * using the configured {@link #converters()}.
 *
 * @see Table
 * @see AsyncTable
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface RequestItem extends ConverterAware,
                                     AttributeAware<Object>,
                                     ToCopyableBuilder<RequestItem.Builder, RequestItem> {
    /**
     * Create a builder that can be used for configuring and creating a {@link RequestItem}.
     */
    static Builder builder() {
        return DefaultRequestItem.builder();
    }

    /**
     * Convert this request item into a {@link GeneratedRequestItem} using the converters attached to this item, only.
     *
     * This will not use the default converter chain or any converters associated with the client unless they were explicitly
     * added via {@link Builder#addConverter(ItemAttributeValueConverter)} (or similar methods).
     */
    GeneratedRequestItem toGeneratedRequestItem();

    /**
     * A builder that can be used for configuring and creating a {@link RequestItem}.
     */
    @NotThreadSafe
    interface Builder extends ConverterAware.Builder,
                              AttributeAware.Builder<Object>,
                              CopyableBuilder<RequestItem.Builder, RequestItem> {
        @Override
        Builder addConverters(Collection<? extends ItemAttributeValueConverter> converters);

        @Override
        Builder addConverter(ItemAttributeValueConverter converter);

        @Override
        Builder clearConverters();

        @Override
        Builder putAttributes(Map<String, Object> attributeValues);

        @Override
        Builder putAttribute(String attributeKey, Object attributeValue);

        /**
         * Adds a sub-item to this request.
         *
         * This is a simpler form of {@link #putAttribute(String, Object)}, avoiding the need to call
         * {@code RequestItem.builder()} and {@code .build()}.
         *
         * Usage example:
         * <code>
         *     requestItem.putAttribute("countryCodes", c -> c.putAttribute("US", "United States of America")
         *                                                    .putAttribute("GB", "United Kingdom"));
         * </code>
         */
        default Builder putAttribute(String attributeKey, Consumer<RequestItem.Builder> subItemAttribute) {
            RequestItem.Builder requestItemBuilder = RequestItem.builder();
            subItemAttribute.accept(requestItemBuilder);
            return putAttribute(attributeKey, requestItemBuilder.build());
        }

        @Override
        Builder removeAttribute(String attributeKey);

        @Override
        Builder clearAttributes();

        @Override
        RequestItem build();
    }
}
