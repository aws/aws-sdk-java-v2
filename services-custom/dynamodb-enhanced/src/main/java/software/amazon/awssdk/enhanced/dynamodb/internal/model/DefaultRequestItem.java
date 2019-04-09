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

package software.amazon.awssdk.enhanced.dynamodb.internal.model;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.GeneratedRequestItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.RequestItem;

/**
 * The default implementation of {@link RequestItem}.
 */
@SdkInternalApi
@ThreadSafe
public class DefaultRequestItem extends DefaultItem<Object> implements RequestItem {
    private DefaultRequestItem(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public GeneratedRequestItem toGeneratedRequestItem() {
        ItemAttributeValue itemAttributeValue = converterChain.toAttributeValue(this, ConversionContext.builder()
                                                                                                       .converter(converterChain)
                                                                                                       .build());
        return GeneratedRequestItem.builder()
                                   .putAttributes(itemAttributeValue.toGeneratedItem())
                                   .build();
    }

    @Override
    public DefaultRequestItem.Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder extends DefaultItem.Builder<Object, Builder> implements RequestItem.Builder {
        private Builder() {}

        private Builder(DefaultRequestItem item) {
            super(item);
        }

        @Override
        public DefaultRequestItem build() {
            return new DefaultRequestItem(this);
        }
    }
}
