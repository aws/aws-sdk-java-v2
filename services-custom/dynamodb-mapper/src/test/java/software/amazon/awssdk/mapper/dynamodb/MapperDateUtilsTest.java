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
package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import org.junit.Test;

/**
 * Tests ISO-8601 parsing and formatting in {@link MapperDateUtils}, including fractional seconds,
 * UTC offsets, and year boundary edge cases.
 */
public class MapperDateUtilsTest {

    @Test
    public void formatIso8601Date() throws ParseException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        String expected = sdf.format(date);
        String actual = MapperDateUtils.formatISO8601Date(date);
        assertEquals(expected, actual);

        Date expectedDate = sdf.parse(expected);
        Date actualDate = MapperDateUtils.parseISO8601Date(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void formatISO8601Date_zeroMilliseconds_keepsFractionalSeconds() {
        Date date = new Date(0L);
        assertEquals("1970-01-01T00:00:00.000Z", MapperDateUtils.formatISO8601Date(date));
    }

    @Test
    public void parseIso8601Date_usingAlternativeFormat() throws ParseException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        String formatted = sdf.format(date);

        Date expectedDate = sdf.parse(formatted);
        Date actualDate = MapperDateUtils.parseISO8601Date(formatted);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void iso8601_withUTCOffset() throws ParseException {
        String input = "2021-05-10T17:12:13-07:00";
        Date actualDate = MapperDateUtils.parseISO8601Date(input);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        assertEquals(sdf.parse(input), actualDate);
    }

    @Test
    public void parseIso8601Date_spotFleetPlusZeroSuffix_isNormalized() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        Date expected = sdf.parse("2014-03-06T14:28:58.123Z");
        assertEquals(expected, MapperDateUtils.parseISO8601Date("2014-03-06T14:28:58.123+0000"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidDate() throws ParseException {
        final String input = "2014-03-06T14:28:58.000Z.000Z";
        MapperDateUtils.parseISO8601Date(input);
    }

    @Test
    public void testIssue233() throws ParseException {
        // https://github.com/aws/aws-sdk-java/issues/233
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        final String edgeCase = "292278994-08-17T07:12:55.807Z";
        Date expected = sdf.parse(edgeCase);
        String formatted = MapperDateUtils.formatISO8601Date(expected);
        assertEquals(edgeCase, formatted);
        Date parsed = MapperDateUtils.parseISO8601Date(edgeCase);
        assertEquals(expected, parsed);
        String reformatted = MapperDateUtils.formatISO8601Date(parsed);
        assertEquals(edgeCase, reformatted);
    }

    @Test
    public void testIssueDaysDiff() throws ParseException {
        // https://github.com/aws/aws-sdk-java/issues/233
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        String edgeCase = "292278994-08-17T07:12:55.807Z";
        String testCase = "292278993-08-17T07:12:55.807Z";
        Date od = sdf.parse(edgeCase);
        Date testd = sdf.parse(testCase);
        long diff = od.getTime() - testd.getTime();
        assertTrue(diff == 365L*24*60*60*1000);
    }

    @Test
    public void testIssue233Overflows() throws ParseException {
        // 1 milli second passed the max time. Fails for JT <2.9, succeeds on >=2.9.
        testOverflow("292278994-08-17T07:12:55.808Z", true);

        // 1 year passed the max year
        testOverflow("292278995-01-17T07:12:55.807Z", false);
    }

    private void testOverflow(String edgeCase, boolean successExpected) {
        try {
            Date parsed = MapperDateUtils.parseISO8601Date(edgeCase);
            if (!successExpected) {
                // We should have failed!
                fail("Unexpected success: " + edgeCase + " --> " + parsed);
            }
        } catch (IllegalArgumentException ex) {
            if (successExpected) {
                fail("Unexpected failure: " + edgeCase);
            }
        }
    }
}
