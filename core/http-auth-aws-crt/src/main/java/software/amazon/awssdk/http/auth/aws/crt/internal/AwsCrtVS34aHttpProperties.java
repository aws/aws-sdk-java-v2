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

package software.amazon.awssdk.http.auth.aws.crt.internal;

import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.CHUNKED_ENCODING;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.PAYLOAD_SIGNING;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.safeParseLong;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.validatedProperty;

import java.time.Clock;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.internal.util.CredentialScope;
import software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;


/**
 * An interface which contains "properties" used by a crt-based v4a-signer at various steps.
 * These properties are derived from the {@link SignerProperty}'s on a {@link SignRequest}.
 */
@SdkInternalApi
public interface AwsCrtVS34aHttpProperties extends AwsCrtV4aHttpProperties {

    static AwsCrtVS34aHttpProperties create(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        boolean payloadSigning = validatedProperty(signRequest, PAYLOAD_SIGNING, false);
        boolean chunkedEncoding = validatedProperty(signRequest, CHUNKED_ENCODING, false);
        boolean unsignedStreamingTrailer = signRequest.request().firstMatchingHeader("x-amz-content-sha256")
            .map("STREAMING-UNSIGNED-PAYLOAD-TRAILER"::equals)
            .orElse(false);
        Optional<Long> maybeContentLength = safeParseLong(signRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)
            .orElse(null));
        Long contentLength = maybeContentLength
            .orElse(signRequest.payload()
                .map(HttpChecksumUtils::calculateContentLength)
                .orElse(0L));

        return new AwsCrtV4aHttpPropertiesImpl(
            contentLength, unsignedStreamingTrailer, chunkedEncoding, payloadSigning, AwsCrtV4aHttpProperties.create(signRequest)
        );
    }

    long getContentLength();

    boolean shouldTrail();

    boolean shouldChunkEncode();

    boolean shouldSignPayload();


    final class AwsCrtV4aHttpPropertiesImpl implements AwsCrtVS34aHttpProperties {
        private final long contentLength;
        private final boolean trailed;
        private final boolean chunkedEncode;
        private final boolean signPayload;
        private final AwsCrtV4aHttpProperties v4aProperties;

        public AwsCrtV4aHttpPropertiesImpl(long contentLength, boolean trailed, boolean chunkedEncode, boolean signPayload,
                                           AwsCrtV4aHttpProperties v4aProperties) {
            this.contentLength = contentLength;
            this.trailed = trailed;
            this.chunkedEncode = chunkedEncode;
            this.signPayload = signPayload;
            this.v4aProperties = v4aProperties;
        }

        @Override
        public AwsCredentialsIdentity getCredentials() {
            return v4aProperties.getCredentials();
        }

        @Override
        public CredentialScope getCredentialScope() {
            return v4aProperties.getCredentialScope();
        }

        @Override
        public Clock getSigningClock() {
            return v4aProperties.getSigningClock();
        }

        @Override
        public ChecksumAlgorithm getChecksumAlgorithm() {
            return v4aProperties.getChecksumAlgorithm();
        }

        @Override
        public String getChecksumHeader() {
            return v4aProperties.getChecksumHeader();
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
