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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.Checksummer;
import software.amazon.awssdk.http.auth.aws.signer.V4Context;
import software.amazon.awssdk.http.auth.aws.signer.V4PayloadSigner;
import software.amazon.awssdk.http.auth.aws.signer.V4RequestSigner;
import software.amazon.awssdk.http.auth.aws.util.CredentialUtils;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * An implementation of a {@link AwsV4HttpSigner}, and signs a request according to the SigV4 process as defined here:
 * https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html
 * <p>
 * Of note, this functionality can be extended by providing implementations of the underlying interfaces: {@link Checksummer},
 * {@link V4RequestSigner}, and {@link V4PayloadSigner}.
 * For example, header-based V4-authorization can be implemented by constructing a V4HttpSigner with an implementation of
 * a {@link V4RequestSigner} that takes the signature and adds it to an Authorization header.
 */
@SdkInternalApi
public final class V4HttpSigner implements AwsV4HttpSigner {
    private final Checksummer checksummer;
    private final V4RequestSigner requestSigner;
    private final V4PayloadSigner payloadSigner;

    public V4HttpSigner(Checksummer checksummer, V4RequestSigner requestSigner, V4PayloadSigner payloadSigner) {
        this.checksummer = checksummer;
        this.requestSigner = requestSigner;
        this.payloadSigner = payloadSigner;
    }

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        // anonymous credentials, don't sign
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

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return AsyncSignedRequest.builder()
                                     .request(request.request())
                                     .payload(request.payload().orElse(null))
                                     .build();
        }

        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();

        CompletableFuture<V4Context> futureV4Context =
            checksummer.checksum(request.payload().orElse(null)).thenApply(
                checksum -> {
                    requestBuilder.putHeader("x-amz-content-sha256", checksum);
                    return requestSigner.sign(requestBuilder);
                });

        Publisher<ByteBuffer> payload = payloadSigner.sign(request.payload().orElse(null), futureV4Context);

        return AsyncSignedRequest.builder()
                                 .request(CompletableFutureUtils.joinLikeSync(futureV4Context).getSignedRequest().build())
                                 .payload(payload)
                                 .build();
    }
}
