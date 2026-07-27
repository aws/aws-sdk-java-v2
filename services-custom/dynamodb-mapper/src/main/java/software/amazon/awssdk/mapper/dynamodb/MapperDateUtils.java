/*
 * Copyright 2010-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Portions copyright 2006-2009 James Murty. Please see LICENSE.txt
 * for applicable license terms and NOTICE.txt for applicable notices.
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
package software.amazon.awssdk.mapper.dynamodb;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.tz.FixedDateTimeZone;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Narrowly-scoped ISO-8601 date parsing and formatting for the mapper's converter layer.
 *
 * <p>The v1 {@code DynamoDBMapper} formatted and parsed {@link Date}/{@code Calendar} attributes
 * with {@code com.amazonaws.util.DateUtils} from {@code aws-java-sdk-core}. That class is v1 SDK
 * infrastructure (it pulls in {@code com.amazonaws.SdkClientException} and
 * {@code com.amazonaws.util.JodaTime}) and cannot be depended on from a v2 module. The v2 SDK's
 * own {@code software.amazon.awssdk.utils.DateUtils} is {@code java.time}-based and is <em>not</em>
 * a drop-in: its parser rejects some ISO-8601 shapes v1 accepts (e.g. basic-format offsets like
 * {@code -0700}) and its formatter omits trailing-zero milliseconds ({@code .000Z}), which would
 * silently change the string form of data written by v1.</p>
 *
 * <p>To preserve byte-for-byte read/write compatibility with data produced by the v1 mapper, this
 * class reproduces exactly the two ISO-8601 methods the converter layer uses, using the same
 * Joda-Time formatters v1 used. It intentionally excludes the other 11 methods of the v1 utility
 * (RFC-822, compressed-ISO, Unix-timestamp, etc.) that the mapper never calls. Do not grow this
 * class beyond ISO-8601 parse/format.</p>
 */
@SdkInternalApi
public final class MapperDateUtils {

    private static final DateTimeZone GMT = new FixedDateTimeZone("GMT", "GMT", 0, 0);
    private static final long MILLI_SECONDS_OF_365_DAYS = 365L * 24 * 60 * 60 * 1000;

    /** ISO 8601 format. */
    private static final DateTimeFormatter ISO8601_DATE_FORMAT =
        ISODateTimeFormat.dateTime().withZone(GMT);

    /** Alternate ISO 8601 format without fractional seconds. */
    private static final DateTimeFormatter ALTERNATE_ISO8601_DATE_FORMAT =
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(GMT);

    /** ISO 8601 format with a UTC offset. */
    private static final DateTimeFormatter ISO8601_DATE_FORMAT_WITH_OFFSET =
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

    private static final List<DateTimeFormatter> ALTERNATE_ISO8601_FORMATTERS = Arrays.asList(
        ALTERNATE_ISO8601_DATE_FORMAT, ISO8601_DATE_FORMAT_WITH_OFFSET);

    private MapperDateUtils() {
    }

    /**
     * Parses the specified date string as an ISO 8601 date and returns the {@link Date} object.
     *
     * @param dateStringOrig the date string to parse
     * @return the parsed {@link Date} object
     */
    public static Date parseISO8601Date(final String dateStringOrig) {
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
                return new Date(ISO8601_DATE_FORMAT.parseMillis(dateString));
            }
            // Handling edge case:
            // Joda-time can only handle up to year 292278993 but we are given
            // 292278994; So we parse the date string by first adjusting
            // the year to 292278993. Then we add 1 year back afterwards.
            final long milliLess365Days = ISO8601_DATE_FORMAT.parseMillis(temp);
            final long milli = milliLess365Days + MILLI_SECONDS_OF_365_DAYS;
            if (milli < 0) { // overflow!
                // re-parse the original date string using JodaTime so as to
                // throw an exception with a consistent message
                return new Date(ISO8601_DATE_FORMAT.parseMillis(dateString));
            }
            return new Date(milli);
        } catch (IllegalArgumentException e) {
            for (DateTimeFormatter dateTimeFormatter : ALTERNATE_ISO8601_FORMATTERS) {
                try {
                    // If the first ISO 8601 parser didn't work, try the alternate
                    // version which doesn't include fractional seconds
                    return new Date(dateTimeFormatter.parseMillis(dateString));
                } catch (Exception oops) {
                    // ignore
                }
            }

            throw e;
        }
    }

    /**
     * Formats the specified date as an ISO 8601 string.
     *
     * @param date the date to format
     * @return the ISO 8601 string representing the specified date
     */
    public static String formatISO8601Date(final Date date) {
        return ISO8601_DATE_FORMAT.print(date.getTime());
    }

    /**
     * Returns a date string with the prefix temporarily substituted, if applicable, so that
     * JodaTime can handle it. Otherwise, if not applicable, the original date string is returned.
     *
     * <p>See https://github.com/aws/aws-sdk-java/issues/233</p>
     */
    private static String tempDateStringForJodaTime(final String dateString) {
        final String fromPrefix = "292278994-";
        final String toPrefix = "292278993-";
        return dateString.startsWith(fromPrefix)
             ? toPrefix + dateString.substring(fromPrefix.length())
             : dateString;
    }
}
