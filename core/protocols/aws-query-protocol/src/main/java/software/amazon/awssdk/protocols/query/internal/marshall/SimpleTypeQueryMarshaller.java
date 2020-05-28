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

package software.amazon.awssdk.protocols.query.internal.marshall;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.core.InstantToString;
import software.amazon.awssdk.protocols.core.StringToValueConverter.StringToValue;
import software.amazon.awssdk.protocols.core.ValueToStringConverter;

/**
 * Simple implementation of {@link QueryMarshaller} that converts a given value to a string using
 * {@link StringToValue} and emits it as a query param.
 *
 * @param <T> Type being marshalled.
 */
@SdkInternalApi
public final class SimpleTypeQueryMarshaller<T> implements QueryMarshaller<T> {

    public static final QueryMarshaller<String> STRING = new SimpleTypeQueryMarshaller<>(ValueToStringConverter.FROM_STRING);

    public static final QueryMarshaller<Integer> INTEGER = new SimpleTypeQueryMarshaller<>(ValueToStringConverter.FROM_INTEGER);

    public static final QueryMarshaller<Float> FLOAT = new SimpleTypeQueryMarshaller<>(ValueToStringConverter.FROM_FLOAT);

    public static final QueryMarshaller<Boolean> BOOLEAN = new SimpleTypeQueryMarshaller<>(ValueToStringConverter.FROM_BOOLEAN);

    public static final QueryMarshaller<Double> DOUBLE = new SimpleTypeQueryMarshaller<>(ValueToStringConverter.FROM_DOUBLE);

    public static final QueryMarshaller<Long> LONG = new SimpleTypeQueryMarshaller<>(ValueToStringConverter.FROM_LONG);

    public static final QueryMarshaller<Instant> INSTANT =
        new SimpleTypeQueryMarshaller<>(InstantToString.create(defaultTimestampFormats()));

    public static final QueryMarshaller<SdkBytes> SDK_BYTES
        = new SimpleTypeQueryMarshaller<>(ValueToStringConverter.FROM_SDK_BYTES);

    public static final QueryMarshaller<Void> NULL = (request, path, val, sdkField) -> {
    };

    private final ValueToStringConverter.ValueToString<T> valueToString;

    private SimpleTypeQueryMarshaller(ValueToStringConverter.ValueToString<T> valueToString) {
        this.valueToString = valueToString;
    }

    @Override
    public void marshall(QueryMarshallerContext context, String path, T val, SdkField<T> sdkField) {
        context.request().putRawQueryParameter(path, valueToString.convert(val, sdkField));
    }

    /**
     * @return Default timestamp formats for each location supported by the Query protocol.
     */
    public static Map<MarshallLocation, TimestampFormatTrait.Format> defaultTimestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new HashMap<>();
        // Query doesn't support location traits
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.ISO_8601);
        return Collections.unmodifiableMap(formats);
    }

}
