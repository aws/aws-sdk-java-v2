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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.document.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigDecimalAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.IntegerStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class CustomClassAttributeConverter implements AttributeConverter<CustomClass> {

    final static Integer DEFAULT_INCREMENT = 10;

    @Override
    public AttributeValue transformFrom(CustomClass input) {

        if(input == null){
            return null;
        }
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();

        if(input.foo() != null){
            attributeValueMap.put("foo", AttributeValue.fromS(input.foo()));
        }

        if(input.stringSet() != null){
            attributeValueMap.put("stringSet", AttributeValue.fromSs(input.stringSet().stream().collect(Collectors.toList())));
        }

        if(input.booleanSet() != null){
            attributeValueMap.put("booleanSet",
                                  AttributeValue.fromL(input.booleanSet().stream().map(b -> AttributeValue.fromBool(b)).collect(Collectors.toList())));
        }

        if(input.bigDecimalSet() != null){
            attributeValueMap.put("stringSet",
                                  AttributeValue.fromNs(input.bigDecimalSet().stream().map(b -> b.toString()).collect(Collectors.toList())));
        }

        if(input.customClassList() != null){
            attributeValueMap.put("customClassList", convertCustomList(input.customClassList()));
        }

        if (input.innerCustomClass() != null){
            attributeValueMap.put("innerCustomClass", transformFrom(input.innerCustomClass()));
        }
        return EnhancedAttributeValue.fromMap(attributeValueMap).toAttributeValue();
    }


    private static AttributeValue convertCustomList(List<CustomClass> customClassList){
        List<AttributeValue> convertCustomList =
            customClassList.stream().map(customClass -> create().transformFrom(customClass)).collect(Collectors.toList());
        return AttributeValue.fromL(convertCustomList);

    }

    @Override
    public CustomClass transformTo(AttributeValue input) {

        Map<String, AttributeValue> customAttr = input.m();

        CustomClass.Builder builder = CustomClass.builder();
        builder.foo(StringAttributeConverter.create().transformTo(customAttr.get("foo")));
        builder.stringSet(SetAttributeConverter.setConverter(StringAttributeConverter.create()).transformTo(customAttr.get("stringSet")));
        builder.binary(SdkBytes.fromByteArray(ByteArrayAttributeConverter.create().transformTo(customAttr.get("binary"))));

        builder.binarySet(SetAttributeConverter.setConverter(ByteArrayAttributeConverter.create()).transformTo(customAttr.get("binarySet")));

        builder.aBoolean(BooleanAttributeConverter.create().transformTo(customAttr.get("aBoolean")));
        builder.booleanSet(SetAttributeConverter.setConverter(BooleanAttributeConverter.create()).transformTo(customAttr.get(
            "booleanSet")));

        builder.longNumber(LongAttributeConverter.create().transformTo(customAttr.get("longNumber")));
        builder.longSet(SetAttributeConverter.setConverter(LongAttributeConverter.create()).transformTo(customAttr.get("longSet")));

        builder.bigDecimal(BigDecimalAttributeConverter.create().transformTo(customAttr.get("bigDecimal")));
        builder.bigDecimalSet(SetAttributeConverter.setConverter(BigDecimalAttributeConverter.create()).transformTo(customAttr.get("bigDecimalSet")));

        builder.customClassList(ListAttributeConverter.create(create()).transformTo(customAttr.get("customClassList")));
        builder.innerCustomClass(create().transformTo(customAttr.get("innerCustomClass")));

        return builder.build();
    }

    public static CustomClassAttributeConverter create() {
        return new CustomClassAttributeConverter();
    }

    @Override
    public EnhancedType<CustomClass> type() {
        return EnhancedType.of(CustomClass.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }


}
