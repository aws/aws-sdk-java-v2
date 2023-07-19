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

import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.CHUNKED_ENCODING;
import static software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner.PAYLOAD_SIGNING;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.safeParseLong;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.validatedProperty;

import java.time.Clock;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.auth.aws.AwsS3V4HttpSigner;
import software.amazon.awssdk.http.auth.aws.checksum.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpProperties;
import software.amazon.awssdk.http.auth.aws.util.CredentialScope;
import software.amazon.awssdk.http.auth.aws.util.HttpChecksumUtils;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An extension of {@link AwsV4HttpProperties}, which provides access to more specific parameters
 * used by {@link AwsS3V4HttpSigner}.
 */
@SdkProtectedApi
interface AwsS3V4HttpProperties extends AwsV4HttpProperties {

    static AwsS3V4HttpProperties create(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        boolean payloadSigning = validatedProperty(signRequest, PAYLOAD_SIGNING, false);
        boolean chunkedEncoding = validatedProperty(signRequest, CHUNKED_ENCODING, false);
        boolean unsignedStreamingTrailer = signRequest.request().firstMatchingHeader("x-amz-content-sha256")
            .map(STREAMING_UNSIGNED_PAYLOAD_TRAILER::equals)
            .orElse(false);
        Optional<Long> maybeContentLength = safeParseLong(signRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)
            .orElse(null));
        Long contentLength = maybeContentLength
            .orElse(signRequest.payload()
                .map(HttpChecksumUtils::calculateContentLength)
                .orElse(0L));

        return new AwsS3V4HttpPropertiesImpl(
            contentLength, unsignedStreamingTrailer, chunkedEncoding, payloadSigning, AwsV4HttpProperties.create(signRequest)
        );
    }

    long getContentLength();

    boolean shouldTrail();

    boolean shouldChunkEncode();

    boolean shouldSignPayload();


    class AwsS3V4HttpPropertiesImpl implements AwsS3V4HttpProperties {
        private final long contentLength;
        private final boolean trailed;
        private final boolean chunkedEncode;
        private final boolean signPayload;
        private final AwsV4HttpProperties v4HttpProperties;

        private AwsS3V4HttpPropertiesImpl(long contentLength, boolean trailed, boolean chunkedEncode,
                                          boolean signPayload, AwsV4HttpProperties v4HttpProperties) {
            this.contentLength = contentLength;
            this.trailed = trailed;
            this.chunkedEncode = chunkedEncode;
            this.signPayload = signPayload;
            this.v4HttpProperties = v4HttpProperties;
        }


        @Override
        public AwsCredentialsIdentity getCredentials() {
            return v4HttpProperties.getCredentials();
        }

        @Override
        public CredentialScope getCredentialScope() {
            return v4HttpProperties.getCredentialScope();
        }

        @Override
        public Clock getSigningClock() {
            return v4HttpProperties.getSigningClock();
        }

        @Override
        public ChecksumAlgorithm getChecksumAlgorithm() {
            return v4HttpProperties.getChecksumAlgorithm();
        }

        @Override
        public String getChecksumHeader() {
            return v4HttpProperties.getChecksumHeader();
        }

        @Override
        public boolean shouldDoubleUrlEncode() {
            return false;
        }

        @Override
        public boolean shouldNormalizePath() {
            return false;
        }

        @Override
        public long getContentLength() {
            return contentLength;
        }

        @Override
        public boolean shouldTrail() {
            return trailed;
        }

        @Override
        public boolean shouldChunkEncode() {
            return chunkedEncode;
        }

        @Override
        public boolean shouldSignPayload() {
            return signPayload;
        }
    }
}
