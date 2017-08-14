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
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
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
    public static Instant parseIso8601Date(String dateString) {
        // For EC2 Spot Fleet.
        if (dateString.endsWith("+0000")) {
            dateString = dateString
                             .substring(0, dateString.length() - 5)
                             .concat("Z");
        }

        try {
            return parseIso8601DateAsInstant(dateString, ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            return parseIso8601DateAsInstant(dateString, ALTERNATE_ISO_8601_DATE_FORMAT);
        }
    }

    private static Instant parseIso8601DateAsInstant(final String dateString, DateTimeFormatter formatter) {
        // https://github.com/aws/aws-sdk-java/issues/233
        // Handling edge case: java time required '+' sign in front of the year 292278994 (or greater).
        boolean isSignRequired = dateString.startsWith(MIN_SIGNED_YEAR_PREFIX);
        if (isSignRequired) {
            return parseInstant("+" + dateString, formatter);
        }
        // Normal case: nothing special here
        return parseInstant(dateString, formatter);
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
        return ISO_DATE_TIME.format(ZonedDateTime.ofInstant(date.toInstant(), UTC));
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
        return ISO_DATE_TIME.format(date);
    }

    /**
     * Parses the specified date string as an RFC 822 date and returns the Date
     * object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Date object.
     * @deprecated use {@link #parseRfc1123Date(String)}. RFC-1123 updates RFC-822 changing the year from two digits to four.
     */
    @Deprecated
    public static Instant parseRfc822Date(String dateString) {
        return parseRfc1123Date(dateString);
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
     * @param temporalAccessor
     *            The date to format.
     *
     * @return The RFC 1123 string representing the specified date.
     * @deprecated use {@link #formatRfc1123Date(TemporalAccessor)}.
     */
    @Deprecated
    public static String formatRfc822Date(TemporalAccessor temporalAccessor) {
        return formatRfc1123Date(temporalAccessor);
    }

    /**
     * Formats the specified date as an RFC 1123 string.
     *
     * @param temporalAccessor
     *            The date to format.
     *
     * @return The RFC 1123 string representing the specified date.
     */
    public static String formatRfc1123Date(TemporalAccessor temporalAccessor) {
        return RFC_1123_DATE_TIME.format(temporalAccessor);
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
    @ReviewBeforeRelease(value = "Used in one test only.")
    public static Instant parseCompressedIso8601Date(String dateString) {
        return parseInstant(dateString, COMPRESSED_ISO_8601_DATE_FORMAT);
    }

    /**
     * Parses the given date string returned by the AWS service into a Date
     * object.
     * @deprecated use {@link #parseServiceSpecificInstant(String)}.
     */
    @Deprecated
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
     * Parses the given date string returned by the AWS service into a Date
     * object.
     */
    public static Instant parseServiceSpecificInstant(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            BigDecimal dateValue = new BigDecimal(dateString);
            return Instant.ofEpochMilli(dateValue.scaleByPowerOfTen(
                    AWS_DATE_MILLI_SECOND_PRECISION).longValue());
        } catch (NumberFormatException nfe) {
            throw new SdkClientException("Unable to parse date : "
                                         + dateString, nfe);
        }
    }

    /**
     * Formats the give date object into an AWS Service format.
     *
     * @deprecated use {@link #formatServiceSpecificDate(Instant)}.
     */
    public static String formatServiceSpecificDate(Date date) {
        if (date == null) {
            return null;
        }
        BigDecimal dateValue = BigDecimal.valueOf(date.getTime());
        return dateValue.scaleByPowerOfTen(0 - AWS_DATE_MILLI_SECOND_PRECISION)
                        .toPlainString();
    }

    /**
     * Formats the give date object into an AWS Service format.
     */
    public static String formatServiceSpecificDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        BigDecimal dateValue = BigDecimal.valueOf(instant.toEpochMilli());
        return dateValue.scaleByPowerOfTen(0 - AWS_DATE_MILLI_SECOND_PRECISION)
                   .toPlainString();
    }

    @Deprecated
    @ReviewBeforeRelease("not used.")
    public static Date cloneDate(Date date) {
        return date == null ? null : new Date(date.getTime());
    }

    /**
     * Returns the number of days since epoch with respect to the given number
     * of milliseconds since epoch.
     * @deprecated use {@link Duration#ofMillis(long)} and {@link Duration#toDays()}.
     */
    public static Duration durationSinceEpoch(long milliSinceEpoch) {
        return Duration.ofMillis(milliSinceEpoch);
    }

    private static Instant parseInstant(String dateString, DateTimeFormatter formatter) {
        return ZonedDateTime.parse(dateString, formatter).toInstant();
    }
}
