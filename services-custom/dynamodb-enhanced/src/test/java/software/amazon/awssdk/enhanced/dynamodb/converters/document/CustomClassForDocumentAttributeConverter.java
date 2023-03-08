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

package software.amazon.awssdk.enhanced.dynamodb.converters.document;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigDecimalAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class CustomClassForDocumentAttributeConverter implements AttributeConverter<CustomClassForDocumentAPI> {

    final static Integer DEFAULT_INCREMENT = 10;

    @Override
    public AttributeValue transformFrom(CustomClassForDocumentAPI input) {

        if(input == null){
            return null;
        }
        Map<String, AttributeValue> attributeValueMap = new LinkedHashMap<>();
        // Maintain the Alphabetical Order ,so that expected json matches
        if(input.booleanSet() != null){
            attributeValueMap.put("booleanSet",
                                  AttributeValue.fromL(input.booleanSet().stream().map(b -> AttributeValue.fromBool(b)).collect(Collectors.toList())));
        }
        if(input.customClassList() != null){
            attributeValueMap.put("customClassList", convertCustomList(input.customClassList()));
        }
        if (input.innerCustomClass() != null){
            attributeValueMap.put("innerCustomClass", transformFrom(input.innerCustomClass()));
        }
        if(input.instantList() != null){
            attributeValueMap.put("instantList", convertInstantList(input.instantList()));
        }
        if(input.longNumber() != null){
            attributeValueMap.put("longNumber", AttributeValue.fromN(input.longNumber().toString()));
        }
        if(input.string() != null){
            attributeValueMap.put("string", AttributeValue.fromS(input.string()));
        }
        if(input.stringSet() != null){
            attributeValueMap.put("stringSet", AttributeValue.fromSs(input.stringSet().stream().collect(Collectors.toList())));
        }
        if(input.bigDecimalSet() != null){
            attributeValueMap.put("stringSet",
                                  AttributeValue.fromNs(input.bigDecimalSet().stream().map(b -> b.toString()).collect(Collectors.toList())));
        }
        return EnhancedAttributeValue.fromMap(attributeValueMap).toAttributeValue();
    }


    private static AttributeValue convertCustomList(List<CustomClassForDocumentAPI> customClassForDocumentAPIList){
        List<AttributeValue> convertCustomList =
            customClassForDocumentAPIList.stream().map(customClassForDocumentAPI -> create().transformFrom(customClassForDocumentAPI)).collect(Collectors.toList());
        return AttributeValue.fromL(convertCustomList);

    }

    private static AttributeValue convertInstantList(List<Instant> customClassForDocumentAPIList){
        return ListAttributeConverter.create(InstantAsStringAttributeConverter.create()).transformFrom(customClassForDocumentAPIList);
    }

    @Override
    public CustomClassForDocumentAPI transformTo(AttributeValue input) {

        Map<String, AttributeValue> customAttr = input.m();

        CustomClassForDocumentAPI.Builder builder = CustomClassForDocumentAPI.builder();

        if (customAttr.get("aBoolean") != null) {
            builder.aBoolean(BooleanAttributeConverter.create().transformTo(customAttr.get("aBoolean")));
        }
        if (customAttr.get("bigDecimal") != null) {
            builder.bigDecimal(BigDecimalAttributeConverter.create().transformTo(customAttr.get("bigDecimal")));
        }
        if (customAttr.get("bigDecimalSet") != null) {
            builder.bigDecimalSet(SetAttributeConverter.setConverter(BigDecimalAttributeConverter.create()).transformTo(customAttr.get("bigDecimalSet")));
        }
        if (customAttr.get("binarySet") != null) {
            builder.binarySet(SetAttributeConverter.setConverter(ByteArrayAttributeConverter.create()).transformTo(customAttr.get("binarySet")));
        }
        if (customAttr.get("binary") != null) {
            builder.binary(SdkBytes.fromByteArray(ByteArrayAttributeConverter.create().transformTo(customAttr.get("binary"))));
        }
        if (customAttr.get("booleanSet") != null) {
            builder.booleanSet(SetAttributeConverter.setConverter(BooleanAttributeConverter.create()).transformTo(customAttr.get(
                "booleanSet")));
        }
        if (customAttr.get("customClassList") != null) {
            builder.customClassList(ListAttributeConverter.create(create()).transformTo(customAttr.get("customClassList")));
        }
        if (customAttr.get("instantList") != null) {
            builder.instantList(ListAttributeConverter.create(AttributeConverterProvider.defaultProvider().converterFor(EnhancedType.of(Instant.class))).transformTo(customAttr.get(
                "instantList")));
        }
        if (customAttr.get("innerCustomClass") != null) {
            builder.innerCustomClass(create().transformTo(customAttr.get("innerCustomClass")));
        }
        if (customAttr.get("longNumber") != null) {
            builder.longNumber(LongAttributeConverter.create().transformTo(customAttr.get("longNumber")));
        }
        if (customAttr.get("longSet") != null) {
            builder.longSet(SetAttributeConverter.setConverter(LongAttributeConverter.create()).transformTo(customAttr.get(
                "longSet")));
        }
        if (customAttr.get("string") != null) {
            builder.string(StringAttributeConverter.create().transformTo(customAttr.get("string")));
        }
        if (customAttr.get("stringSet") != null) {
            builder.stringSet(SetAttributeConverter.setConverter(StringAttributeConverter.create()).transformTo(customAttr.get(
                "stringSet")));
        }
        return builder.build();
    }

    public static CustomClassForDocumentAttributeConverter create() {
        return new CustomClassForDocumentAttributeConverter();
    }

    @Override
    public EnhancedType<CustomClassForDocumentAPI> type() {
        return EnhancedType.of(CustomClassForDocumentAPI.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }


}
