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
import software.amazon.awssdk.services.sts.model.AssumedRoleUser;
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
    void customPrefetchTime_actuallyTriggersRefreshEarly() throws InterruptedException {
        Mockito.reset(stsClient);

        Instant tokenExpiration = Instant.now().plusSeconds(8);
        Duration customPrefetchTime = Duration.ofSeconds(2);
        Duration customStaleTime = Duration.ofSeconds(1);

        when(stsClient.assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class)))
            .thenReturn(AssumeRoleWithWebIdentityResponse.builder()
                                                         .credentials(Credentials.builder()
                                                                                 .accessKeyId("key1")
                                                                                 .secretAccessKey("secret1") 
                                                                                 .sessionToken("session1")
                                                                                 .expiration(tokenExpiration)
                                                                                 .build())
                                                         .assumedRoleUser(AssumedRoleUser.builder()
                                                                                        .arn("arn:aws:iam::123456789012:role/test-role")
                                                                                        .assumedRoleId("role:session")
                                                                                        .build())
                                                         .build())

            .thenReturn(AssumeRoleWithWebIdentityResponse.builder()
                                                         .credentials(Credentials.builder()
                                                                                 .accessKeyId("key2")
                                                                                 .secretAccessKey("secret2")
                                                                                 .sessionToken("session2")
                                                                                 .expiration(Instant.now().plusSeconds(8))
                                                                                 .build())
                                                         .assumedRoleUser(AssumedRoleUser.builder()
                                                                                        .arn("arn:aws:iam::123456789012:role/test-role")
                                                                                        .assumedRoleId("role:session")
                                                                                        .build())
                                                         .build());


        StsWebIdentityTokenFileCredentialsProvider provider =
            StsWebIdentityTokenFileCredentialsProvider.builder()
                                                      .stsClient(stsClient)
                                                      .asyncCredentialUpdateEnabled(true)
                                                      .prefetchTime(customPrefetchTime)
                                                      .staleTime(customStaleTime)
                                                      .build();

        try {
            assertThat(provider.prefetchTime()).isEqualTo(customPrefetchTime);
            assertThat(provider.staleTime()).isEqualTo(customStaleTime);

            provider.resolveCredentials();
            Mockito.verify(stsClient, Mockito.times(1)).assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class));

            // Wait 7 seconds to ensure prefetch completes
            Thread.sleep(7_000);
            Mockito.verify(stsClient, Mockito.times(2)).assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class));

        } finally {
            provider.close();
        }
    }

    @Test
    void defaultTiming_usesStandardValues() {
        StsWebIdentityTokenFileCredentialsProvider provider =
            StsWebIdentityTokenFileCredentialsProvider.builder()
                                                      .stsClient(stsClient)
                                                      .build();

        try {
            assertThat(provider.prefetchTime()).isEqualTo(Duration.ofMinutes(5));
            assertThat(provider.staleTime()).isEqualTo(Duration.ofMinutes(1));
        } finally {
            provider.close();
        }
    }

    @Test
    void toBuilder_preservesCustomTimingConfiguration() {
        Duration customPrefetch = Duration.ofMinutes(10);
        Duration customStale = Duration.ofMinutes(3);

        StsWebIdentityTokenFileCredentialsProvider originalProvider =
            StsWebIdentityTokenFileCredentialsProvider.builder()
                                                      .stsClient(stsClient)
                                                      .prefetchTime(customPrefetch)
                                                      .staleTime(customStale)
                                                      .build();

        try {
            assertThat(originalProvider.prefetchTime()).isEqualTo(customPrefetch);
            assertThat(originalProvider.staleTime()).isEqualTo(customStale);

            StsWebIdentityTokenFileCredentialsProvider copiedProvider = originalProvider.toBuilder().build();

            try {
                assertThat(copiedProvider.prefetchTime()).isEqualTo(customPrefetch);
                assertThat(copiedProvider.staleTime()).isEqualTo(customStale);
            } finally {
                copiedProvider.close();
            }
        } finally {
            originalProvider.close();
        }
    }
}