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

package software.amazon.awssdk.mapper.dynamodb.internal;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * ISO-8601 formatting and parsing for the mapper's {@link Date} and {@link java.util.Calendar} attributes.
 *
 * <p>This reproduces the v1 {@code com.amazonaws.util.DateUtils} methods, which used Joda-Time, with
 * {@code java.time}, so that dates round-trip with data written by the v1 mapper:
 * <ul>
 *     <li>Formatting always writes three fractional digits and a {@code Z} suffix, and writes years past 9999
 *     without a {@code +} sign. Values with four-digit years sort as strings in time order, which matters for key
 *     attributes.</li>
 *     <li>Parsing accepts the same inputs as v1: an optional {@code +} before the year, 1 to 9 fractional digits,
 *     and a {@code Z} or numeric offset ({@code +HH}, {@code +HHMM}, {@code +HH:MM}, {@code +HHMMSS},
 *     {@code +HH:MM:SS}, optionally followed by milliseconds). It throws {@link IllegalArgumentException} for
 *     anything else.</li>
 * </ul>
 * One difference from v1: a string outside the range of {@link Date} is rejected. v1 returned a date that had
 * silently overflowed to an unrelated value.
 */
@SdkInternalApi
public final class MapperDateUtils {

    private static final String CANONICAL_DATE_TEMPLATE = "0000-00-00T00:00:00.000Z";
    private static final int CANONICAL_DATE_LENGTH = CANONICAL_DATE_TEMPLATE.length();

    // Field offsets in yyyy-MM-ddTHH:mm:ss.SSSZ:
    // 0    5  8  11 14 17  20
    private static final int YEAR_OFFSET = 0;
    private static final int MONTH_OFFSET = 5;
    private static final int DAY_OFFSET = 8;
    private static final int HOUR_OFFSET = 11;
    private static final int MINUTE_OFFSET = 14;
    private static final int SECOND_OFFSET = 17;
    private static final int MILLISECOND_OFFSET = 20;

    private static final int MIN_CANONICAL_YEAR = 0;
    private static final int MAX_CANONICAL_YEAR = 9999;
    private static final int HOURS_PER_DAY = 24;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;
    private static final int MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * MILLIS_PER_SECOND;
    private static final int MILLIS_PER_HOUR = MINUTES_PER_HOUR * MILLIS_PER_MINUTE;
    private static final long MILLIS_PER_DAY = (long) HOURS_PER_DAY * MILLIS_PER_HOUR;

    /** Formats dates with years outside the four-digit fast path. */
    private static final DateTimeFormatter ISO8601_FALLBACK_FORMAT = new DateTimeFormatterBuilder()
        .appendValue(YEAR, 4, 10, SignStyle.NORMAL)
        .appendPattern("-MM-dd'T'HH:mm:ss.")
        .appendValue(MILLI_OF_SECOND, 3)
        .appendLiteral('Z')
        .toFormatter(Locale.ROOT)
        .withChronology(IsoChronology.INSTANCE)
        .withZone(ZoneOffset.UTC);

    /** The date and time part of a parsed value. The offset is parsed separately, see {@link #parseOffsetMillis}. */
    private static final DateTimeFormatter ISO8601_FALLBACK_PARSER = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(YEAR, 1, 9, SignStyle.NORMAL)
        .appendLiteral('-')
        .appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
        .appendLiteral('-')
        .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
        .appendLiteral('T')
        .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
        .appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE)
        .optionalStart().appendFraction(NANO_OF_SECOND, 1, 9, true).optionalEnd()
        .toFormatter(Locale.ROOT)
        .withChronology(IsoChronology.INSTANCE)
        .withResolverStyle(ResolverStyle.STRICT);

    private MapperDateUtils() {
    }

    /**
     * Parses the specified date string as an ISO 8601 date and returns the {@link Date} object.
     *
     * @param dateString the date string to parse
     * @return the parsed {@link Date} object
     * @throws IllegalArgumentException if the string is not a supported ISO 8601 date, or is outside the range
     *                                  of {@link Date}
     */
    public static Date parseISO8601Date(String dateString) {
        Date canonicalDate = parseCanonicalDate(dateString);
        return canonicalDate != null ? canonicalDate : parseISO8601DateFallback(dateString);
    }

    /** Parses supported inputs not handled by the fixed 24-character parser. */
    private static Date parseISO8601DateFallback(String dateString) {
        String value = dateString;
        // Joda-Time accepts an explicit '+' before a positive year.
        if (value.length() > 1 && value.charAt(0) == '+' && isDigit(value.charAt(1))) {
            value = value.substring(1);
        }
        ParsePosition position = new ParsePosition(0);
        TemporalAccessor parsed;
        try {
            parsed = ISO8601_FALLBACK_PARSER.parse(value, position);
        } catch (DateTimeException e) {
            throw invalidDate(dateString, e);
        }
        int end = position.getIndex();
        // Joda-Time requires at least one digit after the decimal point. Java 8's appendFraction accepts a bare '.'
        // (later JDKs leave it unparsed, and the offset parser rejects it), so reject it here on every JDK.
        if (value.charAt(end - 1) == '.') {
            throw invalidDate(dateString, null);
        }
        long offsetMillis = parseOffsetMillis(value, end, dateString);
        LocalDateTime local = LocalDateTime.from(parsed);
        try {
            long utcMillis = toEpochMilli(local.toEpochSecond(ZoneOffset.UTC), local.getNano());
            return new Date(Math.subtractExact(utcMillis, offsetMillis));
        } catch (ArithmeticException e) {
            throw invalidDate(dateString, e);
        }
    }

    /**
     * Formats the specified date as an ISO 8601 string, for example {@code 2014-03-06T14:28:58.000Z}.
     *
     * @param date the date to format
     * @return the ISO 8601 string representing the specified date
     */
    public static String formatISO8601Date(Date date) {
        long epochMillis = date.getTime();
        long epochDay = Math.floorDiv(epochMillis, MILLIS_PER_DAY);
        LocalDate localDate = LocalDate.ofEpochDay(epochDay);
        int year = localDate.getYear();
        if (year < MIN_CANONICAL_YEAR || year > MAX_CANONICAL_YEAR) {
            return ISO8601_FALLBACK_FORMAT.format(Instant.ofEpochMilli(epochMillis));
        }

        int millisOfDay = (int) Math.floorMod(epochMillis, MILLIS_PER_DAY);
        return formatCanonicalDate(localDate, millisOfDay);
    }

    /** Formats a date in the fixed 24-character form used for four-digit years. */
    private static String formatCanonicalDate(LocalDate date, int millisOfDay) {
        int hour = millisOfDay / MILLIS_PER_HOUR;
        millisOfDay %= MILLIS_PER_HOUR;
        int minute = millisOfDay / MILLIS_PER_MINUTE;
        millisOfDay %= MILLIS_PER_MINUTE;
        int second = millisOfDay / MILLIS_PER_SECOND;
        int millisecond = millisOfDay % MILLIS_PER_SECOND;

        char[] result = CANONICAL_DATE_TEMPLATE.toCharArray();
        writeFourDigits(result, YEAR_OFFSET, date.getYear());
        writeTwoDigits(result, MONTH_OFFSET, date.getMonthValue());
        writeTwoDigits(result, DAY_OFFSET, date.getDayOfMonth());
        writeTwoDigits(result, HOUR_OFFSET, hour);
        writeTwoDigits(result, MINUTE_OFFSET, minute);
        writeTwoDigits(result, SECOND_OFFSET, second);
        writeThreeDigits(result, MILLISECOND_OFFSET, millisecond);
        return new String(result);
    }

    /**
     * Parses the fixed 24-character format, or returns {@code null} if it does not match.
     */
    private static Date parseCanonicalDate(String value) {
        if (!hasCanonicalDateShape(value)) {
            return null;
        }

        int year = parseFourDigits(value, YEAR_OFFSET);
        int month = parseTwoDigits(value, MONTH_OFFSET);
        int day = parseTwoDigits(value, DAY_OFFSET);
        int hour = parseTwoDigits(value, HOUR_OFFSET);
        int minute = parseTwoDigits(value, MINUTE_OFFSET);
        int second = parseTwoDigits(value, SECOND_OFFSET);
        int millisecond = parseThreeDigits(value, MILLISECOND_OFFSET);
        if (year < MIN_CANONICAL_YEAR || month < 0 || day < 0
            || !isValidTimeOfDay(hour, minute, second, millisecond)) {
            return null;
        }

        try {
            long epochDay = LocalDate.of(year, month, day).toEpochDay();
            long millisOfDay = hour * (long) MILLIS_PER_HOUR
                               + minute * (long) MILLIS_PER_MINUTE
                               + second * (long) MILLIS_PER_SECOND
                               + millisecond;
            return new Date(Math.addExact(Math.multiplyExact(epochDay, MILLIS_PER_DAY), millisOfDay));
        } catch (DateTimeException | ArithmeticException e) {
            return null;
        }
    }

    private static boolean hasCanonicalDateShape(String value) {
        return value.length() == CANONICAL_DATE_LENGTH
               && hasCanonicalCharacter(value, MONTH_OFFSET - 1)
               && hasCanonicalCharacter(value, DAY_OFFSET - 1)
               && hasCanonicalCharacter(value, HOUR_OFFSET - 1)
               && hasCanonicalCharacter(value, MINUTE_OFFSET - 1)
               && hasCanonicalCharacter(value, SECOND_OFFSET - 1)
               && hasCanonicalCharacter(value, MILLISECOND_OFFSET - 1)
               && hasCanonicalCharacter(value, CANONICAL_DATE_LENGTH - 1);
    }

    private static boolean hasCanonicalCharacter(String value, int index) {
        return value.charAt(index) == CANONICAL_DATE_TEMPLATE.charAt(index);
    }

    private static boolean isValidTimeOfDay(int hour, int minute, int second, int millisecond) {
        return hour >= 0 && hour < HOURS_PER_DAY
               && minute >= 0 && minute < MINUTES_PER_HOUR
               && second >= 0 && second < SECONDS_PER_MINUTE
               && millisecond >= 0 && millisecond < MILLIS_PER_SECOND;
    }

    /**
     * Parses the offset that starts at {@code index}, which must end the string: {@code Z}, or {@code +HH},
     * {@code +HHMM}, {@code +HH:MM}, {@code +HHMMSS} or {@code +HH:MM:SS} with HH up to 23 and MM and SS up to 59,
     * optionally followed by 1 to 3 millisecond digits (directly in the compact forms, after {@code .} in the colon
     * forms). These are the offsets Joda-Time's ISO parser accepts.
     */
    private static long parseOffsetMillis(String value, int index, String original) {
        int length = value.length();
        int i = index;
        if (i == length - 1 && (value.charAt(i) == 'Z' || value.charAt(i) == 'z')) {
            return 0;
        }
        if (i >= length || (value.charAt(i) != '+' && value.charAt(i) != '-')) {
            throw invalidDate(original, null);
        }
        int sign = value.charAt(i) == '-' ? -1 : 1;
        i++;

        int[] fields = new int[3];
        int count = 0;
        Boolean colons = null;
        while (count < fields.length) {
            if (count > 0) {
                if (i == length) {
                    break;
                }
                boolean colon = value.charAt(i) == ':';
                if (colons == null) {
                    colons = colon;
                } else if (colons != colon) {
                    throw invalidDate(original, null);
                }
                if (colon) {
                    i++;
                }
            }
            if (i + 2 > length || !isDigit(value.charAt(i)) || !isDigit(value.charAt(i + 1))) {
                throw invalidDate(original, null);
            }
            fields[count++] = (value.charAt(i) - '0') * 10 + (value.charAt(i + 1) - '0');
            i += 2;
        }

        int millis = 0;
        if (count == fields.length && i < length) {
            if (Boolean.TRUE.equals(colons)) {
                if (value.charAt(i) != '.') {
                    throw invalidDate(original, null);
                }
                i++;
            }
            int digits = 0;
            while (i < length && digits < 3 && isDigit(value.charAt(i))) {
                millis = millis * 10 + (value.charAt(i++) - '0');
                digits++;
            }
            if (digits == 0) {
                throw invalidDate(original, null);
            }
            for (; digits < 3; digits++) {
                millis *= 10;
            }
        }

        if (i != length || fields[0] > 23 || fields[1] > 59 || fields[2] > 59) {
            throw invalidDate(original, null);
        }
        return sign * ((fields[0] * 3600L + fields[1] * 60L + fields[2]) * 1000L + millis);
    }

    /**
     * Same as {@code Instant.ofEpochSecond(epochSecond, nano).toEpochMilli()}, which on Java 8 overflows for values
     * just above {@link Long#MIN_VALUE} milliseconds even though they fit in a {@link Date}.
     */
    private static long toEpochMilli(long epochSecond, int nano) {
        if (epochSecond < 0 && nano > 0) {
            return Math.addExact(Math.multiplyExact(epochSecond + 1, 1000L), nano / 1_000_000 - 1000);
        }
        return Math.addExact(Math.multiplyExact(epochSecond, 1000L), nano / 1_000_000);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static int parseTwoDigits(String value, int index) {
        char first = value.charAt(index);
        char second = value.charAt(index + 1);
        if (!isDigit(first) || !isDigit(second)) {
            return -1;
        }
        return (first - '0') * 10 + second - '0';
    }

    private static int parseThreeDigits(String value, int index) {
        int firstTwo = parseTwoDigits(value, index);
        char third = value.charAt(index + 2);
        if (firstTwo < 0 || !isDigit(third)) {
            return -1;
        }
        return firstTwo * 10 + third - '0';
    }

    private static int parseFourDigits(String value, int index) {
        int firstTwo = parseTwoDigits(value, index);
        int secondTwo = parseTwoDigits(value, index + 2);
        if (firstTwo < 0 || secondTwo < 0) {
            return -1;
        }
        return firstTwo * 100 + secondTwo;
    }

    private static void writeTwoDigits(char[] target, int index, int value) {
        target[index] = (char) ('0' + value / 10);
        target[index + 1] = (char) ('0' + value % 10);
    }

    private static void writeThreeDigits(char[] target, int index, int value) {
        target[index] = (char) ('0' + value / 100);
        target[index + 1] = (char) ('0' + value / 10 % 10);
        target[index + 2] = (char) ('0' + value % 10);
    }

    private static void writeFourDigits(char[] target, int index, int value) {
        target[index] = (char) ('0' + value / 1000);
        target[index + 1] = (char) ('0' + value / 100 % 10);
        target[index + 2] = (char) ('0' + value / 10 % 10);
        target[index + 3] = (char) ('0' + value % 10);
    }

    private static IllegalArgumentException invalidDate(String dateString, Exception cause) {
        return new IllegalArgumentException("Invalid ISO 8601 date: \"" + dateString + "\"", cause);
    }
}
