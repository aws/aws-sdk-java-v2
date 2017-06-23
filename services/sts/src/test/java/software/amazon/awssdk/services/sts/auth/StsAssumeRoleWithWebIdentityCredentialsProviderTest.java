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

package software.amazon.awssdk.services.sts.auth;

import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider.Builder;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * Validate the functionality of {@link StsAssumeRoleWithWebIdentityCredentialsProvider}.
 * Inherits tests from {@link StsCredentialsProviderTestBase}.
 */
public class StsAssumeRoleWithWebIdentityCredentialsProviderTest
        extends StsCredentialsProviderTestBase<AssumeRoleWithWebIdentityRequest, AssumeRoleWithWebIdentityResponse> {
    @Override
    protected AssumeRoleWithWebIdentityRequest getRequest() {
        return AssumeRoleWithWebIdentityRequest.builder().build();
    }

    @Override
    protected AssumeRoleWithWebIdentityResponse getResponse(Credentials credentials) {
        return AssumeRoleWithWebIdentityResponse.builder().credentials(credentials).build();
    }

    @Override
    protected Builder createCredentialsProviderBuilder(AssumeRoleWithWebIdentityRequest request) {
        return StsAssumeRoleWithWebIdentityCredentialsProvider.builder().refreshRequest(request);
    }

    @Override
    protected AssumeRoleWithWebIdentityResponse callClient(STSClient client, AssumeRoleWithWebIdentityRequest request) {
        return client.assumeRoleWithWebIdentity(request);
    }
}
