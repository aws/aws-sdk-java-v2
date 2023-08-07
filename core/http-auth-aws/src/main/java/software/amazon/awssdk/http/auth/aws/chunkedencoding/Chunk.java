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

package software.amazon.awssdk.http.auth.aws.chunkedencoding;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.aws.internal.io.ChunkInputStream;

/**
 * An interface which defines a "chunk" of data.
 */
@SdkProtectedApi
public interface Chunk {
    /**
     * Get a default implementation of a chunk, which wraps a stream with a fixed size;
     */
    static Chunk create(InputStream data, int size) {
        return new ChunkImpl(new ChunkInputStream(data, size));
    }

    /**
     * Get the underlying stream of data for a chunk.
     */
    InputStream stream() throws IOException;

    /**
     * Whether the logical end of a chunk has been reached.
     */
    boolean ended();

    /**
     * Close the underlying stream of data for a chunk.
     */
    void close();

    /**
     * An implementation of a chunk, backed by a {@link ChunkInputStream}. This allows it to have awareness of its length and
     * determine the endedness of the chunk.
     */
    final class ChunkImpl implements Chunk {
        private final ChunkInputStream data;

        public ChunkImpl(ChunkInputStream data) {
            this.data = data;
        }

        @Override
        public boolean ended() {
            return data.remaining() <= 0;
        }

        @Override
        public ChunkInputStream stream() {
            return data;
        }

        @Override
        public void close() {
            invokeSafely(data::close);
        }
    }
}
