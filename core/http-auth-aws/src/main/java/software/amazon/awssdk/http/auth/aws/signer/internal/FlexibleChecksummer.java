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

package software.amazon.awssdk.http.auth.aws.signer.internal;

import static software.amazon.awssdk.http.auth.aws.signer.internal.util.ChecksumUtil.fromChecksumAlgorithm;
import static software.amazon.awssdk.http.auth.aws.signer.internal.util.ChecksumUtil.readAll;
import static software.amazon.awssdk.http.auth.aws.signer.internal.util.SignerUtils.getBinaryRequestPayloadStream;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.signer.internal.io.ChecksumInputStream;
import software.amazon.awssdk.http.auth.aws.signer.internal.io.ChecksumSubscriber;

/**
 * A "flexible" implementation of a checksummer. It takes a map of checksums and their header names, computes them efficiently by
 * updating each checksum while reading the payload (once), and adds the computed checksum strings to the request using the given
 * header names in the map. This should be used in cases where a (flexible) checksum algorithm is present during signing.
 */
@SdkInternalApi
public final class FlexibleChecksummer implements Checksummer {
    private final Map<String, SdkChecksum> headerToChecksum;

    public FlexibleChecksummer(Map<String, ChecksumAlgorithm> headerToChecksumAlgorithm) {
        this.headerToChecksum = headerToChecksumAlgorithm
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, v -> fromChecksumAlgorithm(v.getValue())));
    }

    @Override
    public void checksum(ContentStreamProvider payload, SdkHttpRequest.Builder request) {
        InputStream payloadStream = getBinaryRequestPayloadStream(payload);

        ChecksumInputStream computingStream = new ChecksumInputStream(
            payloadStream,
            headerToChecksum.values()
        );

        readAll(computingStream);

        headerToChecksum.forEach((header, checksum) -> request.putHeader(header, checksum.getChecksum()));
    }

    @Override
    public CompletableFuture<Void> checksum(Publisher<ByteBuffer> payload, SdkHttpRequest.Builder request) {
        ChecksumSubscriber checksumSubscriber = new ChecksumSubscriber(headerToChecksum.values());

        if (payload != null) {
            payload.subscribe(checksumSubscriber);
        }

        return checksumSubscriber.checksum().thenRun(
            () -> headerToChecksum.forEach((header, checksum) -> request.putHeader(header, checksum.getChecksum()))
        );
    }
}
