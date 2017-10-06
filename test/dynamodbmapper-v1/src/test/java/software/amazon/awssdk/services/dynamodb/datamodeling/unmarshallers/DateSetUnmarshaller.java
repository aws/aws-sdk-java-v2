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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.core.util.DateUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals sets of ISO-8601-formatted dates as sets of
 * Java {@code Date} objects.
 */
public class DateSetUnmarshaller extends SsUnmarshaller {

    private static final DateSetUnmarshaller INSTANCE =
            new DateSetUnmarshaller();

    private DateSetUnmarshaller() {
    }

    public static DateSetUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        Set<Date> result = new HashSet<Date>();

        for (String s : value.ss()) {
            result.add(Date.from(DateUtils.parseIso8601Date(s)));
        }

        return result;
    }
}
