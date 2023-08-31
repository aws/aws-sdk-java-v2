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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.DefaultChecksummer;
import software.amazon.awssdk.http.auth.aws.internal.signer.FlexibleChecksummer;
import software.amazon.awssdk.http.auth.aws.internal.signer.PrecomputedChecksummer;
import software.amazon.awssdk.utils.ImmutableMap;

/**
 * An interface for defining how a checksum is formed from a payload synchronously and asynchronously.
 * <p>
 * The implementation may choose to also manipulate the request with the checksum, such as adding it as a header.
 */
@SdkProtectedApi
public interface Checksummer {
    /**
     * Get a default implementation of a checksummer.
     */
    static Checksummer create() {
        return new DefaultChecksummer();
    }

    /**
     * Get a flexible checksummer that uses the given checksum-algorithm and the default.
     */
    static Checksummer create(ChecksumAlgorithm checksumAlgorithm) {
        if (checksumAlgorithm != null) {
            Map<String, ChecksumAlgorithm> checksums = ImmutableMap.of(
                X_AMZ_CONTENT_SHA256, SHA256,
                checksumHeaderName(checksumAlgorithm), checksumAlgorithm
            );

            return new FlexibleChecksummer(checksums);
        }
        return create();
    }

    /**
     * Get a flexible checksummer that uses the given checksum-algorithm and the default.
     */
    static Checksummer create(String checksum) {
        return new PrecomputedChecksummer(() -> checksum);
    }

    /**
     * Get a flexible checksummer that uses the given checksum-algorithm and the default.
     */
    static Checksummer create(String checksum, ChecksumAlgorithm checksumAlgorithm) {
        if (checksumAlgorithm != null) {
            Map<String, ChecksumAlgorithm> checksums = ImmutableMap.of(
                X_AMZ_CONTENT_SHA256, new ConstantChecksumAlgorithm(checksum),
                checksumHeaderName(checksumAlgorithm), checksumAlgorithm
            );

            return new FlexibleChecksummer(checksums);
        }
        return create(checksum);
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
