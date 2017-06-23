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

package software.amazon.awssdk.auth.profile.internal.securitytoken;

import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;

/**
 * Loads <code>software.amazon.awssdk.services.securitytoken.internal.STSProfileCredentialsService</code>
 * from the STS SDK module, if the module is on the current classpath.
 */
@SdkInternalApi
public class StsProfileCredentialsServiceLoader implements ProfileCredentialsService {
    private static final StsProfileCredentialsServiceLoader INSTANCE = new StsProfileCredentialsServiceLoader();

    private StsProfileCredentialsServiceLoader() {
    }

    public static StsProfileCredentialsServiceLoader getInstance() {
        return INSTANCE;
    }

    @Override
    public AwsCredentialsProvider getAssumeRoleCredentialsProvider(RoleInfo targetRoleInfo) {
        return new StsProfileCredentialsServiceProvider(targetRoleInfo);
    }
}
