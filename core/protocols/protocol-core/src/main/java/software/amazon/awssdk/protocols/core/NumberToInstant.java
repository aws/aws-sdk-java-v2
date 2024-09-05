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
import software.amazon.awssdk.core.traits.TraitType;

/**
 * Converts a number value to an {@link Instant} type. Respects the {@link TimestampFormatTrait} if present.
 */
@SdkProtectedApi
public final class NumberToInstant {

    /**
     * Default formats for the given location.
     */
    private final Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats;
    private final StringToInstant stringToInstant;

    private NumberToInstant(Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats) {
        this.stringToInstant = StringToInstant.create(defaultFormats);
        this.defaultFormats = defaultFormats;
    }

    public Instant convert(Number value, SdkField<Instant> field) {
        if (value == null) {
            return null;
        }
        if (field.location() != MarshallLocation.PAYLOAD) {
            return stringToInstant.convert(value.toString(), field);
        }
        TimestampFormatTrait.Format format = resolveTimestampFormat(field);
        switch (format) {
            case UNIX_TIMESTAMP:
                return Instant.ofEpochMilli((long) (value.doubleValue() * 1_000d));
            case UNIX_TIMESTAMP_MILLIS:
                return Instant.ofEpochMilli(value.longValue());
            default:
                return stringToInstant.convert(value.toString(), field);
        }
    }

    private TimestampFormatTrait.Format resolveTimestampFormat(SdkField<Instant> field) {
        TimestampFormatTrait trait = field.getTrait(TimestampFormatTrait.class, TraitType.TIMESTAMP_FORMAT_TRAIT);
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
    public static NumberToInstant create(Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats) {
        return new NumberToInstant(defaultFormats);
    }
}
