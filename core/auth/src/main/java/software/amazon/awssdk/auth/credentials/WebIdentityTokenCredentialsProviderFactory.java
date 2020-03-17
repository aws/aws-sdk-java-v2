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

package software.amazon.awssdk.auth.credentials;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityTokenCredentialProperties;
import software.amazon.awssdk.profiles.Profile;

/**
 * A factory for {@link AwsCredentialsProvider}s that are derived from web identity tokens.
 *
 * Currently this is used to allow a {@link Profile} or environment variable configured with a role that should be assumed with
 * a web identity token to create a credentials provider via the
 * 'software.amazon.awssdk.services.sts.internal.StsWebIdentityCredentialsProviderFactory', assuming STS is on the classpath.
 */
@FunctionalInterface
@SdkProtectedApi
public interface WebIdentityTokenCredentialsProviderFactory {
    AwsCredentialsProvider create(WebIdentityTokenCredentialProperties credentialProperties);
}
