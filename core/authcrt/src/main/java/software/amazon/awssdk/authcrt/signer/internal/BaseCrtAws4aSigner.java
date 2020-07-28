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
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.authcrt.signer.params.Aws4aPresignerParams;
import software.amazon.awssdk.authcrt.signer.params.Aws4aSignerParams;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public class BaseCrtAws4aSigner<T extends Aws4aSignerParams, U extends Aws4aPresignerParams> extends AbstractAws4aSigner<T, U> {

    private static Charset UTF8 = Charset.forName("UTF-8");

    protected SdkHttpFullRequest signWithCrt(SdkHttpFullRequest request, AwsSigningConfig signingConfig) {
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

    @Override
    protected SdkHttpFullRequest sign(SdkHttpFullRequest request, Aws4aSignerParams signingParams) {
        try (AwsSigningConfig signingConfig = createCrtSigningConfig(signingParams)) {
            return signWithCrt(request, signingConfig);
        }
    }

    @Override
    protected SdkHttpFullRequest presign(SdkHttpFullRequest request, Aws4aPresignerParams signingParams) {
        try (AwsSigningConfig signingConfig = createCrtSigningConfig(signingParams)) {
            return signWithCrt(request, signingConfig);
        }
    }

    protected void fillInCrtSigningConfig(AwsSigningConfig signingConfig, Aws4aSignerParams signingParams) {
        AwsCredentials sdkCredentials = SigningUtils.sanitizeCredentials(signingParams.awsCredentials());
        byte[] sessionToken = null;
        if (sdkCredentials instanceof AwsSessionCredentials) {
            AwsSessionCredentials sessionCreds = (AwsSessionCredentials) sdkCredentials;
            sessionToken = sessionCreds.sessionToken().getBytes(UTF8);
        }

        signingConfig.setCredentials(new Credentials(sdkCredentials.accessKeyId().getBytes(UTF8),
                sdkCredentials.secretAccessKey().getBytes(UTF8), sessionToken));
        signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        signingConfig.setRegion(signingParams.signingRegionSet());
        signingConfig.setService(signingParams.signingName());
        signingConfig.setTime(signingParams.signingTimestamp().toEpochMilli());
        signingConfig.setUseDoubleUriEncode(signingParams.doubleUrlEncode());
        signingConfig.setShouldNormalizeUriPath(true);
        signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValueType.PAYLOAD);
        signingConfig.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256);
    }

    protected void fillInCrtPresigningConfig(AwsSigningConfig signingConfig, Aws4aPresignerParams signingParams) {
        fillInCrtSigningConfig(signingConfig, signingParams);

        long expirationInSeconds = 0;
        Optional<Instant> expirationTime = signingParams.expirationTime();
        if (expirationTime == null || !expirationTime.isPresent()) {
            expirationInSeconds = SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS;
        } else {
            Instant start = signingParams.signingTimestamp();
            Instant end = expirationTime.get();
            expirationInSeconds = Math.max(0, Duration.between(start, end).getSeconds());
        }

        signingConfig.setExpirationInSeconds(expirationInSeconds);
    }

    protected AwsSigningConfig createCrtSigningConfig(Aws4aSignerParams signingParams) {
        AwsSigningConfig signingConfig = new AwsSigningConfig();

        fillInCrtSigningConfig(signingConfig, signingParams);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);

        return signingConfig;
    }

    protected AwsSigningConfig createCrtSigningConfig(Aws4aPresignerParams signingParams) {
        AwsSigningConfig signingConfig = new AwsSigningConfig();

        fillInCrtPresigningConfig(signingConfig, signingParams);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);

        return signingConfig;
    }
}
