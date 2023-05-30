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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.internal.checksums.Algorithm;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumSpecs;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.internal.util.CanonicalRequest;
import software.amazon.awssdk.http.auth.internal.util.CredentialUtils;
import software.amazon.awssdk.http.auth.internal.util.DigestComputingSubscriber;
import software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.http.auth.internal.util.SignerConstant;
import software.amazon.awssdk.http.auth.spi.AsyncHttpSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedHttpRequest;
import software.amazon.awssdk.http.auth.spi.HttpSignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.http.auth.spi.SyncHttpSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * A default implementation of {@link AwsV4HttpSigner}.
 */
@SdkInternalApi
public class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {

    public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    /**
     * The datetime, in milliseconds, for the request.
     */
    public static final SignerProperty<Long> REQUEST_SIGNING_DATE_TIME_MILLI =
        SignerProperty.create(Long.class, "requestSigningDateTimeMilli");
    /**
     * The AWS region to be used for computing the signature.
     */
    public static final SignerProperty<String> REGION_NAME =
        SignerProperty.create(String.class, "regionName");
    /**
     * The name of the AWS service.
     */
    public static final SignerProperty<String> SERVICE_SIGNING_NAME =
        SignerProperty.create(String.class, "serviceSigningName");
    /**
     * The name of the header for the checksum.
     */
    public static final SignerProperty<String> CHECKSUM_HEADER_NAME =
        SignerProperty.create(String.class, "checksumHeaderName");
    /**
     * The Algorithm used to compute the checksum.
     */
    public static final SignerProperty<Algorithm> CHECKSUM_ALGORITHM =
        SignerProperty.create(Algorithm.class, "checksumAlgorithm");
    /**
     * A boolean to indicate whether to double url-encode the resource path
     * when constructing the canonical request.
     */
    public static final SignerProperty<Boolean> DOUBLE_URL_ENCODE =
        SignerProperty.create(Boolean.class, "doubleUrlEncode");
    /**
     * A boolean to indicate Whether the resource path should be "normalized"
     * according to RFC3986 when constructing the canonical request.
     */
    public static final SignerProperty<Boolean> NORMALIZE_PATH =
        SignerProperty.create(Boolean.class, "normalizePath");

    private static final Logger LOG = Logger.loggerFor(DefaultAwsV4HttpSigner.class);

    @Override
    public SyncSignedHttpRequest sign(SyncHttpSignRequest<? extends AwsCredentialsIdentity> request) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedHttpRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        SdkHttpRequest signedRequest = doSign(request).build();
        return SyncSignedHttpRequest.builder()
            .request(signedRequest)
            .payload(request.payload().orElse(null))
            .build();
    }

    private SdkHttpRequest.Builder doSign(SyncHttpSignRequest<? extends AwsCredentialsIdentity> request) {
        SdkChecksum sdkChecksum = createSdkChecksumFromRequest(request.request(), request.property(CHECKSUM_HEADER_NAME),
            request.property(CHECKSUM_ALGORITHM));
        String contentHash = calculateContentHash(request.payload().orElse(null), sdkChecksum);
        return doSign(request, new ContentChecksum(contentHash, sdkChecksum));
    }

    private SdkHttpRequest.Builder doSign(HttpSignRequest<?, ? extends AwsCredentialsIdentity> request,
                                          ContentChecksum contentChecksum) {
        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();
        Boolean doubleUrlEncode = request.property(DOUBLE_URL_ENCODE);
        Boolean normalizePath = request.property(NORMALIZE_PATH);
        String checksumHeaderName = request.property(CHECKSUM_HEADER_NAME);
        Long requestSigningDateTimeMilli = request.property(REQUEST_SIGNING_DATE_TIME_MILLI);
        String formattedRequestSigningDate = formatDateStamp(requestSigningDateTimeMilli);
        String formattedRequestSigningDateTime = formatTimestamp(requestSigningDateTimeMilli);
        String regionName = request.property(REGION_NAME);
        String serviceSigningName = request.property(SERVICE_SIGNING_NAME);
        String scope = buildScope(
            formattedRequestSigningDate,
            serviceSigningName,
            regionName
        );

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

        CanonicalRequest canonicalRequest = createCanonicalRequest(request.request(), requestBuilder,
            contentChecksum.contentHash(), doubleUrlEncode, normalizePath);


        String canonicalRequestString = canonicalRequest.string();
        String stringToSign = buildStringToSign(
            canonicalRequestString,
            AWS4_SIGNING_ALGORITHM,
            formattedRequestSigningDateTime,
            scope
        );

        byte[] signingKey = deriveSigningKey(
            sanitizedCredentials,
            requestSigningDateTimeMilli,
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


    private void addHostHeader(SdkHttpRequest.Builder requestBuilder) {
        // AWS4 requires that we sign the Host header, so we
        // have to have it in the request by the time we sign.

        StringBuilder hostHeaderBuilder = new StringBuilder(requestBuilder.host());
        if (!SdkHttpUtils.isUsingStandardPort(requestBuilder.protocol(), requestBuilder.port())) {
            hostHeaderBuilder.append(":").append(requestBuilder.port());
        }

        requestBuilder.putHeader(SignerConstant.HOST, hostHeaderBuilder.toString());
    }

    private void addDateHeader(SdkHttpRequest.Builder requestBuilder, String dateTime) {
        requestBuilder.putHeader(SignerConstant.X_AMZ_DATE, dateTime);
    }

    private void putChecksumHeader(SdkChecksum sdkChecksum,
                                   SdkHttpRequest.Builder requestBuilder, String contentHashString, String headerChecksum) {

        if (sdkChecksum != null && !UNSIGNED_PAYLOAD.equals(contentHashString)
            && !"STREAMING-UNSIGNED-PAYLOAD-TRAILER".equals(contentHashString)) {

            if (HttpChecksumUtils.isHttpChecksumPresent(requestBuilder.build(),
                ChecksumSpecs.builder()
                    .headerName(headerChecksum).build())) {
                LOG.debug(() -> "Checksum already added in header ");
                return;
            }
            if (StringUtils.isNotBlank(headerChecksum)) {
                requestBuilder.putHeader(headerChecksum,
                    BinaryUtils.toBase64(sdkChecksum.getChecksumBytes()));
            }
        }
    }

    @Override
    public AsyncSignedHttpRequest signAsync(AsyncHttpSignRequest<? extends AwsCredentialsIdentity> request) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return AsyncSignedHttpRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        SdkChecksum sdkChecksum = createSdkChecksumFromRequest(request.request(), request.property(CHECKSUM_HEADER_NAME),
            request.property(CHECKSUM_ALGORITHM));
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

        return AsyncSignedHttpRequest.builder()
            .request(signedReqFuture.join())
            .payload(request.payload().orElse(null))
            .build();
    }
}
