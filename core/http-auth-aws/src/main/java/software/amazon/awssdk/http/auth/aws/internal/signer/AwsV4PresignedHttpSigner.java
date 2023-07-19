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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.checksum.ContentChecksum;
import software.amazon.awssdk.http.auth.aws.checksum.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpProperties;
import software.amazon.awssdk.http.auth.aws.signer.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.SigV4RequestContext;
import software.amazon.awssdk.http.auth.aws.util.CanonicalRequestV2;
import software.amazon.awssdk.http.auth.aws.util.SignerConstant;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An implementation of a {@link BaseAwsV4HttpSigner} that can be used to generate a presigned request with
 * an expiration (i.e. S3 pre-signed URLs).
 */
@SdkProtectedApi
public final class AwsV4PresignedHttpSigner implements BaseAwsV4HttpSigner<AwsV4PresignedHttpProperties> {

    private final BaseAwsV4HttpSigner<AwsV4HttpProperties> v4Signer;

    public AwsV4PresignedHttpSigner(BaseAwsV4HttpSigner<AwsV4HttpProperties> v4Signer) {
        this.v4Signer = v4Signer;
    }

    @Override
    public SdkChecksum createSdkChecksum(SignRequest<?, ?> signRequest, AwsV4PresignedHttpProperties properties) {
        return v4Signer.createSdkChecksum(signRequest, properties);
    }

    @Override
    public ContentChecksum createChecksum(SyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                          AwsV4PresignedHttpProperties properties) {
        return v4Signer.createChecksum(signRequest, properties);
    }

    @Override
    public CompletableFuture<ContentChecksum> createChecksum(AsyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                                             AwsV4PresignedHttpProperties properties) {
        return v4Signer.createChecksum(signRequest, properties);
    }

    @Override
    public String createContentHash(ContentStreamProvider payload, SdkChecksum sdkChecksum,
                                    AwsV4PresignedHttpProperties properties) {
        return v4Signer.createContentHash(payload, sdkChecksum, properties);
    }

    @Override
    public CompletableFuture<String> createContentHash(Publisher<ByteBuffer> payload, SdkChecksum sdkChecksum,
                                                       AwsV4PresignedHttpProperties properties) {
        return v4Signer.createContentHash(payload, sdkChecksum, properties);
    }

    @Override
    public void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                 ContentChecksum contentChecksum, AwsV4PresignedHttpProperties properties) {
        if (properties.getExpirationDuration() != null) {
            requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_EXPIRES,
                Long.toString(properties.getExpirationDuration().getSeconds()));
        }

        v4Signer.addPrerequisites(requestBuilder, contentChecksum, properties);
    }

    @Override
    public CanonicalRequestV2 createCanonicalRequest(SdkHttpRequest request, ContentChecksum contentChecksum,
                                                     AwsV4PresignedHttpProperties properties) {
        return v4Signer.createCanonicalRequest(request, contentChecksum, properties);
    }

    @Override
    public String createSignString(String canonicalRequestHash, AwsV4PresignedHttpProperties properties) {
        return v4Signer.createSignString(canonicalRequestHash, properties);
    }

    @Override
    public byte[] createSigningKey(AwsV4PresignedHttpProperties properties) {
        return v4Signer.createSigningKey(properties);
    }

    @Override
    public String createSignature(String stringToSign, byte[] signingKey, AwsV4PresignedHttpProperties properties) {
        return v4Signer.createSignature(stringToSign, signingKey, properties);
    }

    @Override
    public void addSignature(SdkHttpRequest.Builder requestBuilder,
                             CanonicalRequestV2 canonicalRequest,
                             String signature,
                             AwsV4PresignedHttpProperties properties) {
        v4Signer.addSignature(requestBuilder, canonicalRequest, signature, properties);
    }

    @Override
    public ContentStreamProvider processPayload(ContentStreamProvider payload,
                                                SigV4RequestContext v4RequestContext, AwsV4PresignedHttpProperties properties) {
        return v4Signer.processPayload(payload, v4RequestContext, properties);
    }

    @Override
    public Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                                CompletableFuture<SigV4RequestContext> futureV4RequestContext,
                                                AwsV4PresignedHttpProperties properties) {
        return v4Signer.processPayload(payload, futureV4RequestContext, properties);
    }

    @Override
    public AwsV4PresignedHttpProperties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        return AwsV4PresignedHttpProperties.create(signRequest);
    }
}
