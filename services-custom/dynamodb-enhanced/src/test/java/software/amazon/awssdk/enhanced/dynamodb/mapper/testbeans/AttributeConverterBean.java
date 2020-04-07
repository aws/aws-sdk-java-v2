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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans;

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@DynamoDbBean
public class AttributeConverterBean {
    private String id;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    private AttributeItem attributeItem;

    @DynamoDbConvertedBy(CustomAttributeConverter.class)
    public AttributeItem getAttributeItem() {
        return attributeItem;
    }
    public void setAttributeItem(AttributeItem attributeItem) {
        this.attributeItem = attributeItem;
    }

    private Integer integerAttribute;
    public Integer getIntegerAttribute() {
        return integerAttribute;
    }
    public void setIntegerAttribute(Integer integerAttribute) {
        this.integerAttribute = integerAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeConverterBean that = (AttributeConverterBean) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(integerAttribute, that.integerAttribute) &&
                Objects.equals(attributeItem, that.attributeItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, integerAttribute, attributeItem);
    }

    public static class CustomAttributeConverter implements AttributeConverter<AttributeItem> {

        public CustomAttributeConverter() {
        }

        @Override
        public AttributeValue transformFrom(AttributeItem input) {
            return EnhancedAttributeValue.fromString(input.getInnerValue()).toAttributeValue();
        }

        @Override
        public AttributeItem transformTo(AttributeValue input) {
            return new AttributeItem(input.s());
        }

        @Override
        public EnhancedType<AttributeItem> type() {
            return EnhancedType.of(AttributeItem.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    public static class AttributeItem {
        private String innerValue;

        public AttributeItem() {
        }

        AttributeItem(String value) {
            innerValue = value;
        }

        public String getInnerValue() {
            return innerValue;
        }

        public void setInnerValue(String innerValue) {
            this.innerValue = innerValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AttributeItem that = (AttributeItem) o;
            return Objects.equals(innerValue, that.innerValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(innerValue);
        }
    }
}
