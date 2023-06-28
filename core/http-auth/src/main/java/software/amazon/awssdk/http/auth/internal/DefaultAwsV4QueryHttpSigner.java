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

import static software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2.getCanonicalHeaders;
import static software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2.getSignedHeadersString;
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.addHostHeader;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.validatedProperty;

import java.time.Duration;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.AwsV4QueryHttpSigner;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2;
import software.amazon.awssdk.http.auth.internal.util.CredentialScope;
import software.amazon.awssdk.http.auth.internal.util.SignerConstant;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.Pair;

/**
 * A default implementation of {@link AwsV4QueryHttpSigner}.
 */
@SdkInternalApi
public final class DefaultAwsV4QueryHttpSigner extends DefaultAwsV4HttpSigner implements AwsV4QueryHttpSigner {

    // optional
    private Duration expirationDuration;

    @Override
    public void setParameters(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        super.setParameters(signRequest);

        // optional
        expirationDuration = validateExpirationDuration(validatedProperty(signRequest, EXPIRATION_DURATION,
            PRESIGN_URL_MAX_EXPIRATION_DURATION));
    }

    @Override
    public void addSessionCredentials(SdkHttpRequest.Builder requestBuilder,
                                      AwsSessionCredentialsIdentity credentials) {
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }

    @Override
    public void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                 ContentChecksum contentChecksum) {
        CredentialScope credentialScope = new CredentialScope(regionName, serviceSigningName, requestSigningInstant);

        addHostHeader(requestBuilder);

        List<Pair<String, List<String>>> canonicalHeaders = getCanonicalHeaders(requestBuilder.build());
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_ALGORITHM, algorithm);
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_DATE, credentialScope.getDatetime());
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SIGNED_HEADERS, getSignedHeadersString(canonicalHeaders));
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_EXPIRES, Long.toString(expirationDuration.getSeconds()));
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_CREDENTIAL, credentialScope.scope(credentials));
    }

    @Override
    public void addSignature(SdkHttpRequest.Builder requestBuilder,
                             CanonicalRequestV2 canonicalRequest,
                             String signature) {
        requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SIGNATURE, signature);
    }

    /**
     * Check if the {@link Duration} is within the valid bounds for a pre-signed url, and return it if it is.
     */
    private Duration validateExpirationDuration(Duration expirationDuration) {
        if (expirationDuration.compareTo(SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION) > 0) {
            throw new IllegalArgumentException("Requests that are pre-signed by SigV4 algorithm are valid for at most 7" +
                " days. The expiration duration set on the current request [" + expirationDuration + "]" +
                " has exceeded this limit.");
        }
        return expirationDuration;
    }
}
