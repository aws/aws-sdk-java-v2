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

import static software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils.createSdkChecksumFromRequest;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addHostHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.hashCanonicalRequest;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.AwsV4HttpSigner;
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
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * An internal extension of {@link AwsV4HttpSigner} that enables composable implementations of aws-signers that use
 * a set of properties, which may extend {@link AwsV4HttpProperties}, in order to sign requests.
 * <p>
 * The process for signing requests to AWS services is documented
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html">here</a>.
 */
@SdkProtectedApi
public interface BaseAwsV4HttpSigner<T extends AwsV4HttpProperties> extends AwsV4HttpSigner {

    /**
     * Get the base implementation of a {@link BaseAwsV4HttpSigner} that uses {@link AwsV4HttpProperties}.
     *
     * @return BaseAwsV4HttpSigner
     */
    static BaseAwsV4HttpSigner<AwsV4HttpProperties> create() {
        return new AwsV4HttpSignerImpl();
    }

    @Override
    default SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        T properties = getProperties(request);

        ContentChecksum contentChecksum = createChecksum(request, properties);

        SigV4RequestContext v4RequestContext = processRequest(request.request(), contentChecksum, properties);

        ContentStreamProvider payload = processPayload(request.payload().orElse(null), v4RequestContext, properties);

        return SyncSignedRequest.builder()
            .request(v4RequestContext.getSignedRequest())
            .payload(payload)
            .build();
    }

    @Override
    default AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return AsyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        T properties = getProperties(request);

        CompletableFuture<SigV4RequestContext> futureV4RequestContext =
            createChecksum(request, properties).thenApply(
                contentChecksum -> processRequest(request.request(), contentChecksum, properties));

        // process the request payload, if necessary
        Publisher<ByteBuffer> payload = processPayload(request.payload().orElse(null), futureV4RequestContext, properties);

        return AsyncSignedRequest.builder()
            .request(CompletableFutureUtils.joinLikeSync(futureV4RequestContext).getSignedRequest())
            .payload(payload)
            .build();
    }

    /**
     * Using a {@link SignRequest} and a {@link ContentChecksum}, process a request in order to
     * form a signed request according to the SigV4 signing documentation:
     * <p>
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html
     */
    default SigV4RequestContext processRequest(SdkHttpRequest request, ContentChecksum contentChecksum, T properties) {

        SdkHttpRequest.Builder requestBuilder = request.toBuilder();

        // Perform any necessary pre-work, such as handling session-credentials or adding required headers
        // to the request before it gets signed
        addPrerequisites(requestBuilder, contentChecksum, properties);

        // Step 1: Create a canonical request
        CanonicalRequestV2 canonicalRequest = createCanonicalRequest(requestBuilder.build(), contentChecksum, properties);

        // Step 2: Create a hash of the canonical request
        String canonicalRequestHash = hashCanonicalRequest(canonicalRequest.getCanonicalRequestString());

        // Step 2: Create a hash of the canonical request
        String stringToSign = createSignString(canonicalRequestHash, properties);

        // Step 4: Calculate the signature
        byte[] signingKey = createSigningKey(properties);

        String signature = createSignature(stringToSign, signingKey, properties);

        // Step 5: Add the signature to the request
        addSignature(requestBuilder, canonicalRequest, signature, properties);

        return new SigV4RequestContext(contentChecksum, canonicalRequest, canonicalRequestHash, stringToSign, signingKey,
            signature,
            requestBuilder.build());
    }

    /**
     * Generate an {@link SdkChecksum} from the {@link SignRequest}.
     */
    SdkChecksum createSdkChecksum(SignRequest<?, ?> signRequest, T properties);

    /**
     * Generate a {@link ContentChecksum} from the {@link SyncSignRequest}.
     */
    ContentChecksum createChecksum(SyncSignRequest<? extends AwsCredentialsIdentity> signRequest, T properties);

    /**
     * Generate a {@link CompletableFuture<ContentChecksum>} from the {@link AsyncSignRequest}
     */
    CompletableFuture<ContentChecksum> createChecksum(AsyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                                      T properties);

    /**
     * Generate a content hash by using the {@link ContentStreamProvider} and the {@link SdkChecksum}.
     */
    String createContentHash(ContentStreamProvider payload, SdkChecksum sdkChecksum, T properties);

    /**
     * Generate a {@link CompletableFuture} for the content hash by using the {@link Publisher<ByteBuffer>}
     * and the {@link SdkChecksum}.
     */
    CompletableFuture<String> createContentHash(Publisher<ByteBuffer> payload, SdkChecksum sdkChecksum,
                                                T properties);

    /**
     * Add any prerequisite items to the request using the {@link SdkHttpRequest.Builder}, the ${@link SignRequest},
     * and the {@link ContentChecksum}
     * <p>
     * Such an item could be a header or query parameter that should be included in the signature of the request.
     */
    void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                          ContentChecksum contentChecksum, T properties);

    /**
     * Generate a {@link CanonicalRequestV2} from the {@link SignRequest},the {@link SdkHttpRequest},
     * and the {@link ContentChecksum}.
     */
    CanonicalRequestV2 createCanonicalRequest(SdkHttpRequest request, ContentChecksum contentChecksum,
                                              T properties);

    /**
     * Generate a string-to-sign using the algorithm, the {@link CredentialScope}, and the hash of the canonical request.
     */
    String createSignString(String canonicalRequestHash, T properties);

    /**
     * Generate a signing key using properties.
     */
    byte[] createSigningKey(T properties);

    /**
     * Generate a signature using the string-to-sign and the signing key.
     */
    String createSignature(String stringToSign, byte[] signingKey, T properties);

    /**
     * Add the signature to the request in some form (as a header, query-parameter, or otherwise).
     */
    void addSignature(SdkHttpRequest.Builder requestBuilder,
                      CanonicalRequestV2 canonicalRequest,
                      String signature,
                      T properties);

    /**
     * Process a request payload ({@link ContentStreamProvider}).
     */
    ContentStreamProvider processPayload(ContentStreamProvider payload, SigV4RequestContext v4RequestContext,
                                         T properties);

    /**
     * Process a request payload ({@link Publisher<ByteBuffer>}).
     */
    Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                         CompletableFuture<SigV4RequestContext> futureV4RequestContext,
                                         T properties);

    T getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest);


    /**
     * An implementation of a {@link BaseAwsV4HttpSigner} that uses the SigV4 process.
     * <p>
     * However, It does not add any product of SigV4 to the request (such as headers or params), as it is meant to be composed
     * with other implementations.
     */
    final class AwsV4HttpSignerImpl implements BaseAwsV4HttpSigner<AwsV4HttpProperties> {

        private static final Logger LOG = Logger.loggerFor(AwsV4HttpSignerImpl.class);

        /**
         * Generate an {@link SdkChecksum} from the {@link SignRequest}.
         */
        public SdkChecksum createSdkChecksum(SignRequest<?, ?> signRequest, AwsV4HttpProperties properties) {
            if (StringUtils.isNotBlank(properties.getChecksumHeader()) && properties.getChecksumAlgorithm() == null) {
                throw new IllegalArgumentException(
                    CHECKSUM_ALGORITHM + " cannot be null when " + CHECKSUM_HEADER_NAME + " is given!");
            }

            return createSdkChecksumFromRequest(signRequest.request(), properties.getChecksumHeader(),
                properties.getChecksumAlgorithm());
        }

        /**
         * Generate a {@link ContentChecksum} from the {@link SyncSignRequest}.
         */
        public ContentChecksum createChecksum(SyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                              AwsV4HttpProperties properties) {
            SdkChecksum sdkChecksum = HttpChecksumUtils.createSdkChecksumFromRequest(
                signRequest.request(),
                properties.getChecksumHeader(),
                properties.getChecksumAlgorithm()
            );
            String contentHash = createContentHash(signRequest.payload().orElse(null), sdkChecksum, properties);

            return new ContentChecksum(contentHash, sdkChecksum);
        }

        /**
         * Generate a {@link CompletableFuture<ContentChecksum>} from the {@link AsyncSignRequest}
         */
        public CompletableFuture<ContentChecksum> createChecksum(AsyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                                                 AwsV4HttpProperties properties) {
            SdkChecksum sdkChecksum = HttpChecksumUtils.createSdkChecksumFromRequest(
                signRequest.request(),
                properties.getChecksumHeader(),
                properties.getChecksumAlgorithm()
            );

            return createContentHash(signRequest.payload().orElse(null), sdkChecksum, properties).thenApply(
                hash -> new ContentChecksum(hash, sdkChecksum));
        }

        /**
         * Generate a content hash by using the {@link ContentStreamProvider} and the {@link SdkChecksum}.
         */
        public String createContentHash(ContentStreamProvider payload, SdkChecksum sdkChecksum, AwsV4HttpProperties properties) {
            return HttpChecksumUtils.calculateContentHash(payload, sdkChecksum);
        }

        /**
         * Generate a {@link CompletableFuture} for the content hash by using the {@link Publisher<ByteBuffer>}
         * and the {@link SdkChecksum}.
         */
        public CompletableFuture<String> createContentHash(Publisher<ByteBuffer> payload, SdkChecksum sdkChecksum,
                                                           AwsV4HttpProperties properties) {
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
        public void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                     ContentChecksum contentChecksum, AwsV4HttpProperties properties) {
            addHostHeader(requestBuilder);
        }

        /**
         * Generate a {@link CanonicalRequestV2} from the {@link SignRequest},the {@link SdkHttpRequest},
         * and the {@link ContentChecksum}.
         */
        public CanonicalRequestV2 createCanonicalRequest(SdkHttpRequest request, ContentChecksum contentChecksum,
                                                         AwsV4HttpProperties properties) {
            return new CanonicalRequestV2(request, contentChecksum.contentHash(), new CanonicalRequestV2.Options(
                properties.shouldDoubleUrlEncode(),
                properties.shouldNormalizePath()
            ));
        }

        /**
         * Generate a string-to-sign using the algorithm, the {@link CredentialScope}, and the hash of the canonical request.
         */
        public String createSignString(String canonicalRequestHash, AwsV4HttpProperties properties) {
            LOG.debug(() -> "AWS4 Canonical Request Hash: " + canonicalRequestHash);

            String stringToSign = AWS4_SIGNING_ALGORITHM +
                SignerConstant.LINE_SEPARATOR +
                properties.getCredentialScope().getDatetime() +
                SignerConstant.LINE_SEPARATOR +
                properties.getCredentialScope().scope() +
                SignerConstant.LINE_SEPARATOR +
                canonicalRequestHash;

            LOG.debug(() -> "AWS4 String to sign: " + stringToSign);
            return stringToSign;
        }

        @Override
        public byte[] createSigningKey(AwsV4HttpProperties properties) {
            return deriveSigningKey(properties.getCredentials(),
                properties.getCredentialScope());
        }

        @Override
        public String createSignature(String stringToSign, byte[] signingKey, AwsV4HttpProperties properties) {
            return BinaryUtils.toHex(
                SignerUtils.computeSignature(stringToSign, signingKey)
            );
        }

        @Override
        public void addSignature(SdkHttpRequest.Builder requestBuilder,
                                 CanonicalRequestV2 canonicalRequest,
                                 String signature,
                                 AwsV4HttpProperties properties) {
        }

        @Override
        public ContentStreamProvider processPayload(ContentStreamProvider payload,
                                                    SigV4RequestContext v4RequestContext, AwsV4HttpProperties properties) {
            // The default implementation does nothing, as this version of signing does not
            // modify or update the payload object
            return payload;
        }

        @Override
        public Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                                    CompletableFuture<SigV4RequestContext> futureV4RequestContext,
                                                    AwsV4HttpProperties properties) {
            // The default implementation does nothing, as this version of signer does not
            // modify or update the payload object
            return payload;
        }

        @Override
        public AwsV4HttpProperties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
            return AwsV4HttpProperties.create(signRequest);
        }
    }
}
