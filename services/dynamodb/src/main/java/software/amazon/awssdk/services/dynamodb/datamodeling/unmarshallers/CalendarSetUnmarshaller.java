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

package software.amazon.awssdk.services.dynamodb.datamodeling.unmarshallers;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.util.DateUtils;

/**
 * An unmarshaller that unmarshals sets of ISO-8601-formatted dates as sets of
 * Java {@code Calendar} objects.
 */
public class CalendarSetUnmarshaller extends SsUnmarshaller {

    private static final CalendarSetUnmarshaller INSTANCE =
            new CalendarSetUnmarshaller();

    private CalendarSetUnmarshaller() {
    }

    public static CalendarSetUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        Set<Calendar> result = new HashSet<Calendar>();

        for (String s : value.ss()) {
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(DateUtils.parseIso8601Date(s));
            result.add(cal);
        }

        return result;
    }
}
