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

package software.amazon.awssdk.services.sts.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider.Builder;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Validate the functionality of {@link StsWebIdentityTokenFileCredentialsProvider}. Inherits tests from {@link
 * StsCredentialsProviderTestBase}.
 */
public class StsWebIdentityTokenCredentialsProviderBaseTest
    extends StsCredentialsProviderTestBase<AssumeRoleWithWebIdentityRequest, AssumeRoleWithWebIdentityResponse> {

    private EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();


    @BeforeEach
    public   void setUp() {
        String webIdentityTokenPath = Paths.get("src/test/resources/token.jwt").toAbsolutePath().toString();
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_ROLE_ARN.environmentVariable(), "someRole");
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE.environmentVariable(), webIdentityTokenPath);
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_ROLE_SESSION_NAME.environmentVariable(), "tempRoleSession");
    }

    @AfterEach
    public void cleanUp(){
        ENVIRONMENT_VARIABLE_HELPER.reset();
    }

    @Override
    protected AssumeRoleWithWebIdentityRequest getRequest() {
        return AssumeRoleWithWebIdentityRequest.builder().webIdentityToken(getToken(Paths.get("src/test/resources"
                                                                                              + "/token.jwt").toAbsolutePath())).build();
    }

    @Override
    protected AssumeRoleWithWebIdentityResponse getResponse(Credentials credentials) {
        return AssumeRoleWithWebIdentityResponse.builder().credentials(credentials).build();
    }

    @Override
    protected Builder createCredentialsProviderBuilder(AssumeRoleWithWebIdentityRequest request) {
        return StsWebIdentityTokenFileCredentialsProvider.builder().stsClient(stsClient).refreshRequest(request);
    }

    @Override
    protected AssumeRoleWithWebIdentityResponse callClient(StsClient client, AssumeRoleWithWebIdentityRequest request) {
        return client.assumeRoleWithWebIdentity(request);
    }

    private String getToken(Path file) {
        try (InputStream webIdentityTokenStream = Files.newInputStream(file)) {
            return IoUtils.toUtf8String(webIdentityTokenStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
