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

package software.amazon.awssdk.protocols.core;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Implementation of {@link StringToValueConverter.StringToValue} that converts a string to an {@link Instant} type.
 * Respects the {@link TimestampFormatTrait} if present.
 */
@SdkProtectedApi
public final class StringToInstant implements StringToValueConverter.StringToValue<Instant> {

    /**
     * Default formats for the given location.
     */
    private final Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats;

    private StringToInstant(Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats) {
        this.defaultFormats = defaultFormats;
    }

    @Override
    public Instant convert(String value, SdkField<Instant> field) {
        if (value == null) {
            return null;
        }
        TimestampFormatTrait.Format format = resolveTimestampFormat(field);
        switch (format) {
            case ISO_8601:
                return DateUtils.parseIso8601Date(value);
            case UNIX_TIMESTAMP:
                return safeParseDate(DateUtils::parseUnixTimestampInstant).apply(value);
            case UNIX_TIMESTAMP_MILLIS:
                return safeParseDate(DateUtils::parseUnixTimestampMillisInstant).apply(value);
            case RFC_822:
                return DateUtils.parseRfc1123Date(value);
            default:
                throw SdkClientException.create("Unrecognized timestamp format - " + format);
        }
    }

    /**
     * Wraps date unmarshalling function to handle the {@link NumberFormatException}.
     * @param dateUnmarshaller Original date unmarshaller function.
     * @return New date unmarshaller function with exception handling.
     */
    private Function<String, Instant> safeParseDate(Function<String, Instant> dateUnmarshaller) {
        return value -> {
            try {
                return dateUnmarshaller.apply(value);
            } catch (NumberFormatException e) {
                throw SdkClientException.builder()
                                        .message("Unable to parse date : " + value)
                                        .cause(e)
                                        .build();
            }
        };
    }

    private TimestampFormatTrait.Format resolveTimestampFormat(SdkField<Instant> field) {
        TimestampFormatTrait trait = field.getTrait(TimestampFormatTrait.class);
        if (trait == null) {
            TimestampFormatTrait.Format format = defaultFormats.get(field.location());
            if (format == null) {
                throw SdkClientException.create(
                    String.format("Timestamps are not supported for this location (%s)", field.location()));
            }
            return format;
        } else {
            return trait.format();
        }
    }

    /**
     * @param defaultFormats Default formats for each {@link MarshallLocation} as defined by the protocol.
     * @return New {@link StringToValueConverter.StringToValue} for {@link Instant} types.
     */
    public static StringToInstant create(Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats) {
        return new StringToInstant(defaultFormats);
    }
}
