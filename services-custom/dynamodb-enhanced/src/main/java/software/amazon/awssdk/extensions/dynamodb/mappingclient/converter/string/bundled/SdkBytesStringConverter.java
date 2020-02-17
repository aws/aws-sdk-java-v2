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

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.StringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * A converter between {@link SdkBytes} and {@link String}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class SdkBytesStringConverter implements StringConverter<SdkBytes> {
    private SdkBytesStringConverter() { }

    public static SdkBytesStringConverter create() {
        return new SdkBytesStringConverter();
    }

    @Override
    public TypeToken<SdkBytes> type() {
        return TypeToken.of(SdkBytes.class);
    }

    @Override
    public String toString(SdkBytes object) {
        return BinaryUtils.toBase64(object.asByteArray());
    }

    @Override
    public SdkBytes fromString(String string) {
        return SdkBytes.fromByteArray(BinaryUtils.fromBase64(string));
    }
}
