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

import java.math.BigInteger;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.StringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * A converter between {@link BigInteger} and {@link String}.
 *
 * <p>
 * This converts bytes to a base 64 string.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class ByteArrayStringConverter implements StringConverter<byte[]> {
    private ByteArrayStringConverter() { }

    public static ByteArrayStringConverter create() {
        return new ByteArrayStringConverter();
    }

    @Override
    public TypeToken<byte[]> type() {
        return TypeToken.of(byte[].class);
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
