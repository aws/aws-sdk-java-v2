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

package software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding;

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * An interface which defines a "chunk" of data.
 */
@SdkInternalApi
public interface Chunk extends SdkAutoCloseable {
    /**
     * Get a default implementation of a chunk, which wraps a stream with a fixed size;
     */
    static Chunk create(InputStream data, int sizeInBytes) {
        return new DefaultChunk(new ChunkInputStream(data, sizeInBytes));
    }

    /**
     * Get the underlying stream of data for a chunk.
     */
    InputStream stream();

    /**
     * Whether the logical end of a chunk has been reached.
     */
    boolean hasRemaining();

}
