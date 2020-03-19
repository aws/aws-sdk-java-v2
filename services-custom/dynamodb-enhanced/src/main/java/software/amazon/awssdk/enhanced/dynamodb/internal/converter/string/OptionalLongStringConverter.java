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

import java.util.OptionalLong;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;

/**
 * A converter between {@link OptionalLong} and {@link String}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public class OptionalLongStringConverter implements StringConverter<OptionalLong> {
    private static LongStringConverter LONG_CONVERTER = LongStringConverter.create();

    private OptionalLongStringConverter() {
    }

    public static OptionalLongStringConverter create() {
        return new OptionalLongStringConverter();
    }

    @Override
    public EnhancedType<OptionalLong> type() {
        return EnhancedType.of(OptionalLong.class);
    }

    @Override
    public String toString(OptionalLong object) {
        if (!object.isPresent()) {
            return null;
        }
        return LONG_CONVERTER.toString(object.getAsLong());
    }

    @Override
    public OptionalLong fromString(String string) {
        if (string == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(LONG_CONVERTER.fromString(string));
    }
}
