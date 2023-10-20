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

package software.amazon.awssdk.http.auth.aws.eventstream.internal.signer;

import java.nio.ByteBuffer;
import java.time.Clock;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.aws.eventstream.internal.io.SigV4DataFramePublisher;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.V4PayloadSigner;
import software.amazon.awssdk.http.auth.aws.internal.signer.V4RequestSigningResult;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation which supports async signing of event-stream payloads.
 */
@SdkInternalApi
public class EventStreamV4PayloadSigner implements V4PayloadSigner {

    private final AwsCredentialsIdentity credentials;
    private final CredentialScope credentialScope;
    private final Clock signingClock;

    public EventStreamV4PayloadSigner(Builder builder) {
        this.credentials = Validate.paramNotNull(builder.credentials, "Credentials");
        this.credentialScope = Validate.paramNotNull(builder.credentialScope, "CredentialScope");
        this.signingClock = Validate.paramNotNull(builder.signingClock, "SigningClock");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ContentStreamProvider sign(ContentStreamProvider payload, V4RequestSigningResult requestSigningResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Publisher<ByteBuffer> signAsync(Publisher<ByteBuffer> payload, V4RequestSigningResult requestSigningResult) {
        return SigV4DataFramePublisher.builder()
                                      .publisher(payload)
                                      .credentials(credentials)
                                      .credentialScope(credentialScope)
                                      .signature(requestSigningResult.getSignature())
                                      .signingClock(signingClock)
                                      .build();
    }

    public static class Builder {
        private AwsCredentialsIdentity credentials;
        private CredentialScope credentialScope;
        private Clock signingClock;

        public Builder credentials(AwsCredentialsIdentity credentials) {
            this.credentials = credentials;
            return this;
        }

        public Builder credentialScope(CredentialScope credentialScope) {
            this.credentialScope = credentialScope;
            return this;
        }

        public Builder signingClock(Clock signingClock) {
            this.signingClock = signingClock;
            return this;
        }

        public EventStreamV4PayloadSigner build() {
            return new EventStreamV4PayloadSigner(this);
        }
    }
}
