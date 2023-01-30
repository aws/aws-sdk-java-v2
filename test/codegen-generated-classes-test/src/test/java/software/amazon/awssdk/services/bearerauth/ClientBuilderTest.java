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

package software.amazon.awssdk.services.bearerauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.regions.Region;

public class ClientBuilderTest {
    @Test
    public void syncClient_includesDefaultProvider_includesDefaultSigner() {
        DefaultBearerauthClientBuilder builder = new DefaultBearerauthClientBuilder();
        SdkClientConfiguration config = getSyncConfig(builder);

        assertThat(config.option(AwsClientOption.TOKEN_PROVIDER))
            .isInstanceOf(DefaultAwsTokenProvider.class);
        assertThat(config.option(SdkAdvancedClientOption.TOKEN_SIGNER))
            .isInstanceOf(BearerTokenSigner.class);
    }

    @Test
    public void syncClient_customTokenProviderSet_presentInFinalConfig() {
        DefaultBearerauthClientBuilder builder = new DefaultBearerauthClientBuilder();

        SdkTokenProvider mockProvider = mock(SdkTokenProvider.class);
        builder.tokenProvider(mockProvider);
        SdkClientConfiguration config = getSyncConfig(builder);

        assertThat(config.option(AwsClientOption.TOKEN_PROVIDER))
            .isSameAs(mockProvider);
    }


    @Test
    public void syncClient_customSignerSet_presentInFinalConfig() {
        DefaultBearerauthClientBuilder builder = new DefaultBearerauthClientBuilder();

        Signer mockSigner = mock(Signer.class);
        builder.overrideConfiguration(o -> o.putAdvancedOption(SdkAdvancedClientOption.TOKEN_SIGNER, mockSigner));

        SdkClientConfiguration config = getSyncConfig(builder);

        assertThat(config.option(SdkAdvancedClientOption.TOKEN_SIGNER))
            .isSameAs(mockSigner);
    }

    @Test
    public void asyncClient_includesDefaultProvider_includesDefaultSigner() {
        DefaultBearerauthAsyncClientBuilder builder = new DefaultBearerauthAsyncClientBuilder();
        SdkClientConfiguration config = getAsyncConfig(builder);

        assertThat(config.option(AwsClientOption.TOKEN_PROVIDER))
            .isInstanceOf(DefaultAwsTokenProvider.class);
        assertThat(config.option(SdkAdvancedClientOption.TOKEN_SIGNER))
            .isInstanceOf(BearerTokenSigner.class);
    }

    @Test
    public void asyncClient_customTokenProviderSet_presentInFinalConfig() {
        DefaultBearerauthAsyncClientBuilder builder = new DefaultBearerauthAsyncClientBuilder();

        SdkTokenProvider mockProvider = mock(SdkTokenProvider.class);
        builder.tokenProvider(mockProvider);
        SdkClientConfiguration config = getAsyncConfig(builder);

        assertThat(config.option(AwsClientOption.TOKEN_PROVIDER))
            .isSameAs(mockProvider);
    }

    @Test
    public void asyncClient_customSignerSet_presentInFinalConfig() {
        DefaultBearerauthAsyncClientBuilder builder = new DefaultBearerauthAsyncClientBuilder();

        Signer mockSigner = mock(Signer.class);
        builder.overrideConfiguration(o -> o.putAdvancedOption(SdkAdvancedClientOption.TOKEN_SIGNER, mockSigner));

        SdkClientConfiguration config = getAsyncConfig(builder);

        assertThat(config.option(SdkAdvancedClientOption.TOKEN_SIGNER))
            .isSameAs(mockSigner);
    }

    @Test
    public void syncClient_buildWithDefaults_validationsSucceed() {
        DefaultBearerauthClientBuilder builder = new DefaultBearerauthClientBuilder();
        builder.region(Region.US_WEST_2).credentialsProvider(AnonymousCredentialsProvider.create());
        assertThatNoException().isThrownBy(builder::build);
    }

    @Test
    public void asyncClient_buildWithDefaults_validationsSucceed() {
        DefaultBearerauthAsyncClientBuilder builder = new DefaultBearerauthAsyncClientBuilder();
        builder.region(Region.US_WEST_2).credentialsProvider(AnonymousCredentialsProvider.create());
        assertThatNoException().isThrownBy(builder::build);
    }

    // syncClientConfiguration() is a protected method, and the concrete builder classes are final
    private static SdkClientConfiguration getSyncConfig(DefaultBearerauthClientBuilder builder) {
        try {
            Method syncClientConfiguration = SdkDefaultClientBuilder.class.getDeclaredMethod("syncClientConfiguration");
            syncClientConfiguration.setAccessible(true);
            return (SdkClientConfiguration) syncClientConfiguration.invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // syncClientConfiguration() is a protected method, and the concrete builder classes are final
    private static SdkClientConfiguration getAsyncConfig(DefaultBearerauthAsyncClientBuilder builder) {
        try {
            Method syncClientConfiguration = SdkDefaultClientBuilder.class.getDeclaredMethod("asyncClientConfiguration");
            syncClientConfiguration.setAccessible(true);
            return (SdkClientConfiguration) syncClientConfiguration.invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
