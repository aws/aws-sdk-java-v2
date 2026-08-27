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
package software.amazon.awssdk.mapper.dynamodb.internal;

import java.nio.ByteBuffer;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;

/**
 * Binary conversion helpers shared by the mapper's converters and unmarshallers.
 */
@SdkInternalApi
public final class MapperBinaryUtils {

    private MapperBinaryUtils() {
    }

    /**
     * Returns a writable {@link ByteBuffer} containing the given bytes, preserving v1 behavior.
     * {@link SdkBytes#asByteBuffer()} is read-only, so this wraps the copy from
     * {@link SdkBytes#asByteArray()} instead.
     *
     * @param sdkBytes the bytes to wrap, may be {@code null}
     * @return a writable buffer, or {@code null} if {@code sdkBytes} is {@code null}
     */
    public static ByteBuffer toWritableByteBuffer(SdkBytes sdkBytes) {
        return sdkBytes == null ? null : ByteBuffer.wrap(sdkBytes.asByteArray());
    }
}
