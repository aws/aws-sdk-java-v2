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

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.profile.internal.securitytoken.ProfileCredentialsService;
import software.amazon.awssdk.auth.profile.internal.securitytoken.RoleInfo;
import software.amazon.awssdk.util.StringUtils;

/**
 * Serves assume role credentials defined in a {@link BasicProfile}. If a profile defines the role_arn property then the profile
 * is treated as an assume role profile. Does basic validation that the role exists and the source (long lived) credentials are
 * valid.
 */
@SdkInternalApi
@Immutable
public class ProfileAssumeRoleCredentialsProvider implements AwsCredentialsProvider {
    private final AllProfiles allProfiles;
    private final BasicProfile profile;
    private final ProfileCredentialsService profileCredentialsService;
    private final AwsCredentialsProvider assumeRoleCredentialsProvider;

    public ProfileAssumeRoleCredentialsProvider(ProfileCredentialsService profileCredentialsService,
                                                AllProfiles allProfiles, BasicProfile profile) {
        this.allProfiles = allProfiles;
        this.profile = profile;
        this.profileCredentialsService = profileCredentialsService;
        this.assumeRoleCredentialsProvider = fromAssumeRole();
    }

    @Override
    public AwsCredentials getCredentials() {
        return assumeRoleCredentialsProvider.getCredentials();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + assumeRoleCredentialsProvider + ")";
    }

    @ReviewBeforeRelease("This is gross. It needs to be cleaned up before GA when we refactor profiles.")
    private AwsCredentialsProvider fromAssumeRole() {
        if (StringUtils.isNullOrEmpty(profile.getRoleSourceProfile())) {
            throw new SdkClientException(String.format(
                    "Unable to load credentials from profile [%s]: Source profile name is not specified",
                    profile.getProfileName()));
        }

        final BasicProfile sourceProfile = allProfiles.getProfile(this.profile.getRoleSourceProfile());
        if (sourceProfile == null) {
            throw new SdkClientException(String.format("Unable to load source profile [%s]: Source profile was not found [%s]",
                                                       profile.getProfileName(), profile.getRoleSourceProfile()));
        }

        AwsCredentials credentials = new ProfileStaticCredentialsProvider(sourceProfile).getCredentials();

        final String roleSessionName = (this.profile.getRoleSessionName() == null) ?
                                       "aws-sdk-java-" + System.currentTimeMillis() : this.profile.getRoleSessionName();
        RoleInfo roleInfo = new RoleInfo().withRoleArn(this.profile.getRoleArn())
                                          .withRoleSessionName(roleSessionName)
                                          .withExternalId(this.profile.getRoleExternalId())
                                          .withLongLivedCredentials(credentials);
        return profileCredentialsService.getAssumeRoleCredentialsProvider(roleInfo);
    }
}
