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

package software.amazon.awssdk.mapper.dynamodb.internal.unmarshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.mapper.dynamodb.internal.MapperDateUtils;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals ISO-8601-formatted dates as Java
 * {@code Date} objects.
 */
@SdkInternalApi
public class DateUnmarshaller extends SUnmarshaller {

    private static final DateUnmarshaller INSTANCE =
            new DateUnmarshaller();

    public static DateUnmarshaller instance() {
        return INSTANCE;
    }

    private DateUnmarshaller() {
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        return MapperDateUtils.parseISO8601Date(value.s());
    }
}
