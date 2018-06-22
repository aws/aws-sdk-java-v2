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

package software.amazon.awssdk.services.sts.internal;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ChildProfileCredentialsProviderFactory;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * An implementation of {@link ChildProfileCredentialsProviderFactory} that uses configuration in a profile to create a
 * {@link StsAssumeRoleCredentialsProvider}.
 */
@SdkProtectedApi
public final class StsProfileCredentialsProviderFactory implements ChildProfileCredentialsProviderFactory {
    private static final String MISSING_PROPERTY_ERROR_FORMAT = "'%s' must be set to use role-based credential loading in the "
                                                                + "'%s' profile.";

    @Override
    public AwsCredentialsProvider create(AwsCredentialsProvider sourceCredentialsProvider, Profile profile) {
        return new StsProfileCredentialsProvider(sourceCredentialsProvider, profile);
    }

    /**
     * A wrapper for a {@link StsAssumeRoleCredentialsProvider} that is returned by this factory when
     * {@link #create(AwsCredentialsProvider, Profile)} is invoked. This wrapper is important because it ensures the parent
     * credentials provider is closed when the assume-role credentials provider is no longer needed.
     */
    private static final class StsProfileCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
        private final StsClient stsClient;
        private final AwsCredentialsProvider parentCredentialsProvider;
        private final StsAssumeRoleCredentialsProvider credentialsProvider;

        private StsProfileCredentialsProvider(AwsCredentialsProvider parentCredentialsProvider, Profile profile) {
            String roleArn = requireProperty(profile, ProfileProperty.ROLE_ARN);
            String roleSessionName = profile.property(ProfileProperty.ROLE_SESSION_NAME)
                                            .orElseGet(() -> "aws-sdk-java-" + System.currentTimeMillis());
            String externalId = profile.property(ProfileProperty.EXTERNAL_ID).orElse(null);

            // Use the default region chain and if that fails us, fall back to AWS_GLOBAL.
            Region stsRegion =
                    new AwsRegionProviderChain(new DefaultAwsRegionProviderChain(), () -> Region.AWS_GLOBAL).getRegion();

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                                                                   .roleArn(roleArn)
                                                                   .roleSessionName(roleSessionName)
                                                                   .externalId(externalId)
                                                                   .build();

            this.stsClient = StsClient.builder()
                                      .region(stsRegion)
                                      .credentialsProvider(parentCredentialsProvider)
                                      .build();

            this.parentCredentialsProvider = parentCredentialsProvider;
            this.credentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                                                                       .stsClient(stsClient)
                                                                       .refreshRequest(assumeRoleRequest)
                                                                       .build();
        }

        private String requireProperty(Profile profile, String requiredProperty) {
            return profile.property(requiredProperty)
                          .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_PROPERTY_ERROR_FORMAT,
                                                                                        requiredProperty, profile.name())));
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return this.credentialsProvider.resolveCredentials();
        }

        @Override
        public void close() {
            IoUtils.closeIfCloseable(parentCredentialsProvider, null);
            IoUtils.closeQuietly(credentialsProvider, null);
            IoUtils.closeQuietly(stsClient, null);
        }
    }
}
