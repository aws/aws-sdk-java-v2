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
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addChecksumHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addDateHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addHostHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addSha256ContentHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.formatDateTime;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.hashCanonicalRequest;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.validatedProperty;

import java.nio.ByteBuffer;
import java.time.Clock;
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

    // required
    protected String regionName;
    protected String serviceSigningName;

    // optional
    protected Clock signingClock;
    protected String checksumHeaderName;
    protected ChecksumAlgorithm checksumAlgorithm;
    protected boolean doubleUrlEncode;
    protected boolean normalizePath;

    // auxiliary
    protected Instant requestSigningInstant;
    protected AwsCredentialsIdentity credentials;
    protected CredentialScope credentialScope;
    protected String algorithm;
    protected SdkChecksum sdkChecksum;

    // compound
    protected String signature;

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        setParameters(request);

        ContentChecksum contentChecksum = createChecksum(request);

        SdkHttpRequest.Builder requestBuilder = sign(request, contentChecksum);

        return SyncSignedRequest.builder()
            .request(requestBuilder.build())
            .payload(request.payload().orElse(null))
            .build();
    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return AsyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        // Set all necessary parameters needed for signing the request
        setParameters(request);

        CompletableFuture<SdkHttpRequest> signedReqFuture =
            createChecksum(request).thenApply(
                contentChecksum -> {
                    SdkHttpRequest.Builder builder = sign(request, contentChecksum);
                    return builder.build();
                });

        // process the request payload, if necessary
        Publisher<ByteBuffer> payload = processPayload(request.payload().orElse(null));

        return AsyncSignedRequest.builder()
            .request(CompletableFutureUtils.joinLikeSync(signedReqFuture))
            .payload(payload)
            .build();
    }

    protected void setParameters(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        // required
        regionName = validatedProperty(signRequest, REGION_NAME);
        serviceSigningName = validatedProperty(signRequest, SERVICE_SIGNING_NAME);

        // optional
        signingClock = validatedProperty(signRequest, SIGNING_CLOCK, Clock.systemUTC());
        checksumHeaderName = validatedProperty(signRequest, CHECKSUM_HEADER_NAME, "");
        checksumAlgorithm = validatedProperty(signRequest, CHECKSUM_ALGORITHM, null);
        doubleUrlEncode = validatedProperty(signRequest, DOUBLE_URL_ENCODE, true);
        normalizePath = validatedProperty(signRequest, NORMALIZE_PATH, true);

        // auxillary
        requestSigningInstant = signingClock.instant();
        credentials = sanitizeCredentials(signRequest.identity());
        credentialScope = new CredentialScope(regionName, serviceSigningName, requestSigningInstant);
        algorithm = AWS4_SIGNING_ALGORITHM;
        sdkChecksum = createSdkChecksum(signRequest);
    }

    /**
     * Using a {@link SignRequest} and a {@link ContentChecksum}, perform all the necessary steps to
     * create a signed request according to the SigV4 signing documentation:
     * <p>
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html
     */
    private <T> SdkHttpRequest.Builder sign(SignRequest<T, ? extends AwsCredentialsIdentity> signRequest,
                                            ContentChecksum contentChecksum) {

        SdkHttpRequest.Builder requestBuilder = signRequest.request().toBuilder();

        // Perform any necessary pre-work, such as handling session-credentials or adding required headers
        // to the request before it gets signed
        if (credentials instanceof AwsSessionCredentialsIdentity) {
            addSessionCredentials(requestBuilder, (AwsSessionCredentialsIdentity) credentials);
        }
        addPrerequisites(requestBuilder, contentChecksum);

        // Step 1: Create a canonical request
        CanonicalRequestV2 canonicalRequest = createCanonicalRequest(requestBuilder.build(), contentChecksum);

        // Step 2: Create a hash of the canonical request
        String canonicalRequestHash = hashCanonicalRequest(canonicalRequest.getCanonicalRequestString());

        // Step 2: Create a hash of the canonical request
        String stringToSign = createSignString(algorithm, credentialScope, canonicalRequestHash);

        // Step 4: Calculate the signature
        byte[] signingKey = deriveSigningKey(credentials, credentialScope);
        signature = createSignature(stringToSign, signingKey);

        // Step 5: Add the signature to the request
        addSignature(requestBuilder, canonicalRequest, signature);

        return requestBuilder;
    }

    /**
     * Generate an {@link SdkChecksum} from the {@link SignRequest}.
     */
    private SdkChecksum createSdkChecksum(SignRequest<?, ?> signRequest) {
        if (StringUtils.isNotBlank(checksumHeaderName) && checksumAlgorithm == null) {
            throw new IllegalArgumentException(
                CHECKSUM_ALGORITHM + " cannot be null when " + CHECKSUM_HEADER_NAME + " is given!");
        }

        return createSdkChecksumFromRequest(signRequest.request(), checksumHeaderName, checksumAlgorithm);
    }

    /**
     * Generate a {@link ContentChecksum} from the {@link SyncSignRequest}.
     */
    private ContentChecksum createChecksum(SyncSignRequest<?> signRequest) {
        String contentHash = createContentHash(signRequest.payload().orElse(null));

        return new ContentChecksum(contentHash, sdkChecksum);
    }

    /**
     * Generate a {@link CompletableFuture<ContentChecksum>} from the {@link AsyncSignRequest}
     */
    private CompletableFuture<ContentChecksum> createChecksum(AsyncSignRequest<?> signRequest) {
        return createContentHash(signRequest.payload().orElse(null)).thenApply(
            hash -> new ContentChecksum(hash, sdkChecksum));
    }

    /**
     * Generate a content hash by using the {@link ContentStreamProvider} and the {@link SdkChecksum}.
     */
    private String createContentHash(ContentStreamProvider payload) {
        return HttpChecksumUtils.calculateContentHash(payload, sdkChecksum);
    }

    /**
     * Generate a {@link CompletableFuture} for the content hash by using the {@link Publisher<ByteBuffer>}
     * and the {@link SdkChecksum}.
     */
    protected CompletableFuture<String> createContentHash(Publisher<ByteBuffer> payload) {
        DigestComputingSubscriber bodyDigester = DigestComputingSubscriber.forSha256(sdkChecksum);

        if (payload != null) {
            payload.subscribe(bodyDigester);
        }

        return bodyDigester.digestBytes().thenApply(BinaryUtils::toHex);
    }

    /**
     * Add any prerequisite items to the request using the {@link SdkHttpRequest.Builder}, the ${@link SignRequest},
     * and the {@link ContentChecksum}
     * <p>
     * Such an item could be a header or query parameter that should be included in the signature of the request.
     */
    protected void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                    ContentChecksum contentChecksum) {
        String formattedRequestSigningDateTime = formatDateTime(requestSigningInstant);

        addSha256ContentHeader(requestBuilder, contentChecksum);
        addHostHeader(requestBuilder);
        addDateHeader(requestBuilder, formattedRequestSigningDateTime);
        addChecksumHeader(requestBuilder, contentChecksum.contentFlexibleChecksum(),
            contentChecksum.contentHash(), checksumHeaderName);
    }

    /**
     * Add an {@link AwsSessionCredentialsIdentity} to the request via {@link SdkHttpRequest.Builder}.
     */
    protected void addSessionCredentials(SdkHttpRequest.Builder requestBuilder,
                                         AwsSessionCredentialsIdentity credentials) {
        requestBuilder.putHeader(SignerConstant.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }

    /**
     * Generate a {@link CanonicalRequestV2} from the {@link SignRequest},the {@link SdkHttpRequest},
     * and the {@link ContentChecksum}.
     */
    private CanonicalRequestV2 createCanonicalRequest(SdkHttpRequest request, ContentChecksum contentChecksum) {
        return new CanonicalRequestV2(request, contentChecksum.contentHash(), new CanonicalRequestV2.Options(
            doubleUrlEncode,
            normalizePath
        ));
    }

    /**
     * Generate a string-to-sign using the algorithm, the {@link CredentialScope}, and the hash of the canonical request.
     */
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

    /**
     * Generate a signature using the string-to-sign and the signing key.
     */
    private String createSignature(String stringToSign, byte[] signingKey) {
        return BinaryUtils.toHex(
            SignerUtils.computeSignature(stringToSign, signingKey)
        );
    }

    /**
     * Add the signature to the request in some form (as a header, query-parameter, or otherwise).
     */
    protected void addSignature(SdkHttpRequest.Builder requestBuilder,
                                CanonicalRequestV2 canonicalRequest,
                                String signature) {
        String authHeader = algorithm
            + " Credential=" + credentialScope.scope(credentials)
            + ", SignedHeaders=" + canonicalRequest.getSignedHeadersString()
            + ", Signature=" + signature;

        requestBuilder.putHeader(SignerConstant.AUTHORIZATION, authHeader);
    }

    /**
     * Process a request payload ({@link Publisher<ByteBuffer>}).
     */
    protected Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload) {
        // The default implementation does nothing, as this version of signing does not
        // modify or updated the payload object
        return payload;
    }
}
