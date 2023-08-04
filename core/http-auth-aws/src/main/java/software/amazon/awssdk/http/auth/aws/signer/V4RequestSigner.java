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

import static software.amazon.awssdk.http.auth.aws.signer.V4CanonicalRequest.getCanonicalHeaders;
import static software.amazon.awssdk.http.auth.aws.signer.V4CanonicalRequest.getSignedHeadersString;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.addDateHeader;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.formatDateTime;

import java.time.Duration;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.DefaultV4RequestSigner;
import software.amazon.awssdk.http.auth.aws.util.SignerConstant;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.Pair;

/**
 * An interface which declares an algorithm that takes a request and a content-hash and signs the request according to the SigV4
 * process.
 */
@SdkProtectedApi
public interface V4RequestSigner {
    /**
     * Retrieve an implementation of a V4RequestSigner, which signs the request, but does not add authentication to the request.
     */
    static V4RequestSigner create(V4Properties properties) {
        return new DefaultV4RequestSigner(properties);
    }

    /**
     * Retrieve an implementation of a V4RequestSigner, which signs the request and adds authentication through headers.
     */
    static V4RequestSigner header(V4Properties properties) {
        return requestBuilder -> {
            // Add pre-requisites
            if (properties.getCredentials() instanceof AwsSessionCredentialsIdentity) {
                requestBuilder.putHeader(SignerConstant.X_AMZ_SECURITY_TOKEN,
                                         ((AwsSessionCredentialsIdentity) properties.getCredentials()).sessionToken());
            }
            addDateHeader(requestBuilder, formatDateTime(properties.getCredentialScope().getInstant()));

            V4Context ctx = create(properties).sign(requestBuilder);

            // Add the signature within an authorization header
            String authHeader = AWS4_SIGNING_ALGORITHM
                                + " Credential=" + properties.getCredentialScope().scope(properties.getCredentials())
                                + ", SignedHeaders=" + ctx.getCanonicalRequest().getSignedHeadersString()
                                + ", Signature=" + ctx.getSignature();

            requestBuilder.putHeader(SignerConstant.AUTHORIZATION, authHeader);
            return ctx;
        };
    }

    /**
     * Retrieve an implementation of a V4RequestSigner, which signs the request and adds authentication through query parameters.
     */
    static V4RequestSigner query(V4Properties properties) {
        return requestBuilder -> {
            // Add pre-requisites
            if (properties.getCredentials() instanceof AwsSessionCredentialsIdentity) {
                requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SECURITY_TOKEN,
                                                    ((AwsSessionCredentialsIdentity) properties.getCredentials()).sessionToken());
            }

            List<Pair<String, List<String>>> canonicalHeaders = getCanonicalHeaders(requestBuilder.build());
            requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_ALGORITHM, AWS4_SIGNING_ALGORITHM);
            requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_DATE, properties.getCredentialScope().getDatetime());
            requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SIGNED_HEADERS, getSignedHeadersString(canonicalHeaders));
            requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_CREDENTIAL,
                                                properties.getCredentialScope().scope(properties.getCredentials()));

            V4Context ctx = create(properties).sign(requestBuilder);

            // Add the signature
            requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_SIGNATURE, ctx.getSignature());

            return ctx;
        };
    }

    /**
     * Retrieve an implementation of a V4RequestSigner, which signs the request and adds authentication through query parameters,
     * which includes an expiration param, signalling how long a request signature is valid.
     */
    static V4RequestSigner presigned(V4Properties properties, Duration expirationDuration) {
        return requestBuilder -> {
            requestBuilder.putRawQueryParameter(SignerConstant.X_AMZ_EXPIRES,
                                                Long.toString(expirationDuration.getSeconds())
            );

            return query(properties).sign(requestBuilder);
        };
    }

    /**
     * Given a request builder, sign a request and return a v4-context containing the signed request and its properties.
     */
    V4Context sign(SdkHttpRequest.Builder requestBuilder);
}

