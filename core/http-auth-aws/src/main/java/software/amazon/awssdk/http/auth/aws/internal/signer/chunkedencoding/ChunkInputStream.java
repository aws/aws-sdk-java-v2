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

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.SdkLengthAwareInputStream;

/**
 * A wrapped stream to represent a "chunk" of data
 */
@SdkInternalApi
public final class ChunkInputStream extends SdkLengthAwareInputStream {

    public ChunkInputStream(InputStream inputStream, long length) {
        super(inputStream, length);
    }

    @Override
    public void close() throws IOException {
        // Drain this chunk on close, so the stream is left at the end of the chunk.
        long remaining = remaining();
        if (remaining > 0 && skip(remaining) < remaining) {
            throw new IOException("Unable to drain stream for chunk. The underlying stream did not allow skipping the "
                                  + "whole chunk.");
        }
        super.close();
    }
}
