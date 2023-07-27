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

package software.amazon.awssdk.http.auth.aws.signer;

import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.addHostHeader;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.getBinaryRequestPayloadStream;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.hash;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.hashCanonicalRequest;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.internal.util.DigestComputingSubscriber;
import software.amazon.awssdk.http.auth.aws.util.CredentialUtils;
import software.amazon.awssdk.http.auth.aws.util.SignerConstant;
import software.amazon.awssdk.http.auth.aws.util.SignerUtils;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * An internal extension of {@link AwsV4HttpSigner} that enables composable implementations of aws-signers that use
 * a set of properties, which may extend {@link AwsV4Properties}, in order to sign requests.
 * <p>
 * The process for signing requests to AWS services is documented
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html">here</a>.
 */
@SdkProtectedApi
public interface BaseAwsV4HttpSigner<T extends AwsV4Properties> extends AwsV4HttpSigner {

    /**
     * Get the base implementation of a {@link BaseAwsV4HttpSigner} that uses {@link AwsV4Properties}.
     */
    static BaseAwsV4HttpSigner<AwsV4Properties> create() {
        return new AwsV4HttpSignerImpl();
    }

    @Override
    default SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        T properties = getProperties(request);

        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        String contentHash = createContentHash(request, properties);

        SigV4Context v4RequestContext = processRequest(request.request(), contentHash, properties);

        ContentStreamProvider payload = processPayload(request.payload().orElse(null), v4RequestContext, properties);

        return SyncSignedRequest.builder()
            .request(v4RequestContext.getSignedRequest())
            .payload(payload)
            .build();
    }

    @Override
    default AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        T properties = getProperties(request);

        if (CredentialUtils.isAnonymous(request.identity())) {
            return AsyncSignedRequest.builder()
                .request(request.request())
                .payload(request.payload().orElse(null))
                .build();
        }

        CompletableFuture<SigV4Context> futureV4RequestContext =
            createContentHash(request, properties).thenApply(
                contentHash -> processRequest(request.request(), contentHash, properties));

        Publisher<ByteBuffer> payload = processPayload(request.payload().orElse(null), futureV4RequestContext, properties);

        return AsyncSignedRequest.builder()
            .request(CompletableFutureUtils.joinLikeSync(futureV4RequestContext).getSignedRequest())
            .payload(payload)
            .build();
    }

    /**
     * Using a {@link SdkHttpRequest}, a content hash, and properties, process a request in order to
     * form a signed request (in the form of a {@link SigV4Context}) according to the SigV4 signing documentation:
     * <p>
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html
     */
    default SigV4Context processRequest(SdkHttpRequest request, String contentHash, T properties) {

        SdkHttpRequest.Builder requestBuilder = request.toBuilder();

        // Perform any necessary pre-work, such as handling session-credentials or adding required headers
        // to the request before it gets signed
        addPrerequisites(requestBuilder, contentHash, properties);

        // Step 1: Create a canonical request
        AwsV4CanonicalRequest canonicalRequest = createCanonicalRequest(requestBuilder.build(), contentHash, properties);

        // Step 2: Create a hash of the canonical request
        String canonicalRequestHash = hashCanonicalRequest(canonicalRequest.getCanonicalRequestString());

        // Step 2: Create a hash of the canonical request
        String stringToSign = createSignString(canonicalRequestHash, properties);

        // Step 4: Calculate the signature
        byte[] signingKey = createSigningKey(properties);

        String signature = createSignature(stringToSign, signingKey, properties);

        // Step 5: Add the signature to the request
        addSignature(requestBuilder, canonicalRequest, signature, properties);

        return new SigV4Context(contentHash, signingKey, signature, requestBuilder.build());
    }

    /**
     * Generate a content hash.
     */
    String createContentHash(SyncSignRequest<?> signRequest, T properties);

    /**
     * Generate a {@link CompletableFuture} for the content hash.
     */
    CompletableFuture<String> createContentHash(AsyncSignRequest<?> signRequest, T properties);

    /**
     * Add any prerequisite items to the request.
     * <p>
     * Such an item could be a header or query parameter that should be included in the signature of the request.
     */
    void addPrerequisites(SdkHttpRequest.Builder requestBuilder, String contentHash, T properties);

    /**
     * Generate a {@link AwsV4CanonicalRequest}.
     */
    AwsV4CanonicalRequest createCanonicalRequest(SdkHttpRequest request, String contentHash, T properties);

    /**
     * Generate a string-to-sign.
     */
    String createSignString(String canonicalRequestHash, T properties);

    /**
     * Generate a signing key.
     */
    byte[] createSigningKey(T properties);

    /**
     * Generate a signature for the string-to-sign.
     */
    String createSignature(String stringToSign, byte[] signingKey, T properties);

    /**
     * Add the signature to the request in some form (as a header, query-parameter, or otherwise).
     */
    void addSignature(SdkHttpRequest.Builder requestBuilder,
                      AwsV4CanonicalRequest canonicalRequest,
                      String signature,
                      T properties);

    /**
     * Process a request payload ({@link ContentStreamProvider}).
     */
    ContentStreamProvider processPayload(ContentStreamProvider payload, SigV4Context v4RequestContext,
                                         T properties);

    /**
     * Process a request payload ({@link Publisher<ByteBuffer>}).
     */
    Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                         CompletableFuture<SigV4Context> futureV4RequestContext,
                                         T properties);

    /**
     * Derive the properties from the {@link SignRequest}.
     */
    T getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest);


    /**
     * An implementation of a {@link BaseAwsV4HttpSigner} that uses the SigV4 process.
     * <p>
     * However, It does not add any product of SigV4 to the request (such as headers or params), as it is meant to be composed
     * with other implementations.
     */
    final class AwsV4HttpSignerImpl implements BaseAwsV4HttpSigner<AwsV4Properties> {

        private static final Logger LOG = Logger.loggerFor(AwsV4HttpSignerImpl.class);

        @Override
        public String createContentHash(SyncSignRequest<?> signRequest, AwsV4Properties properties) {
            ContentStreamProvider payload = signRequest.payload().orElse(null);
            InputStream payloadStream = getBinaryRequestPayloadStream(payload);
            return BinaryUtils.toHex(hash(payloadStream));
        }

        @Override
        public CompletableFuture<String> createContentHash(AsyncSignRequest<?> signRequest, AwsV4Properties properties) {
            DigestComputingSubscriber bodyDigester = DigestComputingSubscriber.forSha256();
            Publisher<ByteBuffer> payload = signRequest.payload().orElse(null);

            if (payload != null) {
                payload.subscribe(bodyDigester);
            }

            return bodyDigester.digestBytes().thenApply(BinaryUtils::toHex);
        }

        @Override
        public void addPrerequisites(SdkHttpRequest.Builder requestBuilder, String contentHash, AwsV4Properties properties) {
            addHostHeader(requestBuilder);
        }

        @Override
        public AwsV4CanonicalRequest createCanonicalRequest(SdkHttpRequest request, String contentHash,
                                                            AwsV4Properties properties) {
            return new AwsV4CanonicalRequest(request, contentHash, new AwsV4CanonicalRequest.Options(
                properties.shouldDoubleUrlEncode(),
                properties.shouldNormalizePath()
            ));
        }

        @Override
        public String createSignString(String canonicalRequestHash, AwsV4Properties properties) {
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
        public byte[] createSigningKey(AwsV4Properties properties) {
            return deriveSigningKey(properties.getCredentials(),
                properties.getCredentialScope());
        }

        @Override
        public String createSignature(String stringToSign, byte[] signingKey, AwsV4Properties properties) {
            return BinaryUtils.toHex(
                SignerUtils.computeSignature(stringToSign, signingKey)
            );
        }

        @Override
        public void addSignature(SdkHttpRequest.Builder requestBuilder,
                                 AwsV4CanonicalRequest canonicalRequest,
                                 String signature,
                                 AwsV4Properties properties) {
        }

        @Override
        public ContentStreamProvider processPayload(ContentStreamProvider payload,
                                                    SigV4Context v4RequestContext, AwsV4Properties properties) {
            // The default implementation does nothing, as this version of signing does not
            // modify or update the payload object
            return payload;
        }

        @Override
        public Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                                    CompletableFuture<SigV4Context> futureV4RequestContext,
                                                    AwsV4Properties properties) {
            // The default implementation does nothing, as this version of signer does not
            // modify or update the payload object
            return payload;
        }

        @Override
        public AwsV4Properties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
            return AwsV4Properties.create(signRequest);
        }
    }
}
