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

package software.amazon.awssdk.http.auth;

import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.hashCanonicalRequest;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.internal.AwsV4HttpProperties;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2;
import software.amazon.awssdk.http.auth.internal.util.CredentialScope;
import software.amazon.awssdk.http.auth.internal.util.CredentialUtils;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * An {@link HttpSigner} that will sign a request using an AWS credentials {@link AwsCredentialsIdentity}).
 * <p>
 * The process for signing requests to AWS services is documented
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html">here</a>.
 */
@SdkPublicApi
public interface AwsV4HttpSigner<T extends AwsV4HttpProperties> extends HttpSigner<AwsCredentialsIdentity> {
    /**
     * The AWS region name to be used for computing the signature.
     * This property is required.
     */
    SignerProperty<String> REGION_NAME =
        SignerProperty.create(String.class, "RegionName");

    /**
     * The name of the AWS service.
     * This property is required.
     */
    SignerProperty<String> SERVICE_SIGNING_NAME =
        SignerProperty.create(String.class, "ServiceSigningName");

    /**
     * A {@link Clock} to be used at the time of signing.
     * This property defaults to the time at which signing occurs.
     */
    SignerProperty<Clock> SIGNING_CLOCK =
        SignerProperty.create(Clock.class, "SigningClock");

    /**
     * The name of the header for the checksum.
     * This property is optional.
     */
    SignerProperty<String> CHECKSUM_HEADER_NAME =
        SignerProperty.create(String.class, "ChecksumHeaderName");

    /**
     * The {@link ChecksumAlgorithm} used to compute the checksum.
     * This property is required *if* a checksum-header name is given.
     */
    SignerProperty<ChecksumAlgorithm> CHECKSUM_ALGORITHM =
        SignerProperty.create(ChecksumAlgorithm.class, "ChecksumAlgorithm");

    /**
     * A boolean to indicate whether to double url-encode the resource path
     * when constructing the canonical request.
     * This property defaults to true.
     */
    SignerProperty<Boolean> DOUBLE_URL_ENCODE =
        SignerProperty.create(Boolean.class, "DoubleUrlEncode");

    /**
     * A boolean to indicate whether the resource path should be "normalized"
     * according to RFC3986 when constructing the canonical request.
     * This property defaults to true.
     */
    SignerProperty<Boolean> NORMALIZE_PATH =
        SignerProperty.create(Boolean.class, "NormalizePath");

    /**
     * The location where auth-related data is inserted, as a result of signing.
     * The valid choices are "Header" and "Query", where "Header" indicates that request
     * headers are added, and "Query" indicates query-parameters are added.
     * This property defaults to "Header".
     */
    SignerProperty<String> AUTH_LOCATION =
        SignerProperty.create(String.class, "AuthLocation");

    /**
     * The duration for the request to be valid.
     * This property defaults to the max valid duration (7 days).
     * This is only used in the case of a pre-signing implementation.
     */
    SignerProperty<Duration> EXPIRATION_DURATION =
        SignerProperty.create(Duration.class, "ExpirationDuration");

    /**
     * Whether to enable chunked encoding or not.
     * This property defaults to false.
     * This is only used in the case of an implementation that supports chunked-encoding.
     */
    SignerProperty<Boolean> CHUNKED_ENCODING =
        SignerProperty.create(Boolean.class, "ChunkedEncoding");

    /**
     * Whether to indicate that a payload is signed or not.
     * This property defaults to false.
     */
    SignerProperty<Boolean> PAYLOAD_SIGNING =
        SignerProperty.create(Boolean.class, "PayloadSigning");


    /**
     * Get a default implementation of a {@link AwsV4HttpSigner}
     *
     * @return AwsV4HttpSigner
     */
    static AwsV4HttpSigner<AwsV4HttpProperties> create() {
        return new DefaultAwsV4HttpSigner();
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
}
