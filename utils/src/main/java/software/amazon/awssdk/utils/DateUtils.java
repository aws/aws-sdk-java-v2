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

package software.amazon.awssdk.utils;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Utilities for parsing and formatting dates.
 */
@ThreadSafe
@SdkProtectedApi
public final class DateUtils {

    /**
     * Alternate ISO 8601 format without fractional seconds.
     */
    static final DateTimeFormatter ALTERNATE_ISO_8601_DATE_FORMAT =
        new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .toFormatter()
            .withZone(UTC);

    private static final int MILLI_SECOND_PRECISION = 3;

    private DateUtils() {
    }

    /**
     * Parses the specified date string as an ISO 8601 date (yyyy-MM-dd'T'HH:mm:ss.SSSZZ)
     * and returns the {@link Instant} object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Instant object.
     */
    public static Instant parseIso8601Date(String dateString) {
        // For EC2 Spot Fleet.
        if (dateString.endsWith("+0000")) {
            dateString = dateString
                             .substring(0, dateString.length() - 5)
                             .concat("Z");
        }

        try {
            return parseInstant(dateString, ISO_INSTANT);
        } catch (DateTimeParseException e) {
            return parseInstant(dateString, ALTERNATE_ISO_8601_DATE_FORMAT);
        }
    }

    /**
     * Formats the specified date as an ISO 8601 string.
     *
     * @param date the date to format
     * @return the ISO-8601 string representing the specified date
     */
    public static String formatIso8601Date(Instant date) {
        return ISO_INSTANT.format(date);
    }

    /**
     * Parses the specified date string as an RFC 1123 date and returns the Date
     * object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Date object.
     */
    public static Instant parseRfc1123Date(String dateString) {
        if (dateString == null) {
            return null;
        }
        return parseInstant(dateString, RFC_1123_DATE_TIME);
    }

    /**
     * Formats the specified date as an RFC 1123 string.
     *
     * @param instant
     *            The instant to format.
     *
     * @return The RFC 1123 string representing the specified date.
     */
    public static String formatRfc1123Date(Instant instant) {
        return RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(instant, UTC));
    }

    /**
     * Returns the number of days since epoch with respect to the given number
     * of milliseconds since epoch.
     */
    public static long numberOfDaysSinceEpoch(long milliSinceEpoch) {
        return Duration.ofMillis(milliSinceEpoch).toDays();
    }

    private static Instant parseInstant(String dateString, DateTimeFormatter formatter) {
        return formatter.withZone(ZoneOffset.UTC).parse(dateString, Instant::from);
    }

    /**
     * Parses the given string containing a Unix timestamp with millisecond decimal precision into an {@link Instant} object.
     */
    public static Instant parseUnixTimestampInstant(String dateString) throws NumberFormatException {
        if (dateString == null) {
            return null;
        }
        BigDecimal dateValue = new BigDecimal(dateString);
        return Instant.ofEpochMilli(dateValue.scaleByPowerOfTen(MILLI_SECOND_PRECISION).longValue());
    }

    /**
     * Parses the given string containing a Unix timestamp in epoch millis into a {@link Instant} object.
     */
    public static Instant parseUnixTimestampMillisInstant(String dateString) throws NumberFormatException {
        if (dateString == null) {
            return null;
        }
        return Instant.ofEpochMilli(Long.parseLong(dateString));
    }

    /**
     * Formats the give {@link Instant} object into an Unix timestamp with millisecond decimal precision.
     */
    public static String formatUnixTimestampInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        BigDecimal dateValue = BigDecimal.valueOf(instant.toEpochMilli());
        return dateValue.scaleByPowerOfTen(0 - MILLI_SECOND_PRECISION)
                        .toPlainString();
    }
}
