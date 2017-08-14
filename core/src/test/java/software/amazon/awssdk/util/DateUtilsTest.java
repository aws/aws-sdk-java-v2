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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.util.DateUtils.ALTERNATE_ISO_8601_DATE_FORMAT;

import com.fasterxml.jackson.core.JsonFactory;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import software.amazon.awssdk.protocol.json.SdkJsonGenerator;
import software.amazon.awssdk.protocol.json.StructuredJsonGenerator;

public class DateUtilsTest {
    private static final boolean DEBUG = false;
    private static final int MAX_MILLIS_YEAR = 292278994;

    @Test
    public void tt0031561767() {
        String input = "Fri, 16 May 2014 23:56:46 GMT";
        Instant instant = DateUtils.parseRfc1123Date(input);
        assertEquals(input, DateUtils.formatRfc1123Date(ZonedDateTime.ofInstant(instant, UTC)));
    }

    @Test
    public void formatIso8601Date() throws ParseException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone(UTC));
        String expected = sdf.format(date);
        String actual = DateUtils.formatIso8601Date(date);
        assertEquals(expected, actual);

        Instant expectedDate = sdf.parse(expected).toInstant();
        Instant actualDate = DateUtils.parseIso8601Date(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void formatRfc1123Date() throws ParseException {
        ZonedDateTime date = ZonedDateTime.now(UTC);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String expected = sdf.format(Date.from(date.toInstant()));
        String actual = DateUtils.formatRfc1123Date(date);
        assertEquals(expected, actual);

        Instant expectedDate = sdf.parse(expected).toInstant();
        Instant actualDate = DateUtils.parseRfc1123Date(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void parseCompressedIso8601Date() throws ParseException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone(UTC));
        String formatted = sdf.format(date);
        Instant expected = sdf.parse(formatted).toInstant();
        Instant actual = DateUtils.parseCompressedIso8601Date(formatted);
        assertEquals(expected, actual);
    }

    @Test
    public void parseRfc822Date() throws ParseException {
        Instant date = Instant.now();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone(UTC));
        String formatted = sdf.format(Date.from(date));
        Instant expected = sdf.parse(formatted).toInstant();
        Instant actual = DateUtils.parseRfc1123Date(formatted);
        assertEquals(expected, actual);
    }

    @Test
    public void parseIso8601Date() throws ParseException {
        checkParsing(DateTimeFormatter.ISO_DATE_TIME, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }

    @Test
    public void parseIso8601Date_usingAlternativeFormat() throws ParseException {
        checkParsing(ALTERNATE_ISO_8601_DATE_FORMAT, "yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    private void checkParsing(DateTimeFormatter dateTimeFormatter, String pattern) throws ParseException {
        ZonedDateTime date = ZonedDateTime.now(UTC);

        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getTimeZone(UTC));
        String formatted = sdf.format(Date.from(date.toInstant()));

        String alternative = dateTimeFormatter.format(ZonedDateTime.ofInstant(date.toInstant(), UTC));
        assertEquals(formatted, alternative);
        Instant expected = sdf.parse(formatted).toInstant();
        Instant actualDate = DateUtils.parseIso8601Date(formatted);
        assertEquals(expected, actualDate);
    }

    @Test
    public void alternateIso8601DateFormat() throws ParseException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone(UTC));
        String expected = sdf.format(date);
        String actual = ALTERNATE_ISO_8601_DATE_FORMAT.format(ZonedDateTime.ofInstant(date.toInstant(), UTC));
        assertEquals(expected, actual);

        Date expectedDate = sdf.parse(expected);
        ZonedDateTime actualDateTime = ZonedDateTime.parse(actual, ALTERNATE_ISO_8601_DATE_FORMAT);
        assertEquals(expectedDate, new Date(actualDateTime.toInstant().toEpochMilli()));
    }

    @Test
    public void legacyHandlingOfInvalidDate() throws ParseException {
        final String input = "2014-03-06T14:28:58.000Z.000Z";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone(UTC));
        sdf.parse(input);
    }

    @Test(expected = DateTimeParseException.class)
    public void invalidDate() {
        final String input = "2014-03-06T14:28:58.000Z.000Z";
        DateUtils.parseIso8601Date(input);
    }

    @Test
    public void test() {
        ZonedDateTime date = ZonedDateTime.now(UTC);
        System.out.println("         formatISO8601Date: " + ISO_DATE_TIME.format(date));
        System.out.println("alternateIso8601DateFormat: " +
                               ALTERNATE_ISO_8601_DATE_FORMAT.format(date));
    }

    @Test
    public void testIssue233() throws ParseException {
        // https://github.com/aws/aws-sdk-java/issues/233
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone(UTC));
        final String edgeCase = "292278994-08-17T07:12:55.807Z";
        Instant expected = sdf.parse(edgeCase).toInstant();
        if (DEBUG) {
            System.out.println("date: " + expected);
        }
        String formatted = ISO_DATE_TIME.format(ZonedDateTime.ofInstant(expected, UTC));
        if (DEBUG) {
            System.out.println("formatted: " + formatted);
        }
        assertEquals("+" + edgeCase, formatted);
        ZonedDateTime parsed = ZonedDateTime.ofInstant(DateUtils.parseIso8601Date(edgeCase), UTC);
        if (DEBUG) {
            System.out.println("parsed: " + parsed);
        }
        assertEquals(expected, parsed.toInstant());
        String reformatted = ISO_DATE_TIME.format(parsed);
        assertEquals("+" + edgeCase, reformatted);
    }

    @Test
    public void testIssue233JavaTimeLimit() {
        // https://github.com/aws/aws-sdk-java/issues/233
        String s = ALTERNATE_ISO_8601_DATE_FORMAT.format(
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), UTC));
        System.out.println("s: " + s);

        ZonedDateTime parsed = ZonedDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
        assertTrue(parsed.getYear() == MAX_MILLIS_YEAR);
    }

    @Test
    public void testIssueDaysDiff() throws ParseException {
        // https://github.com/aws/aws-sdk-java/issues/233
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone(UTC));
        String edgeCase = String.valueOf(MAX_MILLIS_YEAR) + "-08-17T07:12:55.807Z";
        String testCase = String.valueOf(MAX_MILLIS_YEAR - 1) +"-08-17T07:12:55.807Z";
        Date od = sdf.parse(edgeCase);
        Date testd = sdf.parse(testCase);
        long diff = od.getTime() - testd.getTime();
        assertTrue(diff == TimeUnit.DAYS.toMillis(365));
    }

    /**
     * Tests the Date marshalling and unmarshalling. Asserts that the value is
     * same before and after marshalling/unmarshalling
     */
    @Test
    public void testAwsFormatDateUtils() throws Exception {
        testInstant(System.currentTimeMillis());
        testInstant(1L);
        testInstant(0L);
    }

    private void testInstant(long dateInMilliSeconds) {
        Instant instant = Instant.ofEpochMilli(dateInMilliSeconds);
        String serverSpecificDateFormat = DateUtils.formatServiceSpecificDate(instant);

        Instant parsed = DateUtils.parseServiceSpecificInstant(String.valueOf(serverSpecificDateFormat));

        assertEquals(String.valueOf(instant.toEpochMilli()), String.valueOf(parsed.toEpochMilli()));
    }

    // See https://forums.aws.amazon.com/thread.jspa?threadID=158756
    @Test
    public void testNumericNoQuote() {
        StructuredJsonGenerator jw = new SdkJsonGenerator(new JsonFactory(), null);
        jw.writeStartObject();
        jw.writeFieldName("foo").writeValue(Instant.now());
        jw.writeEndObject();
        String s = new String(jw.getBytes(), Charset.forName("UTF-8"));
        // Something like: {"foo":1408378076.135}.
        // Note prior to the changes, it was {"foo":1408414571}
        // (with no decimal point nor places.)
        System.out.println(s);
        final String prefix = "{\"foo\":";
        assertTrue(s, s.startsWith(prefix));
        final int startPos = prefix.length();
        // verify no starting quote for the value
        assertFalse(s, s.startsWith("{\"foo\":\""));
        assertTrue(s, s.endsWith("}"));
        // Not: {"foo":"1408378076.135"}.
        // verify no ending quote for the value
        assertFalse(s, s.endsWith("\"}"));
        final int endPos = s.indexOf("}");
        final int dotPos = s.length() - 5;
        assertTrue(s, s.charAt(dotPos) == '.');
        // verify all numeric before '.'
        char[] a = s.toCharArray();
        for (int i = startPos; i < dotPos; i++) {
            assertTrue(a[i] <= '9' && a[i] >= '0');
        }
        int j = 0;
        // verify all numeric after '.'
        for (int i = dotPos + 1; i < endPos; i++) {
            assertTrue(a[i] <= '9' && a[i] >= '0');
            j++;
        }
        // verify decimal precision of exactly 3
        assertTrue(j == 3);
    }

    @Test
    public void numberOfDaysSinceEpoch() {
        final long now = System.currentTimeMillis();
        final long days = DateUtils.durationSinceEpoch(now).toDays();
        final long oneDayMilli = TimeUnit.DAYS.toMillis(1);
        // Could be equal at 00:00:00.
        assertTrue(now >= TimeUnit.DAYS.toMillis(days));
        assertTrue((now - TimeUnit.DAYS.toMillis(days)) <= oneDayMilli);
    }
}
