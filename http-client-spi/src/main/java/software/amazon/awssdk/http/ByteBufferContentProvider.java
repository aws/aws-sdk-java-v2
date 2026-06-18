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

package software.amazon.awssdk.http;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Optional capability that byte-backed {@link ContentStreamProvider} implementations may advertise to let consumers
 * read the underlying bytes without going through the {@code newStream() + InputStream.read(buf)} loop.
 *
 * <p>Consumers (signers, request builders, checksum stages) can downcast a {@link ContentStreamProvider} to this
 * interface to obtain a {@link ByteBuffer} view that can be passed straight to APIs like
 * {@link java.security.MessageDigest#update(ByteBuffer)} or {@link javax.crypto.Mac#update(ByteBuffer)} without an
 * intermediate copy.
 *
 * <p>Implementations that wrap an in-memory {@code byte[]} (such as those returned from
 * {@link ContentStreamProvider#fromByteArray(byte[])} and
 * {@link ContentStreamProvider#fromByteArrayUnsafe(byte[])}) implement this interface. Stream-based providers (file,
 * input-stream, supplier) do not, and consumers must fall back to the {@code newStream()} path for those.
 *
 * <p>This is an opt-in SPI hook. The contract:
 * <ul>
 *     <li>{@link #asByteBuffer()} may return a fresh {@link ByteBuffer} per call or a cached one. The returned
 *         buffer's {@code position} is at the start of the body and {@code limit} is at the end. Callers may advance
 *         the {@code position} (digesting/HMAC-ing operations do this) without affecting subsequent calls.</li>
 *     <li>The bytes the buffer exposes must be stable for the lifetime of the provider. Implementations that share
 *         state with externally-mutable arrays (e.g. {@link ContentStreamProvider#fromByteArrayUnsafe(byte[])}) keep
 *         the same concurrency caveats they already have for {@code newStream()}.</li>
 *     <li>Implementations should not mutate the underlying bytes through this view.</li>
 * </ul>
 */
@SdkProtectedApi
public interface ByteBufferContentProvider {

    /**
     * Returns a {@link ByteBuffer} view over the body. The buffer's {@code position} is at the start of the body
     * and {@code limit} is at the end. Each call returns an independent view: the caller can advance the position
     * (e.g. by passing it to {@code MessageDigest.update(ByteBuffer)}) without affecting concurrent or subsequent
     * calls to this method.
     */
    ByteBuffer asByteBuffer();
}
