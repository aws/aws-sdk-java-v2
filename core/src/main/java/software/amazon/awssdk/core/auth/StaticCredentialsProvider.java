/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import software.amazon.awssdk.annotation.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link AwsCredentialsProvider} that returns a set implementation of {@link AwsCredentials}.
 */
@SdkPublicApi
public class StaticCredentialsProvider implements AwsCredentialsProvider {
    private final AwsCredentials credentials;

    /**
     * Create a credentials provider that always returns the provided set of credentials.
     */
    public StaticCredentialsProvider(AwsCredentials credentials) {
        this.credentials = Validate.notNull(credentials, "Credentials must not be null.");
    }

    @Override
    public AwsCredentials getCredentials() {
        return credentials;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + credentials + ")";
    }
}
