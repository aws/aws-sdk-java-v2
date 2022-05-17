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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import static software.amazon.awssdk.transfer.s3.internal.serialization.TransferManagerMarshallingUtils.getMarshaller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.protocols.core.Marshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;

/**
 * Interface to marshall data according to the JSON protocol specification.
 *
 * @param <T> Type to marshall.
 */
@FunctionalInterface
@SdkInternalApi
public interface TransferManagerJsonMarshaller<T> extends Marshaller<T> {

    TransferManagerJsonMarshaller<String> STRING = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<Short> SHORT = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<Integer> INTEGER = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<Long> LONG = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<Float> FLOAT = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<Double> DOUBLE = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<BigDecimal> BIG_DECIMAL = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<Boolean> BOOLEAN = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<Instant> INSTANT = (val, jsonGenerator) -> jsonGenerator.writeValue(val);
    TransferManagerJsonMarshaller<SdkBytes> SDK_BYTES = (val, jsonGenerator) -> jsonGenerator.writeValue(val.asByteBuffer());

    TransferManagerJsonMarshaller<Void> NULL = new TransferManagerJsonMarshaller<Void>() {
        @Override
        public void marshall(Void val, JsonWriter generator, String paramName) {
            if (paramName == null) {
                generator.writeNull();
            }
        }

        @Override
        public void marshall(Void val, JsonWriter jsonGenerator) {

        }
    };

    TransferManagerJsonMarshaller<List<?>> LIST = new TransferManagerJsonMarshaller<List<?>>() {
        @Override
        public void marshall(List<?> list, JsonWriter jsonGenerator) {
            jsonGenerator.writeStartArray();
            list.forEach(val -> getMarshaller(val).marshall(val, jsonGenerator, null));
            jsonGenerator.writeEndArray();
        }

        @Override
        public boolean shouldEmit(List<?> list) {
            return !list.isEmpty() || !(list instanceof SdkAutoConstructList);

        }
    };

    TransferManagerJsonMarshaller<Map<String, ?>> MAP = new TransferManagerJsonMarshaller<Map<String, ?>>() {
        @Override
        public void marshall(Map<String, ?> map, JsonWriter jsonGenerator) {
            jsonGenerator.writeStartObject();
            map.forEach((key, value) -> {
                if (value != null) {
                    jsonGenerator.writeFieldName(key);
                    getMarshaller(value).marshall(value, jsonGenerator, null);
                }
            });
            jsonGenerator.writeEndObject();
        }

        @Override
        public boolean shouldEmit(Map<String, ?> map) {
            return !map.isEmpty() || !(map instanceof SdkAutoConstructMap);

        }
    };

    default void marshall(T val, JsonWriter generator, String paramName) {
        if (!shouldEmit(val)) {
            return;
        }
        if (paramName != null) {
            generator.writeFieldName(paramName);
        }
        marshall(val, generator);
    }

    void marshall(T val, JsonWriter jsonGenerator);

    default boolean shouldEmit(T val) {
        return true;
    }
}
