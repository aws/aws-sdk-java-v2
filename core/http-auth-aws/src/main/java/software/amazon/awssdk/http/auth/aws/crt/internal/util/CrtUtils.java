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

package software.amazon.awssdk.http.auth.aws.crt.internal.util;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.AUTHORIZATION;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.HOST;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_CREDENTIAL;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_DATE;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_EXPIRES;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_SIGNATURE;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_SIGNED_HEADERS;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public final class CrtUtils {
    private static final String BODY_HASH_NAME = "x-amz-content-sha256";
    private static final String REGION_SET_NAME = "X-amz-region-set";

    private static final Set<String> FORBIDDEN_HEADERS =
        Stream.of(BODY_HASH_NAME, X_AMZ_DATE, AUTHORIZATION, REGION_SET_NAME)
              .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    private static final Set<String> FORBIDDEN_PARAMS =
        Stream.of(X_AMZ_SIGNATURE, X_AMZ_DATE, X_AMZ_CREDENTIAL, X_AMZ_ALGORITHM, X_AMZ_SIGNED_HEADERS, REGION_SET_NAME,
                  X_AMZ_EXPIRES)
              .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

    private CrtUtils() {
    }

    /**
     * Sanitize an {@link SdkHttpRequest}, in order to prepare it for converting to a CRT request destined to be signed.
     * <p>
     * Sanitizing includes checking the path is not empty, filtering headers and query parameters that are forbidden in CRT, and
     * adding the host header (overriding if already presesnt).
     */
    public static SdkHttpRequest sanitizeRequest(SdkHttpRequest request) {

        SdkHttpRequest.Builder builder = request.toBuilder();

        // Ensure path is non-empty
        String path = builder.encodedPath();
        if (path == null || path.isEmpty()) {
            builder.encodedPath("/");
        }

        builder.clearHeaders();

        // Filter headers that will cause signing to fail
        request.forEachHeader((name, value) -> {
            if (!FORBIDDEN_HEADERS.contains(name)) {
                builder.putHeader(name, value);
            }
        });

        // Add host, which must be signed. We ignore any pre-existing Host header to match the behavior of the SigV4 signer.
        String hostHeader = SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())
                            ? request.host()
                            : request.host() + ":" + request.port();
        builder.putHeader(HOST, hostHeader);

        builder.clearQueryParameters();

        // Filter query parameters that will cause signing to fail
        request.forEachRawQueryParameter((key, value) -> {
            if (!FORBIDDEN_PARAMS.contains(key)) {
                builder.putRawQueryParameter(key, value);
            }
        });

        return builder.build();
    }

    /**
     * Convert an {@link AwsCredentialsIdentity} to the CRT equivalent of credentials ({@link Credentials}).
     */
    public static Credentials toCredentials(AwsCredentialsIdentity credentialsIdentity) {
        byte[] sessionToken = null;

        if (credentialsIdentity == null ||
            credentialsIdentity.accessKeyId() == null ||
            credentialsIdentity.secretAccessKey() == null
        ) {
            return null;
        }

        if (credentialsIdentity instanceof AwsSessionCredentialsIdentity) {
            sessionToken = ((AwsSessionCredentialsIdentity) credentialsIdentity)
                .sessionToken()
                .getBytes(StandardCharsets.UTF_8);
        }

        return new Credentials(
            credentialsIdentity.accessKeyId().getBytes(StandardCharsets.UTF_8),
            credentialsIdentity.secretAccessKey().getBytes(StandardCharsets.UTF_8),
            sessionToken
        );
    }

}
