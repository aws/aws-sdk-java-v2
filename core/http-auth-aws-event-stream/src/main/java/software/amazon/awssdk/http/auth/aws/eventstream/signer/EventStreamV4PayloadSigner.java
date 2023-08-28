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

package software.amazon.awssdk.http.auth.aws.eventstream.signer;

import java.nio.ByteBuffer;
import java.time.Clock;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.aws.eventstream.internal.SigV4DataFramePublisher;
import software.amazon.awssdk.http.auth.aws.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.signer.V4Context;
import software.amazon.awssdk.http.auth.aws.signer.V4PayloadSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation which supports async signing of event-stream payloads.
 */
@SdkProtectedApi
public class EventStreamV4PayloadSigner implements V4PayloadSigner {

    private final AwsCredentialsIdentity credentials;
    private final CredentialScope credentialScope;
    private final Clock signingClock;

    public EventStreamV4PayloadSigner(AwsCredentialsIdentity credentials, CredentialScope credentialScope, Clock signingClock) {
        this.credentials = Validate.notNull(credentials, "Credentials cannot be null!");
        this.credentialScope = Validate.notNull(credentialScope, "Scope cannot be null!");
        this.signingClock = Validate.notNull(signingClock, "Clock cannot be null!");
    }

    @Override
    public ContentStreamProvider sign(ContentStreamProvider payload, V4Context v4Context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Publisher<ByteBuffer> signAsync(Publisher<ByteBuffer> payload, V4Context v4Context) {
        return SigV4DataFramePublisher.builder()
                                      .publisher(payload)
                                      .credentials(credentials)
                                      .credentialScope(credentialScope)
                                      .signature(v4Context.getSignature())
                                      .signingClock(signingClock)
                                      .build();
    }
}
