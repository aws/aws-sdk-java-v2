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

import java.math.BigInteger;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;

/**
 * A converter between {@link BigInteger} and {@link String}.
 *
 * <p>
 * This converts values to strings using {@link Boolean#toString()}. This converts the literal string values "true" and "false"
 * to a boolean. Any other string values will result in an exception.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public class BooleanStringConverter implements StringConverter<Boolean>, PrimitiveConverter<Boolean> {
    private BooleanStringConverter() {
    }

    public static BooleanStringConverter create() {
        return new BooleanStringConverter();
    }

    @Override
    public EnhancedType<Boolean> type() {
        return EnhancedType.of(Boolean.class);
    }

    @Override
    public EnhancedType<Boolean> primitiveType() {
        return EnhancedType.of(boolean.class);
    }

    @Override
    public Boolean fromString(String string) {
        switch (string) {
            case "true": return true;
            case "false": return false;
            default: throw new IllegalArgumentException("Boolean string was not 'true' or 'false': " + string);
        }
    }
}
