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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.BuddhistChronology;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;

/**
 * Tests {@link DynamoDBTypeConvertedTimestamp}, which formats with Joda-Time patterns. The expected strings are the
 * ones the v1 mapper writes, so that tables written by v1 keep reading back.
 */
class DynamoDBTypeConvertedTimestampTest {

    /** 2023-11-14T22:13:20.123Z, which is 17:13:20.123 in New York (EST, UTC-5). */
    private static final long MILLIS = 1_700_000_000_123L;

    private static final LocalDynamoDB LOCAL = new LocalDynamoDB();
    private static DynamoDbClient localClient;

    /** A mapper that is only used to build table models, so it never sends a request. */
    private final DynamoDBMapper modelMapper = DynamoDBMapper.builder()
                                                             .dynamoDbClient(DynamoDbClient.builder()
                                                                 .region(Region.US_EAST_1)
                                                                 .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                 .build())
                                                             .build();

    @BeforeAll
    static void startLocalDynamoDb() {
        LOCAL.start();
        localClient = LOCAL.createClient();
    }

    @AfterAll
    static void stopLocalDynamoDb() {
        localClient.close();
        LOCAL.stop();
    }

    @Test
    void convert_defaultPattern_writesIso8601InUtc() {
        assertThat(convert(DefaultPatternDate.class, new Date(MILLIS))).isEqualTo("2023-11-14T22:13:20.123Z");
    }

    @Test
    void convertAndUnconvert_defaultPattern_eachSupportedJavaType_roundTrips() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(MILLIS);

        assertThat(roundTrip(DefaultPatternDate.class, new Date(MILLIS))).isEqualTo(new Date(MILLIS));
        assertThat(((Calendar) roundTrip(DefaultPatternCalendar.class, calendar)).getTimeInMillis()).isEqualTo(MILLIS);
        assertThat(roundTrip(DefaultPatternLong.class, MILLIS)).isEqualTo(MILLIS);
        assertThat(roundTrip(DefaultPatternString.class, "2023-11-14T22:13:20.123Z"))
            .isEqualTo("2023-11-14T22:13:20.123Z");
    }

    @Test
    void defaultPattern_formatting_matchesJodaAtDateBoundaries() {
        JodaTimestampConverter<Date> expected = defaultPatternJodaConverter();

        for (long millis : new long[] {Long.MIN_VALUE, -1L, 0L, MILLIS, Long.MAX_VALUE}) {
            Date date = new Date(millis);
            String formatted = convert(DefaultPatternDate.class, date);
            assertThat(formatted).as("epoch millis %s", millis).isEqualTo(expected.convert(date));
            assertThat(unconvertDefaultPattern(formatted)).as("epoch millis %s", millis)
                .isEqualTo(expected.unconvert(formatted))
                .isEqualTo(date);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2023-11-14T22:13:20.123Z",
        "2023-1-4T2:3:4.1Z",
        "2023-01-04t02:03:04.12z",
        "+2023-01-04T02:03:04.123Z",
        "-1-1-1T0:0:0.000Z",
        "12023-1-1T0:0:0.000Z",
        "0-1-1T0:0:0.000Z",
        "-0001-05-09T08:08:00.600Z",
        "2023-11-14T22:13:20.99Z"
    })
    void defaultPattern_parsingAcceptedByJoda_returnsSameDate(String input) {
        assertThat(unconvertDefaultPattern(input)).isEqualTo(defaultPatternJodaConverter().unconvert(input));
    }

    /** The default pattern is strict, as in v1: these are rejected even though the ISO-8601 parser accepts some. */
    @ParameterizedTest
    @ValueSource(strings = {
        "2023-11-14T22:13:20Z",
        "2023-11-14T22:13:20.1234Z",
        "2023-11-14T22:13:20.123+00:00",
        "2023-11-14T22:13:20.123+0000",
        "2023-11-14T22:13:20.123",
        "2023-11-14T22:13:20.Z",
        "2023-02-29T22:13:20.123Z"
    })
    void defaultPattern_inputsRejectedByJoda_remainRejected(String input) {
        JodaTimestampConverter<Date> expected = defaultPatternJodaConverter();

        assertThatThrownBy(() -> expected.unconvert(input)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> unconvertDefaultPattern(input)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * A {@code DateTime} carries its own chronology, which a {@link Date} cannot represent, so it stays on the
     * Joda-Time path even with the default pattern.
     */
    @Test
    void defaultPattern_dateTime_keepsJodaChronology() {
        DateTime buddhist = new DateTime(MILLIS, BuddhistChronology.getInstance(DateTimeZone.UTC));

        assertThat(convert(DefaultPatternDateTime.class, buddhist))
            .isEqualTo(new JodaTimestampConverter<DateTime>(
                DateTime.class, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC").convert(buddhist))
            .isEqualTo("2566-11-14T22:13:20.123Z");
    }

    private static JodaTimestampConverter<Date> defaultPatternJodaConverter() {
        return new JodaTimestampConverter<Date>(Date.class, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC");
    }

    private Object unconvertDefaultPattern(String stored) {
        return field(DefaultPatternDate.class).unconvert(AttributeValue.builder().s(stored).build());
    }

    @Test
    void convert_customPatternAndTimeZone_writesLocalTime() {
        assertThat(convert(UtcPatternDate.class, new Date(MILLIS))).isEqualTo("20231114221320123UTC");
        assertThat(convert(NewYorkPatternDate.class, new Date(MILLIS))).isEqualTo("20231114171320123EST");
    }

    @Test
    void convertAndUnconvert_eachSupportedType_roundTrips() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(MILLIS);

        assertThat(roundTrip(NewYorkPatternDate.class, new Date(MILLIS))).isEqualTo(new Date(MILLIS));
        assertThat(((Calendar) roundTrip(NewYorkPatternCalendar.class, calendar)).getTimeInMillis()).isEqualTo(MILLIS);
        assertThat(roundTrip(NewYorkPatternLong.class, MILLIS)).isEqualTo(MILLIS);
        assertThat(((DateTime) roundTrip(NewYorkPatternDateTime.class, new DateTime(MILLIS))).getMillis())
            .isEqualTo(MILLIS);
    }

    @Test
    void convertAndUnconvert_stringAttribute_readsAndWritesTheFormattedValue() {
        DynamoDBMapperFieldModel<UtcPatternString, Object> field = field(UtcPatternString.class);

        AttributeValue stored = field.convert("2014-03-06T14:28:58.123Z");

        assertThat(stored.s()).isEqualTo("20140306142858123UTC");
        assertThat(field.unconvert(stored)).isEqualTo("2014-03-06T14:28:58.123Z");
    }

    @Test
    void convert_stringAttributeThatIsNotADate_throwsIllegalArgumentException() {
        DynamoDBMapperFieldModel<UtcPatternString, Object> field = field(UtcPatternString.class);

        assertThatThrownBy(() -> field.convert("NotTimestamp")).isInstanceOf(IllegalArgumentException.class);
    }

    /** Joda-Time and {@code java.time} disagree on these pattern letters; the mapper keeps Joda-Time's meaning. */
    @Test
    void convert_patternLettersWithJodaMeaning_matchV1() {
        assertThat(convert(OffsetWithColonPattern.class, new Date(MILLIS))).isEqualTo("2023-11-14T17:13:20-05:00");
        // 2023-12-31 is in week-based year 2024, which java.time's 'Y' would write.
        assertThat(convert(YearOfEraPattern.class, new Date(1_703_980_800_000L))).isEqualTo("2023-12-31");
    }

    /** Joda-Time fills in the fields a pattern leaves out, from 1970-01-01T00:00 in the pattern's time zone. */
    @Test
    void unconvert_patternWithoutTime_readsMidnight() {
        Object date = field(DateOnlyPattern.class).unconvert(AttributeValue.builder().s("20231114").build());

        assertThat(date).isEqualTo(new Date(1_699_920_000_000L));
    }

    @Test
    void getTableModel_invalidPattern_throwsDynamoDBMappingException() {
        assertThatThrownBy(() -> modelMapper.getTableModel(InvalidPattern.class))
            .isInstanceOf(DynamoDBMappingException.class);
        assertThatThrownBy(() -> modelMapper.getTableModel(EmptyPattern.class))
            .isInstanceOf(DynamoDBMappingException.class);
    }

    /**
     * As in v1, the time zone is resolved with {@link TimeZone#getTimeZone(String)}, which falls back to GMT for an
     * unknown ID. Joda-Time names that zone UTC.
     */
    @Test
    void convert_unknownTimeZone_fallsBackToUtc() {
        assertThat(convert(UnknownTimeZone.class, new Date(MILLIS))).isEqualTo("20231114221320123UTC");
    }

    @Test
    void saveAndLoad_localDynamoDb_storesFormattedStringAndRoundTrips() {
        DynamoDBMapper mapper = DynamoDBMapper.builder().dynamoDbClient(localClient).build();
        localClient.createTable(mapper.generateCreateTableRequest(StoredItem.class).toBuilder()
                                      .billingMode(BillingMode.PAY_PER_REQUEST)
                                      .build());
        try {
            StoredItem item = new StoredItem();
            item.setId("item-1");
            item.setCreated(new Date(MILLIS));
            item.setUpdated(new Date(MILLIS));
            mapper.save(item);

            Map<String, AttributeValue> raw = localClient.getItem(r -> r.tableName(StoredItem.TABLE)
                .key(Collections.singletonMap("id", AttributeValue.builder().s("item-1").build()))
                .consistentRead(true)).item();
            assertThat(raw.get("created").s()).isEqualTo("20231114171320123EST");
            assertThat(raw.get("updated").s()).isEqualTo("2023-11-14T22:13:20.123Z");

            StoredItem loaded = mapper.load(StoredItem.class, "item-1");
            assertThat(loaded.getCreated()).isEqualTo(new Date(MILLIS));
            assertThat(loaded.getUpdated()).isEqualTo(new Date(MILLIS));
        } finally {
            localClient.deleteTable(r -> r.tableName(StoredItem.TABLE));
        }
    }

    @Test
    void convert_nullValue_omitsAttribute() {
        Map<String, AttributeValue> item = modelMapper.getTableModel(UtcPatternDate.class).convert(new UtcPatternDate());

        assertThat(item).doesNotContainKey("val");
    }

    private <T extends Holder<?>> String convert(Class<T> type, Object value) {
        return field(type).convert(value).s();
    }

    private <T extends Holder<?>> Object roundTrip(Class<T> type, Object value) {
        DynamoDBMapperFieldModel<T, Object> field = field(type);
        return field.unconvert(field.convert(value));
    }

    private <T extends Holder<?>> DynamoDBMapperFieldModel<T, Object> field(Class<T> type) {
        return modelMapper.getTableModel(type).field("val");
    }

    /**
     * A table class with a hash key and one attribute, {@code val}, of type {@code V}. The annotation is required for
     * the mapper to map getters that subclasses inherit from it.
     */
    @DynamoDBTable(tableName = "timestamps")
    public abstract static class Holder<V> {
        private String id;
        private V val;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public V getVal() {
            return val;
        }

        public void setVal(V val) {
            this.val = val;
        }
    }

    public static class DefaultPatternDate extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp
        public Date getVal() {
            return super.getVal();
        }
    }

    public static class DefaultPatternDateTime extends Holder<DateTime> {
        @Override
        @DynamoDBTypeConvertedTimestamp
        public DateTime getVal() {
            return super.getVal();
        }
    }

    public static class DefaultPatternCalendar extends Holder<Calendar> {
        @Override
        @DynamoDBTypeConvertedTimestamp
        public Calendar getVal() {
            return super.getVal();
        }
    }

    public static class DefaultPatternLong extends Holder<Long> {
        @Override
        @DynamoDBTypeConvertedTimestamp
        public Long getVal() {
            return super.getVal();
        }
    }

    public static class DefaultPatternString extends Holder<String> {
        @Override
        @DynamoDBTypeConvertedTimestamp
        public String getVal() {
            return super.getVal();
        }
    }

    public static class UtcPatternDate extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz")
        public Date getVal() {
            return super.getVal();
        }
    }

    public static class NewYorkPatternDate extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "America/New_York")
        public Date getVal() {
            return super.getVal();
        }
    }

    public static class NewYorkPatternCalendar extends Holder<Calendar> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "America/New_York")
        public Calendar getVal() {
            return super.getVal();
        }
    }

    public static class NewYorkPatternLong extends Holder<Long> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "America/New_York")
        public Long getVal() {
            return super.getVal();
        }
    }

    public static class NewYorkPatternDateTime extends Holder<DateTime> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "America/New_York")
        public DateTime getVal() {
            return super.getVal();
        }
    }

    public static class UtcPatternString extends Holder<String> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz")
        public String getVal() {
            return super.getVal();
        }
    }

    public static class OffsetWithColonPattern extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyy-MM-dd'T'HH:mm:ssZZ", timeZone = "America/New_York")
        public Date getVal() {
            return super.getVal();
        }
    }

    public static class YearOfEraPattern extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "YYYY-MM-dd")
        public Date getVal() {
            return super.getVal();
        }
    }

    public static class DateOnlyPattern extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMdd")
        public Date getVal() {
            return super.getVal();
        }
    }

    public static class InvalidPattern extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "invalid")
        public Date getVal() {
            return super.getVal();
        }
    }

    public static class EmptyPattern extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "")
        public Date getVal() {
            return super.getVal();
        }
    }

    public static class UnknownTimeZone extends Holder<Date> {
        @Override
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "Not/AZone")
        public Date getVal() {
            return super.getVal();
        }
    }

    @DynamoDBTable(tableName = StoredItem.TABLE)
    public static class StoredItem {
        static final String TABLE = "DynamoDBTypeConvertedTimestampTest";

        private String id;
        private Date created;
        private Date updated;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "America/New_York")
        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        @DynamoDBTypeConvertedTimestamp
        public Date getUpdated() {
            return updated;
        }

        public void setUpdated(Date updated) {
            this.updated = updated;
        }
    }
}
