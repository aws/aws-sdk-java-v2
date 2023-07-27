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

import static software.amazon.awssdk.http.auth.aws.signer.AwsV4CanonicalRequest.getCanonicalHeaders;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4CanonicalRequest.getSignedHeadersString;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.AWS4_SIGNING_ALGORITHM;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4CanonicalRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4Properties;
import software.amazon.awssdk.http.auth.aws.signer.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.SigV4Context;
import software.amazon.awssdk.http.auth.aws.util.SignerConstant;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.Pair;

/**
 * An implementation of {@link BaseAwsV4HttpSigner} that adds auth to a request via query-params.
 * TODO: Rename this correctly once the interface is gone and checkstyle can pass
 */
@SdkInternalApi
public class DefaultAwsV4QueryHttpSigner implements BaseAwsV4HttpSigner<AwsV4Properties> {

    private final BaseAwsV4HttpSigner<AwsV4Properties> v4Signer;

    public DefaultAwsV4QueryHttpSigner(BaseAwsV4HttpSigner<AwsV4Properties> v4Signer) {
        this.v4Signer = v4Signer;
    }

    @Override
    public String createContentHash(SyncSignRequest<?> signRequest, AwsV4Properties properties) {
        return v4Signer.createContentHash(signRequest, properties);
    }

    @Override
    public CompletableFuture<String> createContentHash(AsyncSignRequest<?> signRequest, AwsV4Properties properties) {
        return v4Signer.createContentHash(signRequest, properties);
    }

    @Override
    public void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                 String contentHash, AwsV4Properties properties) {
        v4Signer.addPrerequisites(requestBuilder, contentHash, properties);

        if (properties.getCredentials() instanceof AwsSessionCredentialsIdentity) {
            requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SECURITY_TOKEN,
                ((AwsSessionCredentialsIdentity) properties.getCredentials()).sessionToken());
        }

        List<Pair<String, List<String>>> canonicalHeaders = getCanonicalHeaders(requestBuilder.build());
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_ALGORITHM, AWS4_SIGNING_ALGORITHM);
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_DATE, properties.getCredentialScope().getDatetime());
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SIGNED_HEADERS, getSignedHeadersString(canonicalHeaders));
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_CREDENTIAL,
            properties.getCredentialScope().scope(properties.getCredentials()));
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
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SIGNATURE, signature);
    }

    @Override
    public ContentStreamProvider processPayload(ContentStreamProvider payload,
                                                SigV4Context v4RequestContext, AwsV4Properties properties) {
        return v4Signer.processPayload(payload, v4RequestContext, properties);
    }

    @Override
    public Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                                CompletableFuture<SigV4Context> futureV4RequestContext,
                                                AwsV4Properties properties) {
        return v4Signer.processPayload(payload, futureV4RequestContext, properties);
    }

    @Override
    public AwsV4Properties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        return AwsV4Properties.create(signRequest);
    }
}
