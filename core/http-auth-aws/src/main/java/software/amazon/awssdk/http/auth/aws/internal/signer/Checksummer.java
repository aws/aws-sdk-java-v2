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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;
import static software.amazon.awssdk.http.auth.aws.internal.signer.FlexibleChecksummer.option;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.ConstantChecksumAlgorithm;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.checksumHeaderName;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * An interface for defining how a checksum is formed from a payload synchronously and asynchronously.
 * <p>
 * The implementation may choose to also manipulate the request with the checksum, such as adding it as a header.
 */
@SdkInternalApi
public interface Checksummer {
    /**
     * Get a default implementation of a checksummer, which calculates the SHA-256 checksum and places it in the
     * x-amz-content-sha256 header.
     */
    static Checksummer create() {
        return new FlexibleChecksummer(
            option().headerName(X_AMZ_CONTENT_SHA256).algorithm(SHA256).formatter(BinaryUtils::toHex).build()
        );
    }

    /**
     * Get a flexible checksummer that performs two checksums: the given checksum-algorithm and the SHA-256 checksum. It places
     * the SHA-256 checksum in x-amz-content-sha256 header, and the given checksum-algorithm in the x-amz-checksum-[name] header.
     */
    static Checksummer forFlexibleChecksum(ChecksumAlgorithm checksumAlgorithm) {
        if (checksumAlgorithm != null) {
            return new FlexibleChecksummer(
                option().headerName(X_AMZ_CONTENT_SHA256).algorithm(SHA256).formatter(BinaryUtils::toHex)
                        .build(),
                option().headerName(checksumHeaderName(checksumAlgorithm)).algorithm(checksumAlgorithm)
                        .formatter(BinaryUtils::toBase64).build()
            );
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
            return new FlexibleChecksummer(
                option().headerName(X_AMZ_CONTENT_SHA256).algorithm(new ConstantChecksumAlgorithm(precomputedSha256))
                        .formatter(b -> new String(b, StandardCharsets.UTF_8)).build(),
                option().headerName(checksumHeaderName(checksumAlgorithm)).algorithm(checksumAlgorithm)
                        .formatter(BinaryUtils::toBase64).build()
            );
        }

        throw new IllegalArgumentException("Checksum Algorithm cannot be null!");
    }

    static Checksummer forNoOp() {
        return new FlexibleChecksummer();
    }

    /**
     * Given a payload, calculate a checksum and add it to the request.
     */
    void checksum(ContentStreamProvider payload, SdkHttpRequest.Builder request);

    /**
     * Given a payload, asynchronously calculate a checksum and promise to add it to the request.
     */
    CompletableFuture<Publisher<ByteBuffer>> checksum(Publisher<ByteBuffer> payload, SdkHttpRequest.Builder request);
}
