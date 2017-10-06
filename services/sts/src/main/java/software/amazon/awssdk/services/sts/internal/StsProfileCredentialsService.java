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

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.auth.profile.internal.securitytoken.ProfileCredentialsService;
import software.amazon.awssdk.core.auth.profile.internal.securitytoken.RoleInfo;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

/**
 * Loaded via reflection by the core module when role assumption is configured in a
 * credentials profile.
 */
public class StsProfileCredentialsService implements ProfileCredentialsService {
    @ReviewBeforeRelease("How should the STS client be cleaned up?")
    @Override
    public AwsCredentialsProvider getAssumeRoleCredentialsProvider(RoleInfo targetRoleInfo) {
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn(targetRoleInfo.getRoleArn())
                .roleSessionName(targetRoleInfo.getRoleSessionName())
                .externalId(targetRoleInfo.getExternalId())
                .build();
        STSClient stsClient = STSClient.builder().credentialsProvider(targetRoleInfo.getLongLivedCredentialsProvider()).build();

        return StsAssumeRoleCredentialsProvider.builder()
                                               .refreshRequest(assumeRoleRequest)
                                               .stsClient(stsClient)
                                               .build();
    }
}
