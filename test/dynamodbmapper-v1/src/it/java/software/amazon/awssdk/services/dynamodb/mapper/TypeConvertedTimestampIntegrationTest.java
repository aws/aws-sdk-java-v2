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

package software.amazon.awssdk.services.dynamodb.mapper;

import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConvertedTimestamp;
import software.amazon.awssdk.services.dynamodb.pojos.AutoKeyAndVal;

/**
 * Tests updating component attribute fields correctly.
 */
public class TypeConvertedTimestampIntegrationTest extends AbstractKeyAndValIntegrationTestCase {

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testCalendarTimestamp() throws Exception {
        final KeyAndCalendarTimestamp object = new KeyAndCalendarTimestamp();
        object.setVal(Calendar.getInstance());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testCalendarTimestampNull() {
        final KeyAndCalendarTimestamp object = new KeyAndCalendarTimestamp();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testDateTimestamp() throws Exception {
        final KeyAndDateTimestamp object = new KeyAndDateTimestamp();
        object.setVal(Calendar.getInstance().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testDateTimestampNull() {
        final KeyAndDateTimestamp object = new KeyAndDateTimestamp();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testLongTimestamp() throws Exception {
        final KeyAndLongTimestamp object = new KeyAndLongTimestamp();
        object.setVal(Calendar.getInstance().getTime().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testLongTimestampNull() {
        final KeyAndLongTimestamp object = new KeyAndLongTimestamp();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testEstCalendarTimestamp() throws Exception {
        final KeyAndEstCalendarTimestamp object = new KeyAndEstCalendarTimestamp();
        object.setVal(Calendar.getInstance());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testEstDateTimestamp() {
        final KeyAndEstDateTimestamp object = new KeyAndEstDateTimestamp();
        object.setVal(Calendar.getInstance().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testEstLongTimestamp() {
        final KeyAndEstLongTimestamp object = new KeyAndEstLongTimestamp();
        object.setVal(Calendar.getInstance().getTime().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test(expected = DateTimeParseException.class)
    public void testStringNotTimestamp() {
        final KeyAndStringTimestamp object = new KeyAndStringTimestamp();
        object.setVal("NotTimestamp");
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test(expected = DynamoDbMappingException.class)
    @Ignore
    @ReviewBeforeRelease("This behavior is different with the java.time classes because you can construct a formatter using an empty string as a pattern.")
    public void testEmptyPattern() throws Exception {
        final KeyAndEmptyPattern object = new KeyAndEmptyPattern();
        object.setVal(Calendar.getInstance().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test(expected = DynamoDbMappingException.class)
    public void testInvalidPattern() throws Exception {
        final KeyAndInvalidPattern object = new KeyAndInvalidPattern();
        object.setVal(Calendar.getInstance().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * An object with {@code Calendar}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndCalendarTimestamp extends AutoKeyAndVal<Calendar> {
        @DynamoDbTypeConvertedTimestamp(pattern = "yyyy MMddHHmmssSSSz")
        public Calendar getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Calendar val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndDateTimestamp extends AutoKeyAndVal<Date> {
        @DynamoDbTypeConvertedTimestamp(pattern = "yyyy MMddHHmmssSSSz")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Long}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndLongTimestamp extends AutoKeyAndVal<Long> {
        @DynamoDbTypeConvertedTimestamp(pattern = "yyyy MMddHHmmssSSSz")
        public Long getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Long val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Calendar}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEstCalendarTimestamp extends AutoKeyAndVal<Calendar> {
        @DynamoDbTypeConvertedTimestamp(pattern = "yyyy MMddHHmmssSSSz", timeZone = "America/New_York")
        public Calendar getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Calendar val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEstDateTimestamp extends AutoKeyAndVal<Date> {
        @DynamoDbTypeConvertedTimestamp(pattern = "yyyy MMddHHmmssSSSz", timeZone = "America/New_York")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Long}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEstLongTimestamp extends AutoKeyAndVal<Long> {
        @DynamoDbTypeConvertedTimestamp(pattern = "yyyy MMddHHmmssSSSz", timeZone = "America/New_York")
        public Long getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Long val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code String}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndStringTimestamp extends AutoKeyAndVal<String> {
        @DynamoDbTypeConvertedTimestamp(pattern = "yyyy MMddHHmmssSSSz")
        public String getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final String val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEmptyPattern extends KeyAndDateTimestamp {
        @DynamoDbTypeConvertedTimestamp(pattern = "")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEmptyTimeZone extends KeyAndDateTimestamp {
        @DynamoDbTypeConvertedTimestamp(pattern = "yyyy MMddHHmmssSSSz", timeZone = "")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndInvalidPattern extends KeyAndDateTimestamp {
        @DynamoDbTypeConvertedTimestamp(pattern = "invalid")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

}
