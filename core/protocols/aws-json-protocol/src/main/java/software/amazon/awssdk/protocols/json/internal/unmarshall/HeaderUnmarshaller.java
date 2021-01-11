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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.traits.JsonValueTrait;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.json.internal.dom.SdkJsonNode;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Header unmarshallers for all the simple types we support.
 */
@SdkInternalApi
final class HeaderUnmarshaller {

    public static final JsonUnmarshaller<String> STRING =
        new SimpleHeaderUnmarshaller<>(HeaderUnmarshaller::unmarshallStringHeader);
    public static final JsonUnmarshaller<Integer> INTEGER = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_INTEGER);
    public static final JsonUnmarshaller<Long> LONG = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_LONG);
    public static final JsonUnmarshaller<Double> DOUBLE = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_DOUBLE);
    public static final JsonUnmarshaller<Boolean> BOOLEAN = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_BOOLEAN);
    public static final JsonUnmarshaller<Float> FLOAT = new SimpleHeaderUnmarshaller<>(StringToValueConverter.TO_FLOAT);

    private HeaderUnmarshaller() {
    }

    /**
     * Unmarshalls a string header, taking into account whether it's a Base 64 encoded JSON value.
     *
     * @param value Value to unmarshall
     * @param field {@link SdkField} containing metadata about member being unmarshalled.
     * @return Unmarshalled value.
     */
    private static String unmarshallStringHeader(String value,
                                                 SdkField<String> field) {
        return field.containsTrait(JsonValueTrait.class) ?
               new String(BinaryUtils.fromBase64(value), StandardCharsets.UTF_8) : value;
    }

    public static JsonUnmarshaller<Instant> createInstantHeaderUnmarshaller(
        StringToValueConverter.StringToValue<Instant> instantStringToValue) {
        return new SimpleHeaderUnmarshaller<>(instantStringToValue);
    }

    /**
     * Simple unmarshaller implementation that calls a {@link StringToValueConverter} with the header value if it's present.
     *
     * @param <T> Type to unmarshall into.
     */
    private static class SimpleHeaderUnmarshaller<T> implements JsonUnmarshaller<T> {

        private final StringToValueConverter.StringToValue<T> stringToValue;

        private SimpleHeaderUnmarshaller(StringToValueConverter.StringToValue<T> stringToValue) {
            this.stringToValue = stringToValue;
        }

        @Override
        public T unmarshall(JsonUnmarshallerContext context,
                            SdkJsonNode jsonContent,
                            SdkField<T> field) {
            return context.response().firstMatchingHeader(field.locationName())
                          .map(s -> stringToValue.convert(s, field))
                          .orElse(null);
        }
    }
}
