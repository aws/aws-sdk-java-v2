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

import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.AUTHORIZATION;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.HOST;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_CREDENTIAL;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_DATE;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_EXPIRES;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_SIGNATURE;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_SIGNED_HEADERS;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public class SigningUtils {

    /**
     * Attribute allowing the user to inject a clock that will be used for the signing timestamp
     */
    public static final ExecutionAttribute<Clock> SIGNING_CLOCK = new ExecutionAttribute<>("SigningClock");

    private static final String REGION_SET_NAME = "X-amz-region-set";

    private static final Set<String> FORBIDDEN_HEADERS = buildForbiddenHeaderSet();
    private static final Set<String> FORBIDDEN_PARAMS = buildForbiddenQueryParamSet();

    private SigningUtils() {
    }

    public static Credentials buildCredentials(ExecutionAttributes executionAttributes) {
        AwsCredentials sdkCredentials = SigningUtils.sanitizeCredentials(
            executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS));
        byte[] sessionToken = null;
        if (sdkCredentials instanceof AwsSessionCredentials) {
            AwsSessionCredentials sessionCreds = (AwsSessionCredentials) sdkCredentials;
            sessionToken = sessionCreds.sessionToken().getBytes(StandardCharsets.UTF_8);
        }

        return new Credentials(sdkCredentials.accessKeyId().getBytes(StandardCharsets.UTF_8),
                               sdkCredentials.secretAccessKey().getBytes(StandardCharsets.UTF_8), sessionToken);
    }

    public static Clock getSigningClock(ExecutionAttributes executionAttributes) {
        Clock clock = executionAttributes.getAttribute(SIGNING_CLOCK);
        if (clock != null) {
            return clock;
        }

        Clock baseClock = Clock.systemUTC();
        Optional<Integer> timeOffset = Optional.ofNullable(executionAttributes.getAttribute(
            SdkExecutionAttribute.TIME_OFFSET));
        return timeOffset
            .map(offset -> Clock.offset(baseClock, Duration.ofSeconds(-offset)))
            .orElse(baseClock);
    }

    public static AwsCredentials sanitizeCredentials(AwsCredentials credentials) {
        String accessKeyId = StringUtils.trim(credentials.accessKeyId());
        String secretKey = StringUtils.trim(credentials.secretAccessKey());

        if (credentials instanceof AwsSessionCredentials) {
            AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentials;
            return AwsSessionCredentials.create(accessKeyId,
                    secretKey,
                    StringUtils.trim(sessionCredentials.sessionToken()));
        }

        return AwsBasicCredentials.create(accessKeyId, secretKey);
    }

    public static SdkHttpFullRequest sanitizeSdkRequestForCrtSigning(SdkHttpFullRequest request) {

        SdkHttpFullRequest.Builder builder = request.toBuilder();

        // Ensure path is non-empty
        String path = builder.encodedPath();
        if (path == null || path.length() == 0) {
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

    private static Set<String> buildForbiddenHeaderSet() {
        Set<String> forbiddenHeaders = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        forbiddenHeaders.add(X_AMZ_CONTENT_SHA256);
        forbiddenHeaders.add(X_AMZ_DATE);
        forbiddenHeaders.add(AUTHORIZATION);
        forbiddenHeaders.add(REGION_SET_NAME);

        return forbiddenHeaders;
    }

    private static Set<String> buildForbiddenQueryParamSet() {
        Set<String> forbiddenParams = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        forbiddenParams.add(X_AMZ_SIGNATURE);
        forbiddenParams.add(X_AMZ_DATE);
        forbiddenParams.add(X_AMZ_CREDENTIAL);
        forbiddenParams.add(X_AMZ_ALGORITHM);
        forbiddenParams.add(X_AMZ_SIGNED_HEADERS);
        forbiddenParams.add(REGION_SET_NAME);
        forbiddenParams.add(X_AMZ_EXPIRES);

        return forbiddenParams;
    }
}
