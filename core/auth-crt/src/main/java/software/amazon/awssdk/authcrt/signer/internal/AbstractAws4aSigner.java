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

package software.amazon.awssdk.authcrt.signer.internal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public abstract class AbstractAws4aSigner implements Signer, Presigner {

    /**
     * Attribute allowing the user to inject a clock that will be used for the signing timestamp
     */
    public static final ExecutionAttribute<Clock> SIGNING_CLOCK = new ExecutionAttribute<>("SigningClock");

    private static final Boolean DEFAULT_DOUBLE_URL_ENCODE = Boolean.TRUE;
    private static Charset UTF8 = StandardCharsets.UTF_8;

    private Credentials buildCredentials(ExecutionAttributes executionAttributes) {
        AwsCredentials sdkCredentials = SigningUtils.sanitizeCredentials(
                executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS));
        byte[] sessionToken = null;
        if (sdkCredentials instanceof AwsSessionCredentials) {
            AwsSessionCredentials sessionCreds = (AwsSessionCredentials) sdkCredentials;
            sessionToken = sessionCreds.sessionToken().getBytes(UTF8);
        }

        return new Credentials(sdkCredentials.accessKeyId().getBytes(UTF8),
                sdkCredentials.secretAccessKey().getBytes(UTF8), sessionToken);
    }

    protected Clock getSigningClock(ExecutionAttributes executionAttributes) {
        Clock clock = executionAttributes.getAttribute(AbstractAws4aSigner.SIGNING_CLOCK);
        if (clock != null) {
            return clock;
        }

        Clock baseClock = Clock.systemUTC();
        Optional<Integer> timeOffset = Optional.ofNullable(executionAttributes.getAttribute(
                AwsSignerExecutionAttribute.TIME_OFFSET));
        return timeOffset
                .map(offset -> Clock.offset(baseClock, Duration.ofSeconds(-offset)))
                .orElse(baseClock);
    }

    protected void fillInCrtSigningConfig(AwsSigningConfig signingConfig,
                                          SdkHttpFullRequest request,
                                          ExecutionAttributes executionAttributes) {
        signingConfig.setCredentials(buildCredentials(executionAttributes));
        signingConfig.setService(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME));
        signingConfig.setRegion(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)
                .toString()); // TODO: Temporary, real solution TBD with Lemmy integration

        signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        signingConfig.setShouldNormalizeUriPath(true);
        signingConfig.setTime(getSigningClock(executionAttributes).instant().toEpochMilli());

        if (executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE) != null) {
            signingConfig.setUseDoubleUriEncode(executionAttributes
                    .getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
        } else {
            signingConfig.setUseDoubleUriEncode(DEFAULT_DOUBLE_URL_ENCODE);
        }
    }

    protected void fillInCrtPresigningConfig(AwsSigningConfig signingConfig,
                                          SdkHttpFullRequest request,
                                          ExecutionAttributes executionAttributes) {

        fillInCrtSigningConfig(signingConfig, request, executionAttributes);

        long expirationInSeconds;
        Optional<Instant> expirationTime = Optional.ofNullable(executionAttributes
                .getAttribute(AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION));
        if (expirationTime == null || !expirationTime.isPresent()) {
            expirationInSeconds = SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS;
        } else {
            Instant start = getSigningClock(executionAttributes).instant();
            Instant end = expirationTime.get();
            expirationInSeconds = Math.max(0, Duration.between(start, end).getSeconds());
        }

        signingConfig.setExpirationInSeconds(expirationInSeconds);
    }

    @SdkTestInternalApi
    public AwsSigningConfig createCrtSigningConfig(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = new AwsSigningConfig();

        fillInCrtSigningConfig(signingConfig, request, executionAttributes);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);

        return signingConfig;
    }

    @SdkTestInternalApi
    public AwsSigningConfig createCrtPreSigningConfig(SdkHttpFullRequest request,
                                                      ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = new AwsSigningConfig();

        fillInCrtPresigningConfig(signingConfig, request, executionAttributes);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);

        return signingConfig;
    }

    /**
     * Creates an Aws Sigv4a signed http request from an unsigned http request via header-based signing.
     * @param request The request to sign
     * @param executionAttributes Contains the attributes required for signing the request
     * @return a sigv4a-signed request
     */
    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (CredentialUtils.isAnonymous(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS))) {
            return request;
        }

        AwsSigningConfig signingConfig = createCrtSigningConfig(request, executionAttributes);
        return signWithCrt(request, signingConfig);
    }

    /**
     * Creates an Aws Sigv4a signed http request from an unsigned http request via query param signing.
     * @param request The request to presign
     * @param executionAttributes Contains the attributes required for pre signing the request
     * @return a sigv4a-signed request
     */
    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = createCrtPreSigningConfig(request, executionAttributes);
        return signWithCrt(request, signingConfig);
    }

    private SdkHttpFullRequest signWithCrt(SdkHttpFullRequest request, AwsSigningConfig signingConfig) {
        HttpRequest crtRequest = CrtHttpUtils.createCrtRequest(SigningUtils.sanitizeSdkRequestForCrtSigning(request));
        CompletableFuture<HttpRequest> future = AwsSigner.signRequest(crtRequest, signingConfig);
        try {
            HttpRequest signedRequest = future.get();
            return CrtHttpUtils.createSignedSdkRequest(request, signedRequest);
        } catch (Exception e) {
            throw SdkClientException.builder()
                    .message("Unable to sign request: " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }


}
