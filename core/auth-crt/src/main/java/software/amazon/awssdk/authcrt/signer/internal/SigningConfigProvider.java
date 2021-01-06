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

import static software.amazon.awssdk.authcrt.signer.internal.SigningUtils.buildCredentials;
import static software.amazon.awssdk.authcrt.signer.internal.SigningUtils.getSigningClock;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;

@SdkInternalApi
public class SigningConfigProvider {

    private static final Boolean DEFAULT_DOUBLE_URL_ENCODE = Boolean.TRUE;

    public SigningConfigProvider() {
    }

    public AwsSigningConfig createCrtSigningConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = createDefaultRequestConfig(executionAttributes);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);

        return signingConfig;
    }

    public AwsSigningConfig createCrtPresigningConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = createPresigningConfig(executionAttributes);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);

        return signingConfig;
    }

    public AwsSigningConfig createS3CrtSigningConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = createDefaultRequestConfig(executionAttributes);
        signingConfig.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);
        return signingConfig;
    }

    public AwsSigningConfig createS3CrtPresigningConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = createPresigningConfig(executionAttributes);
        signingConfig.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.NONE);
        signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);

        return signingConfig;
    }

    public AwsSigningConfig createChunkedSigningConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = createStringToSignConfig(executionAttributes);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_CHUNK);
        signingConfig.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.NONE);
        return signingConfig;
    }

    private AwsSigningConfig createPresigningConfig(ExecutionAttributes executionAttributes) {
        Optional<Instant> expirationTime = Optional.ofNullable(
            executionAttributes.getAttribute(AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION));

        long expirationInSeconds = expirationTime
            .map(end -> Math.max(0, Duration.between(getSigningClock(executionAttributes).instant(), end).getSeconds()))
            .orElse(SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS);

        AwsSigningConfig signingConfig = createDefaultRequestConfig(executionAttributes);
        signingConfig.setExpirationInSeconds(expirationInSeconds);
        return signingConfig;
    }

    private AwsSigningConfig createDefaultRequestConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = createStringToSignConfig(executionAttributes);

        signingConfig.setShouldNormalizeUriPath(true);
        if (executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE) != null) {
            signingConfig.setUseDoubleUriEncode(executionAttributes
                                                    .getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
        } else {
            signingConfig.setUseDoubleUriEncode(DEFAULT_DOUBLE_URL_ENCODE);
        }
        return signingConfig;
    }

    private AwsSigningConfig createStringToSignConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig signingConfig = new AwsSigningConfig();
        signingConfig.setCredentials(buildCredentials(executionAttributes));
        signingConfig.setService(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME));
        signingConfig.setRegion(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id());
        // TODO: Temporary, real solution TBD with Lemmy integration

        signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        signingConfig.setTime(getSigningClock(executionAttributes).instant().toEpochMilli());
        return signingConfig;
    }

}
