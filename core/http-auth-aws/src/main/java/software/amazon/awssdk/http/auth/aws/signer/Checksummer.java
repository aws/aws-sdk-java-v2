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

package software.amazon.awssdk.http.auth.aws.signer;

import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;
import static software.amazon.awssdk.http.auth.aws.internal.util.ChecksumUtil.ConstantChecksumAlgorithm;
import static software.amazon.awssdk.http.auth.aws.internal.util.ChecksumUtil.checksumHeaderName;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.FlexibleChecksummer;
import software.amazon.awssdk.http.auth.aws.internal.signer.PrecomputedSha256Checksummer;
import software.amazon.awssdk.utils.ImmutableMap;

/**
 * An interface for defining how a checksum is formed from a payload synchronously and asynchronously.
 * <p>
 * The implementation may choose to also manipulate the request with the checksum, such as adding it as a header.
 */
@SdkProtectedApi
public interface Checksummer {
    /**
     * Get a default implementation of a checksummer, which calculates the SHA-256 checksum and places it in the
     * x-amz-content-sha256 header.
     */
    static Checksummer create() {
        return new FlexibleChecksummer(Collections.singletonMap(X_AMZ_CONTENT_SHA256, SHA256));
    }

    /**
     * Get a flexible checksummer that performs two checksums: the given checksum-algorithm and the SHA-256 checksum. It places
     * the SHA-256 checksum in x-amz-content-sha256 header, and the given checksum-algorithm in the x-amz-checksum-[name] header.
     */
    static Checksummer forFlexibleChecksum(ChecksumAlgorithm checksumAlgorithm) {
        if (checksumAlgorithm != null) {
            Map<String, ChecksumAlgorithm> checksums = ImmutableMap.of(
                X_AMZ_CONTENT_SHA256, SHA256,
                checksumHeaderName(checksumAlgorithm), checksumAlgorithm
            );

            return new FlexibleChecksummer(checksums);
        }

        throw new IllegalArgumentException("Checksum Algorithm cannot be null!");
    }

    /**
     * Get a precomputed checksummer which places the precomputed checksum to the x-amz-content-sha256 header.
     */
    static Checksummer forPrecomputed256Checksum(String precomputedSha256) {
        return new PrecomputedSha256Checksummer(() -> precomputedSha256);
    }

    /**
     * Get a flexible checksummer that performs two checksums: the given checksum-algorithm and a precomputed checksum from the
     * given checksum string. It places the precomputed checksum in x-amz-content-sha256 header, and the given checksum-algorithm
     * in the x-amz-checksum-[name] header.
     */
    static Checksummer forFlexibleChecksum(String precomputedSha256, ChecksumAlgorithm checksumAlgorithm) {
        if (checksumAlgorithm != null) {
            Map<String, ChecksumAlgorithm> checksums = ImmutableMap.of(
                X_AMZ_CONTENT_SHA256, new ConstantChecksumAlgorithm(precomputedSha256),
                checksumHeaderName(checksumAlgorithm), checksumAlgorithm
            );

            return new FlexibleChecksummer(checksums);
        }

        throw new IllegalArgumentException("Checksum Algorithm cannot be null!");
    }

    /**
     * Given a payload, calculate a checksum and add it to the request.
     */
    void checksum(ContentStreamProvider payload, SdkHttpRequest.Builder request);

    /**
     * Given a payload, asynchronously calculate a checksum and promise to add it to the request.
     */
    CompletableFuture<Void> checksum(Publisher<ByteBuffer> payload, SdkHttpRequest.Builder request);
}
