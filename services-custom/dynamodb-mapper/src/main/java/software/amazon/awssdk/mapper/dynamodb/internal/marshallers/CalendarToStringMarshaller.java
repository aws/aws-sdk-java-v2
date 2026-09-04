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

package software.amazon.awssdk.mapper.dynamodb.internal.marshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.mapper.dynamodb.internal.MapperDateUtils;

import java.util.Calendar;

import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.StringAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals Java {@code Calendar} objects into DynamoDB
 * Strings (in ISO 8601 format, ie "2014-01-01T00:00:00Z").
 */
@SdkInternalApi
public class CalendarToStringMarshaller implements StringAttributeMarshaller {

    private static final CalendarToStringMarshaller INSTANCE =
            new CalendarToStringMarshaller();

    public static CalendarToStringMarshaller instance() {
        return INSTANCE;
    }

    private CalendarToStringMarshaller() {
    }

    @Override
    public AttributeValue marshall(Object obj) {
        return AttributeValue.builder().s(
                MapperDateUtils.formatISO8601Date(((Calendar) obj).getTime())).build();
    }
}
