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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.SigV4RequestContext;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2;
import software.amazon.awssdk.http.auth.internal.util.CredentialScope;
import software.amazon.awssdk.http.auth.internal.util.DigestComputingSubscriber;
import software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.http.auth.internal.util.SignerConstant;
import software.amazon.awssdk.http.auth.internal.util.SignerUtils;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * An implementation of a {@link AwsV4HttpSigner} that uses the SigV4 mechanism.
 * <p>
 * It does not add any product of SigV4 to the request (such as headers or params), as it is meant to be composed
 * with other implementations.
 */
@SdkProtectedApi
public final class DefaultAwsV4HttpSigner implements AwsV4HttpSigner<AwsV4HttpProperties> {

    private static final Logger LOG = Logger.loggerFor(DefaultAwsV4HttpSigner.class);

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
