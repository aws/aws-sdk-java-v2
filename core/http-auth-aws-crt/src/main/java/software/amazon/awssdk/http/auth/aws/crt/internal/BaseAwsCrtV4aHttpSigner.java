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

package software.amazon.awssdk.http.auth.aws.crt.internal;

import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtHttpRequestConverter.toRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.sanitizeRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.toCredentials;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningResult;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.internal.util.CredentialUtils;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An internal extension of {@link AwsCrtV4aHttpSigner} that enables composable implementations of aws-signers that use
 * a set of properties, which may extend {@link AwsCrtV4aHttpProperties}, in order to sign requests.
 * <p>
 * The process for signing requests to AWS services is documented
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html">here</a>.
 */
@SdkInternalApi
public interface BaseAwsCrtV4aHttpSigner<T extends AwsCrtV4aHttpProperties> extends AwsCrtV4aHttpSigner {

    /**
     * Get the base implementation of a {@link BaseAwsCrtV4aHttpSigner} that uses {@link AwsCrtV4aHttpProperties}.
     */
    static BaseAwsCrtV4aHttpSigner<AwsCrtV4aHttpProperties> create() {
        return new BaseAwsCrtV4aHttpSignerImpl();
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

        AwsSigningConfig signingConfig = createSigningConfig(properties);

        SdkHttpRequest sanitizedRequest = adaptRequest(request.request(), properties);

        HttpRequest crtRequest = transformRequest(sanitizedRequest, request.payload().orElse(null), properties);

        SigV4aRequestContext sigV4aRequestContext = sign(sanitizedRequest, crtRequest, signingConfig, properties);

        ContentStreamProvider payload = sign(request.payload().orElse(null), sigV4aRequestContext, properties);

        return SyncSignedRequest.builder()
            .request(sigV4aRequestContext.getSignedRequest())
            .payload(payload)
            .build();
    }

    @Override
    default AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request)
            throws UnsupportedOperationException {
        // There isn't currently a concept of async for crt signers
        throw new UnsupportedOperationException();
    }

    AwsSigningConfig createSigningConfig(T properties);

    /**
     * Adapt the {@link SdkHttpRequest}, making any necessary changes to the request (modify headers,
     * query-parameters, etc) before it is translated to the CRT-equivalent.
     */
    SdkHttpRequest adaptRequest(SdkHttpRequest request, T properties);

    /**
     * Transform the {@link SdkHttpRequest} and the {@link ContentStreamProvider} to a {@link HttpRequest}
     */
    HttpRequest transformRequest(SdkHttpRequest request, ContentStreamProvider payload,
                                 T properties);

    /**
     * Sign the request with CRT using an {@link AwsSigningConfig} and a set of properties, and return a
     * {@link SigV4aRequestContext}.
     */
    SigV4aRequestContext sign(SdkHttpRequest request, HttpRequest crtRequest, AwsSigningConfig signingConfig, T properties);

    /**
     * Sign the payload, if applicable, using a {@link SigV4aRequestContext} and properties.
     */
    ContentStreamProvider sign(ContentStreamProvider payload, SigV4aRequestContext sigV4aRequestContext, T properties);

    /**
     * Derive the properties from the {@link SignRequest}.
     */
    T getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest);

    /**
     * An implementation of a {@link BaseAwsCrtV4aHttpSigner} that uses the SigV4a process.
     * <p>
     * It calls the AWS-CRT to handle the actual V4a signing, and mainly performs conversions to and from
     * formats of the signer and CRT.
     */
    final class BaseAwsCrtV4aHttpSignerImpl implements BaseAwsCrtV4aHttpSigner<AwsCrtV4aHttpProperties> {

        @Override
        public AwsSigningConfig createSigningConfig(AwsCrtV4aHttpProperties properties) {
            try (AwsSigningConfig signingConfig = new AwsSigningConfig()) {
                signingConfig.setCredentials(toCredentials(properties.getCredentials()));
                signingConfig.setService(properties.getCredentialScope().getService());
                signingConfig.setRegion(properties.getCredentialScope().getRegion());
                signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
                signingConfig.setTime(properties.getCredentialScope().getInstant().toEpochMilli());
                signingConfig.setUseDoubleUriEncode(properties.shouldDoubleUrlEncode());
                signingConfig.setShouldNormalizeUriPath(properties.shouldNormalizePath());
                signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);
                return signingConfig;
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate CRT signing config: ", e);
            }
        }

        @Override
        public SdkHttpRequest adaptRequest(SdkHttpRequest request, AwsCrtV4aHttpProperties properties) {
            return sanitizeRequest(request);
        }

        @Override
        public HttpRequest transformRequest(SdkHttpRequest request, ContentStreamProvider payload,
                                            AwsCrtV4aHttpProperties properties) {
            return toRequest(request, payload);
        }

        @Override
        public SigV4aRequestContext sign(SdkHttpRequest request, HttpRequest crtRequest, AwsSigningConfig signingConfig,
                                         AwsCrtV4aHttpProperties properties) {
            CompletableFuture<AwsSigningResult> future = AwsSigner.sign(crtRequest, signingConfig);
            try {
                AwsSigningResult signingResult = future.get();
                return new SigV4aRequestContext(
                    toRequest(request, signingResult.getSignedRequest()),
                    signingResult.getSignedRequest(),
                    signingResult.getSignature(),
                    signingConfig);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("The thread got interrupted while attempting to sign request: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException("Unable to sign request: " + e.getMessage(), e);
            }
        }

        @Override
        public ContentStreamProvider sign(ContentStreamProvider payload, SigV4aRequestContext sigV4aRequestContext,
                                          AwsCrtV4aHttpProperties properties) {
            // the base implementation does not sign or chunk-encode the payload.
            return payload;
        }


        @Override
        public AwsCrtV4aHttpProperties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
            return AwsCrtV4aHttpProperties.create(signRequest);
        }
    }
}
