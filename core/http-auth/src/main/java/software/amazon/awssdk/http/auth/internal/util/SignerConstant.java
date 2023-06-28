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

package software.amazon.awssdk.http.auth.internal.util;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class SignerConstant {

    public static final String AWS4_TERMINATOR = "aws4_request";

    public static final String AWS4_SIGNING_ALGORITHM = "AWS4-HMAC-SHA256";

    public static final String X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256";

    public static final String AUTHORIZATION = "Authorization";

    public static final String X_AMZ_SECURITY_TOKEN = "X-Amz-Security-Token";

    public static final String X_AMZ_CREDENTIAL = "X-Amz-Credential";

    public static final String X_AMZ_DATE = "X-Amz-Date";

    public static final String X_AMZ_EXPIRES = "X-Amz-Expires";

    public static final String X_AMZ_SIGNED_HEADERS = "X-Amz-SignedHeaders";

    public static final String X_AMZ_SIGNATURE = "X-Amz-Signature";

    public static final String X_AMZ_ALGORITHM = "X-Amz-Algorithm";

    public static final String HOST = "Host";

    public static final String LINE_SEPARATOR = "\n";

    /**
     * Seconds in a week, which is the max expiration time Sig-v4 accepts.
     */
    public static final Duration PRESIGN_URL_MAX_EXPIRATION_DURATION = Duration.ofDays(7);


    private SignerConstant() {
    }
}
