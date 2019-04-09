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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionCondition;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * A base class that simplifies the process of implementing an {@link ItemAttributeValueConverter} with the
 * {@link ConversionCondition#isInstanceOf(Class)} conversion type. This handles casting to/from the mapped type and
 * validates that the converter is being invoked with the correct types.
 */
@SdkInternalApi
@ThreadSafe
public abstract class InstanceOfConverter<T> implements ItemAttributeValueConverter {
    private final Class<T> type;

    protected InstanceOfConverter(Class<?> type) {
        this.type = (Class<T>) type;
    }

    @Override
    public final ConversionCondition defaultConversionCondition() {
        return ConversionCondition.isInstanceOf(type);
    }

    @Override
    public final ItemAttributeValue toAttributeValue(Object input, ConversionContext context) {
        T typedInput = Validate.isInstanceOf(type, input,
                                             "Input type %s could not be converted to a %s.",
                                             input.getClass(), type);

        return convertToAttributeValue(typedInput, context);
    }

    @Override
    public final Object fromAttributeValue(ItemAttributeValue input, TypeToken<?> desiredType, ConversionContext context) {
        Validate.isAssignableFrom(type, desiredType.rawClass(),
                                  "Requested type %s is not a subtype of %s.",
                                  desiredType, type);

        return convertFromAttributeValue(input, desiredType, context);
    }

    protected Class<T> type() {
        return type;
    }

    protected abstract ItemAttributeValue convertToAttributeValue(T input,
                                                                  ConversionContext conversionContext);

    protected abstract T convertFromAttributeValue(ItemAttributeValue input,
                                                   TypeToken<?> desiredType,
                                                   ConversionContext context);
}
