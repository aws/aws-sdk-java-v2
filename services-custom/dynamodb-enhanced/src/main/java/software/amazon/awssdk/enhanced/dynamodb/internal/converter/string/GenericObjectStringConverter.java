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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.string;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.utils.Logger;

/**
 * A generic fallback {@link StringConverter} that uses Jackson's ObjectMapper to serialize and deserialize arbitrary types to and
 * from JSON.
 * <p>
 * This converter is useful for types that don't have built-in support and is typically used in fallback scenarios. If
 * serialization or deserialization fails, an error is logged and {@code null} is returned.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class GenericObjectStringConverter<T> implements StringConverter<T> {

    private static final Logger log = Logger.loggerFor(GenericObjectStringConverter.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final EnhancedType<T> type;

    private GenericObjectStringConverter(EnhancedType<T> type) {
        this.type = type;
    }

    public static <T> GenericObjectStringConverter<T> create(EnhancedType<T> type) {
        return new GenericObjectStringConverter<>(type);
    }

    @Override
    public EnhancedType<T> type() {
        return type;
    }

    @Override
    public String toString(T value) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error(() -> "Failed to serialize object of type " + type.rawClass().getName(), e);
        }
        return null;
    }

    @Override
    public T fromString(String string) {
        if (string == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(string, type.rawClass());
        } catch (JsonProcessingException e) {
            log.error(() -> "Failed to deserialize object of type " + type.rawClass().getName(), e);
        }
        return null;
    }
}
