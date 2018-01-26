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

import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithSamlCredentialsProvider.Builder;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithSAMLRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithSAMLResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * Validate the functionality of {@link StsAssumeRoleWithSamlCredentialsProvider}.
 * Inherits tests from {@link StsCredentialsProviderTestBase}.
 */
public class StsAssumeRoleWithSamlCredentialsProviderTest
        extends StsCredentialsProviderTestBase<AssumeRoleWithSAMLRequest, AssumeRoleWithSAMLResponse> {
    @Override
    protected AssumeRoleWithSAMLRequest getRequest() {
        return AssumeRoleWithSAMLRequest.builder().build();
    }

    @Override
    protected AssumeRoleWithSAMLResponse getResponse(Credentials credentials) {
        return AssumeRoleWithSAMLResponse.builder().credentials(credentials).build();
    }

    @Override
    protected Builder createCredentialsProviderBuilder(AssumeRoleWithSAMLRequest request) {
        return StsAssumeRoleWithSamlCredentialsProvider.builder().refreshRequest(request);
    }

    @Override
    protected AssumeRoleWithSAMLResponse callClient(STSClient client, AssumeRoleWithSAMLRequest request) {
        return client.assumeRoleWithSAML(request);
    }
}
