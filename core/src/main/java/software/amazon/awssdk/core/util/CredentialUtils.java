/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.util;

import software.amazon.awssdk.core.AwsRequest;
import software.amazon.awssdk.core.AwsRequestOverrideConfig;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;

public final class CredentialUtils {

    private CredentialUtils() {
    }

    /**
     *  Returns the credentials provider that will be used to fetch the
     *  credentials when signing the request. Request specific credentials
     *  takes precedence over the credentials/credentials provider set in the
     *  client.
     */
    public static AwsCredentialsProvider getCredentialsProvider(AwsRequest req, AwsCredentialsProvider base) {
        if (req == null) {
            return base;
        }
        return req.requestOverrideConfig()
                .flatMap(AwsRequestOverrideConfig::credentialsProvider)
                .orElse(base);
    }

    public static AwsCredentialsProvider getCredentialsProvider(AwsRequestOverrideConfig requestConfig,
                                                                AwsCredentialsProvider base) {
        return requestConfig.credentialsProvider().orElse(base);
    }

    /**
     * Determine whether the provided credentials are anonymous credentials, indicating that the customer is not attempting to
     * authenticate themselves.
     */
    public static boolean isAnonymous(AwsCredentials credentials) {
        return credentials.secretAccessKey() == null && credentials.accessKeyId() == null;
    }
}
