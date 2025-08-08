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

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.AwsChunkedV4PayloadSigner;

/**
 * Abstraction interface to simplify payload signing in {@link AwsChunkedV4PayloadSigner} by allowing us to have a uniform
 * interface for signing both sync and async payloads. See the {@code signCommon} method in {@link AwsChunkedV4PayloadSigner}.
 */
@SdkInternalApi
public interface ChunkedEncodedPayload {
    void addTrailer(TrailerProvider trailerProvider);

    List<TrailerProvider> trailers();

    void addExtension(ChunkExtensionProvider chunkExtensionProvider);

    /**
     * Update the payload so that its data is fed to the given checksum.
     */
    void checksumPayload(SdkChecksum checksum);

    /**
     * Set the decoded content length of the payload.
     */
    default void decodedContentLength(long contentLength) {
    }
}
