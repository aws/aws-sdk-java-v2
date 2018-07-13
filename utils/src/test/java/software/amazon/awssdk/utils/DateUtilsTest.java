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

package software.amazon.awssdk.utils;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.utils.DateUtils.ALTERNATE_ISO_8601_DATE_FORMAT;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class DateUtilsTest {
    private static final boolean DEBUG = false;
    private static final int MAX_MILLIS_YEAR = 292278994;
    private static final SimpleDateFormat COMMON_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat LONG_DATE_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    static {
        COMMON_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(UTC));
        LONG_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(UTC));
    }

    private static final Instant INSTANT = Instant.ofEpochMilli(1400284606000L);

    @Test
    public void tt0031561767() {
        String input = "Fri, 16 May 2014 23:56:46 GMT";
        Instant instant = DateUtils.parseRfc1123Date(input);
        assertEquals(input, DateUtils.formatRfc1123Date(instant));
    }

    @Test
    public void formatIso8601Date() throws ParseException {
        Date date = Date.from(INSTANT);
        String expected = COMMON_DATE_FORMAT.format(date);
        String actual = DateUtils.formatIso8601Date(date.toInstant());
        assertEquals(expected, actual);

        Instant expectedDate = COMMON_DATE_FORMAT.parse(expected).toInstant();
        Instant actualDate = DateUtils.parseIso8601Date(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void formatRfc1123Date() throws ParseException {
        String string = DateUtils.formatRfc1123Date(INSTANT);
        Instant parsedDateAsInstant = LONG_DATE_FORMAT.parse(string).toInstant();
        assertEquals(INSTANT, parsedDateAsInstant);

        String formattedDate = LONG_DATE_FORMAT.format(Date.from(INSTANT));
        Instant parsedInstant = DateUtils.parseRfc1123Date(formattedDate);
        assertEquals(INSTANT, parsedInstant);
    }

    @Test
    public void parseRfc822Date() throws ParseException {
        String formatted = LONG_DATE_FORMAT.format(Date.from(INSTANT));
        Instant expected = LONG_DATE_FORMAT.parse(formatted).toInstant();
        Instant actual = DateUtils.parseRfc1123Date(formatted);
        assertEquals(expected, actual);
    }

    @Test
    public void parseIso8601Date() throws ParseException {
        checkParsing(DateTimeFormatter.ISO_INSTANT, COMMON_DATE_FORMAT);
    }

    @Test
    public void parseIso8601Date_usingAlternativeFormat() throws ParseException {
        checkParsing(ALTERNATE_ISO_8601_DATE_FORMAT, COMMON_DATE_FORMAT);
    }

    private void checkParsing(DateTimeFormatter dateTimeFormatter, SimpleDateFormat dateFormat) throws ParseException {
        String formatted = dateFormat.format(Date.from(INSTANT));
        String alternative = dateTimeFormatter.format(INSTANT);
        assertEquals(formatted, alternative);
        Instant expected = dateFormat.parse(formatted).toInstant();
        Instant actualDate = DateUtils.parseIso8601Date(formatted);
        assertEquals(expected, actualDate);
    }

    @Test
    public void alternateIso8601DateFormat() throws ParseException {
        String expected = COMMON_DATE_FORMAT.format(Date.from(INSTANT));
        String actual = ALTERNATE_ISO_8601_DATE_FORMAT.format(INSTANT);
        assertEquals(expected, actual);

        Date expectedDate = COMMON_DATE_FORMAT.parse(expected);
        ZonedDateTime actualDateTime = ZonedDateTime.parse(actual, ALTERNATE_ISO_8601_DATE_FORMAT);
        assertEquals(expectedDate, Date.from(actualDateTime.toInstant()));
    }

    @Test(expected = ParseException.class)
    public void legacyHandlingOfInvalidDate() throws ParseException {
        final String input = "2014-03-06T14:28:58.000Z.000Z";
        COMMON_DATE_FORMAT.parse(input);
    }

    @Test(expected = DateTimeParseException.class)
    public void invalidDate() {
        final String input = "2014-03-06T14:28:58.000Z.000Z";
        DateUtils.parseIso8601Date(input);
    }

    @Test
    public void testIssue233() throws ParseException {
        // https://github.com/aws/aws-sdk-java/issues/233
        final String edgeCase = String.valueOf(MAX_MILLIS_YEAR) + "-08-17T07:12:00Z";
        Instant expected = COMMON_DATE_FORMAT.parse(edgeCase).toInstant();
        if (DEBUG) {
            System.out.println("date: " + expected);
        }
        String formatted = DateUtils.formatIso8601Date(expected);
        if (DEBUG) {
            System.out.println("formatted: " + formatted);
        }
        // we have '+' sign as prefix for years. See java.time.format.SignStyle.EXCEEDS_PAD
        assertEquals(edgeCase, formatted.substring(1));

        Instant parsed = DateUtils.parseIso8601Date(formatted);
        if (DEBUG) {
            System.out.println("parsed: " + parsed);
        }
        assertEquals(expected, parsed);
        String reformatted = ISO_INSTANT.format(parsed);
        // we have '+' sign as prefix for years. See java.time.format.SignStyle.EXCEEDS_PAD
        assertEquals(edgeCase, reformatted.substring(1));
    }

    @Test
    public void testIssue233JavaTimeLimit() {
        // https://github.com/aws/aws-sdk-java/issues/233
        String s = ALTERNATE_ISO_8601_DATE_FORMAT.format(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), UTC));
        System.out.println("s: " + s);

        Instant parsed = DateUtils.parseIso8601Date(s);
        assertEquals(ZonedDateTime.ofInstant(parsed, UTC).getYear(), MAX_MILLIS_YEAR);
    }

    @Test
    public void testIssueDaysDiff() throws ParseException {
        // https://github.com/aws/aws-sdk-java/issues/233
        COMMON_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(UTC));
        String edgeCase = String.valueOf(MAX_MILLIS_YEAR) + "-08-17T07:12:55Z";
        String testCase = String.valueOf(MAX_MILLIS_YEAR - 1) + "-08-17T07:12:55Z";
        Date edgeDate = COMMON_DATE_FORMAT.parse(edgeCase);
        Date testDate = COMMON_DATE_FORMAT.parse(testCase);
        long diff = edgeDate.getTime() - testDate.getTime();
        assertTrue(diff == TimeUnit.DAYS.toMillis(365));
    }

    @Test
    public void numberOfDaysSinceEpoch() {
        final long now = System.currentTimeMillis();
        final long days = DateUtils.numberOfDaysSinceEpoch(now);
        final long oneDayMilli = Duration.ofDays(1).toMillis();
        // Could be equal at 00:00:00.
        assertTrue(now >=  Duration.ofDays(days).toMillis());
        assertTrue((now -  Duration.ofDays(days).toMillis()) <= oneDayMilli);
    }
}
