/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.ssooidc.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.token.AwsToken;
import software.amazon.awssdk.auth.token.AwsTokenProvider;
import software.amazon.awssdk.auth.token.SsoOidcTokenProviderFactoryProperties;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssooidc.SsoOidcClient;
import software.amazon.awssdk.services.ssooidc.SsoOidcTokenProvider;
import software.amazon.awssdk.services.ssooidc.SsoOidcTokenProviderFactory;
import software.amazon.awssdk.services.ssooidc.internal.common.SsoOidcTokenRefreshTestBase;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenRequest;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenResponse;
import software.amazon.awssdk.services.ssooidc.model.InvalidRequestException;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;
import software.amazon.awssdk.utils.StringInputStream;

public final class SsoOidcTokenRefreshTest extends SsoOidcTokenRefreshTestBase {


    @Override
    public void initializeClient() {
        shouldMockServiceClient = true;
        baseTokenResourceFile = "src/test/resources/baseCreateToken.json";
        testStartUrl = "http://caws-testing.com/start-url";
        ssoOidcClient = mockSsoOidcClient();
    }

}
