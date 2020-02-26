/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import java.util.function.Function;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeValueType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class StaticAttributeType<T> implements AttributeType<T> {
    private final Function<T, AttributeValue> objectTransformer;
    private final Function<AttributeValue, T> attributeValueTransformer;
    private final AttributeValueType attributeValueType;

    private StaticAttributeType(Function<T, AttributeValue> objectTransformer,
                                Function<AttributeValue, T> attributeValueTransformer, AttributeValueType attributeValueType) {
        this.objectTransformer = objectTransformer;
        this.attributeValueTransformer = attributeValueTransformer;
        this.attributeValueType = attributeValueType;
    }

    public static <T> StaticAttributeType<T> create(
        Function<T, AttributeValue> objectTransformer,
        Function<AttributeValue, T> attributeValueTransformer,
        AttributeValueType attributeValueType) {

        return new StaticAttributeType<>(objectTransformer, attributeValueTransformer, attributeValueType);
    }

    @Override
    public AttributeValue objectToAttributeValue(T object) {
        return this.objectTransformer.apply(object);
    }

    @Override
    public T attributeValueToObject(AttributeValue attributeValue) {
        return this.attributeValueTransformer.apply(attributeValue);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return attributeValueType;
    }
}
