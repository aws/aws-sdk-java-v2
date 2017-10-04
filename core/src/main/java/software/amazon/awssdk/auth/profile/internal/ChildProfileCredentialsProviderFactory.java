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

package software.amazon.awssdk.auth.profile.internal;

import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.profile.Profile;

/**
 * A factory for {@link AwsCredentialsProvider}s that are derived from another set of credentials in a profile file.
 *
 * Currently this is used to allow a {@link Profile} configured with a role that should be assumed to create a credentials
 * provider via the 'software.amazon.awssdk.services.sts.internal.StsProfileCredentialsProviderFactory', assuming STS is on the
 * classpath.
 */
@FunctionalInterface
public interface ChildProfileCredentialsProviderFactory {
    /**
     * Create a credentials provider for the provided profile, using the provided parental credentials provider to authenticate
     * with AWS. In the case of STS, the returned credentials provider is for a role that has been assumed, and the provided
     * parental credentials provider is the credentials that should be used to authenticate that the user is allowed to assume
     * that role.
     *
     * @param parentCredentialsProvider The credentials provider that should be used to authenticate the child credentials
     * provider.
     * @param profile The profile that should be used to load the configuration necessary to create the child credentials
     * provider.
     * @return The credentials provider with permissions derived from the parental credentials provider and profile.
     */
    AwsCredentialsProvider create(AwsCredentialsProvider parentCredentialsProvider, Profile profile);
}
