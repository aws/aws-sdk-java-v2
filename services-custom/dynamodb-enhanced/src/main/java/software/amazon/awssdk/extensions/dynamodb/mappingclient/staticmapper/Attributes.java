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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attribute.AttributeSupplier;

@SdkPublicApi
@SuppressWarnings("WeakerAccess")
public final class Attributes {
    private Attributes() {
    }

    public static <T> AttributeSupplier<T> binaryAttribute(String attributeName,
                                                           Function<T, ByteBuffer> getAttributeMethod,
                                                           BiConsumer<T, ByteBuffer> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.binaryType());
    }

    public static <T> AttributeSupplier<T> binarySetAttribute(String attributeName,
                                                              Function<T, Set<ByteBuffer>> getAttributeMethod,
                                                              BiConsumer<T, Set<ByteBuffer>> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.binarySetType());
    }

    public static <T> AttributeSupplier<T> boolAttribute(String attributeName,
                                                         Function<T, Boolean> getAttributeMethod,
                                                         BiConsumer<T, Boolean> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.booleanType());
    }

    public static <T> AttributeSupplier<T> stringAttribute(String attributeName,
                                                           Function<T, String> getAttributeMethod,
                                                           BiConsumer<T, String> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.stringType());
    }

    public static <T> AttributeSupplier<T> stringSetAttribute(String attributeName,
                                                              Function<T, Set<String>> getAttributeMethod,
                                                              BiConsumer<T, Set<String>> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.stringSetType());
    }

    public static <T> AttributeSupplier<T> integerNumberAttribute(String attributeName,
                                                                  Function<T, Integer> getAttributeMethod,
                                                                  BiConsumer<T, Integer> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.integerNumberType());
    }

    public static <T> AttributeSupplier<T> longNumberAttribute(String attributeName,
                                                               Function<T, Long> getAttributeMethod,
                                                               BiConsumer<T, Long> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.longNumberType());
    }

    public static <T> AttributeSupplier<T> shortNumberAttribute(String attributeName,
                                                                Function<T, Short> getAttributeMethod,
                                                                BiConsumer<T, Short> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.shortNumberType());
    }

    public static <T> AttributeSupplier<T> doubleNumberAttribute(String attributeName,
                                                                 Function<T, Double> getAttributeMethod,
                                                                 BiConsumer<T, Double> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.doubleNumberType());
    }

    public static <T> AttributeSupplier<T> floatNumberAttribute(String attributeName,
                                                                Function<T, Float> getAttributeMethod,
                                                                BiConsumer<T, Float> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.floatNumberType());
    }

    public static <T> AttributeSupplier<T> byteNumberAttribute(String attributeName,
                                                               Function<T, Byte> getAttributeMethod,
                                                               BiConsumer<T, Byte> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.byteNumberType());
    }

    public static <T> AttributeSupplier<T> integerSetAttribute(String attributeName,
                                                               Function<T, Set<Integer>> getAttributeMethod,
                                                               BiConsumer<T, Set<Integer>> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.integerNumberSetType());
    }

    public static <T> AttributeSupplier<T> longSetAttribute(String attributeName,
                                                            Function<T, Set<Long>> getAttributeMethod,
                                                            BiConsumer<T, Set<Long>> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.longNumberSetType());
    }

    public static <T> AttributeSupplier<T> shortSetAttribute(String attributeName,
                                                             Function<T, Set<Short>> getAttributeMethod,
                                                             BiConsumer<T, Set<Short>> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.shortNumberSetType());
    }

    public static <T> AttributeSupplier<T> doubleSetAttribute(String attributeName,
                                                              Function<T, Set<Double>> getAttributeMethod,
                                                              BiConsumer<T, Set<Double>> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.doubleNumberSetType());
    }

    public static <T> AttributeSupplier<T> floatSetAttribute(String attributeName,
                                                             Function<T, Set<Float>> getAttributeMethod,
                                                             BiConsumer<T, Set<Float>> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.floatNumberSetType());
    }

    public static <T> AttributeSupplier<T> byteSetAttribute(String attributeName,
                                                            Function<T, Set<Byte>> getAttributeMethod,
                                                            BiConsumer<T, Set<Byte>> updateItemMethod) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.byteNumberSetType());
    }

    public static <T, K> AttributeSupplier<T> documentMapAttribute(String attributeName,
                                                                   Function<T, K> getAttributeMethod,
                                                                   BiConsumer<T, K> updateItemMethod,
                                                                   TableSchema<K> documentSchema) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.documentMapType(documentSchema));
    }

    public static <T, K> AttributeSupplier<T> mapAttribute(String attributeName,
                                                           Function<T, Map<String, K>> getAttributeMethod,
                                                           BiConsumer<T, Map<String, K>> updateItemMethod,
                                                           AttributeType<K> mappedValueType) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.mapType(mappedValueType));
    }

    public static <T, K> AttributeSupplier<T> listAttribute(String attributeName,
                                                            Function<T, List<K>> getAttributeMethod,
                                                            BiConsumer<T, List<K>> updateItemMethod,
                                                            AttributeType<K> listElementsType) {
        return Attribute.create(
            attributeName,
            getAttributeMethod,
            updateItemMethod,
            AttributeTypes.listType(listElementsType));
    }
}
