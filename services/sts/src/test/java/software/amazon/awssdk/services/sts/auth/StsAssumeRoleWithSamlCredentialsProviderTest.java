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
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithSamlCredentialsProvider.Builder;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithSamlRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithSamlResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * Validate the functionality of {@link StsAssumeRoleWithSamlCredentialsProvider}.
 * Inherits tests from {@link StsCredentialsProviderTestBase}.
 */
public class StsAssumeRoleWithSamlCredentialsProviderTest
        extends StsCredentialsProviderTestBase<AssumeRoleWithSamlRequest, AssumeRoleWithSamlResponse> {
    @Override
    protected AssumeRoleWithSamlRequest getRequest() {
        return AssumeRoleWithSamlRequest.builder().build();
    }

    @Override
    protected AssumeRoleWithSamlResponse getResponse(Credentials credentials) {
        return AssumeRoleWithSamlResponse.builder().credentials(credentials).build();
    }

    @Override
    protected Builder createCredentialsProviderBuilder(AssumeRoleWithSamlRequest request) {
        return StsAssumeRoleWithSamlCredentialsProvider.builder().refreshRequest(request);
    }

    @Override
    protected AssumeRoleWithSamlResponse callClient(StsClient client, AssumeRoleWithSamlRequest request) {
        return client.assumeRoleWithSAML(request);
    }
}
