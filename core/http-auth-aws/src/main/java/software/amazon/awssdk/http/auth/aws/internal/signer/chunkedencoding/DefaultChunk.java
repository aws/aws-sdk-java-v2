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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An implementation of a chunk, backed by a {@link ChunkInputStream}. This allows it to have awareness of its length and
 * determine the endedness of the chunk.
 */
@SdkInternalApi
final class DefaultChunk implements Chunk {
    private final ChunkInputStream data;

    DefaultChunk(ChunkInputStream data) {
        this.data = data;
    }

    @Override
    public boolean hasRemaining() {
        return data.remaining() > 0;
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
