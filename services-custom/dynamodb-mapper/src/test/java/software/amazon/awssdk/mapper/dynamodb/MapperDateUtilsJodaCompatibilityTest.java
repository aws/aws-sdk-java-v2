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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.mapper.dynamodb.internal.MapperDateUtils;

/**
 * Checks that the {@code java.time} implementation of {@link MapperDateUtils} formats and parses as the original
 * Joda-Time implementation ({@link JodaIso8601Reference}) did, except that values outside the range of {@link Date}
 * are rejected instead of overflowing.
 */
class MapperDateUtilsJodaCompatibilityTest {

    private static final long MAX_YEAR_9999_MILLIS = 253402300799999L;

    @Test
    void formatISO8601Date_randomDates_matchesJoda() {
        Random random = new Random(42);
        for (int i = 0; i < 100_000; i++) {
            long millis = random.nextLong();
            if (i % 2 == 0) {
                // Half of the values in years 0000 to 9999, where real data lives.
                millis = (long) ((random.nextDouble() * 2 - 1) * MAX_YEAR_9999_MILLIS);
            }
            if (i % 10 == 0) {
                millis -= millis % 1000;
            }
            assertFormatMatchesJoda(millis);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, -1L, 120L, 1_700_000_000_000L, 1_700_000_000_123L,
                          -62135596800000L, -62135596800001L, -62198755200000L, 253402300799999L,
                          253402300800000L, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1})
    void formatISO8601Date_edgeValues_matchesJodaAndRoundTrips(long millis) {
        assertFormatMatchesJoda(millis);
        Date date = new Date(millis);
        assertThat(MapperDateUtils.parseISO8601Date(MapperDateUtils.formatISO8601Date(date))).isEqualTo(date);
    }

    @Test
    void formatISO8601Date_zeroMillisAndLongYears_keepsFixedWidthUnsignedFormat() {
        assertThat(MapperDateUtils.formatISO8601Date(new Date(1_700_000_000_000L)))
            .isEqualTo("2023-11-14T22:13:20.000Z");
        assertThat(MapperDateUtils.formatISO8601Date(new Date(253402300800000L)))
            .isEqualTo("10000-01-01T00:00:00.000Z");
        assertThat(MapperDateUtils.formatISO8601Date(new Date(Long.MIN_VALUE)))
            .isEqualTo("-292275055-05-16T16:47:04.192Z");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0000-01-01T00:00:00.000Z",
        "0000-02-29T23:59:59.999Z",
        "1900-02-28T23:59:59.999Z",
        "1969-12-31T23:59:59.999Z",
        "1970-01-01T00:00:00.000Z",
        "2000-02-29T00:00:00.000Z",
        "9999-12-31T23:59:59.999Z"
    })
    void canonicalDateFastPath_matchesJodaAndRoundTrips(String input) {
        Date expected = JodaIso8601Reference.parseISO8601Date(input);
        assertThat(MapperDateUtils.parseISO8601Date(input)).isEqualTo(expected);
        assertThat(MapperDateUtils.formatISO8601Date(expected)).isEqualTo(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "-0001-12-31T23:59:59.999Z",
        "10000-01-01T00:00:00.000Z"
    })
    void datesOutsideCanonicalYearRange_useCompatibleFallback(String input) {
        Date expected = JodaIso8601Reference.parseISO8601Date(input);
        assertThat(MapperDateUtils.parseISO8601Date(input)).isEqualTo(expected);
        assertThat(MapperDateUtils.formatISO8601Date(expected))
            .isEqualTo(JodaIso8601Reference.formatISO8601Date(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2014-03-06T14:28:58.123Z", "2014-03-06T14:28:58.000Z", "2014-03-06T14:28:58Z", "2014-03-06t14:28:58.123z",
        "2014-03-06T14:28:58.1Z", "2014-03-06T14:28:58.12Z", "2014-03-06T14:28:58.123456Z",
        "2014-03-06T14:28:58.123456789Z", "2014-3-6T4:8:8Z",
        "2014-03-06T14:28:58.123+0000", "2014-03-06T14:28:58+0000", "2014-03-06T14:28:58.123-0000",
        "2014-03-06T14:28:58.123+00:00", "2014-03-06T14:28:58.123-07:00", "2014-03-06T14:28:58.123-0700",
        "2014-03-06T14:28:58.123-07", "2014-03-06T14:28:58.123+05:30", "2014-03-06T14:28:58.123+23:59",
        "2014-03-06T14:28:58.123+07:00:00", "2014-03-06T14:28:58.123+070000", "2014-03-06T14:28:58+0700001",
        "2014-03-06T14:28:58+07:00:00.12", "2021-05-10T17:12:13-07:00",
        "+2014-03-06T14:28:58.123Z", "0000-01-01T00:00:00.000Z", "-0001-01-01T00:00:00.000Z",
        "10000-01-01T00:00:00.000Z", "+10000-01-01T00:00:00.000Z", "292278994-08-17T07:12:55.807Z",
        "-292275055-05-16T16:47:04.192Z", "2016-02-29T00:00:00.000Z", "2014-12-31T23:59:59.999Z"
    })
    void parseISO8601Date_inputsJodaAccepts_matchesJoda(String input) {
        assertThat(MapperDateUtils.parseISO8601Date(input)).isEqualTo(JodaIso8601Reference.parseISO8601Date(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "garbage", "2014-03-06", "2014-03-06T14:28Z", "2014-03-06T14:28:58", "2014-03-06T14:28:58.123",
        "2014-03-06 14:28:58.123Z", "20140306T142858.123Z", "2014-03-06T14:28:58,123Z", "2014-03-06T14:28:58.Z",
        "2014-03-06T14:28:58.+00:00", "2014-03-06T14:28:58.-0700", "2014-03-06T14:28:58.",
        "2014-03-06T14:28:58.1234567890Z", "2014-03-06T24:00:00.000Z", "2014-03-06T14:60:00.000Z",
        "2014-03-06T14:28:60.000Z", "2014-02-29T00:00:00.000Z", "2014-02-30T00:00:00.000Z", "2014-13-01T00:00:00Z",
        "2014-03-06T14:28:58.123+24:00", "2014-03-06T14:28:58.123+07:60", "2014-03-06T14:28:58.123+7",
        "2014-03-06T14:28:58.123+07:0", "2014-03-06T14:28:58.123+07:0000", "2014-03-06T14:28:58.123UTC",
        "2014-03-06T14:28:58.123GMT", "2014-03-06T14:28:58.123ZZ", " 2014-03-06T14:28:58.123Z",
        "2014-03-06T14:28:58.123Z ", "2014-03-06T14:28:58.000Z.000Z", "\uFF12\uFF10\uFF11\uFF14-03-06T14:28:58Z"
    })
    void parseISO8601Date_inputsJodaRejects_throwsIllegalArgumentException(String input) {
        assertThatThrownBy(() -> JodaIso8601Reference.parseISO8601Date(input))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MapperDateUtils.parseISO8601Date(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * The Joda-Time parser accepted these and returned a date that had overflowed to an unrelated value. They are
     * outside the range of {@link Date}, so they are now rejected.
     */
    @ParameterizedTest
    @ValueSource(strings = {"292278994-08-17T07:12:55.808Z", "292278994-12-31T23:59:59Z",
                            "-292275055-05-16T16:47:04.191Z", "-292275055-01-01T00:00:00Z"})
    void parseISO8601Date_outsideDateRange_throwsWhereJodaOverflowed(String input) {
        Date joda = JodaIso8601Reference.parseISO8601Date(input);
        assertThat(JodaIso8601Reference.formatISO8601Date(joda)).isNotEqualTo(input);
        assertThatThrownBy(() -> MapperDateUtils.parseISO8601Date(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(input);
    }

    @Test
    void parseISO8601Date_randomFractionsAndOffsets_matchesJoda() {
        Random random = new Random(7);
        String symbols = "+-:.Zz ";
        List<String> mismatches = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) {
            StringBuilder input = new StringBuilder("2014-03-06T14:28:58");
            if (random.nextBoolean()) {
                input.append('.');
                for (int digits = random.nextInt(11); digits > 0; digits--) {
                    input.append((char) ('0' + random.nextInt(10)));
                }
            }
            for (int k = random.nextInt(10); k > 0; k--) {
                if (k % 3 == 0 && random.nextInt(4) == 0) {
                    input.append(symbols.charAt(random.nextInt(symbols.length())));
                } else if (input.charAt(input.length() - 1) >= '0' && random.nextInt(5) == 0) {
                    input.append(random.nextBoolean() ? '+' : '-');
                } else {
                    input.append((char) ('0' + random.nextInt(10)));
                }
            }
            String value = input.toString();
            String expected = parseOrError(() -> JodaIso8601Reference.parseISO8601Date(value));
            String actual = parseOrError(() -> MapperDateUtils.parseISO8601Date(value));
            if (!expected.equals(actual)) {
                mismatches.add(value + " joda=" + expected + " actual=" + actual);
            }
        }
        assertThat(mismatches).isEmpty();
    }

    @Test
    void parseISO8601Date_randomDatesInEveryOffsetForm_matchesJoda() {
        Random random = new Random(11);
        String[] forms = {"Z", "+HH", "+HHMM", "+HH:MM", "+HHMMSS", "+HH:MM:SS"};
        for (int i = 0; i < 50_000; i++) {
            long millis = (long) ((random.nextDouble() * 2 - 1) * MAX_YEAR_9999_MILLIS);
            String iso = MapperDateUtils.formatISO8601Date(new Date(millis));
            String local = iso.substring(0, iso.length() - 1);
            if (random.nextBoolean()) {
                local = local.substring(0, local.length() - 4);
            }
            String form = forms[random.nextInt(forms.length)];
            String offset = "Z".equals(form) ? "Z"
                : (random.nextBoolean() ? "+" : "-") + form.substring(1)
                    .replace("HH", twoDigits(random.nextInt(24)))
                    .replace("MM", twoDigits(random.nextInt(60)))
                    .replace("SS", twoDigits(random.nextInt(60)));
            String input = local + offset;
            assertThat(MapperDateUtils.parseISO8601Date(input))
                .as(input)
                .isEqualTo(JodaIso8601Reference.parseISO8601Date(input));
        }
    }

    private static void assertFormatMatchesJoda(long millis) {
        Date date = new Date(millis);
        assertThat(MapperDateUtils.formatISO8601Date(date))
            .as("epoch millis %d", millis)
            .isEqualTo(JodaIso8601Reference.formatISO8601Date(date));
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static String parseOrError(DateSupplier parse) {
        try {
            return String.valueOf(parse.get().getTime());
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException";
        }
    }

    private interface DateSupplier {
        Date get();
    }
}
