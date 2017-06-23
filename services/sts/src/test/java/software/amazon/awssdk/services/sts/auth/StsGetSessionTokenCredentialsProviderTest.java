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
import software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider.Builder;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;

/**
 * Validate the functionality of {@link StsGetSessionTokenCredentialsProvider}.
 * Inherits tests from {@link StsCredentialsProviderTestBase}.
 */
public class StsGetSessionTokenCredentialsProviderTest
        extends StsCredentialsProviderTestBase<GetSessionTokenRequest, GetSessionTokenResponse> {
    @Override
    protected GetSessionTokenRequest getRequest() {
        return GetSessionTokenRequest.builder().build();
    }

    @Override
    protected GetSessionTokenResponse getResponse(Credentials credentials) {
        return GetSessionTokenResponse.builder().credentials(credentials).build();
    }

    @Override
    protected Builder createCredentialsProviderBuilder(GetSessionTokenRequest request) {
        return StsGetSessionTokenCredentialsProvider.builder().refreshRequest(request);
    }

    @Override
    protected GetSessionTokenResponse callClient(STSClient client, GetSessionTokenRequest request) {
        return client.getSessionToken(request);
    }
}
