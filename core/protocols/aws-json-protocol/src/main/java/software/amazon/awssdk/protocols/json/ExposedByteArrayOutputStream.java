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

package software.amazon.awssdk.protocols.json;

import java.io.ByteArrayOutputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A thin subclass of {@link ByteArrayOutputStream} that exposes the internal buffer and count
 * without copying. This allows {@link SdkJsonGenerator} to create a {@code ContentStreamProvider}
 * that wraps the buffer directly via {@code ByteArrayInputStream(buf, 0, count)}, avoiding the
 * contiguous copy that {@link ByteArrayOutputStream#toByteArray()} performs.
 *
 * <p>The write path is identical to {@code ByteArrayOutputStream} — no overhead is added.
 * Only the final "get the bytes" step is optimized.
 *
 * <p>This class is not thread-safe.
 */
@SdkInternalApi
final class ExposedByteArrayOutputStream extends ByteArrayOutputStream {

    ExposedByteArrayOutputStream(int size) {
        super(size);
    }

    /**
     * Returns the internal buffer. The valid data is in {@code buf[0..count-1]}.
     * The returned array may be larger than {@link #size()}; callers must use
     * {@link #size()} to determine the valid range.
     *
     * <p><b>Warning:</b> The returned array is the live internal buffer. Do not modify it,
     * and do not write to this stream after capturing the reference — the buffer may be
     * replaced by a larger one on the next write if growth is needed.
     */
    byte[] buf() {
        return buf;
    }
}
