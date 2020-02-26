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

package software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled;

import java.util.OptionalDouble;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.StringConverter;

/**
 * A converter between {@link OptionalDouble} and {@link String}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class OptionalDoubleStringConverter implements StringConverter<OptionalDouble> {
    private static DoubleStringConverter DOUBLE_CONVERTER = DoubleStringConverter.create();

    private OptionalDoubleStringConverter() { }

    public static OptionalDoubleStringConverter create() {
        return new OptionalDoubleStringConverter();
    }

    @Override
    public TypeToken<OptionalDouble> type() {
        return TypeToken.of(OptionalDouble.class);
    }

    @Override
    public String toString(OptionalDouble object) {
        if (!object.isPresent()) {
            return null;
        }
        return DOUBLE_CONVERTER.toString(object.getAsDouble());
    }

    @Override
    public OptionalDouble fromString(String string) {
        if (string == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(DOUBLE_CONVERTER.fromString(string));
    }
}
