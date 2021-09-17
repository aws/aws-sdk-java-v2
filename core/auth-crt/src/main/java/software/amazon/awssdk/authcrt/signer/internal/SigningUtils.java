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

import static software.amazon.awssdk.utils.CollectionUtils.isNullOrEmpty;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public class SigningUtils {

    /**
     * Attribute allowing the user to inject a clock that will be used for the signing timestamp
     */
    public static final ExecutionAttribute<Clock> SIGNING_CLOCK = new ExecutionAttribute<>("SigningClock");

    private static final String BODY_HASH_NAME = "x-amz-content-sha256";
    private static final String DATE_NAME = "X-Amz-Date";
    private static final String AUTHORIZATION_NAME = "Authorization";
    private static final String REGION_SET_NAME = "X-amz-region-set";

    private static final String SIGNATURE_NAME = "X-Amz-Signature";
    private static final String CREDENTIAL_NAME = "X-Amz-Credential";
    private static final String ALGORITHM_NAME = "X-Amz-Algorithm";
    private static final String SIGNED_HEADERS_NAME = "X-Amz-SignedHeaders";
    private static final String EXPIRES_NAME = "X-Amz-Expires";

    private static final Set<String> FORBIDDEN_HEADERS = buildForbiddenHeaderSet();
    private static final Set<String> FORBIDDEN_PARAMS = buildForbiddenQueryParamSet();

    private static final String HOST_HEADER = "Host";

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
            AwsSignerExecutionAttribute.TIME_OFFSET));
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

        // Add host if missing
        Map<String, List<String>> headers = request.headers();
        if (isNullOrEmpty(headers.get(HOST_HEADER))) {
            builder.putHeader(HOST_HEADER, request.host());
        }

        // Filter headers that will cause signing to fail
        for (Map.Entry<String, List<String>> header: headers.entrySet()) {
            if (!FORBIDDEN_HEADERS.contains(header.getKey())) {
                builder.putHeader(header.getKey(), header.getValue());
            }
        }

        builder.clearQueryParameters();

        // Filter query parameters that will cause signing to fail
        Map<String, List<String>> params = request.rawQueryParameters();
        for (Map.Entry<String, List<String>> param: params.entrySet()) {
            if (!FORBIDDEN_PARAMS.contains(param.getKey())) {
                builder.putRawQueryParameter(param.getKey(), param.getValue());
            }
        }

        return builder.build();
    }

    private static Set<String> buildForbiddenHeaderSet() {
        Set<String> forbiddenHeaders = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        forbiddenHeaders.add(BODY_HASH_NAME);
        forbiddenHeaders.add(DATE_NAME);
        forbiddenHeaders.add(AUTHORIZATION_NAME);
        forbiddenHeaders.add(REGION_SET_NAME);

        return forbiddenHeaders;
    }

    private static Set<String> buildForbiddenQueryParamSet() {
        Set<String> forbiddenParams = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        forbiddenParams.add(SIGNATURE_NAME);
        forbiddenParams.add(DATE_NAME);
        forbiddenParams.add(CREDENTIAL_NAME);
        forbiddenParams.add(ALGORITHM_NAME);
        forbiddenParams.add(SIGNED_HEADERS_NAME);
        forbiddenParams.add(REGION_SET_NAME);
        forbiddenParams.add(EXPIRES_NAME);

        return forbiddenParams;
    }
}
