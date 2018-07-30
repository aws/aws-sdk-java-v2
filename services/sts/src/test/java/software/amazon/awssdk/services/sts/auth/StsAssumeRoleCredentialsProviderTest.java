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

package software.amazon.awssdk.services.sts.auth;

import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * Validate the functionality of {@link StsAssumeRoleCredentialsProvider}.
 * Inherits tests from {@link StsCredentialsProviderTestBase}.
 */
public class StsAssumeRoleCredentialsProviderTest extends StsCredentialsProviderTestBase<AssumeRoleRequest, AssumeRoleResponse> {
    @Override
    protected AssumeRoleRequest getRequest() {
        return AssumeRoleRequest.builder().build();
    }

    @Override
    protected AssumeRoleResponse getResponse(Credentials credentials) {
        return AssumeRoleResponse.builder().credentials(credentials).build();
    }

    @Override
    protected StsAssumeRoleCredentialsProvider.Builder createCredentialsProviderBuilder(AssumeRoleRequest request) {
        return StsAssumeRoleCredentialsProvider.builder().refreshRequest(request);
    }

    @Override
    protected AssumeRoleResponse callClient(StsClient client, AssumeRoleRequest request) {
        return client.assumeRole(request);
    }
}
