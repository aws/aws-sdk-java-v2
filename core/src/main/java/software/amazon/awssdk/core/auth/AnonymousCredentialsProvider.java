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

/**
 * Credentials provider that always returns anonymous {@link AwsCredentials}. Anonymous AWS credentials result in un-authenticated
 * requests and will fail unless the resource or API's policy has been configured to specifically allow anonymous access.
 */
public class AnonymousCredentialsProvider implements AwsCredentialsProvider {
    @Override
    public AwsCredentials getCredentials() {
        return AwsCredentials.ANONYMOUS_CREDENTIALS;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
