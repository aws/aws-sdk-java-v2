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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_EVENTS_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_SIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_SIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_TRAILER;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.Checksummer;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.ConstantChecksum;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class ChecksumUtil {

    private static final Logger log = Logger.loggerFor(ChecksumUtil.class);
    private static final String CONSTANT_CHECKSUM = "CONSTANT";

    private ChecksumUtil() {
    }

    /**
     * Get the correct checksum header name based on the checksum-algorithm. This is required to be of the form
     * {@code x-amz-checksum-*}, where '*' is alphanumeric checksum-algorithm-id in lower-case form. Examples include:
     * <p>
     * x-amz-checksum-sha256, x-amz-checksum-sha1, x-amz-checksum-crc32, x-amz-checksum-crc32c, x-amz-checksum-md5
     * </p>
     */
    public static String checksumHeaderName(ChecksumAlgorithm checksumAlgorithm) {
        return "x-amz-checksum-" + checksumAlgorithm.algorithmId().toLowerCase(Locale.US);
    }

    /**
     * Gets the SdkChecksum object based on the given ChecksumAlgorithm.
     */
    public static SdkChecksum fromChecksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
        String algorithmId = checksumAlgorithm.algorithmId();
        if (CONSTANT_CHECKSUM.equals(algorithmId)) {
            return new ConstantChecksum(((ConstantChecksumAlgorithm) checksumAlgorithm).value);
        }

        SdkChecksum checksum = SdkChecksum.forAlgorithm(checksumAlgorithm);
        if (checksum != null) {
            return checksum;
        }

        throw new UnsupportedOperationException("Checksum not supported for " + algorithmId);
    }

    /**
     * Read the entirety of an input-stream - this is useful when the stream has side-effects (such as calculating a checksum)
     * when it gets read.
     */
    public static void readAll(InputStream inputStream) {
        FunctionalUtils.invokeSafely(() -> {
            byte[] buffer = new byte[4096];
            while (inputStream.read(buffer) > -1) {
            }
        });
    }

    public static byte[] longToByte(Long input) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(input);
        return buffer.array();
    }

    /**
     * An implementation of a {@link ChecksumAlgorithm} that will map to {@link ConstantChecksum}, which provides a constant
     * checksum. This isn't super useful, but is needed in cases such as signing, where the content-hash (a
     * cryptographically-secure "checksum") can be a set of pre-defined values.
     */
    public static class ConstantChecksumAlgorithm implements ChecksumAlgorithm {

        private final String value;

        public ConstantChecksumAlgorithm(String value) {
            this.value = value;
        }

        @Override
        public String algorithmId() {
            return CONSTANT_CHECKSUM;
        }
    }

    public static boolean hasChecksumHeader(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request) {
        ChecksumAlgorithm checksumAlgorithm = request.property(CHECKSUM_ALGORITHM);

        if (checksumAlgorithm != null) {
            String checksumHeaderName = checksumHeaderName(checksumAlgorithm);
            return request.request().firstMatchingHeader(checksumHeaderName).isPresent();
        }

        return false;
    }

    public static boolean useChunkEncoding(boolean payloadSigningEnabled, boolean chunkEncodingEnabled,
                                            boolean isTrailingOrFlexible) {

        return (payloadSigningEnabled && chunkEncodingEnabled) || (chunkEncodingEnabled && isTrailingOrFlexible);
    }

    public static boolean isPayloadSigning(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request) {
        boolean isAnonymous = CredentialUtils.isAnonymous(request.identity());
        boolean isPayloadSigningEnabled = request.requireProperty(PAYLOAD_SIGNING_ENABLED, true);
        boolean isEncrypted = "https".equals(request.request().protocol());

        if (isAnonymous) {
            return false;
        }

        // presigning requests should always have a null payload, and should always be unsigned-payload
        if (!isEncrypted && request.payload().isPresent()) {
            if (!isPayloadSigningEnabled) {
                log.debug(() -> "Payload signing was disabled for an HTTP request with a payload. " +
                                "Signing will be enabled. Use HTTPS for unsigned payloads.");
            }
            return true;
        }

        return isPayloadSigningEnabled;
    }

    public static boolean isEventStreaming(SdkHttpRequest request) {
        return "application/vnd.amazon.eventstream".equals(request.firstMatchingHeader(Header.CONTENT_TYPE).orElse(""));
    }

    public static Checksummer checksummer(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request,
                                          Boolean isPayloadSigningOverride) {
        boolean isPayloadSigning = isPayloadSigningOverride != null ? isPayloadSigningOverride : isPayloadSigning(request);
        boolean isEventStreaming = isEventStreaming(request.request());
        boolean hasChecksumHeader = hasChecksumHeader(request);
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);
        boolean isTrailing = request.request().firstMatchingHeader(X_AMZ_TRAILER).isPresent();
        boolean isFlexible = request.hasProperty(CHECKSUM_ALGORITHM) && !hasChecksumHeader;
        boolean isAnonymous = CredentialUtils.isAnonymous(request.identity());

        if (isEventStreaming) {
            return Checksummer.forPrecomputed256Checksum(STREAMING_EVENTS_PAYLOAD);
        }

        if (isPayloadSigning) {
            if (isChunkEncoding) {
                if (isFlexible || isTrailing) {
                    return Checksummer.forPrecomputed256Checksum(STREAMING_SIGNED_PAYLOAD_TRAILER);
                }
                return Checksummer.forPrecomputed256Checksum(STREAMING_SIGNED_PAYLOAD);
            }

            if (isFlexible) {
                return Checksummer.forFlexibleChecksum(request.property(CHECKSUM_ALGORITHM));
            }
            return Checksummer.create();
        }

        if (isFlexible || isTrailing) {
            if (isChunkEncoding) {
                return Checksummer.forPrecomputed256Checksum(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
            }
        }

        if (isFlexible) {
            return Checksummer.forFlexibleChecksum(UNSIGNED_PAYLOAD, request.property(CHECKSUM_ALGORITHM));
        }

        if (isAnonymous) {
            return Checksummer.forNoOp();
        }

        return Checksummer.forPrecomputed256Checksum(UNSIGNED_PAYLOAD);
    }
}
