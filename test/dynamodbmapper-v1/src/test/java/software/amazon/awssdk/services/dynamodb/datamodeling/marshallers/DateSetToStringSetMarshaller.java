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

package software.amazon.awssdk.services.dynamodb.datamodeling.marshallers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.StringSetAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.util.DateUtils;

/**
 * A marshaller that marshals sets of Java {@code Date} objects into DynamoDB
 * StringSets (in ISO 8601 format, ie {"2014-01-01T00:00:00Z", ...}).
 */
public class DateSetToStringSetMarshaller
        implements StringSetAttributeMarshaller {

    private static final DateSetToStringSetMarshaller INSTANCE =
            new DateSetToStringSetMarshaller();

    private DateSetToStringSetMarshaller() {
    }

    public static DateSetToStringSetMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        @SuppressWarnings("unchecked")
        Set<Date> dates = (Set<Date>) obj;

        List<String> timestamps = new ArrayList<String>(dates.size());
        for (Date date : dates) {
            timestamps.add(DateUtils.formatIso8601Date(date.toInstant()));
        }

        return AttributeValue.builder().ss(timestamps).build();
    }
}
