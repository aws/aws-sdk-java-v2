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

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ChainAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.RequestItem;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * A converter between {@link RequestItem} and {@link ItemAttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a map from String to ItemAttributeValue. This uses the
 * {@link ConversionContext#attributeConverter()} to convert the map value to an attribute value. This means that the client
 * or item must be configured with converters for the value types in the request item.
 *
 * <p>
 * This does not support reading values from DynamoDB. Consider using {@link ResponseItemSubtypeAttributeConverter} for reading
 * responses.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class RequestItemSubtypeAttributeConverter implements SubtypeAttributeConverter<RequestItem> {
    private RequestItemSubtypeAttributeConverter() {}

    public static RequestItemSubtypeAttributeConverter create() {
        return new RequestItemSubtypeAttributeConverter();
    }

    @Override
    public TypeToken<RequestItem> type() {
        return TypeToken.of(RequestItem.class);
    }

    @Override
    public ItemAttributeValue toAttributeValue(RequestItem input, ConversionContext conversionContext) {
        SubtypeAttributeConverter<Object> converter =
                ChainAttributeConverter.builder()
                                       .addConverters(input.converters())
                                       .addSubtypeConverters(input.subtypeConverters())
                                       .parent(conversionContext.attributeConverter())
                                       .build();

        ConversionContext.Builder conversionContextBuilder = conversionContext.toBuilder()
                                                                              .attributeConverter(converter);

        Map<String, ItemAttributeValue> result = new LinkedHashMap<>();
        input.attributes().forEach((key, value) -> result.put(key, toItemAttributeValue(conversionContextBuilder, key, value)));
        return ItemAttributeValue.fromMap(result);
    }

    private ItemAttributeValue toItemAttributeValue(ConversionContext.Builder contextBuilder, String key, Object value) {
        ConversionContext context = contextBuilder.attributeName(key).build();
        return context.attributeConverter().toAttributeValue(value, context);
    }

    @Override
    public <U extends RequestItem> U fromAttributeValue(ItemAttributeValue input,
                                                        TypeToken<U> desiredType,
                                                        ConversionContext context) {
        throw new UnsupportedOperationException("Cannot convert an ItemAttributeValue to a RequestItem.");
    }
}
