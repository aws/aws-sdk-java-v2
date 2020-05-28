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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.string;

import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;

/**
 * A converter between {@link AtomicLong} and {@link String}.
 *
 * <p>
 * This converts values using {@link LongStringConverter}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public class AtomicLongStringConverter implements StringConverter<AtomicLong> {
    private static LongStringConverter LONG_CONVERTER = LongStringConverter.create();

    private AtomicLongStringConverter() {
    }

    public static AtomicLongStringConverter create() {
        return new AtomicLongStringConverter();
    }

    @Override
    public EnhancedType<AtomicLong> type() {
        return EnhancedType.of(AtomicLong.class);
    }

    @Override
    public String toString(AtomicLong object) {
        return LONG_CONVERTER.toString(object.get());
    }

    @Override
    public AtomicLong fromString(String string) {
        return new AtomicLong(LONG_CONVERTER.fromString(string));
    }
}
