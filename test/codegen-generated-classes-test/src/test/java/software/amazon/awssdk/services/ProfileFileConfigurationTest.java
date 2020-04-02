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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

public class ProfileFileConfigurationTest {
    @Test
    public void profileIsHonoredForCredentialsAndRegion() {
        EnvironmentVariableHelper.run(env -> {
            env.remove(SdkSystemSetting.AWS_REGION);
            env.remove(SdkSystemSetting.AWS_ACCESS_KEY_ID);
            env.remove(SdkSystemSetting.AWS_SECRET_ACCESS_KEY);

            String profileContent = "[profile foo]\n" +
                                    "region = us-banana-46\n" +
                                    "aws_access_key_id = profileIsHonoredForCredentials_akid\n" +
                                    "aws_secret_access_key = profileIsHonoredForCredentials_skid";
            String profileName = "foo";
            Signer signer = mock(NoOpSigner.class);

            ProtocolRestJsonClient client =
                ProtocolRestJsonClient.builder()
                                      .overrideConfiguration(overrideConfig(profileContent, profileName, signer))
                                      .build();

            Mockito.when(signer.sign(any(), any())).thenCallRealMethod();

            try {
                client.allTypes();
            } catch (SdkClientException e) {
                // expected
            }

            ArgumentCaptor<SdkHttpFullRequest> httpRequest = ArgumentCaptor.forClass(SdkHttpFullRequest.class);
            ArgumentCaptor<ExecutionAttributes> attributes = ArgumentCaptor.forClass(ExecutionAttributes.class);
            Mockito.verify(signer).sign(httpRequest.capture(), attributes.capture());

            AwsCredentials credentials = attributes.getValue().getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS);
            assertThat(credentials.accessKeyId()).isEqualTo("profileIsHonoredForCredentials_akid");
            assertThat(credentials.secretAccessKey()).isEqualTo("profileIsHonoredForCredentials_skid");

            Region region = attributes.getValue().getAttribute(AwsExecutionAttribute.AWS_REGION);
            assertThat(region.id()).isEqualTo("us-banana-46");

            assertThat(httpRequest.getValue().getUri().getHost()).contains("us-banana-46");
        });
    }

    private ClientOverrideConfiguration overrideConfig(String profileContent, String profileName, Signer signer) {
        return ClientOverrideConfiguration.builder()
                                          .defaultProfileFile(profileFile(profileContent))
                                          .defaultProfileName(profileName)
                                          .retryPolicy(r -> r.numRetries(0))
                                          .putAdvancedOption(SdkAdvancedClientOption.SIGNER, signer)
                                          .build();
    }

    private ProfileFile profileFile(String content) {
        return ProfileFile.builder()
                          .content(new StringInputStream(content))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }
}
