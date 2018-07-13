/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.model;

import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.utils.CollectionUtils.toMap;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Used to build a DynamoDB item map.
 *
 * For example to pass to a {@link PutItemRequest.Builder#item(Map)}.
 */
@ReviewBeforeRelease("May want to do this a different way")
public final class Item extends HashMap<String, AttributeValue> {

    private Item(Builder builder) {
        putAll(builder.item);
    }

    /**
     * Create a new instance of the {@link Builder}.
     *
     * @return a new instance of the {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements SdkBuilder<Builder, Item> {
        private final Map<String, AttributeValue> item = new HashMap<>();

        private Builder() {
        }

        /**
         * Build a {@link Map} representing a DyanmoDB item.
         *
         * @return a {@link Map} representing a DyanmoDB item
         */
        @Override
        public Item build() {
            return new Item(this);
        }

        /**
         * Adds an {@link AttributeValue} representing a String to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().s(stringValue).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param stringValue the string value of the attribute
         * @return the builder for method chaining
         */
        public Builder attribute(String key, String stringValue) {
            item.put(key, AttributeValue.builder().s(stringValue).build());
            return this;
        }

        /**
         * Adds an {@link AttributeValue} representing a Boolean to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().bool(booleanValue).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param booleanValue the boolean value of the attribute
         * @return the builder for method chaining
         */
        public Builder attribute(String key, Boolean booleanValue) {
            item.put(key, AttributeValue.builder().bool(booleanValue).build());
            return this;
        }

        /**
         * Adds an {@link AttributeValue} representing a Number to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().n(String.valueOf(numericValue)).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param numericValue the numeric value of the attribute
         * @return the builder for method chaining
         */
        public Builder attribute(String key, Number numericValue) {
            item.put(key, AttributeValue.builder().n(String.valueOf(numericValue)).build());
            return this;
        }

        /**
         * Adds an {@link AttributeValue} representing binary data to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().b(ByteBuffer.wrap(binaryValue)).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param binaryValue the binary value of the attribute
         * @return the builder for method chaining
         */
        public Builder attribute(String key, byte[] binaryValue) {
            return attribute(key, ByteBuffer.wrap(binaryValue));
        }

        /**
         * Adds an {@link AttributeValue} representing binary data to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().b(binaryValue).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param binaryValue the binary value of the attribute
         * @return the builder for method chaining
         */
        public Builder attribute(String key, ByteBuffer binaryValue) {
            item.put(key, AttributeValue.builder().b(SdkBytes.fromByteBuffer(binaryValue)).build());
            return this;
        }

        /**
         * Adds an {@link AttributeValue} representing a list of AttributeValues to the item with the specified key.
         *
         * This will attempt to infer the of of the {@link AttributeValue} for each given {@link Object} in the list
         * based on the type. Supported types are:
         * <ul>
         * <li><strong>{@link AttributeValue}</strong> which is unaltered</li>
         * <li><strong>{@link String}</strong> becomes {@link AttributeValue#s()}</li>
         * <li><strong>{@link Number}</strong> (and anything that can be automatically boxed to {@link Number} including
         * int, long, float etc.) becomes {@link AttributeValue#n()}</li>
         * <li><strong>{@link Boolean}</strong> (and bool) becomes {@link AttributeValue#bool()}</li>
         * <li><strong>byte[]</strong> becomes {@link AttributeValue#b()}</li>
         * <li><strong>{@link ByteBuffer}</strong> becomes {@link AttributeValue#b()}</li>
         * <li><strong>{@link List}&lt;Object&gt;</strong> (where the containing objects are one of the types in
         * this list) becomes {@link AttributeValue#l()}</li>
         * <li><strong>{@link Map}&lt;String, Object&gt;</strong> (where the containing object values are one of the types
         * in this list) becomes {@link AttributeValue#m()}</li>
         * </ul>
         *
         * @param key the key of this attribute
         * @param values the object values
         * @return the builder for method chaining
         */
        public Builder attribute(String key, List<?> values) {
            item.put(key, fromObject(values));
            return this;
        }

        /**
         * Adds an {@link AttributeValue} representing a map of string to AttributeValues to the item with the specified key.
         *
         * This will attempt to infer the most appropriate {@link AttributeValue} for each given {@link Object} value in the map
         * based on the type. Supported types are:
         * <ul>
         * <li><strong>{@link AttributeValue}</strong> which is unaltered</li>
         * <li><strong>{@link String}</strong> becomes {@link AttributeValue#s()}</li>
         * <li><strong>{@link Number}</strong> (and anything that can be automatically boxed to {@link Number} including
         * int, long, float etc.) becomes {@link AttributeValue#n()}</li>
         * <li><strong>{@link Boolean}</strong> (and bool) becomes {@link AttributeValue#bool()}</li>
         * <li><strong>byte[]</strong> becomes {@link AttributeValue#b()}</li>
         * <li><strong>{@link ByteBuffer}</strong> becomes {@link AttributeValue#b()}</li>
         * <li><strong>{@link List}&lt;Object&gt;</strong> (where the containing objects are one of the types in
         * this list) becomes {@link AttributeValue#l()}</li>
         * <li><strong>{@link Map}&lt;String, Object&gt;</strong> (where the containing object values are one of the types
         * in this list) becomes {@link AttributeValue#m()}</li>
         * </ul>
         *
         * @param key the key of this attribute
         * @param values the map of key to object
         * @return the builder for method chaining
         */
        public Builder attribute(String key, Map<String, ?> values) {
            item.put(key, fromObject(values));
            return this;
        }

        /**
         * Adds an {@link AttributeValue} representing a list of strings to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().ss(stringValue1, stringValue2, ...).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param stringValues the string values of the attribute
         * @return the builder for method chaining
         */
        public Builder strings(String key, String... stringValues) {
            return strings(key, Arrays.asList(stringValues));
        }

        /**
         * Adds an {@link AttributeValue} representing a list of strings to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().ss(stringValues).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param stringValues the string values of the attribute
         * @return the builder for method chaining
         */
        public Builder strings(String key, Collection<String> stringValues) {
            item.put(key, AttributeValue.builder().ss(stringValues).build());
            return this;
        }

        /**
         * Adds an {@link AttributeValue} representing a list of numbers to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().ns(numberValues1, numberValues2, ...).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param numberValues the number values of the attribute
         * @return the builder for method chaining
         */
        public Builder numbers(String key, Number... numberValues) {
            return numbers(key, Arrays.asList(numberValues));
        }

        /**
         * Adds an {@link AttributeValue} representing a list of numbers to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().ns(numberValues).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param numberValues the number values of the attribute
         * @return the builder for method chaining
         */
        public Builder numbers(String key, Collection<? extends Number> numberValues) {
            item.put(key, AttributeValue.builder().ns(numberValues.stream().map(String::valueOf).collect(toList())).build());
            return this;
        }

        /**
         * Adds an {@link AttributeValue} representing a list of binary data to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().bs(Arrays.stream(byteArrays)
         *                                                    .map(ByteBuffer::wrap)
         *                                                    .collect(Collectors.toList())).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param byteArrays the binary values of the attribute
         * @return the builder for method chaining
         */
        public Builder byteArrays(String key, byte[]... byteArrays) {
            return byteArrays(key, Arrays.asList(byteArrays));
        }

        /**
         * Adds an {@link AttributeValue} representing a list of binary data to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().bs(byteArrays.stream()
         *                                                        .map(ByteBuffer::wrap)
         *                                                        .collect(Collectors.toList())).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param byteArrays the binary values of the attribute
         * @return the builder for method chaining
         */
        public Builder byteArrays(String key, Collection<byte[]> byteArrays) {
            return byteBuffers(key, byteArrays.stream().map(ByteBuffer::wrap).collect(Collectors.toList()));
        }

        /**
         * Adds an {@link AttributeValue} representing a list of binary data to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().bs(binaryValues1, binaryValues2, ...)).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param binaryValues the binary values of the attribute
         * @return the builder for method chaining
         */
        public Builder byteBuffers(String key, ByteBuffer... binaryValues) {
            return byteBuffers(key, Arrays.asList(binaryValues));
        }

        /**
         * Adds an {@link AttributeValue} representing a list of binary data to the item with the specified key.
         *
         * Equivalent of:
         * <pre><code>
         * itemMap.put(key, AttributeValue.builder().bs(binaryValues).build());
         * </code></pre>
         *
         * @param key the key of this attribute
         * @param binaryValues the binary values of the attribute
         * @return the builder for method chaining
         */
        public Builder byteBuffers(String key, Collection<? extends ByteBuffer> binaryValues) {
            item.put(key, AttributeValue.builder().bs(binaryValues.stream().map(SdkBytes::fromByteBuffer).collect(toList())).build());
            return this;
        }

        private static AttributeValue fromObject(Object object) {
            if (object instanceof AttributeValue) {
                return (AttributeValue) object;
            }
            if (object instanceof String) {
                return AttributeValue.builder().s((String) object).build();
            }
            if (object instanceof Number) {
                return AttributeValue.builder().n(String.valueOf((Number) object)).build();
            }
            if (object instanceof byte[]) {
                return AttributeValue.builder().b(SdkBytes.fromByteArray((byte[]) object)).build();
            }
            if (object instanceof ByteBuffer) {
                return AttributeValue.builder().b(SdkBytes.fromByteBuffer((ByteBuffer) object)).build();
            }
            if (object instanceof Boolean) {
                return AttributeValue.builder().bool((Boolean) object).build();
            }
            if (object instanceof List) {
                List<AttributeValue> attributeValues = ((List<?>) object).stream()
                                                                         .map(Builder::fromObject)
                                                                         .collect(toList());
                return AttributeValue.builder().l(attributeValues).build();
            }
            if (object instanceof Map) {
                Map<String, AttributeValue> attributeValues =
                    ((Map<String, ?>) object).entrySet()
                                             .stream()
                                             .map(e -> new SimpleImmutableEntry<>(e.getKey(), fromObject(e.getValue())))
                                             .collect(toMap());
                return AttributeValue.builder().m(attributeValues).build();
            }
            throw new IllegalArgumentException("Unsupported type: " + object.getClass());
        }
    }
}
