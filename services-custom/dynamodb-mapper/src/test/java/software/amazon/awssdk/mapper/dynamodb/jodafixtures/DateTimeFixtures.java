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

package software.amazon.awssdk.mapper.dynamodb.jodafixtures;

import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBDocument;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConverted;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConvertedEpochDate;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConvertedTimestamp;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConverter;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTyped;

/**
 * Table classes with {@code org.joda.time.DateTime} attributes, for {@code DynamoDBMapperWithoutJodaTimeTest}. That
 * test loads this package in a class loader that has Joda-Time while the mapper's class loader does not.
 */
public final class DateTimeFixtures {

    private DateTimeFixtures() {
    }

    /** A table class with a hash key. Subclasses add one {@code DateTime} attribute. */
    @DynamoDBTable(tableName = "dateTimes")
    public abstract static class Base {
        private String id;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Plain extends Base {
        private DateTime when;

        public DateTime getWhen() {
            return when;
        }

        public void setWhen(DateTime when) {
            this.when = when;
        }
    }

    public static class EpochDate extends Base {
        private DateTime when;

        @DynamoDBTypeConvertedEpochDate
        public DateTime getWhen() {
            return when;
        }

        public void setWhen(DateTime when) {
            this.when = when;
        }
    }

    public static class TypedNumber extends Base {
        private DateTime when;

        @DynamoDBTyped(DynamoDBAttributeType.N)
        public DateTime getWhen() {
            return when;
        }

        public void setWhen(DateTime when) {
            this.when = when;
        }
    }

    public static class TypedString extends Base {
        private DateTime when;

        @DynamoDBTyped(DynamoDBAttributeType.S)
        public DateTime getWhen() {
            return when;
        }

        public void setWhen(DateTime when) {
            this.when = when;
        }
    }

    public static class Timestamp extends Base {
        private DateTime when;

        @DynamoDBTypeConvertedTimestamp
        public DateTime getWhen() {
            return when;
        }

        public void setWhen(DateTime when) {
            this.when = when;
        }
    }

    public static class ListOfDateTimes extends Base {
        private List<DateTime> when;

        public List<DateTime> getWhen() {
            return when;
        }

        public void setWhen(List<DateTime> when) {
            this.when = when;
        }
    }

    public static class MapOfDateTimes extends Base {
        private Map<String, DateTime> when;

        public Map<String, DateTime> getWhen() {
            return when;
        }

        public void setWhen(Map<String, DateTime> when) {
            this.when = when;
        }
    }

    public static class NestedDocument extends Base {
        private Event when;

        public Event getWhen() {
            return when;
        }

        public void setWhen(Event when) {
            this.when = when;
        }
    }

    @DynamoDBDocument
    public static class Event {
        private DateTime at;

        @DynamoDBTypeConvertedEpochDate
        public DateTime getAt() {
            return at;
        }

        public void setAt(DateTime at) {
            this.at = at;
        }
    }

    /**
     * Uses an application-defined converter, so the mapper never handles the {@code DateTime} itself. This works
     * whether or not the mapper can load Joda-Time.
     */
    public static class CustomConverter extends Base {
        private DateTime when;

        @DynamoDBTypeConverted(converter = EpochMillisConverter.class)
        public DateTime getWhen() {
            return when;
        }

        public void setWhen(DateTime when) {
            this.when = when;
        }

        /** Returns an item whose {@code when} is the given instant. */
        public static CustomConverter withMillis(long millis) {
            CustomConverter item = new CustomConverter();
            item.setId("1");
            item.setWhen(new DateTime(millis));
            return item;
        }

        public long whenMillis() {
            return when.getMillis();
        }
    }

    public static final class EpochMillisConverter implements DynamoDBTypeConverter<String, DateTime> {
        @Override
        public String convert(DateTime object) {
            return String.valueOf(object.getMillis());
        }

        @Override
        public DateTime unconvert(String object) {
            return new DateTime(Long.parseLong(object));
        }
    }
}
