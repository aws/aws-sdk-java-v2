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
import static software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils.createSdkChecksumFromRequest;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addDateHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addHostHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.formatTimestamp;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.hashCanonicalRequest;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.putChecksumHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.validatedProperty;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2;
import software.amazon.awssdk.http.auth.internal.util.CredentialScope;
import software.amazon.awssdk.http.auth.internal.util.CredentialUtils;
import software.amazon.awssdk.http.auth.internal.util.DigestComputingSubscriber;
import software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.http.auth.internal.util.SignerConstant;
import software.amazon.awssdk.http.auth.internal.util.SignerUtils;
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

/**
 * A default implementation of {@link AwsV4HttpSigner}.
 */
@SdkInternalApi
public class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {

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

        ContentChecksum contentChecksum = createChecksum(request);

        // preSign()

        SdkHttpRequest.Builder requestBuilder = sign(request, contentChecksum);

        // postSign()

        return SyncSignedRequest.builder()
            .request(requestBuilder.build())
            .payload(request.payload().orElse(null))
            .build();
    }

    private SdkChecksum createSdkChecksum(SignRequest<?, ?> request) {
        String checksumHeaderName = validatedProperty(request, CHECKSUM_HEADER_NAME, "");
        ChecksumAlgorithm checksumAlgorithm = validatedProperty(request, CHECKSUM_ALGORITHM, null);

        if (StringUtils.isNotBlank(checksumHeaderName) && checksumAlgorithm == null) {
            throw new IllegalArgumentException(
                CHECKSUM_ALGORITHM + " cannot be null when " + CHECKSUM_HEADER_NAME + " is given!");
        }

        return createSdkChecksumFromRequest(request.request(), checksumHeaderName, checksumAlgorithm);
    }

    private ContentChecksum createChecksum(SyncSignRequest<?> request) {
        SdkChecksum sdkChecksum = createSdkChecksum(request);
        String contentHash = createContentHash(request.payload().orElse(null), sdkChecksum);

        return new ContentChecksum(contentHash, sdkChecksum);
    }

    private CompletableFuture<ContentChecksum> createChecksum(AsyncSignRequest<?> request) {
        SdkChecksum sdkChecksum = createSdkChecksum(request);

        return createContentHash(request.payload().orElse(null), sdkChecksum).thenApply(
            hash -> new ContentChecksum(hash, sdkChecksum));
    }

    private SdkHttpRequest.Builder sign(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest,
                                        ContentChecksum contentChecksum) {
        SdkHttpRequest.Builder requestBuilder = signRequest.request().toBuilder();

        Instant requestSigningInstant = validatedProperty(signRequest, SIGNING_CLOCK).instant();
        String regionName = validatedProperty(signRequest, REGION_NAME);
        String serviceSigningName = validatedProperty(signRequest, SERVICE_SIGNING_NAME);
        AwsCredentialsIdentity credentials = sanitizeCredentials(signRequest.identity());
        CredentialScope credentialScope = new CredentialScope(regionName, serviceSigningName, requestSigningInstant);
        String algorithm = AWS4_SIGNING_ALGORITHM;

        // 0
        if (credentials instanceof AwsSessionCredentialsIdentity) {
            addSessionCredentials(requestBuilder, (AwsSessionCredentialsIdentity) credentials);
        }

        addPrerequisites(requestBuilder, signRequest, contentChecksum);

        // 1
        CanonicalRequestV2 canonicalRequest = createCanonicalRequest(signRequest, requestBuilder.build(), contentChecksum);

        // 2
        String canonicalRequestHash = hashCanonicalRequest(canonicalRequest.getString());

        // 3
        String stringToSign = createSignString(algorithm, credentialScope, canonicalRequestHash);

        // 4
        byte[] signingKey = deriveSigningKey(credentials, credentialScope);
        String signature = createSignature(stringToSign, signingKey);

        // 5
        addSignature(requestBuilder, algorithm, credentials, credentialScope, canonicalRequest, signature);
        // query signer implements this differently ^^^

        return requestBuilder;
    }

    public void addPrerequisites(SdkHttpRequest.Builder requestBuilder, SignRequest<?, ? extends AwsCredentialsIdentity> signRequest,
                                 ContentChecksum contentChecksum) {
        Instant requestSigningInstant = validatedProperty(signRequest, SIGNING_CLOCK).instant();
        String formattedRequestSigningDateTime = formatTimestamp(requestSigningInstant);
        String checksumHeaderName = signRequest.property(CHECKSUM_HEADER_NAME);

        // addContentHeader(requestBuilder, String contentHash); ??
        requestBuilder.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)
            .filter(h -> h.equals("required"))
            .ifPresent(h ->
                requestBuilder.putHeader(
                    SignerConstant.X_AMZ_CONTENT_SHA256, contentChecksum.contentHash()));

        addHostHeader(requestBuilder);
        addDateHeader(requestBuilder, formattedRequestSigningDateTime);
        putChecksumHeader(contentChecksum.contentFlexibleChecksum(),
            requestBuilder, contentChecksum.contentHash(), checksumHeaderName);

    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return AsyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        // create a checksum with an empty hash as a placeholder

        CompletableFuture<SdkHttpRequest> signedReqFuture =
            createChecksum(request).thenApply(
                contentChecksum -> {

                    SdkHttpRequest.Builder builder = sign(request, contentChecksum);
                    return builder.build();
                });


        return AsyncSignedRequest.builder()
            .request(CompletableFutureUtils.joinLikeSync(signedReqFuture))
            .payload(request.payload().orElse(null))
            .build();
    }

    /**
     * Adds session credentials to the request given.
     */
    public void addSessionCredentials(SdkHttpRequest.Builder requestBuilder,
                                       AwsSessionCredentialsIdentity credentials) {
        requestBuilder.putHeader(SignerConstant.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }

    private String createContentHash(ContentStreamProvider payload, SdkChecksum checksum) {
        return HttpChecksumUtils.calculateContentHash(payload, checksum);
    }

    private CompletableFuture<String> createContentHash(Publisher<ByteBuffer> payload, SdkChecksum checksum) {
        DigestComputingSubscriber bodyDigester = DigestComputingSubscriber.forSha256(checksum);

        if (payload != null) {
            payload.subscribe(bodyDigester);
        }

        return bodyDigester.digestBytes().thenApply(BinaryUtils::toHex);
    }

    private CanonicalRequestV2 createCanonicalRequest(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest,
                                                      SdkHttpRequest request,
                                                      ContentChecksum contentChecksum) {
        Boolean doubleUrlEncode = validatedProperty(signRequest, DOUBLE_URL_ENCODE, true);
        Boolean normalizePath = validatedProperty(signRequest, NORMALIZE_PATH, true);

        return new CanonicalRequestV2(request, contentChecksum.contentHash(), new CanonicalRequestV2.Options(
            doubleUrlEncode,
            normalizePath
        ));
    }

    private String createSignString(String algorithm, CredentialScope credentialScope, String canonicalRequestHash) {
        LOG.debug(() -> "AWS4 Canonical Request Hash: " + canonicalRequestHash);

        String stringToSign = algorithm +
            SignerConstant.LINE_SEPARATOR +
            credentialScope.getDatetime() +
            SignerConstant.LINE_SEPARATOR +
            credentialScope.scope() +
            SignerConstant.LINE_SEPARATOR +
            canonicalRequestHash;

        LOG.debug(() -> "AWS4 String to sign: " + stringToSign);
        return stringToSign;
    }

    private String createSignature(String stringToSign, byte[] signingKey) {
        return BinaryUtils.toHex(
            SignerUtils.computeSignature(stringToSign, signingKey)
        );
    }

    // TODO: Rename??
    public void addSignature(SdkHttpRequest.Builder requestBuilder, String algorithm, AwsCredentialsIdentity credentials,
                              CredentialScope credentialScope,
                              CanonicalRequestV2 canonicalRequest,
                              String signature) {
        String authHeader = AWS4_SIGNING_ALGORITHM
            + " Credential="
            + credentialScope.scope(credentials)
            + ", SignedHeaders="
            + canonicalRequest.getSignedHeadersString()
            + ", Signature="
            + signature;

        requestBuilder
            .putHeader(SignerConstant.AUTHORIZATION, authHeader);
    }
}
