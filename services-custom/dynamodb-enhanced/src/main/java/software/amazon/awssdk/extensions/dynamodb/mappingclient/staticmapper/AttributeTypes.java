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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.AttributeValueType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkPublicApi
public final class AttributeTypes {
    private AttributeTypes() {

    }

    public static AttributeType<Boolean> booleanType() {
        return AttributeType.create(obj -> AttributeValue.builder().bool(obj).build(),
                                AttributeValue::bool,
                                AttributeValueType.BOOL);
    }

    public static AttributeType<String> stringType() {
        return AttributeType.create(obj -> AttributeValue.builder().s(obj).build(),
                                AttributeValue::s,
                                AttributeValueType.S);
    }

    public static AttributeType<Integer> integerNumberType() {
        return numberType(Integer::parseInt);
    }

    public static AttributeType<Long> longNumberType() {
        return numberType(Long::parseLong);
    }

    public static AttributeType<Short> shortNumberType() {
        return numberType(Short::parseShort);
    }

    public static AttributeType<Byte> byteNumberType() {
        return numberType(Byte::parseByte);
    }

    public static AttributeType<Double> doubleNumberType() {
        return numberType(Double::parseDouble);
    }

    public static AttributeType<Float> floatNumberType() {
        return numberType(Float::parseFloat);
    }

    public static AttributeType<Set<Integer>> integerNumberSetType() {
        return numberSetType(Integer::parseInt);
    }

    public static AttributeType<Set<Long>> longNumberSetType() {
        return numberSetType(Long::parseLong);
    }

    public static AttributeType<Set<Short>> shortNumberSetType() {
        return numberSetType(Short::parseShort);
    }

    public static AttributeType<Set<Byte>> byteNumberSetType() {
        return numberSetType(Byte::parseByte);
    }

    public static AttributeType<Set<Double>> doubleNumberSetType() {
        return numberSetType(Double::parseDouble);
    }

    public static AttributeType<Set<Float>> floatNumberSetType() {
        return numberSetType(Float::parseFloat);
    }

    public static AttributeType<ByteBuffer> binaryType() {
        return AttributeType.create(byteBuffer -> AttributeValue.builder().b(SdkBytes.fromByteBuffer(byteBuffer)).build(),
            attributeValue -> attributeValue.b().asByteBuffer(),
            AttributeValueType.B);
    }

    public static AttributeType<Set<ByteBuffer>> binarySetType() {
        return AttributeType.create(
            bbSet -> AttributeValue.builder()
                                   .bs(bbSet.stream().map(SdkBytes::fromByteBuffer).collect(Collectors.toList()))
                                   .build(),
            attributeValue -> attributeValue.bs().stream().map(SdkBytes::asByteBuffer).collect(Collectors.toSet()),
            AttributeValueType.BS);
    }

    public static AttributeType<Set<String>> stringSetType() {
        return AttributeType.create(stringSet -> AttributeValue.builder().ss(stringSet).build(),
            attributeValue -> Collections.unmodifiableSet(new HashSet<>(attributeValue.ss())),
            AttributeValueType.SS);
    }

    public static <T> AttributeType<List<T>> listType(AttributeType<T> elementType) {
        return AttributeType.create(
            list -> AttributeValue.builder()
                                  .l(list.stream()
                                         .map(elementType::objectToAttributeValue)
                                         .collect(Collectors.toList()))
                                  .build(),
            attributeValue -> attributeValue.l()
                                            .stream()
                                            .map(elementType::attributeValueToObject)
                                            .collect(Collectors.toList()),
            AttributeValueType.L);
    }

    public static <T> AttributeType<Map<String, T>> mapType(AttributeType<T> mappedValueType) {
        return AttributeType.create(
            map -> AttributeValue.builder()
                                 .m(map.entrySet()
                                       .stream()
                                       .collect(Collectors.toMap(
                                           Map.Entry::getKey,
                                           entry -> mappedValueType.objectToAttributeValue(entry.getValue()))))
                                 .build(),
            attributeValue -> attributeValue.m()
                                            .entrySet()
                                            .stream()
                                            .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> mappedValueType.attributeValueToObject(entry.getValue()))),
            AttributeValueType.M);
    }

    public static <T> AttributeType<T> documentMapType(TableSchema<T> documentSchema) {
        return AttributeType.create(
            document -> AttributeValue.builder().m(documentSchema.itemToMap(document, false)).build(),
            attributeValue -> documentSchema.mapToItem(attributeValue.m()),
            AttributeValueType.M);
    }

    public static <T extends Number> AttributeType<T> numberType(Function<String, T> stringToNumber) {
        return AttributeType.create(obj -> AttributeValue.builder().n(obj.toString()).build(),
            attributeValue -> stringToNumber.apply(attributeValue.n()),
            AttributeValueType.N);
    }

    private static <T extends Number> AttributeType<Set<T>> numberSetType(Function<String, T> stringToNumber) {
        return AttributeType.create(
            nSet -> AttributeValue.builder()
                                  .ns(nSet.stream().map(Number::toString).collect(Collectors.toList()))
                                  .build(),
            attributeValue -> attributeValue.ns().stream().map(stringToNumber).collect(Collectors.toSet()),
            AttributeValueType.NS);
    }
}
