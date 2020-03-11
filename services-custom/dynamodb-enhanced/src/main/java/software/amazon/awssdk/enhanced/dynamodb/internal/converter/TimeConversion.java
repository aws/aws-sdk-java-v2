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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils.padLeft;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils.padRight;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils.splitNumberOnDecimal;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils.trimNumber;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalQuery;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class TimeConversion {
    private static final InstantVisitor INSTANT_VISITOR = new InstantVisitor();
    private static final OffsetDateTimeVisitor OFFSET_DATE_TIME_VISITOR = new OffsetDateTimeVisitor();
    private static final ZonedDateTimeVisitor ZONED_DATE_TIME_VISITOR = new ZonedDateTimeVisitor();

    private TimeConversion() {
    }

    public static EnhancedAttributeValue toIntegerAttributeValue(Instant instant) {
        long instantSeconds = instant.getEpochSecond();
        int nanos = instant.getNano();

        String value;
        if (nanos == 0) {
            value = Long.toString(instantSeconds);
        } else if (instantSeconds >= 0) {
            value = instantSeconds +
                    "." + padLeft(9, nanos);
        } else {
            instantSeconds++;
            nanos = 1_000_000_000 - nanos;

            value = "-" +
                    Math.abs(instantSeconds) +
                    "." + padLeft(9, nanos);
        }

        return EnhancedAttributeValue.fromNumber(trimNumber(value));
    }

    public static EnhancedAttributeValue toStringAttributeValue(Instant instant) {
        return EnhancedAttributeValue.fromString(DateTimeFormatter.ISO_INSTANT.format(instant));
    }

    public static EnhancedAttributeValue toStringAttributeValue(OffsetDateTime accessor) {
        return EnhancedAttributeValue.fromString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(accessor));
    }

    public static EnhancedAttributeValue toStringAttributeValue(ZonedDateTime accessor) {
        return EnhancedAttributeValue.fromString(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(accessor));
    }

    public static Instant instantFromAttributeValue(EnhancedAttributeValue itemAttributeValue) {
        return convert(itemAttributeValue, INSTANT_VISITOR);
    }

    public static OffsetDateTime offsetDateTimeFromAttributeValue(EnhancedAttributeValue itemAttributeValue) {
        return convert(itemAttributeValue, OFFSET_DATE_TIME_VISITOR);
    }

    public static ZonedDateTime zonedDateTimeFromAttributeValue(EnhancedAttributeValue itemAttributeValue) {
        return convert(itemAttributeValue, ZONED_DATE_TIME_VISITOR);
    }

    private static <T> T convert(EnhancedAttributeValue itemAttributeValue, TypeConvertingVisitor<T> visitor) {
        try {
            return itemAttributeValue.convert(visitor);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final class InstantVisitor extends BaseVisitor<Instant> {
        protected InstantVisitor() {
            super(Instant.class, Instant::from);
        }

        @Override
        public Instant convertString(String value) {
            try {
                return super.convertString(value);
            } catch (DateTimeParseException e) {
                // Instant has a larger date range (-1,000,000,000 to 1,000,000,000) than zoned or offset date times
                // (-999,999,999 to -999,999,999). An Instant was requested, so we try falling back to the ISO_INSTANT
                // parser that supports Instant.MIN through Instant.MAX.
                try {
                    return DateTimeFormatter.ISO_INSTANT.parse(value, Instant::from);
                } catch (DateTimeParseException e2) {
                    // Nope, that didn't work either. Report the failures.
                    throw new IllegalArgumentException("Record could not be parsed with either " +
                                                       "DateTimeFormatter.ISO_ZONED_DATE_TIME (" + e.getMessage() +
                                                       ") or DateTimeFormatter.ISO_INSTANT (" + e2.getMessage() +
                                                       ").");
                }
            }
        }

        @Override
        protected Instant fromUtcInstant(Instant instant) {
            return instant;
        }
    }

    private static final class OffsetDateTimeVisitor extends BaseVisitor<OffsetDateTime> {
        protected OffsetDateTimeVisitor() {
            super(OffsetDateTime.class, OffsetDateTime::from);
        }

        @Override
        protected OffsetDateTime fromUtcInstant(Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
    }

    private static final class ZonedDateTimeVisitor extends BaseVisitor<ZonedDateTime> {
        protected ZonedDateTimeVisitor() {
            super(ZonedDateTime.class, ZonedDateTime::from);
        }

        @Override
        protected ZonedDateTime fromUtcInstant(Instant instant) {
            return instant.atZone(ZoneOffset.UTC);
        }
    }

    private abstract static class BaseVisitor<T> extends TypeConvertingVisitor<T> {
        private final TemporalQuery<T> query;

        protected BaseVisitor(Class<T> targetType, TemporalQuery<T> query) {
            super(targetType);
            this.query = query;
        }

        @Override
        public T convertString(String value) {
            return DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(value, query);
        }

        @Override
        public T convertNumber(String value) {
            String[] splitOnDecimal = splitNumberOnDecimal(value);

            Validate.isTrue(splitOnDecimal[1].length() <= 9, "Nanoseconds must be expressed in 9 or fewer digits.");

            long epochSecond = splitOnDecimal[0].length() == 0 ? 0 : Long.parseLong(splitOnDecimal[0]);
            int nanoAdjustment = Integer.parseInt(padRight(9, splitOnDecimal[1]));

            if (value.startsWith("-")) {
                nanoAdjustment = -nanoAdjustment;
            }

            return fromUtcInstant(Instant.ofEpochSecond(epochSecond, nanoAdjustment));
        }

        protected abstract T fromUtcInstant(Instant instant);
    }
}
