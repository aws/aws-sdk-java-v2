/*
 * Copyright 2014-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.mapper.dynamodb.internal.unmarshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.mapper.dynamodb.internal.MapperDateUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals sets of ISO-8601-formatted dates as sets of
 * Java {@code Calendar} objects.
 */
@SdkInternalApi
public class CalendarSetUnmarshaller extends SSUnmarshaller {

    private static final CalendarSetUnmarshaller INSTANCE =
            new CalendarSetUnmarshaller();

    public static CalendarSetUnmarshaller instance() {
        return INSTANCE;
    }

    private CalendarSetUnmarshaller() {
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        Set<Calendar> result = new HashSet<Calendar>();

        for (String s : value.ss()) {
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(MapperDateUtils.parseISO8601Date(s));
            result.add(cal);
        }

        return result;
    }
}
