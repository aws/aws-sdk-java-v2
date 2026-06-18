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

package software.amazon.awssdk.http.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ByteBufferContentProvider;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * Named {@link ContentStreamProvider} implementation backed by a {@code byte[]}. Replaces the anonymous inner classes
 * the {@code ContentStreamProvider.fromByteArray*} factories used to create. As a named class it can implement
 * {@link ByteBufferContentProvider}, which lets consumers (signers, checksum stages) read the body as a
 * {@link ByteBuffer} without going through {@code InputStream.read(byte[])} + intermediate buffer copies.
 *
 * <p>This is an internal type; callers obtain instances through
 * {@link ContentStreamProvider#fromByteArray(byte[])} or
 * {@link ContentStreamProvider#fromByteArrayUnsafe(byte[])} and downcast through {@link ByteBufferContentProvider}
 * when they want the fast read path.
 */
@SdkInternalApi
public final class ByteArrayContentStreamProvider implements ContentStreamProvider, ByteBufferContentProvider {

    private final byte[] bytes;
    private final int offset;
    private final int length;

    public ByteArrayContentStreamProvider(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public ByteArrayContentStreamProvider(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public InputStream newStream() {
        return new ByteArrayInputStream(bytes, offset, length);
    }

    @Override
    public String name() {
        return ProviderType.BYTES.getName();
    }

    /**
     * Returns a fresh {@link ByteBuffer} view over the underlying bytes. {@link ByteBuffer#wrap(byte[], int, int)}
     * does not copy; the buffer aliases the same array. Callers may advance the position (digest/MAC updates do)
     * but must not mutate the bytes the buffer covers.
     */
    @Override
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(bytes, offset, length);
    }
}
