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
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.core.ValueToStringConverter.ValueToString;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Implementation of {@link ValueToString} that converts and {@link Instant} to a string. * Respects the
 * {@link TimestampFormatTrait} if present.
 */
@SdkProtectedApi
public final class InstantToString implements ValueToString<Instant> {

    private final Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats;

    private InstantToString(Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats) {
        this.defaultFormats = defaultFormats;
    }

    @Override
    public String convert(Instant val, SdkField<Instant> sdkField) {
        if (val == null) {
            return null;
        }
        TimestampFormatTrait.Format format =
            sdkField.getOptionalTrait(TimestampFormatTrait.class)
                    .map(TimestampFormatTrait::format)
                    .orElseGet(() -> getDefaultTimestampFormat(sdkField.location(), defaultFormats));
        switch (format) {
            case ISO_8601:
                return DateUtils.formatIso8601Date(val);
            case RFC_822:
                return DateUtils.formatRfc1123Date(val);
            case UNIX_TIMESTAMP:
                return DateUtils.formatUnixTimestampInstant(val);
            default:
                throw SdkClientException.create("Unsupported timestamp format - " + format);
        }
    }

    private TimestampFormatTrait.Format getDefaultTimestampFormat(
        MarshallLocation location, Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats) {

        TimestampFormatTrait.Format format = defaultFormats.get(location);
        if (format == null) {
            throw SdkClientException.create("No default timestamp marshaller found for location - " + location);
        }
        return format;
    }

    public static InstantToString create(Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats) {
        return new InstantToString(defaultFormats);
    }
}
