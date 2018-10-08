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

package software.amazon.awssdk.core.internal.protocol.json;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.AwsDateUtils;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.SdkField;
import software.amazon.awssdk.core.protocol.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.util.StringConversion;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Converts various types to Strings. Used for Query Param/Header/Path marshalling.
 */
@SdkInternalApi
public final class ValueToStringConverter {

    /**
     * Interface to convert a type to a String.
     *
     * @param <T> Type to convert.
     */
    @FunctionalInterface
    public interface ValueToString<T> {

        /**
         * Converts the value to a string.
         *
         * @param t Value to convert.
         * @param field {@link SdkField} containing metadata about the member being marshalled.
         * @return String value.
         */
        String convert(T t, SdkField<T> field);
    }

    /**
     * Simple interface to convert a type to a String. Useful for implementations that don't need the {@link SdkField}.
     *
     * @param <T> Type to convert.
     */
    @FunctionalInterface
    public interface SimpleValueToString<T> extends ValueToString<T> {

        @Override
        default String convert(T t, SdkField<T> field) {
            return convert(t);
        }

        /**
         * Converts the value to a string.
         *
         * @param t Value to convert.
         * @return String value.
         */
        String convert(T t);
    }

    /**
     * Identity converter.
     */
    public static final SimpleValueToString<String> FROM_STRING = val -> val;

    public static final SimpleValueToString<Integer> FROM_INTEGER = StringConversion::fromInteger;

    public static final SimpleValueToString<Long> FROM_LONG = StringConversion::fromLong;

    public static final SimpleValueToString<Float> FROM_FLOAT = StringConversion::fromFloat;

    public static final SimpleValueToString<Double> FROM_DOUBLE = StringConversion::fromDouble;

    /**
     * Marshalls boolean as a literal 'true' or 'false' string.
     */
    public static final SimpleValueToString<Boolean> FROM_BOOLEAN = StringConversion::fromBoolean;

    /**
     * Marshalls date to an ISO8601 date string.
     */
    public static final ValueToString<Instant> FROM_INSTANT = ValueToStringConverter::marshallTimestamp;

    private static final Map<MarshallLocation, TimestampFormatTrait.Format> DEFAULT_FORMATS;

    static {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new HashMap<>();
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.ISO_8601);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        formats.put(MarshallLocation.QUERY_PARAM, TimestampFormatTrait.Format.ISO_8601);
        // Not currently supported for paths
        DEFAULT_FORMATS = Collections.unmodifiableMap(formats);
    }

    private ValueToStringConverter() {
    }

    private static String marshallTimestamp(Instant val, SdkField<Instant> sdkField) {
        if (val == null) {
            return null;
        }
        TimestampFormatTrait.Format format = sdkField.getOptionalTrait(TimestampFormatTrait.class)
                                                     .map(TimestampFormatTrait::format)
                                                     .orElseGet(() -> getDefaultTimestampFormat(sdkField.location()));
        switch (format) {
            case ISO_8601:
                return DateUtils.formatIso8601Date(val);
            case RFC_822:
                return DateUtils.formatRfc1123Date(val);
            case UNIX_TIMESTAMP:
                return AwsDateUtils.formatServiceSpecificDate(val);
            default:
                throw SdkClientException.create("Unsupported timestamp format - " + format);
        }
    }

    private static TimestampFormatTrait.Format getDefaultTimestampFormat(MarshallLocation location) {
        TimestampFormatTrait.Format format = DEFAULT_FORMATS.get(location);
        if (format == null) {
            throw SdkClientException.create("No default timestamp marshaller found for location - " + location);
        }
        return format;
    }
}
