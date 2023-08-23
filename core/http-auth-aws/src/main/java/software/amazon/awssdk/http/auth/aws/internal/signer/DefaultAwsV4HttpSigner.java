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

import static software.amazon.awssdk.http.auth.aws.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.UNSIGNED_PAYLOAD;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.Checksummer;
import software.amazon.awssdk.http.auth.aws.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.signer.V4Context;
import software.amazon.awssdk.http.auth.aws.signer.V4PayloadSigner;
import software.amazon.awssdk.http.auth.aws.signer.V4Properties;
import software.amazon.awssdk.http.auth.aws.signer.V4RequestSigner;
import software.amazon.awssdk.http.auth.aws.util.CredentialUtils;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An implementation of a {@link AwsV4HttpSigner} that uses properties to compose v4-signers in order to delegate signing of a
 * request and payload (if applicable) accordingly.
 */
@SdkInternalApi
public final class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        Checksummer checksummer = checksummer(request);
        V4Properties v4Properties = v4Properties(request);
        V4RequestSigner v4RequestSigner = v4RequestSigner(request, v4Properties);
        V4PayloadSigner payloadSigner = V4PayloadSigner.create();

        return doSign(request, checksummer, v4RequestSigner, payloadSigner);
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        Checksummer checksummer = checksummer(request);
        V4Properties v4Properties = v4Properties(request);
        V4RequestSigner v4RequestSigner = v4RequestSigner(request, v4Properties);
        V4PayloadSigner payloadSigner = V4PayloadSigner.create();

        return doSign(request, checksummer, v4RequestSigner, payloadSigner);
    }

    private V4Properties v4Properties(SignRequest<?, ? extends AwsCredentialsIdentity> request) {
        Clock signingClock = request.requireProperty(AwsV4HttpSigner.SIGNING_CLOCK, Clock.systemUTC());
        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = sanitizeCredentials(request.identity());
        String regionName = request.requireProperty(AwsV4HttpSigner.REGION_NAME);
        String serviceSigningName = request.requireProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME);
        CredentialScope credentialScope = new CredentialScope(regionName, serviceSigningName, signingInstant);
        boolean doubleUrlEncode = request.requireProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, true);
        boolean normalizePath = request.requireProperty(AwsV4HttpSigner.NORMALIZE_PATH, true);

        return new V4Properties(credentials,
                                credentialScope,
                                signingClock,
                                doubleUrlEncode,
                                normalizePath);
    }

    private static V4RequestSigner v4RequestSigner(
        SignRequest<?, ? extends AwsCredentialsIdentity> request,
        V4Properties v4Properties) {

        AuthLocation authLocation = request.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        Duration expirationDuration = request.property(EXPIRATION_DURATION);

        Function<V4Properties, V4RequestSigner> requestSigner;
        switch (authLocation) {
            case HEADER:
                if (expirationDuration != null) {
                    throw new UnsupportedOperationException(
                        String.format("%s is not supported for %s.", EXPIRATION_DURATION, AuthLocation.HEADER));
                }
                requestSigner = V4RequestSigner::header;
                break;
            case QUERY_STRING:
                requestSigner = expirationDuration == null ? V4RequestSigner::query :
                                properties -> V4RequestSigner.presigned(properties,
                                                                        validateExpirationDuration(expirationDuration));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported authLocation " + authLocation);
        }

        return requestSigner.apply(v4Properties);
    }

    private Checksummer checksummer(SignRequest<?, ? extends AwsCredentialsIdentity> request) {
        boolean isPayloadSigning = request.requireProperty(PAYLOAD_SIGNING_ENABLED, true);
        return isPayloadSigning ? Checksummer.create() : new PrecomputedChecksummer(() -> UNSIGNED_PAYLOAD);
    }

    private static SyncSignedRequest doSign(SyncSignRequest<? extends AwsCredentialsIdentity> request,
                                            Checksummer checksummer,
                                            V4RequestSigner requestSigner,
                                            V4PayloadSigner payloadSigner) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedRequest.builder()
                                    .request(request.request())
                                    .payload(request.payload().orElse(null))
                                    .build();
        }

        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();

        String checksum = checksummer.checksum(request.payload().orElse(null));
        requestBuilder.putHeader("x-amz-content-sha256", checksum);

        V4Context v4Context = requestSigner.sign(requestBuilder);

        ContentStreamProvider payload = payloadSigner.sign(request.payload().orElse(null), v4Context);

        return SyncSignedRequest.builder()
                                .request(v4Context.getSignedRequest().build())
                                .payload(payload)
                                .build();
    }

    private static CompletableFuture<AsyncSignedRequest> doSign(AsyncSignRequest<? extends AwsCredentialsIdentity> request,
                                                                Checksummer checksummer,
                                                                V4RequestSigner requestSigner,
                                                                V4PayloadSigner payloadSigner) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return CompletableFuture.completedFuture(
                AsyncSignedRequest.builder()
                                  .request(request.request())
                                  .payload(request.payload().orElse(null))
                                  .build()
            );
        }

        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();

        CompletableFuture<V4Context> futureV4Context =
            checksummer.checksum(request.payload().orElse(null)).thenApply(
                checksum -> {
                    requestBuilder.putHeader("x-amz-content-sha256", checksum);
                    return requestSigner.sign(requestBuilder);
                });

        Publisher<ByteBuffer> payload = payloadSigner.sign(request.payload().orElse(null), futureV4Context);

        return futureV4Context.thenApply(v4Context -> AsyncSignedRequest.builder()
                                                                        .request(v4Context.getSignedRequest().build())
                                                                        .payload(payload)
                                                                        .build());
    }

    private static Duration validateExpirationDuration(Duration expirationDuration) {
        if (expirationDuration.compareTo(PRESIGN_URL_MAX_EXPIRATION_DURATION) > 0) {
            throw new IllegalArgumentException(
                "Requests that are pre-signed by SigV4 algorithm are valid for at most 7" +
                " days. The expiration duration set on the current request [" + expirationDuration + "]" +
                " has exceeded this limit."
            );
        }
        return expirationDuration;
    }
}
