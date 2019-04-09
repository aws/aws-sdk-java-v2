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
import software.amazon.awssdk.enhanced.dynamodb.model.GeneratedResponseItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ResponseItem;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * The default implementation of {@link GeneratedResponseItem}.
 */
@SdkInternalApi
@ThreadSafe
public class DefaultGeneratedResponseItem extends DefaultItem<AttributeValue> implements GeneratedResponseItem {
    private DefaultGeneratedResponseItem(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ResponseItem toResponseItem() {
        ItemAttributeValue attributeValue = ItemAttributeValue.fromGeneratedItem(attributes());
        Object result = converterChain.fromAttributeValue(attributeValue,
                                                          TypeToken.from(ResponseItem.class),
                                                          ConversionContext.builder().converter(converterChain).build());
        return Validate.isInstanceOf(ResponseItem.class, result, "Conversion chain did not generated a ResponseItem.");
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder
            extends DefaultItem.Builder<AttributeValue, Builder>
            implements GeneratedResponseItem.Builder {
        private Builder() {}

        private Builder(DefaultGeneratedResponseItem item) {
            super(item);
        }

        @Override
        public DefaultGeneratedResponseItem build() {
            return new DefaultGeneratedResponseItem(this);
        }
    }
}
