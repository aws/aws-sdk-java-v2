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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper;

import java.util.function.Function;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.AttributeValueType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkPublicApi
public class AttributeType<T> {
    private final Function<T, AttributeValue> objectTransformer;
    private final Function<AttributeValue, T> attributeValueTransformer;
    private final AttributeValueType attributeValueType;

    private AttributeType(Function<T, AttributeValue> objectTransformer,
                          Function<AttributeValue, T> attributeValueTransformer, AttributeValueType attributeValueType) {
        this.objectTransformer = objectTransformer;
        this.attributeValueTransformer = attributeValueTransformer;
        this.attributeValueType = attributeValueType;
    }

    public static <T> AttributeType<T> create(
        Function<T, AttributeValue> objectTransformer,
        Function<AttributeValue, T> attributeValueTransformer,
        AttributeValueType attributeValueType) {

        return new AttributeType<>(objectTransformer, attributeValueTransformer, attributeValueType);
    }

    public AttributeValue objectToAttributeValue(T object) {
        return this.objectTransformer.apply(object);
    }

    public T attributeValueToObject(AttributeValue attributeValue) {
        return this.attributeValueTransformer.apply(attributeValue);
    }

    AttributeValueType attributeValueType() {
        return attributeValueType;
    }
}
