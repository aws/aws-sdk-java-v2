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

import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The Joda-Time implementation behind {@link DynamoDBTypeConvertedTimestamp}. It formats with
 * {@link DateTimeFormat#forPattern(String)}, as the v1 mapper did, so that stored values stay compatible.
 *
 * <p>Load this class only after {@link JodaTimeSupport#isAvailable()} returns {@code true}.
 */
@SdkInternalApi
final class JodaTimestampConverter<T> implements DynamoDBTypeConverter<String,T> {
    private final DynamoDBTypeConverter<DateTime,T> converter;
    private final DateTimeFormatter formatter;

    /**
     * @throws IllegalArgumentException if the pattern or time zone is invalid
     */
    JodaTimestampConverter(Class<T> targetType, String pattern, String timeZone) {
        this.formatter = DateTimeFormat.forPattern(pattern).withZone(
            DateTimeZone.forTimeZone(StandardTypeConverters.Scalar.TIME_ZONE.<TimeZone>convert(timeZone)));
        this.converter = StandardTypeConverters.factory().getConverter(DateTime.class, targetType);
    }

    @Override
    public String convert(final T object) {
        return formatter.print(converter.convert(object));
    }

    @Override
    public T unconvert(final String object) {
        return converter.unconvert(formatter.parseDateTime(object));
    }
}
