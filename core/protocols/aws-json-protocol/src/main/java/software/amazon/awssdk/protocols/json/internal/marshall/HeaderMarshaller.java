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

package software.amazon.awssdk.protocols.json.internal.marshall;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.traits.JsonValueTrait;
import software.amazon.awssdk.protocols.core.ValueToStringConverter;
import software.amazon.awssdk.utils.BinaryUtils;

@SdkInternalApi
public final class HeaderMarshaller {

    public static final JsonMarshaller<String> STRING = new SimpleHeaderMarshaller<>(
        (val, field) -> field.containsTrait(JsonValueTrait.class) ?
                        BinaryUtils.toBase64(val.getBytes(StandardCharsets.UTF_8)) : val);

    public static final JsonMarshaller<Integer> INTEGER = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_INTEGER);

    public static final JsonMarshaller<Long> LONG = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_LONG);

    public static final JsonMarshaller<Double> DOUBLE = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_DOUBLE);

    public static final JsonMarshaller<Float> FLOAT = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_FLOAT);

    public static final JsonMarshaller<Boolean> BOOLEAN = new SimpleHeaderMarshaller<>(ValueToStringConverter.FROM_BOOLEAN);

    public static final JsonMarshaller<Instant> INSTANT
        = new SimpleHeaderMarshaller<>(JsonProtocolMarshaller.INSTANT_VALUE_TO_STRING);

    private HeaderMarshaller() {
    }

    private static class SimpleHeaderMarshaller<T> implements JsonMarshaller<T> {

        private final ValueToStringConverter.ValueToString<T> converter;

        private SimpleHeaderMarshaller(ValueToStringConverter.ValueToString<T> converter) {
            this.converter = converter;
        }

        @Override
        public void marshall(T val, JsonMarshallerContext context, String paramName, SdkField<T> sdkField) {
            context.request().putHeader(paramName, converter.convert(val, sdkField));
        }
    }

}
