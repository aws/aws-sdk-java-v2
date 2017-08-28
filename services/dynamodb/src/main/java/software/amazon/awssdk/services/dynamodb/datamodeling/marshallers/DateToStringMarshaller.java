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

import static software.amazon.awssdk.util.DateUtils.formatIso8601Date;

import java.util.Date;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.StringAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals Java {@code Date} objects into DynamoDB Strings
 * (in ISO 8601 format, ie "2014-01-01T00:00:00Z").
 */
public class DateToStringMarshaller implements StringAttributeMarshaller {

    private static final DateToStringMarshaller INSTANCE =
            new DateToStringMarshaller();

    private DateToStringMarshaller() {
    }

    public static DateToStringMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        return AttributeValue.builder().s(
                formatIso8601Date(Date.class.cast(obj).toInstant()))
                .build();
    }
}
