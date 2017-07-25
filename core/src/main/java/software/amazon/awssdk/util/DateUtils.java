/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.util;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.ThreadSafe;

/**
 * Utilities for parsing and formatting dates.
 */
@ThreadSafe
public class DateUtils {
    /** Alternate ISO 8601 format without fractional seconds. */
    static final DateTimeFormatter ALTERNATE_ISO_8601_DATE_FORMAT =
        new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .toFormatter()
            .withZone(UTC);

    private static final int AWS_DATE_MILLI_SECOND_PRECISION = 3;

    public static final DateTimeFormatter ISO_8601_DATETIME_FORMAT =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendFraction(NANO_OF_SECOND, AWS_DATE_MILLI_SECOND_PRECISION, 9, true)
            .appendLiteral('Z')
            .toFormatter(Locale.US)
            .withZone(UTC);

    /** RFC 822 format. */
    private static final DateTimeFormatter RFC_822_DATE_FORMAT =
            new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                        .toFormatter(Locale.US)
                        .withZone(UTC);
    /**
     * This is another ISO 8601 format that's used in clock skew error response
     */
    private static final DateTimeFormatter COMPRESSED_ISO_8601_DATE_FORMAT =
            new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("yyyyMMdd'T'HHmmss'Z'")
                        .toFormatter()
                        .withZone(UTC);

    private static final String MIN_SIGNED_YEAR_PREFIX = "292278994-";

    /**
     * Parses the specified date string as an ISO 8601 date (yyyy-MM-dd'T'HH:mm:ss.SSSZZ) and returns the Date
     * object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Date object.
     */
    public static Date parseIso8601Date(String dateString) {
        // For EC2 Spot Fleet.
        if (dateString.endsWith("+0000")) {
            dateString = dateString
                             .substring(0, dateString.length() - 5)
                             .concat("Z");
        }

        long millis;
        try {
            millis = parseIso8601DateAsMillis(dateString, ISO_8601_DATETIME_FORMAT);
        } catch (DateTimeParseException e) {
            millis = parseIso8601DateAsMillis(dateString, ALTERNATE_ISO_8601_DATE_FORMAT);
        }
        return new Date(millis);
    }

    private static long parseIso8601DateAsMillis(final String dateString, DateTimeFormatter formatter) {
        // https://github.com/aws/aws-sdk-java/issues/233
        // Handling edge case: java time required '+' sign in front of the year 292278994 (or greater).
        boolean isSignRequired = dateString.startsWith(MIN_SIGNED_YEAR_PREFIX);
        if (isSignRequired) {
            // Normal case: nothing special here
            return parseMillis("+" + dateString, formatter);
        }
        return parseMillis(dateString, formatter);
    }

    /**
     * Formats the specified date as an ISO 8601 string.
     *
     * @param date
     *            The date to format.
     *
     * @return The ISO 8601 string representing the specified date.
     */
    public static String formatIso8601Date(Date date) {
        return ISO_8601_DATETIME_FORMAT.format(ZonedDateTime.ofInstant(date.toInstant(), UTC));
    }

    /**
     * Formats the specified date as an ISO 8601 string.
     *
     * @param date the date to format
     * @return the ISO-8601 string representing the specified date
     * @deprecated use {@link DateTimeFormatter#ISO_DATE_TIME} directly.
     */
    @Deprecated
    public static String formatIso8601Date(ZonedDateTime date) {
        return ISO_8601_DATETIME_FORMAT.format(date);
    }

    /**
     * Parses the specified date string as an RFC 822 date and returns the Date
     * object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Date object.
     */
    public static Instant parseRfc822Date(String dateString) {
        if (dateString == null) {
            return null;
        }
        return Instant.ofEpochMilli(parseMillis(dateString, RFC_822_DATE_FORMAT));
    }

    /**
     * Formats the specified date as an RFC 822 string.
     *
     * @param instant
     *            The date to format.
     *
     * @return The RFC 822 string representing the specified date.
     */
    public static String formatRfc822Date(TemporalAccessor instant) {
        return RFC_822_DATE_FORMAT.format(instant);
    }

    /**
     * Parses the specified date string as a compressedIso8601DateFormat ("yyyyMMdd'T'HHmmss'Z'") and returns the Date
     * object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Date object.
     */
    public static Date parseCompressedIso8601Date(String dateString) {
        return new Date(parseMillis(dateString, COMPRESSED_ISO_8601_DATE_FORMAT));
    }

    /**
     * Parses the given date string returned by the AWS service into a Date
     * object.
     */
    public static Date parseServiceSpecificDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            BigDecimal dateValue = new BigDecimal(dateString);
            return new Date(dateValue.scaleByPowerOfTen(
                    AWS_DATE_MILLI_SECOND_PRECISION).longValue());
        } catch (NumberFormatException nfe) {
            throw new SdkClientException("Unable to parse date : "
                                         + dateString, nfe);
        }
    }

    /**
     * Formats the give date object into an AWS Service format.
     */
    public static String formatServiceSpecificDate(Date date) {
        if (date == null) {
            return null;
        }
        BigDecimal dateValue = BigDecimal.valueOf(date.getTime());
        return dateValue.scaleByPowerOfTen(0 - AWS_DATE_MILLI_SECOND_PRECISION)
                        .toPlainString();
    }

    public static Date cloneDate(Date date) {
        return date == null ? null : new Date(date.getTime());
    }

    /**
     * Returns the number of days since epoch with respect to the given number
     * of milliseconds since epoch.
     */
    public static long numberOfDaysSinceEpoch(long milliSinceEpoch) {
        return TimeUnit.MILLISECONDS.toDays(milliSinceEpoch);
    }

    private static long parseMillis(String dateString, DateTimeFormatter formatter) {
        return ZonedDateTime.parse(dateString, formatter).toInstant().toEpochMilli();
    }
}
