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

package software.amazon.awssdk.http.auth.internal;

import static software.amazon.awssdk.http.auth.internal.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils.calculateContentHash;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addHostHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.buildScope;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.buildStringToSign;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.computeSignature;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.createCanonicalRequest;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.formatDateStamp;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.formatTimestamp;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.validatedProperty;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.AwsV4QueryHttpSigner;
import software.amazon.awssdk.http.auth.internal.util.CanonicalRequest;
import software.amazon.awssdk.http.auth.internal.util.CredentialUtils;
import software.amazon.awssdk.http.auth.internal.util.DigestComputingSubscriber;
import software.amazon.awssdk.http.auth.internal.util.SignerConstant;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * A default implementation of {@link AwsV4QueryHttpSigner}.
 */
@SdkInternalApi
public final class DefaultAwsV4QueryHttpSigner implements AwsV4QueryHttpSigner {

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        String contentHash = calculateContentHash(request.payload().orElse(null), null);

        SdkHttpRequest signedRequest = doSign(request, contentHash).build();
        return SyncSignedRequest.builder()
            .request(signedRequest)
            .payload(request.payload().orElse(null))
            .build();
    }

    private SdkHttpRequest.Builder doSign(SignRequest<?, ? extends AwsCredentialsIdentity> request, String contentHash) {
        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();

        Instant requestSigningInstant = validatedProperty(request, REQUEST_SIGNING_INSTANT);
        Instant expirationInstant = validatedProperty(request, EXPIRATION_INSTANT,
            requestSigningInstant.plusSeconds(SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS));
        Boolean doubleUrlEncode = validatedProperty(request, DOUBLE_URL_ENCODE, true);
        Boolean normalizePath = validatedProperty(request, NORMALIZE_PATH, true);
        String formattedRequestSigningDate = formatDateStamp(requestSigningInstant);
        String formattedRequestSigningDateTime = formatTimestamp(requestSigningInstant);
        String regionName = validatedProperty(request, REGION_NAME);
        String serviceSigningName = validatedProperty(request, SERVICE_SIGNING_NAME);
        String scope = buildScope(formattedRequestSigningDate, serviceSigningName, regionName);

        long expirationInSeconds = getSignatureDurationInSeconds(requestSigningInstant, expirationInstant);

        addHostHeader(requestBuilder);

        AwsCredentialsIdentity sanitizedCredentials = sanitizeCredentials(request.identity());
        if (sanitizedCredentials instanceof AwsSessionCredentialsIdentity) {
            // For SigV4 query-signing, we need to add "X-Amz-Security-Token"
            // as a query string parameter, before constructing the canonical
            // request.
            addSessionCredentials(requestBuilder, (AwsSessionCredentialsIdentity) sanitizedCredentials);
        }

        CanonicalRequest canonicalRequest =
            createCanonicalRequest(request.request(), requestBuilder, contentHash, doubleUrlEncode,
                normalizePath);

        // add presign info
        String signingCredentials = sanitizedCredentials.accessKeyId() + "/" + scope;
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_ALGORITHM, SignerConstant.AWS4_SIGNING_ALGORITHM);
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_DATE, formattedRequestSigningDateTime);
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SIGNED_HEADER, canonicalRequest.signedHeaderString());
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_EXPIRES, Long.toString(expirationInSeconds));
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_CREDENTIAL, signingCredentials);

        String canonicalRequestString = canonicalRequest.string();
        String stringToSign = buildStringToSign(
            canonicalRequestString,
            AWS4_SIGNING_ALGORITHM,
            formattedRequestSigningDateTime,
            scope
        );

        byte[] signingKey = deriveSigningKey(
            sanitizedCredentials,
            requestSigningInstant,
            regionName,
            serviceSigningName
        );

        byte[] signature = computeSignature(stringToSign, signingKey);

        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SIGNATURE, BinaryUtils.toHex(signature));

        return requestBuilder;
    }

    private void addSessionCredentials(SdkHttpRequest.Builder requestBuilder,
                                       AwsSessionCredentialsIdentity credentials) {
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }


    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return AsyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        DigestComputingSubscriber bodyDigester = DigestComputingSubscriber.forSha256();

        request.payload().ifPresent((payload) ->
            payload.subscribe(bodyDigester)
        );

        CompletableFuture<byte[]> digestBytes = bodyDigester.digestBytes();

        CompletableFuture<SdkHttpRequest> signedReqFuture = digestBytes.thenApply(bodyHash -> {
            String digestHex = BinaryUtils.toHex(bodyHash);

            SdkHttpRequest.Builder builder = doSign(request, digestHex);

            return builder.build();
        });

        return AsyncSignedRequest.builder()
            .request(CompletableFutureUtils.joinLikeSync(signedReqFuture))
            .payload(request.payload().orElse(null))
            .build();
    }

    /**
     * Generates an expiration time for the presigned url. If user has specified
     * an expiration time, check if it is in the given limit.
     */
    private long getSignatureDurationInSeconds(Instant requestSigningInstant, Instant expirationInstant) {
        if (requestSigningInstant == null) {
            throw new RuntimeException("The request signing instant must be specified!");
        }

        long expirationInSeconds = expirationInstant.getEpochSecond() - requestSigningInstant.getEpochSecond();

        if (expirationInSeconds > SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS) {
            throw new RuntimeException("Requests that are pre-signed by SigV4 algorithm are valid for at most 7" +
                " days. The expiration date set on the current request [" +
                formatTimestamp(Instant.ofEpochSecond(expirationInSeconds)) + "] +" +
                " has exceeded this limit.");
        }
        return expirationInSeconds;
    }
}
