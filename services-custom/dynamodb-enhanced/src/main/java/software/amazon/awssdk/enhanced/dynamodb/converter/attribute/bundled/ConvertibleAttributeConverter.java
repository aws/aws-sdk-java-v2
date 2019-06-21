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
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.model.DefaultConvertibleItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ConvertibleItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * Converter between {@link ItemAttributeValue} and {@link ConvertibleItemAttributeValue}, allowing type conversion to be
 * deferred to the SDK user.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class ConvertibleAttributeConverter implements SubtypeAttributeConverter<ConvertibleItemAttributeValue> {
    private static final TypeToken<ConvertibleItemAttributeValue> TYPE = TypeToken.of(ConvertibleItemAttributeValue.class);

    private ConvertibleAttributeConverter() {}

    public static ConvertibleAttributeConverter create() {
        return new ConvertibleAttributeConverter();
    }

    @Override
    public TypeToken<ConvertibleItemAttributeValue> type() {
        return TYPE;
    }

    @Override
    public ItemAttributeValue toAttributeValue(ConvertibleItemAttributeValue input, ConversionContext context) {
        return input.attributeValue();
    }

    @Override
    public <U extends ConvertibleItemAttributeValue> U fromAttributeValue(ItemAttributeValue input,
                                                                          TypeToken<U> desiredType,
                                                                          ConversionContext context) {
        DefaultConvertibleItemAttributeValue result =
                DefaultConvertibleItemAttributeValue.builder()
                                                    .conversionContext(context)
                                                    .attributeValue(input)
                                                    .build();

        return Validate.isInstanceOf(desiredType.rawClass(),
                                     result,
                                     "Subtypes of ConvertibleItemAttributeValue are not supported, but %s was requested.",
                                     desiredType);
    }
}
