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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

public class ProfileFileConfigurationTest {

    private static final String PROFILE_CONTENT = "[profile foo]\n" +
                                                  "region = us-banana-46\n" +
                                                  "aws_access_key_id = profileIsHonoredForCredentials_akid\n" +
                                                  "aws_secret_access_key = profileIsHonoredForCredentials_skid";
    private static final String PROFILE_NAME = "foo";
    private AwsV4HttpSigner signer;

    @BeforeEach
    public void setup() {
        signer = Mockito.mock(AwsV4HttpSigner.class);
    }

    @Test
    public void legacySigner_profileIsHonoredForCredentialsAndRegion() {
        EnvironmentVariableHelper.run(env -> {
            env.remove(SdkSystemSetting.AWS_REGION);
            env.remove(SdkSystemSetting.AWS_ACCESS_KEY_ID);
            env.remove(SdkSystemSetting.AWS_SECRET_ACCESS_KEY);

            Signer signer = mock(Signer.class);

            ProtocolRestJsonClient client =
                ProtocolRestJsonClient.builder()
                                      .overrideConfiguration(overrideConfig(PROFILE_CONTENT, PROFILE_NAME, signer))
                                      .build();

            Mockito.when(signer.sign(any(), any())).thenReturn(signedSdkHttpRequest());

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

    private static ClientOverrideConfiguration overrideConfig(String profileContent, String profileName, Signer signer) {
        return ClientOverrideConfiguration.builder()
                                          .defaultProfileFile(profileFile(profileContent))
                                          .defaultProfileName(profileName)
                                          .retryPolicy(r -> r.numRetries(0))
                                          .putAdvancedOption(SdkAdvancedClientOption.SIGNER, signer)
                                          .build();
    }

    private static ProfileFile profileFile(String content) {
        return ProfileFile.builder()
                          .content(new StringInputStream(content))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    @Test
    public void nonStreaming_syncHttpSigner_profileIsHonoredForCredentialsAndRegion() {
        EnvironmentVariableHelper.run(env -> {
            env.remove(SdkSystemSetting.AWS_REGION);
            env.remove(SdkSystemSetting.AWS_ACCESS_KEY_ID);
            env.remove(SdkSystemSetting.AWS_SECRET_ACCESS_KEY);

            ProtocolRestJsonClient client = clientWithHttpSignerOverride();

            SignedRequest signedRequest = SignedRequest.builder().request(signedSdkHttpRequest()).build();
            Mockito.when(signer.sign(any(SignRequest.class))).thenReturn(signedRequest);

            try {
                client.allTypes();
            } catch (Exception e) {
                // expected
            }

            verifySignerProperty(signer);

        });
    }

    @Test
    public void streaming_syncHttpSigner_profileIsHonoredForCredentialsAndRegion() {
        EnvironmentVariableHelper.run(env -> {
            env.remove(SdkSystemSetting.AWS_REGION);
            env.remove(SdkSystemSetting.AWS_ACCESS_KEY_ID);
            env.remove(SdkSystemSetting.AWS_SECRET_ACCESS_KEY);

            ProtocolRestJsonClient client = clientWithHttpSignerOverride();

            SignedRequest signedRequest = SignedRequest.builder().request(signedSdkHttpRequest()).build();
            Mockito.when(signer.sign(any(SignRequest.class))).thenReturn(signedRequest);

            try {
                client.streamingInputOperation(StreamingInputOperationRequest.builder().build(), RequestBody.fromString(
                    "helloworld"));
            } catch (SdkClientException e) {
                // expected
            }

            verifySignerProperty(signer);
        });
    }

    @Test
    public void nonStreaming_asyncHttpSigner_profileIsHonoredForCredentialsAndRegion() {
        EnvironmentVariableHelper.run(env -> {
            env.remove(SdkSystemSetting.AWS_REGION);
            env.remove(SdkSystemSetting.AWS_ACCESS_KEY_ID);
            env.remove(SdkSystemSetting.AWS_SECRET_ACCESS_KEY);

            ProtocolRestJsonAsyncClient asyncClient = asyncClientWithHttpSignerOverride();

            SignedRequest signedRequest = SignedRequest.builder().request(signedSdkHttpRequest()).build();
            Mockito.when(signer.sign(any(SignRequest.class))).thenReturn(signedRequest);

            try {
                asyncClient.allTypes().join();
            } catch (Exception e) {
                // expected
            }

            verifySignerProperty(signer);

        });
    }

    @Test
    public void streamingOperation_asyncHttpSigner_profileIsHonoredForCredentialsAndRegion() {
        EnvironmentVariableHelper.run(env -> {
            env.remove(SdkSystemSetting.AWS_REGION);
            env.remove(SdkSystemSetting.AWS_ACCESS_KEY_ID);
            env.remove(SdkSystemSetting.AWS_SECRET_ACCESS_KEY);

            Mockito.when(signer.signAsync(any(AsyncSignRequest.class))).thenReturn(CompletableFuture.completedFuture(any(AsyncSignRequest.class)));

            ProtocolRestJsonAsyncClient asyncClient = asyncClientWithHttpSignerOverride();

            try {
                asyncClient.streamingInputOperation(StreamingInputOperationRequest.builder().build(), AsyncRequestBody.fromString(
                    "helloworld")).join();
            } catch (Exception e) {
                // expected
            }

            ArgumentCaptor<AsyncSignRequest> signRequest = ArgumentCaptor.forClass(AsyncSignRequest.class);
            Mockito.verify(signer).signAsync(signRequest.capture());

            AsyncSignRequest actualSignRequest = signRequest.getValue();

            String regionName = (String) actualSignRequest.property(REGION_NAME);
            assertThat(regionName).isEqualTo("us-banana-46");

            assertThat(actualSignRequest.identity()).isInstanceOf(AwsCredentials.class);
            AwsCredentials credentials = (AwsCredentials) actualSignRequest.identity();
            assertThat(credentials.accessKeyId()).isEqualTo("profileIsHonoredForCredentials_akid");
            assertThat(credentials.secretAccessKey()).isEqualTo("profileIsHonoredForCredentials_skid");

        });
    }

    private ProtocolRestJsonAsyncClient asyncClientWithHttpSignerOverride() {
        return ProtocolRestJsonAsyncClient.builder()
                                          .overrideConfiguration(overrideConfig(PROFILE_CONTENT, PROFILE_NAME, null))
                                          .putAuthScheme(new MockAuthScheme(signer)).build();
    }

    private ProtocolRestJsonClient clientWithHttpSignerOverride() {
        return ProtocolRestJsonClient.builder()
                                     .overrideConfiguration(overrideConfig(PROFILE_CONTENT, PROFILE_NAME, null))
                                     .putAuthScheme(new MockAuthScheme(signer)).build();
    }

    private static void verifySignerProperty(AwsV4HttpSigner signer) {
        ArgumentCaptor<SignRequest> signRequest = ArgumentCaptor.forClass(SignRequest.class);
        Mockito.verify(signer).sign(signRequest.capture());

        SignRequest actualSignRequest = signRequest.getValue();

        String regionName = (String) actualSignRequest.property(REGION_NAME);
        assertThat(regionName).isEqualTo("us-banana-46");

        assertThat(actualSignRequest.identity()).isInstanceOf(AwsCredentials.class);
        AwsCredentials credentials = (AwsCredentials) actualSignRequest.identity();
        assertThat(credentials.accessKeyId()).isEqualTo("profileIsHonoredForCredentials_akid");
        assertThat(credentials.secretAccessKey()).isEqualTo("profileIsHonoredForCredentials_skid");
    }

    private static SdkHttpFullRequest signedSdkHttpRequest() {
        return SdkHttpFullRequest.builder()
                                 .protocol("https")
                                 .host("test")
                                 .method(SdkHttpMethod.GET)
                                 .build();
    }

    private static class MockAuthScheme implements AwsV4AuthScheme {
        private final AwsV4HttpSigner signer;

        public MockAuthScheme(AwsV4HttpSigner signer) {
            this.signer = signer;
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return providers.identityProvider(AwsCredentialsIdentity.class);
        }

        @Override
        public AwsV4HttpSigner signer() {
            return signer;
        }

        @Override
        public String schemeId() {
            return SCHEME_ID;
        }
    }

}
