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

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.ChecksumInputStream;

@SdkInternalApi
public class SyncChunkEncodedPayload implements ChunkedEncodedPayload {
    private final ChunkedEncodedInputStream.Builder chunkedInputStream;

    public SyncChunkEncodedPayload(ChunkedEncodedInputStream.Builder chunkedInputStream) {
        this.chunkedInputStream = chunkedInputStream;
    }

    @Override
    public void addTrailer(TrailerProvider trailerProvider) {
        chunkedInputStream.addTrailer(trailerProvider);
    }

    @Override
    public List<TrailerProvider> trailers() {
        return chunkedInputStream.trailers();
    }

    @Override
    public void addExtension(ChunkExtensionProvider chunkExtensionProvider) {
        chunkedInputStream.addExtension(chunkExtensionProvider);
    }

    @Override
    public void checksumPayload(SdkChecksum checksum) {
        ChecksumInputStream checksumInputStream = new ChecksumInputStream(
            chunkedInputStream.inputStream(),
            Collections.singleton(checksum)
        );

        chunkedInputStream.inputStream(checksumInputStream);
    }
}
