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

package software.amazon.awssdk.services.sts.internal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.nio.file.Paths;
import java.time.Instant;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StsWebIdentityTokenCredentialProviderTest {

    @Mock
    StsClient stsClient;

    @Test
    public void createAssumeRoleWithWebIdentityTokenCredentialsProviderWithoutStsClient() {

        String webIdentityTokenPath = Paths.get("src/test/resources/token.jwt").toAbsolutePath().toString();
        System.setProperty("aws.roleArn", "someRole");
        System.setProperty("aws.webIdentityTokenFile", webIdentityTokenPath);
        System.setProperty("aws.roleSessionName", "tempRoleSession");
        StsWebIdentityTokenFileCredentialsProvider provider =
                StsWebIdentityTokenFileCredentialsProvider.create();
        Assert.assertNotNull(provider);
    }

    @Test
    public void createAssumeRoleWithWebIdentityTokenCredentialsProviderCreateStsClient() {
        String webIdentityTokenPath = Paths.get("src/test/resources/token.jwt").toAbsolutePath().toString();
        System.setProperty("aws.roleArn", "someRole");
        System.setProperty("aws.webIdentityTokenFile", webIdentityTokenPath);
        System.setProperty("aws.roleSessionName", "/src/test/token.jwt");
        StsWebIdentityTokenFileCredentialsProvider provider =
                StsWebIdentityTokenFileCredentialsProvider.create(stsClient);
        when(stsClient.assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class)))
                .thenReturn(AssumeRoleWithWebIdentityResponse.builder()
                        .credentials(Credentials.builder().accessKeyId("key")
                                .expiration(Instant.now())
                                .sessionToken("session").secretAccessKey("secret").build()).build());
        provider.resolveCredentials();
        Mockito.verify(stsClient, Mockito.times(1)).assumeRoleWithWebIdentity(Mockito.any(AssumeRoleWithWebIdentityRequest.class));
    }

    @Test
    public void createAssumeRoleWithWebIdentityTokenCredentialsProviderStsClientBuilder() {

        String webIdentityTokenPath = Paths.get("src/test/resources/token.jwt").toAbsolutePath().toString();
        StsWebIdentityTokenFileCredentialsProvider provider =
                StsWebIdentityTokenFileCredentialsProvider.builder().stsClient(stsClient)
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
}
