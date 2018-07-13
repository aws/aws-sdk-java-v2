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

package software.amazon.awssdk.services.dynamodb.datamodeling.marshallers;

import static software.amazon.awssdk.utils.DateUtils.formatIso8601Date;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.StringSetAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals sets of Java {@code Calendar} objects into
 * DynamoDB StringSets (in ISO 8601 format, ie {"2014-01-01T00:00:00Z", ...}).
 */
public class CalendarSetToStringSetMarshaller
        implements StringSetAttributeMarshaller {

    private static final CalendarSetToStringSetMarshaller INSTANCE =
            new CalendarSetToStringSetMarshaller();

    private CalendarSetToStringSetMarshaller() {
    }

    public static CalendarSetToStringSetMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        @SuppressWarnings("unchecked")
        Set<Calendar> dates = (Set<Calendar>) obj;

        List<String> timestamps = new ArrayList<String>(dates.size());
        for (Calendar calendar : dates) {
            timestamps.add(formatIso8601Date(calendar.toInstant()));
        }

        return AttributeValue.builder().ss(timestamps).build();
    }
}
