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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.tz.FixedDateTimeZone;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.ThreadSafe;

/**
 * Utilities for parsing and formatting dates.
 */
@ThreadSafe
public class DateUtils {
    private static final DateTimeZone GMT = new FixedDateTimeZone("GMT", "GMT", 0, 0);
    /** ISO 8601 format. */
    protected static final DateTimeFormatter ISO_8601_DATE_FORMAT =
            ISODateTimeFormat.dateTime().withZone(GMT);
    /** Alternate ISO 8601 format without fractional seconds. */
    protected static final DateTimeFormatter ALTERNATE_ISO_8601_DATE_FORMAT =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(GMT);
    /** RFC 822 format. */
    protected static final DateTimeFormatter RFC_822_DATE_FORMAT =
            DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                          .withLocale(Locale.US)
                          .withZone(GMT);
    /**
     * This is another ISO 8601 format that's used in clock skew error response
     */
    protected static final DateTimeFormatter COMPRESSED_ISO_8601_DATE_FORMAT =
            DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'")
                          .withZone(GMT);
    private static final long MILLI_SECONDS_OF_365_DAYS = 365L * 24 * 60 * 60 * 1000;
    private static final int AWS_DATE_MILLI_SECOND_PRECISION = 3;

    /**
     * Parses the specified date string as an ISO 8601 date and returns the Date
     * object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Date object.
     */
    public static Date parseIso8601Date(String dateString) {
        try {
            return doParseIso8601Date(dateString);
        } catch (RuntimeException ex) {
            throw handleException(ex);
        }
    }

    static Date doParseIso8601Date(final String dateStringOrig) {
        String dateString = dateStringOrig;

        // For EC2 Spot Fleet.
        if (dateString.endsWith("+0000")) {
            dateString = dateString
                    .substring(0, dateString.length() - 5)
                    .concat("Z");
        }

        // https://github.com/aws/aws-sdk-java/issues/233
        String temp = tempDateStringForJodaTime(dateString);
        try {
            if (temp.equals(dateString)) {
                // Normal case: nothing special here
                return new Date(ISO_8601_DATE_FORMAT.parseMillis(dateString));
            }
            // Handling edge case:
            // Joda-time can only handle up to year 292278993 but we are given
            // 292278994;  So we parse the date string by first adjusting
            // the year to 292278993. Then we add 1 year back afterwards.
            final long milliLess365Days = ISO_8601_DATE_FORMAT.parseMillis(temp);
            final long milli = milliLess365Days + MILLI_SECONDS_OF_365_DAYS;
            if (milli < 0) { // overflow!
                // re-parse the original date string using JodaTime so as to
                // throw  an exception with a consistent message
                return new Date(ISO_8601_DATE_FORMAT.parseMillis(dateString));
            }
            return new Date(milli);
        } catch (IllegalArgumentException e) {
            try {
                return new Date(ALTERNATE_ISO_8601_DATE_FORMAT.parseMillis(dateString));
                // If the first ISO 8601 parser didn't work, try the alternate
                // version which doesn't include fractional seconds
            } catch (Exception oops) {
                // no the alternative route doesn't work; let's bubble up the original exception
                throw e;
            }
        }
    }

    /**
     * Returns a date string with the prefix temporarily substituted, if
     * applicable, so that JodaTime can handle it.  Otherwise, if not applicable,
     * the original date string is returned.
     * <p>
     * See https://github.com/aws/aws-sdk-java/issues/233
     */
    private static String tempDateStringForJodaTime(String dateString) {
        final String fromPrefix = "292278994-";
        final String toPrefix = "292278993-";
        return dateString.startsWith(fromPrefix)
               ? toPrefix + dateString.substring(fromPrefix.length())
               : dateString;
    }

    /**
     * Returns the original runtime exception iff the joda-time being used
     * at runtime behaves as expected.
     *
     * @throws IllegalStateException if the joda-time being used at runtime
     *     doens't appear to be of the right version.
     */
    private static <E extends RuntimeException> E handleException(E ex) {
        if (JodaTime.hasExpectedBehavior()) {
            return ex;
        }
        throw new IllegalStateException("Joda-time 2.2 or later version is required, but found version: " +
                                        JodaTime.getVersion(), ex);
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
        try {
            return ISO_8601_DATE_FORMAT.print(date.getTime());
        } catch (RuntimeException ex) {
            throw handleException(ex);
        }
    }

    /**
     * Formats the specified date as an ISO 8601 string.
     *
     * @param date the date to format
     * @return the ISO-8601 string representing the specified date
     */
    public static String formatIso8601Date(DateTime date) {
        try {
            return ISO_8601_DATE_FORMAT.print(date);
        } catch (RuntimeException ex) {
            throw handleException(ex);
        }
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
        try {
            return Instant.ofEpochMilli(RFC_822_DATE_FORMAT.parseMillis(dateString));
        } catch (RuntimeException ex) {
            throw handleException(ex);
        }
    }

    /**
     * Formats the specified date as an RFC 822 string.
     *
     * @param instant
     *            The date to format.
     *
     * @return The RFC 822 string representing the specified date.
     */
    public static String formatRfc822Date(Instant instant) {
        try {
            return RFC_822_DATE_FORMAT.print(instant.toEpochMilli());
        } catch (RuntimeException ex) {
            throw handleException(ex);
        }
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
        try {
            return new Date(COMPRESSED_ISO_8601_DATE_FORMAT.parseMillis(dateString));
        } catch (RuntimeException ex) {
            throw handleException(ex);
        }
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
}
