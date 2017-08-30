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

package software.amazon.awssdk.services.sts.internal;

import java.util.Map;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.profile.internal.ChildProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.profile.internal.ProfileProperties;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

public class StsProfileCredentialsProviderFactory implements ChildProfileCredentialsProviderFactory {
    private static final String MISSING_PROPERTY_ERROR_FORMAT = "'%s' must be set to use role-based credential loading.";

    @Override
    public AwsCredentialsProvider create(AwsCredentialsProvider parentCredentialsProvider,
                                         Map<String, String> profileProperties) {
        return new StsProfileCredentialsProvider(parentCredentialsProvider, profileProperties);
    }

    private static class StsProfileCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
        private final STSClient stsClient;
        private final StsAssumeRoleCredentialsProvider credentialsProvider;

        private StsProfileCredentialsProvider(AwsCredentialsProvider parentCredentialsProvider,
                                              Map<String, String> profileProperties) {
            requireProperty(profileProperties, ProfileProperties.ROLE_ARN);
            requireProperty(profileProperties, ProfileProperties.ROLE_SESSION_NAME);
            requireProperty(profileProperties, ProfileProperties.EXTERNAL_ID);

            this.stsClient = STSClient.builder().credentialsProvider(parentCredentialsProvider).build();

            AssumeRoleRequest assumeRoleRequest =
                    AssumeRoleRequest.builder()
                                     .roleArn(profileProperties.get(ProfileProperties.ROLE_ARN))
                                     .roleSessionName(profileProperties.get(ProfileProperties.ROLE_SESSION_NAME))
                                     .externalId(profileProperties.get(ProfileProperties.EXTERNAL_ID))
                                     .build();

            this.credentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                                                                       .stsClient(stsClient)
                                                                       .refreshRequest(assumeRoleRequest)
                                                                       .build();
        }

        private void requireProperty(Map<String, String> profileProperties, String requiredProperty) {
            Validate.isTrue(profileProperties.containsKey(requiredProperty), MISSING_PROPERTY_ERROR_FORMAT, requiredProperty);
        }

        @Override
        public AwsCredentials getCredentials() {
            return this.credentialsProvider.getCredentials();
        }

        @Override
        public void close() {
            IoUtils.closeQuietly(credentialsProvider, null);
            IoUtils.closeQuietly(stsClient, null);
        }
    }
}
