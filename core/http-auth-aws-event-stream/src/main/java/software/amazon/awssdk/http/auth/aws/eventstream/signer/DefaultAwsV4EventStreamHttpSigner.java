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

import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.eventstream.AwsV4EventStreamHttpSigner;
import software.amazon.awssdk.http.auth.aws.eventstream.internal.SigV4DataFramePublisher;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4CanonicalRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4Properties;
import software.amazon.awssdk.http.auth.aws.signer.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.SigV4Context;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * A default implementation of {@link AwsV4EventStreamHttpSigner}.
 * TODO: Rename this correctly once the interface is gone and checkstyle can pass
 */
@SdkProtectedApi
public final class DefaultAwsV4EventStreamHttpSigner implements BaseAwsV4HttpSigner<AwsV4Properties> {

    private static final String HTTP_CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-EVENTS";

    private final BaseAwsV4HttpSigner<AwsV4Properties> v4Signer;

    public DefaultAwsV4EventStreamHttpSigner(BaseAwsV4HttpSigner<AwsV4Properties> v4Signer) {
        this.v4Signer = v4Signer;
    }

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request)
            throws UnsupportedOperationException {
        // synchronous signing is not something this signer should do since it deals with event-streams
        throw new UnsupportedOperationException();
    }

    @Override
    public String createContentHash(SyncSignRequest<?> signRequest, AwsV4Properties properties)
            throws UnsupportedOperationException {
        // synchronous signing is not something this signer should do since it deals with event-streams
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<String> createContentHash(AsyncSignRequest<?> signRequest, AwsV4Properties properties) {
        return CompletableFuture.completedFuture(HTTP_CONTENT_SHA_256);
    }

    @Override
    public void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                 String contentHash, AwsV4Properties properties) {
        requestBuilder.putHeader(X_AMZ_CONTENT_SHA256, HTTP_CONTENT_SHA_256);
        v4Signer.addPrerequisites(requestBuilder, contentHash, properties);
    }

    @Override
    public AwsV4CanonicalRequest createCanonicalRequest(SdkHttpRequest request, String contentHash,
                                                        AwsV4Properties properties) {
        return v4Signer.createCanonicalRequest(request, contentHash, properties);
    }

    @Override
    public String createSignString(String canonicalRequestHash, AwsV4Properties properties) {
        return v4Signer.createSignString(canonicalRequestHash, properties);
    }

    @Override
    public byte[] createSigningKey(AwsV4Properties properties) {
        return v4Signer.createSigningKey(properties);
    }

    @Override
    public String createSignature(String stringToSign, byte[] signingKey, AwsV4Properties properties) {
        return v4Signer.createSignature(stringToSign, signingKey, properties);
    }

    @Override
    public void addSignature(SdkHttpRequest.Builder requestBuilder,
                             AwsV4CanonicalRequest canonicalRequest,
                             String signature,
                             AwsV4Properties properties) {
        v4Signer.addSignature(requestBuilder, canonicalRequest, signature, properties);
    }

    @Override
    public ContentStreamProvider processPayload(ContentStreamProvider payload,
                                                SigV4Context v4RequestContext, AwsV4Properties properties)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                                CompletableFuture<SigV4Context> futureV4RequestContext,
                                                AwsV4Properties properties) {
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
    public AwsV4Properties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        return AwsV4Properties.create(signRequest);
    }
}
