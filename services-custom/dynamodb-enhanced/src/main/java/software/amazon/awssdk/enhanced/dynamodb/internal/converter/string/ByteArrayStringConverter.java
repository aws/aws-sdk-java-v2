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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * A converter between {@link BigInteger} and {@link String}.
 *
 * <p>
 * This converts bytes to a base 64 string.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public class ByteArrayStringConverter implements StringConverter<byte[]> {
    private ByteArrayStringConverter() {
    }

    public static ByteArrayStringConverter create() {
        return new ByteArrayStringConverter();
    }

    @Override
    public EnhancedType<byte[]> type() {
        return EnhancedType.of(byte[].class);
    }

    @Override
    public String toString(byte[] object) {
        return BinaryUtils.toBase64(object);
    }

    @Override
    public byte[] fromString(String string) {
        return BinaryUtils.fromBase64(string);
    }
}
