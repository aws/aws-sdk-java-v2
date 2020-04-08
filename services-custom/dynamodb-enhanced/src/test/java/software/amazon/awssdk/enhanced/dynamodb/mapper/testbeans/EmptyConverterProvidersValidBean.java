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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.IntegerStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@DynamoDbBean(converterProviders = {})
public class EmptyConverterProvidersValidBean {
    private String id;
    private Integer integerAttribute;

    @DynamoDbPartitionKey
    @DynamoDbConvertedBy(CustomStringAttributeConverter.class)
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbConvertedBy(CustomIntegerAttributeConverter.class)
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
        EmptyConverterProvidersValidBean that = (EmptyConverterProvidersValidBean) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(integerAttribute, that.integerAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, integerAttribute);
    }

    public static class CustomStringAttributeConverter implements AttributeConverter<String> {
        final static String DEFAULT_SUFFIX = "-custom";

        public CustomStringAttributeConverter() {
        }

        @Override
        public AttributeValue transformFrom(String input) {
            return EnhancedAttributeValue.fromString(input + DEFAULT_SUFFIX).toAttributeValue();
        }

        @Override
        public String transformTo(AttributeValue input) {
            return input.s();
        }

        @Override
        public EnhancedType<String> type() {
            return EnhancedType.of(String.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    public static class CustomIntegerAttributeConverter implements AttributeConverter<Integer> {
        final static Integer DEFAULT_INCREMENT = 10;

        public CustomIntegerAttributeConverter() {
        }

        @Override
        public AttributeValue transformFrom(Integer input) {
            return EnhancedAttributeValue.fromNumber(IntegerStringConverter.create().toString(input + DEFAULT_INCREMENT))
                    .toAttributeValue();
        }

        @Override
        public Integer transformTo(AttributeValue input) {
            return Integer.valueOf(input.n());
        }

        @Override
        public EnhancedType<Integer> type() {
            return EnhancedType.of(Integer.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.N;
        }
    }
}
