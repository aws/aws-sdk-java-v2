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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * Identity converter, allowing direct use of an {@link ItemAttributeValue} without any type conversion.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class AttributeAttributeConverter implements AttributeConverter<ItemAttributeValue> {
    private AttributeAttributeConverter() {}

    public static AttributeAttributeConverter create() {
        return new AttributeAttributeConverter();
    }

    @Override
    public TypeToken<ItemAttributeValue> type() {
        return TypeToken.of(ItemAttributeValue.class);
    }

    @Override
    public ItemAttributeValue toAttributeValue(ItemAttributeValue input, ConversionContext context) {
        return input;
    }

    @Override
    public ItemAttributeValue fromAttributeValue(ItemAttributeValue input,
                                                 ConversionContext context) {
        return input;
    }
}
