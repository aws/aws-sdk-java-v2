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

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StsWebIdentityTokenCredentialProviderTest {

    @Mock
    StsClient stsClient;

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

    private void mockStsClientResponse(Instant expiration) {
        when(stsClient.assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class)))
            .thenReturn(AssumeRoleWithWebIdentityResponse.builder()
                                                         .credentials(Credentials.builder()
                                                                                 .accessKeyId("key")
                                                                                 .expiration(expiration)
                                                                                 .sessionToken("session")
                                                                                 .secretAccessKey("secret")
                                                                                 .build())
                                                         .build());
    }

    @Test
    void createAssumeRoleWithWebIdentityTokenCredentialsProviderWithoutStsClient_throws_Exception() {

        Assert.assertThrows(NullPointerException.class,
                            () -> StsWebIdentityTokenFileCredentialsProvider.builder().refreshRequest(r -> r.build()).build());
    }

    @Test
    void createAssumeRoleWithWebIdentityTokenCredentialsProviderCreateStsClient() {
        StsWebIdentityTokenFileCredentialsProvider provider =
            StsWebIdentityTokenFileCredentialsProvider.builder().stsClient(stsClient).refreshRequest(r -> r.build())
                                                      .build();
        when(stsClient.assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class)))
            .thenReturn(AssumeRoleWithWebIdentityResponse.builder()
                                                         .credentials(Credentials.builder().accessKeyId("key")
                                                                                 .expiration(Instant.now())
                                                                                 .sessionToken("session").secretAccessKey("secret").build()).build());
        provider.resolveCredentials();
        Mockito.verify(stsClient, Mockito.times(1)).assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class));
    }

    @Test
    void createAssumeRoleWithWebIdentityTokenCredentialsProviderStsClientBuilder() {

        String webIdentityTokenPath = Paths.get("src/test/resources/token.jwt").toAbsolutePath().toString();
        StsWebIdentityTokenFileCredentialsProvider provider =
            StsWebIdentityTokenFileCredentialsProvider.builder().stsClient(stsClient)
                .refreshRequest(r -> r.build())
                                                      .roleArn("someRole")
                                                      .webIdentityTokenFile(Paths.get(webIdentityTokenPath))
                                                      .roleSessionName("tempRoleSession")
                                                      .build();
        when(stsClient.assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class)))
            .thenReturn(AssumeRoleWithWebIdentityResponse.builder()
                                                         .credentials(Credentials.builder().accessKeyId("key")
                                                                                 .expiration(Instant.now())
                                                                                 .sessionToken("session").secretAccessKey("secret").build()).build());
        provider.resolveCredentials();
        Mockito.verify(stsClient, Mockito.times(1)).assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class));
    }

    @Test
    void createAssumeRoleWithWebIdentityTokenCredentialsProvider_raisesInResolveCredentials() {
        ENVIRONMENT_VARIABLE_HELPER.remove(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE.environmentVariable());

        StsWebIdentityTokenFileCredentialsProvider provider =
            StsWebIdentityTokenFileCredentialsProvider.builder().stsClient(stsClient)
                                                      .refreshRequest(r -> r.build())
                                                      .roleArn("someRole")
                                                      .roleSessionName("tempRoleSession")
                                                      .build();
        // exception should be raised lazily when resolving credentials, not at creation time.
        Assert.assertThrows(IllegalStateException.class, provider::resolveCredentials);
    }

    @Test
    void prefetchTimeAndStaleTime_withCustomConfiguration_shouldReturnConfiguredValues() {
        mockStsClientResponse(Instant.now().plusSeconds(3600));

        StsWebIdentityTokenFileCredentialsProvider provider =
            StsWebIdentityTokenFileCredentialsProvider.builder()
                                                      .stsClient(stsClient)
                                                      .prefetchTime(Duration.ofMinutes(10))
                                                      .staleTime(Duration.ofMinutes(2))
                                                      .build();

        provider.resolveCredentials();
        
        assertThat(provider.prefetchTime()).isEqualTo(Duration.ofMinutes(10));
        assertThat(provider.staleTime()).isEqualTo(Duration.ofMinutes(2));

    }

    @Test
    void prefetchTimeAndStaleTime_withoutConfiguration_shouldReturnDefaultValues() {
        mockStsClientResponse(Instant.now().plusSeconds(3600));

        StsWebIdentityTokenFileCredentialsProvider provider =
            StsWebIdentityTokenFileCredentialsProvider.builder()
                                                      .stsClient(stsClient)
                                                      .build();


        provider.resolveCredentials();
        
        assertThat(provider.prefetchTime()).isEqualTo(Duration.ofMinutes(5));
        assertThat(provider.staleTime()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void toBuilder_withTimingConfiguration_shouldPreserveConfiguration() {
        mockStsClientResponse(Instant.now().plusSeconds(3600));
        StsWebIdentityTokenFileCredentialsProvider originalProvider =
            StsWebIdentityTokenFileCredentialsProvider.builder()
                                                      .stsClient(stsClient)
                                                      .prefetchTime(Duration.ofMinutes(8))
                                                      .staleTime(Duration.ofMinutes(3))
                                                      .build();


        StsWebIdentityTokenFileCredentialsProvider copiedProvider = originalProvider.toBuilder().build();

        copiedProvider.resolveCredentials();

        assertThat(copiedProvider.prefetchTime()).isEqualTo(Duration.ofMinutes(8));
        assertThat(copiedProvider.staleTime()).isEqualTo(Duration.ofMinutes(3));
    }
}