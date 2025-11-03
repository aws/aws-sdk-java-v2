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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.UnbufferedChecksumSubscriber;

@SdkInternalApi
public class AsyncChunkEncodedPayload implements ChunkedEncodedPayload {
    private final ChunkedEncodedPublisher.Builder publisherBuilder;

    public AsyncChunkEncodedPayload(ChunkedEncodedPublisher.Builder publisherBuilder) {
        this.publisherBuilder = publisherBuilder;
    }

    @Override
    public void addTrailer(TrailerProvider trailerProvider) {
        publisherBuilder.addTrailer(trailerProvider);
    }

    @Override
    public List<TrailerProvider> trailers() {
        return publisherBuilder.trailers();
    }

    @Override
    public void addExtension(ChunkExtensionProvider chunkExtensionProvider) {
        publisherBuilder.addExtension(chunkExtensionProvider);
    }

    @Override
    public void checksumPayload(SdkChecksum checksum) {
        Publisher<ByteBuffer> checksumPayload = computeChecksum(publisherBuilder.publisher(), checksum);
        publisherBuilder.publisher(checksumPayload);
    }

    @Override
    public void decodedContentLength(long contentLength) {
        publisherBuilder.contentLength(contentLength);
    }

    private Publisher<ByteBuffer> computeChecksum(Publisher<ByteBuffer> publisher, SdkChecksum checksum) {
        return subscriber -> publisher.subscribe(
            new UnbufferedChecksumSubscriber(Collections.singletonList(checksum), subscriber));
    }
}
