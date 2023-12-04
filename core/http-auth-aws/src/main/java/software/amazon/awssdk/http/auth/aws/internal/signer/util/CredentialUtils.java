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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class CredentialUtils {

    private CredentialUtils() {
    }

    /**
     * Determine whether the provided credentials are anonymous credentials, indicating that the customer is not attempting to
     * authenticate themselves.
     */
    public static boolean isAnonymous(AwsCredentialsIdentity credentials) {
        return credentials.secretAccessKey() == null && credentials.accessKeyId() == null;
    }

    /**
     * Sanitize given credentials by trimming whitespace
     */
    public static AwsCredentialsIdentity sanitizeCredentials(AwsCredentialsIdentity credentials) {
        String accessKeyId = StringUtils.trim(credentials.accessKeyId());
        String secretKey = StringUtils.trim(credentials.secretAccessKey());
        String credentialScope = StringUtils.trim(credentials.credentialScope().orElse(null));

        if (credentials instanceof AwsSessionCredentialsIdentity) {
            AwsSessionCredentialsIdentity sessionCredentials = (AwsSessionCredentialsIdentity) credentials;
            return AwsSessionCredentialsIdentity.builder()
                                                .accessKeyId(accessKeyId)
                                                .secretAccessKey(secretKey)
                                                .sessionToken(StringUtils.trim(sessionCredentials.sessionToken()))
                                                .credentialScope(credentialScope)
                                                .build();
        }

        // given credentials are anonymous, so don't create new instance
        if (accessKeyId == null && secretKey == null) {
            return credentials;
        }

        return AwsCredentialsIdentity.builder()
                                     .accessKeyId(accessKeyId)
                                     .secretAccessKey(secretKey)
                                     .credentialScope(credentialScope)
                                     .build();
    }
}
