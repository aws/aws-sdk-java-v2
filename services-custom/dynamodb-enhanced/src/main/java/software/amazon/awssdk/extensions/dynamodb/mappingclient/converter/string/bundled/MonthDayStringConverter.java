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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.bundled;

import java.time.MonthDay;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.StringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;

/**
 * A converter between {@link MonthDay} and {@link String}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class MonthDayStringConverter implements StringConverter<MonthDay> {
    private MonthDayStringConverter() { }

    public static MonthDayStringConverter create() {
        return new MonthDayStringConverter();
    }

    @Override
    public TypeToken<MonthDay> type() {
        return TypeToken.of(MonthDay.class);
    }

    @Override
    public MonthDay fromString(String string) {
        return MonthDay.parse(string);
    }
}
