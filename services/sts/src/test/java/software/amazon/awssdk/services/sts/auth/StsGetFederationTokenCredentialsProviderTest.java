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
import software.amazon.awssdk.services.sts.auth.StsGetFederationTokenCredentialsProvider.Builder;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetFederationTokenRequest;
import software.amazon.awssdk.services.sts.model.GetFederationTokenResponse;

/**
 * Validate the functionality of {@link StsGetFederationTokenCredentialsProvider}.
 * Inherits tests from {@link StsCredentialsProviderTestBase}.
 */
public class StsGetFederationTokenCredentialsProviderTest
        extends StsCredentialsProviderTestBase<GetFederationTokenRequest, GetFederationTokenResponse> {
    @Override
    protected GetFederationTokenRequest getRequest() {
        return GetFederationTokenRequest.builder().build();
    }

    @Override
    protected GetFederationTokenResponse getResponse(Credentials credentials) {
        return GetFederationTokenResponse.builder().credentials(credentials).build();
    }

    @Override
    protected Builder createCredentialsProviderBuilder(GetFederationTokenRequest request) {
        return StsGetFederationTokenCredentialsProvider.builder().refreshRequest(request);
    }

    @Override
    protected GetFederationTokenResponse callClient(STSClient client, GetFederationTokenRequest request) {
        return client.getFederationToken(request);
    }
}
