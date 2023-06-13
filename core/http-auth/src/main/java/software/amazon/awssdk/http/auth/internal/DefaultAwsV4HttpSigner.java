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
import static software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils.createSdkChecksumFromRequest;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.buildAuthorizationHeader;
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
import software.amazon.awssdk.http.auth.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumSpecs;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.internal.util.CanonicalRequest;
import software.amazon.awssdk.http.auth.internal.util.CredentialUtils;
import software.amazon.awssdk.http.auth.internal.util.DigestComputingSubscriber;
import software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils;
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
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * A default implementation of {@link AwsV4HttpSigner}.
 */
@SdkInternalApi
public final class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {

    public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    private static final Logger LOG = Logger.loggerFor(DefaultAwsV4HttpSigner.class);

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        SdkHttpRequest signedRequest = doSign(request).build();
        return SyncSignedRequest.builder()
            .request(signedRequest)
            .payload(request.payload().orElse(null))
            .build();
    }

    private SdkHttpRequest.Builder doSign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        SdkChecksum sdkChecksum =
            createSdkChecksumFromRequest(request.request(), validatedProperty(request, CHECKSUM_HEADER_NAME),
                validatedProperty(request, CHECKSUM_ALGORITHM));
        String contentHash = calculateContentHash(request.payload().orElse(null), sdkChecksum);
        return doSign(request, new ContentChecksum(contentHash, sdkChecksum));
    }

    private SdkHttpRequest.Builder doSign(SignRequest<?, ? extends AwsCredentialsIdentity> request,
                                          ContentChecksum contentChecksum) {
        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();

        Boolean doubleUrlEncode = validatedProperty(request, DOUBLE_URL_ENCODE, true);
        Boolean normalizePath = validatedProperty(request, NORMALIZE_PATH, true);
        String checksumHeaderName = validatedProperty(request, CHECKSUM_HEADER_NAME);
        Instant requestSigningInstant = validatedProperty(request, REQUEST_SIGNING_INSTANT);
        String formattedRequestSigningDate = formatDateStamp(requestSigningInstant);
        String formattedRequestSigningDateTime = formatTimestamp(requestSigningInstant);
        String regionName = validatedProperty(request, REGION_NAME);
        String serviceSigningName = validatedProperty(request, SERVICE_SIGNING_NAME);
        String scope = buildScope(formattedRequestSigningDate, serviceSigningName, regionName);

        AwsCredentialsIdentity sanitizedCredentials = sanitizeCredentials(request.identity());
        if (sanitizedCredentials instanceof AwsSessionCredentialsIdentity) {
            addSessionCredentials(requestBuilder, (AwsSessionCredentialsIdentity) sanitizedCredentials);
        }

        addHostHeader(requestBuilder);
        addDateHeader(requestBuilder, formattedRequestSigningDateTime);

        requestBuilder.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)
            .filter(h -> h.equals("required"))
            .ifPresent(h ->
                requestBuilder.putHeader(
                    SignerConstant.X_AMZ_CONTENT_SHA256, contentChecksum.contentHash()));

        putChecksumHeader(contentChecksum.contentFlexibleChecksum(),
            requestBuilder, contentChecksum.contentHash(), checksumHeaderName);

        CanonicalRequest canonicalRequest =
            createCanonicalRequest(request.request(), requestBuilder, contentChecksum.contentHash(), doubleUrlEncode,
                normalizePath);

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

        requestBuilder.putHeader(SignerConstant.AUTHORIZATION,
            buildAuthorizationHeader(signature, sanitizedCredentials, scope, canonicalRequest));

        processRequestPayload(requestBuilder, signature, signingKey,
            contentChecksum.contentFlexibleChecksum());

        return requestBuilder;
    }


    /**
     * AWS4 requires that we sign the Host header, so we
     * have to have it in the request by the time we sign.
     */
    private void addHostHeader(SdkHttpRequest.Builder requestBuilder) {

        String host = requestBuilder.host();
        if (!SdkHttpUtils.isUsingStandardPort(requestBuilder.protocol(), requestBuilder.port())) {
            host += ":" + requestBuilder.port();
        }
        requestBuilder.putHeader(SignerConstant.HOST, requestBuilder.host());
    }

    private void addDateHeader(SdkHttpRequest.Builder requestBuilder, String dateTime) {
        requestBuilder.putHeader(SignerConstant.X_AMZ_DATE, dateTime);
    }

    private void putChecksumHeader(SdkChecksum sdkChecksum,
                                   SdkHttpRequest.Builder requestBuilder, String contentHashString, String headerChecksum) {
        if (shouldAddChecksumHeader(sdkChecksum, requestBuilder.build(), contentHashString, headerChecksum)) {
            requestBuilder.putHeader(headerChecksum, BinaryUtils.toBase64(sdkChecksum.getChecksumBytes()));
        }
    }

    /**
     * Return false if content is unsigned or if checksum is already added.
     * Otherwise, return true if checksum is not blank (false if it is)
     */
    private Boolean shouldAddChecksumHeader(SdkChecksum sdkChecksum,
                                            SdkHttpRequest request, String contentHashString,
                                            String headerChecksum) {
        if (sdkChecksum != null && !UNSIGNED_PAYLOAD.equals(contentHashString) &&
            !"STREAMING-UNSIGNED-PAYLOAD-TRAILER".equals(contentHashString)) {
            return false;
        }
        if (HttpChecksumUtils.isHttpChecksumPresent(request,
            ChecksumSpecs.builder()
                .headerName(headerChecksum).build())) {
            LOG.debug(() -> "Checksum already added in header ");
            return false;
        }
        return StringUtils.isNotBlank(headerChecksum);
    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return AsyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        SdkChecksum sdkChecksum =
            createSdkChecksumFromRequest(request.request(), validatedProperty(request, CHECKSUM_HEADER_NAME),
                validatedProperty(request, CHECKSUM_ALGORITHM));
        DigestComputingSubscriber bodyDigester = DigestComputingSubscriber.forSha256(sdkChecksum);

        request.payload().ifPresent((payload) ->
            payload.subscribe(bodyDigester)
        );

        CompletableFuture<byte[]> digestBytes = bodyDigester.digestBytes();

        CompletableFuture<SdkHttpRequest> signedReqFuture = digestBytes.thenApply(bodyHash -> {
            String digestHex = BinaryUtils.toHex(bodyHash);

            SdkHttpRequest.Builder builder = doSign(request,
                new ContentChecksum(digestHex, sdkChecksum));

            return builder.build();
        });

        return AsyncSignedRequest.builder()
            .request(CompletableFutureUtils.joinLikeSync(signedReqFuture))
            .payload(request.payload().orElse(null))
            .build();
    }

    /**
     * Perform any additional procedure on the request payload, with access to the result
     * from signing the header. (e.g. Signing the payload by chunk-encoding). The default
     * implementation doesn't need to do anything.
     */
    private void processRequestPayload(SdkHttpRequest.Builder requestBuilder,
                                       byte[] signature,
                                       byte[] signingKey,
                                       SdkChecksum sdkChecksum) {
    }

    /**
     * Adds session credentials to the request given.
     */
    private void addSessionCredentials(SdkHttpRequest.Builder requestBuilder,
                                       AwsSessionCredentialsIdentity credentials) {
        requestBuilder.putHeader(SignerConstant.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }
}
