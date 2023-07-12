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

package software.amazon.awssdk.http.auth.eventstream.internal;

import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.SigV4RequestContext;
import software.amazon.awssdk.http.auth.eventstream.AwsV4EventStreamHttpSigner;
import software.amazon.awssdk.http.auth.internal.AwsV4HttpProperties;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * A default implementation of {@link AwsV4EventStreamHttpSigner}.
 */
@SdkInternalApi
public final class DefaultAwsV4EventStreamHttpSigner implements AwsV4EventStreamHttpSigner {

    private static final String HTTP_CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-EVENTS";

    private final AwsV4HttpSigner<AwsV4HttpProperties> v4Signer;

    public DefaultAwsV4EventStreamHttpSigner(AwsV4HttpSigner<AwsV4HttpProperties> v4Signer) {
        this.v4Signer = v4Signer;
    }

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request)
            throws UnsupportedOperationException {
        // synchronous signing is not something this signer should do since it deals with event-streams
        throw new UnsupportedOperationException();
    }

    @Override
    public SdkChecksum createSdkChecksum(SignRequest<?, ?> signRequest, AwsV4HttpProperties properties) {
        return v4Signer.createSdkChecksum(signRequest, properties);
    }

    @Override
    public ContentChecksum createChecksum(SyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                          AwsV4HttpProperties properties) {
        // synchronous signing is not something this signer should do since it deals with event-streams
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<ContentChecksum> createChecksum(AsyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                                             AwsV4HttpProperties properties) {
        return createContentHash(signRequest.payload().orElse(null), null, properties).thenApply(
            hash -> new ContentChecksum(hash, null));
    }

    @Override
    public String createContentHash(ContentStreamProvider payload, SdkChecksum sdkChecksum, AwsV4HttpProperties properties) {
        // synchronous signing is not something this signer should do since it deals with event-streams
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<String> createContentHash(Publisher<ByteBuffer> payload, SdkChecksum sdkChecksum,
                                                       AwsV4HttpProperties properties) {
        return CompletableFuture.completedFuture(HTTP_CONTENT_SHA_256);
    }

    @Override
    public void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                 ContentChecksum contentChecksum, AwsV4HttpProperties properties) {
        requestBuilder.putHeader(X_AMZ_CONTENT_SHA256, HTTP_CONTENT_SHA_256);
        v4Signer.addPrerequisites(requestBuilder, contentChecksum, properties);
    }

    @Override
    public CanonicalRequestV2 createCanonicalRequest(SdkHttpRequest request, ContentChecksum contentChecksum,
                                                     AwsV4HttpProperties properties) {
        return v4Signer.createCanonicalRequest(request, contentChecksum, properties);
    }

    @Override
    public String createSignString(String canonicalRequestHash, AwsV4HttpProperties properties) {
        return v4Signer.createSignString(canonicalRequestHash, properties);
    }

    @Override
    public byte[] createSigningKey(AwsV4HttpProperties properties) {
        return v4Signer.createSigningKey(properties);
    }

    @Override
    public String createSignature(String stringToSign, byte[] signingKey, AwsV4HttpProperties properties) {
        return v4Signer.createSignature(stringToSign, signingKey, properties);
    }

    @Override
    public void addSignature(SdkHttpRequest.Builder requestBuilder,
                             CanonicalRequestV2 canonicalRequest,
                             String signature,
                             AwsV4HttpProperties properties) {
        v4Signer.addSignature(requestBuilder, canonicalRequest, signature, properties);
    }

    @Override
    public ContentStreamProvider processPayload(ContentStreamProvider payload,
                                                SigV4RequestContext v4RequestContext, AwsV4HttpProperties properties) {
        return v4Signer.processPayload(payload, v4RequestContext, properties);
    }

    @Override
    public Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                                CompletableFuture<SigV4RequestContext> futureV4RequestContext,
                                                AwsV4HttpProperties properties) {
        if (payload == null) {
            return null;
        }

        // The request future will always be completed when we access it here, because it doesn't depend on the payload since
        // we override the `createContentHash` method with an already completed future
        return futureV4RequestContext.thenApply(
            v4RequestContext -> new SigV4DataFramePublisher(
                payload,
                properties.getCredentials(),
                properties.getCredentialScope(),
                v4RequestContext.getSignature(),
                properties.getSigningClock()
            )
        ).join();
    }

    @Override
    public AwsV4HttpProperties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        return AwsV4HttpProperties.create(signRequest);
    }
}
