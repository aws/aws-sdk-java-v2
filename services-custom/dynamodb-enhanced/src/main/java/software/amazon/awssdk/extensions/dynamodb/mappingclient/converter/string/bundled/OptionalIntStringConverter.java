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

import java.util.OptionalInt;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.StringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;

/**
 * A converter between {@link OptionalInt} and {@link String}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class OptionalIntStringConverter implements StringConverter<OptionalInt> {
    private static IntegerStringConverter INTEGER_CONVERTER = IntegerStringConverter.create();

    private OptionalIntStringConverter() { }

    public static OptionalIntStringConverter create() {
        return new OptionalIntStringConverter();
    }

    @Override
    public TypeToken<OptionalInt> type() {
        return TypeToken.of(OptionalInt.class);
    }

    @Override
    public String toString(OptionalInt object) {
        if (!object.isPresent()) {
            return null;
        }
        return INTEGER_CONVERTER.toString(object.getAsInt());
    }

    @Override
    public OptionalInt fromString(String string) {
        if (string == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(INTEGER_CONVERTER.fromString(string));
    }
}
