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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;

@SdkInternalApi
public class StaticKeyAttributeMetadata implements KeyAttributeMetadata {
    private final String name;
    private final AttributeValueType attributeValueType;
    private final Order order;

    private StaticKeyAttributeMetadata(String name, AttributeValueType attributeValueType, Order order) {
        this.name = name;
        this.attributeValueType = attributeValueType;
        this.order = order;
    }

    public static StaticKeyAttributeMetadata create(String name, AttributeValueType attributeValueType) {
        return new StaticKeyAttributeMetadata(name, attributeValueType, Order.UNSPECIFIED);
    }

    public static StaticKeyAttributeMetadata create(String name, AttributeValueType attributeValueType, Order order) {
        return new StaticKeyAttributeMetadata(name, attributeValueType, order);
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public AttributeValueType attributeValueType() {
        return this.attributeValueType;
    }

    @Override
    public Order order() {
        return this.order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StaticKeyAttributeMetadata staticKey = (StaticKeyAttributeMetadata) o;

        if (name != null ? !name.equals(staticKey.name) : staticKey.name != null) {
            return false;
        }
        if (attributeValueType != staticKey.attributeValueType) {
            return false;
        }
        return order == staticKey.order;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (attributeValueType != null ? attributeValueType.hashCode() : 0);
        result = 31 * result + order.hashCode();
        return result;
    }
}
