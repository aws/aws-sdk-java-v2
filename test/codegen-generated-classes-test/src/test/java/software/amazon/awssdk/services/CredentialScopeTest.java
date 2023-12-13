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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClientBuilder;
import software.amazon.awssdk.services.protocolquery.model.OperationWithNoInputOrOutputRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.StringInputStream;

@WireMockTest
public class CredentialScopeTest {
    private ProtocolQueryClientBuilder clientBuilder;
    private CapturingInterceptor capturingInterceptor;

    @BeforeEach
    public void setUp(WireMockRuntimeInfo wiremock) {
        capturingInterceptor = new CapturingInterceptor();
        clientBuilder = ProtocolQueryClient.builder()
                                    .overrideConfiguration(o -> o.addExecutionInterceptor(capturingInterceptor))
                                    .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()));
    }

    @AfterEach
    public void reset() {
        clearSystemProperties();
    }

    /**
     * Returns all possible Boolean combinations
     */
    private static Stream<Arguments> testParams() {
        return IntStream.range(0, 1 << 4)
                        .mapToObj(i -> Arguments.of((i & 1) == 1, (i & 2) == 2, (i & 4) == 4, (i & 8) == 8));
    }

    @ParameterizedTest
    @MethodSource("testParams")
    public void testCredentialScopeConfigPrecedence(boolean requestLevel, boolean clientLevel, boolean envVarLevel, boolean profileLevel) {

        if (clientLevel) {
            clientBuilder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.builder()
                                                                                                  .accessKeyId("akid")
                                                                                                  .secretAccessKey("skid")
                                                                                                  .credentialScope("us-east-2")
                                                                                                  .build()));
        }
        if (envVarLevel) {
            System.setProperty("aws.accessKeyId", "akid");
            System.setProperty("aws.secretAccessKey", "skid");
            System.setProperty("aws.credentialScope", "us-west-1");
        }
        if (!clientLevel && profileLevel) {
            clientBuilder.credentialsProvider(DefaultCredentialsProvider
                                                  .builder()
                                                  .profileFile(credentialFile("test", "access", "secret","us-west-2"))
                                                  .profileName("test")
                                                  .build());
        }

        ProtocolQueryClient client = clientBuilder.build();
        OperationWithNoInputOrOutputRequest.Builder requestBuilder = OperationWithNoInputOrOutputRequest.builder();
        if (requestLevel) {
            requestBuilder.overrideConfiguration(o -> o.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.builder()
                                                                                                                                .accessKeyId("akid")
                                                                                                                                .secretAccessKey("skid")
                                                                                                                                .credentialScope("us-east-1")
                                                                                                                                .build())));
        }

        OperationWithNoInputOrOutputRequest request = requestBuilder.build();
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
        client.operationWithNoInputOrOutput(request);

        Region credentialScope = capturingInterceptor.credentialScope;
        if (requestLevel) {
            assertThat(credentialScope).isEqualTo(Region.US_EAST_1);
        } else if (clientLevel) {
            assertThat(credentialScope).isEqualTo(Region.US_EAST_2);
        } else if (envVarLevel) {
            assertThat(credentialScope).isEqualTo(Region.US_WEST_1);
        } else if (profileLevel) {
            assertThat(credentialScope).isEqualTo(Region.US_WEST_2);
        } else {
            assertThat(credentialScope).isNull();
        }
    }

    private void clearSystemProperties() {
        System.clearProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property());
        System.clearProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property());
        System.clearProperty(SdkSystemSetting.AWS_CREDENTIAL_SCOPE.property());
    }

    private ProfileFile credentialFile(String name, String accessKeyId, String secretAccessKey, String credentialScope) {
        String contents = String.format("[%s]\naws_access_key_id = %s\naws_secret_access_key = %s\naws_credential_scope = %s",
                                        name, accessKeyId, secretAccessKey, credentialScope);
        return credentialFile(contents);
    }

    private ProfileFile credentialFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CREDENTIALS)
                          .build();
    }

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private Region credentialScope;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SelectedAuthScheme<?> authScheme = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            Identity identity = CompletableFutureUtils.joinLikeSync(authScheme.identity());
            if (identity instanceof AwsCredentialsIdentity) {
                AwsCredentialsIdentity awsCredentialsIdentity = (AwsCredentialsIdentity) identity;
                credentialScope = awsCredentialsIdentity.credentialScope().map(Region::of).orElse(null);
            }
        }
    }
}
