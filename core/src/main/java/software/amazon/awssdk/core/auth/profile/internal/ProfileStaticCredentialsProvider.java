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
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.AwsSessionCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.util.StringUtils;

/**
 * Serves credentials defined in a {@link BasicProfile}. Does validation that both access key and
 * secret key exists and are non empty.
 */
@SdkInternalApi
@Immutable
public class ProfileStaticCredentialsProvider implements AwsCredentialsProvider {

    private final BasicProfile profile;
    private final AwsCredentialsProvider credentialsProvider;

    public ProfileStaticCredentialsProvider(BasicProfile profile) {
        this.profile = profile;
        this.credentialsProvider = new StaticCredentialsProvider(fromStaticCredentials());
    }

    @Override
    public AwsCredentials getCredentials() {
        return credentialsProvider.getCredentials();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + credentialsProvider + ")";
    }

    private AwsCredentials fromStaticCredentials() {
        if (StringUtils.isNullOrEmpty(profile.getAwsAccessIdKey())) {
            throw new SdkClientException(String.format(
                    "Unable to load credentials into profile [%s]: AWS Access Key ID is not specified.",
                    profile.getProfileName()));
        }
        if (StringUtils.isNullOrEmpty(profile.getAwsSecretAccessKey())) {
            throw new SdkClientException(String.format(
                    "Unable to load credentials into profile [%s]: AWS Secret Access Key is not specified.",
                    profile.getAwsSecretAccessKey()));
        }

        if (profile.getAwsSessionToken() == null) {
            return new AwsCredentials(profile.getAwsAccessIdKey(),
                                      profile.getAwsSecretAccessKey());
        } else {
            if (profile.getAwsSessionToken().isEmpty()) {
                throw new SdkClientException(String.format(
                        "Unable to load credentials into profile [%s]: AWS Session Token is empty.",
                        profile.getProfileName()));
            }

            return new AwsSessionCredentials(profile.getAwsAccessIdKey(),
                                             profile.getAwsSecretAccessKey(),
                                             profile.getAwsSessionToken());
        }
    }

}
