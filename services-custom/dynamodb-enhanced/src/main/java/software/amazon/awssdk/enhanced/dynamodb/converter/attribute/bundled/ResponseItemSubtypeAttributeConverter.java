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
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ConvertibleItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ResponseItem;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between {@link ResponseItem} and {@link ItemAttributeValue}.
 *
 * <p>
 * This does not support writing values to DynamoDB. Consider using {@link RequestItemSubtypeAttributeConverter} for writing
 * requests.
 *
 * <p>
 * This supports reading any map from String to ItemAttributeValue. This exposes a {@link ConvertibleItemAttributeValue}
 * backed by the {@link ConversionContext#attributeConverter()}, allowing callers to convert the map value from an attribute
 * value. This means that the client or item must be configured with a converter for the value types that might be requested by
 * the customer.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ResponseItemSubtypeAttributeConverter implements SubtypeAttributeConverter<ResponseItem> {
    private ResponseItemSubtypeAttributeConverter() {}

    public static ResponseItemSubtypeAttributeConverter create() {
        return new ResponseItemSubtypeAttributeConverter();
    }

    @Override
    public TypeToken<ResponseItem> type() {
        return TypeToken.of(ResponseItem.class);
    }

    @Override
    public ItemAttributeValue toAttributeValue(ResponseItem input, ConversionContext context) {
        throw new UnsupportedOperationException("Cannot convert a ResponseItem to an ItemAttributeValue.");
    }

    @Override
    public <U extends ResponseItem> U fromAttributeValue(ItemAttributeValue input,
                                                         TypeToken<U> desiredType,
                                                         ConversionContext context) {
        Validate.isTrue(desiredType.rawClass().equals(ResponseItem.class), "Subtypes of ResponseItem are not supported.");

        ResponseItem result = input.convert(
                new TypeConvertingVisitor<ResponseItem>(ResponseItem.class, ResponseItemSubtypeAttributeConverter.class) {
                    @Override
                    public ResponseItem convertMap(Map<String, ItemAttributeValue> value) {
                        Map<String, ConvertibleItemAttributeValue> attributes = new LinkedHashMap<>();
                        value.forEach((k, v) -> attributes.put(k, toConvertibleAttribute(context, k, v)));

                        return ResponseItem.builder()
                                           .putAttributes(attributes)
                                           .build();
                    }
                });

        return desiredType.rawClass().cast(result);
    }

    private ConvertibleItemAttributeValue toConvertibleAttribute(ConversionContext context,
                                                                 String key,
                                                                 ItemAttributeValue value) {
        return context.attributeConverter()
                      .fromAttributeValue(value,
                                          TypeToken.of(ConvertibleItemAttributeValue.class),
                                          context.toBuilder().attributeName(key).build());
    }
}
