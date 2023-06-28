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
import static software.amazon.awssdk.http.auth.internal.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.validatedProperty;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
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
 * A default implementation of {@link AwsCrtV4aHttpSigner}.
 */
@SdkInternalApi
public class DefaultAwsCrtV4aHttpSigner implements AwsCrtV4aHttpSigner {

    // required
    protected String regionName;
    protected String serviceSigningName;

    // optional
    protected Clock signingClock;
    protected boolean doubleUrlEncode;
    protected boolean normalizePath;

    // auxiliary
    protected Instant requestSigningInstant;
    protected AwsCredentialsIdentity credentials;
    protected AwsSigningConfig.AwsSigningAlgorithm algorithm;
    protected AwsSigningConfig signingConfig = new AwsSigningConfig();

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

        setSigningConfig(signingConfig);

        SdkHttpRequest sanitizedRequest = adaptRequest(request.request());

        HttpRequest crtRequest = transformRequest(sanitizedRequest, request.payload().orElse(null));

        HttpRequest signedCrtRequest = sign(crtRequest, signingConfig);

        SdkHttpRequest signedRequest = transformRequest(signedCrtRequest, request.request());

        return SyncSignedRequest.builder()
            .request(signedRequest)
            .payload(request.payload().orElse(null))
            .build();
    }

    /**
     * Set required, optional, and auxiliary parameters that are needed throughout the process
     * of signing a request (i.e. validated signer properties, credentials, etc).
     */
    private void setParameters(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        // required
        regionName = validatedProperty(signRequest, REGION_NAME);
        serviceSigningName = validatedProperty(signRequest, SERVICE_SIGNING_NAME);

        // optional
        signingClock = validatedProperty(signRequest, SIGNING_CLOCK, Clock.systemUTC());
        doubleUrlEncode = validatedProperty(signRequest, DOUBLE_URL_ENCODE, true);
        normalizePath = validatedProperty(signRequest, NORMALIZE_PATH, true);

        // auxiliary
        requestSigningInstant = signingClock.instant();
        credentials = sanitizeCredentials(signRequest.identity());
        algorithm = AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC;
    }

    /**
     * Set configuration parameters on the {@link AwsSigningConfig}, which will be sent to CRT.
     */
    private void setSigningConfig(AwsSigningConfig config) {
        config.setCredentials(toCredentials(credentials));
        config.setService(serviceSigningName);
        config.setRegion(regionName);
        config.setAlgorithm(algorithm);
        config.setTime(requestSigningInstant.toEpochMilli());
        config.setUseDoubleUriEncode(doubleUrlEncode);
        config.setShouldNormalizeUriPath(normalizePath);
        config.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);
    }

    /**
     * Adapt the {@link SdkHttpRequest}, making any necessary changes to the request (modify headers,
     * query-parameters, etc) before it is translated to the CRT-equivalent.
     */
    private SdkHttpRequest adaptRequest(SdkHttpRequest request) {
        return sanitizeRequest(request);
    }

    /**
     * Transform the {@link SdkHttpRequest} and the {@link ContentStreamProvider} to a {@link HttpRequest}
     */
    private HttpRequest transformRequest(SdkHttpRequest request, ContentStreamProvider payload) {
        return toRequest(request, payload);
    }

    private HttpRequest sign(HttpRequest crtRequest, AwsSigningConfig signingConfig) {
        CompletableFuture<HttpRequest> future = AwsSigner.signRequest(crtRequest, signingConfig);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("The thread got interrupted while attempting to sign request: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign request: " + e.getMessage(), e);
        }
    }

    /**
     * Transform the signed {@link HttpRequest} using the *original* {@link SdkHttpRequest} to create a signed
     * equivalent of an {@link SdkHttpRequest}.
     */
    private SdkHttpRequest transformRequest(HttpRequest signedCrtRequest, SdkHttpRequest request) {
        return toRequest(request, signedCrtRequest);
    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request)
        throws UnsupportedOperationException {
        // There isn't currently a concept of async for crt signers
        throw new UnsupportedOperationException();
    }
}
