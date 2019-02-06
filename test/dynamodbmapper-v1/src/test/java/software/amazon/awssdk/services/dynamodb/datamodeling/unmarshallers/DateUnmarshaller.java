/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Date;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals ISO-8601-formatted dates as Java
 * {@code Date} objects.
 */
public class DateUnmarshaller extends SUnmarshaller {

    private static final DateUnmarshaller INSTANCE =
            new DateUnmarshaller();

    private DateUnmarshaller() {
    }

    public static DateUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        return Date.from(DateUtils.parseIso8601Date(value.s()));
    }
}
