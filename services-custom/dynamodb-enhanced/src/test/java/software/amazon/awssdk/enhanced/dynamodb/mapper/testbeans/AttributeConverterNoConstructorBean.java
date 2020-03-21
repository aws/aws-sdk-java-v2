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

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@DynamoDbBean
public class AttributeConverterNoConstructorBean extends AbstractBean {

    private String id;

    @DynamoDbConvertedBy(AttributeConverterNoConstructorBean.CustomAttributeConverter.class)
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public static class CustomAttributeConverter implements AttributeConverter {

        private CustomAttributeConverter() {
        }

        @Override
        public AttributeValue transformFrom(Object input) {
            return null;
        }

        @Override
        public Object transformTo(AttributeValue input) {
            return null;
        }

        @Override
        public EnhancedType type() {
            return null;
        }

        @Override
        public AttributeValueType attributeValueType() {
            return null;
        }
    }
}
