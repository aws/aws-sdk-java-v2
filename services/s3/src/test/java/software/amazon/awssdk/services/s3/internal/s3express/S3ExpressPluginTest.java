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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.auth.scheme.internal.DefaultS3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.auth.scheme.internal.ModeledS3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.internal.S3ServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;

class S3ExpressPluginTest {
    private static final S3ExpressPlugin S3_EXPRESS_PLUGIN = new S3ExpressPlugin();

    @Test
    void s3Config_withNullAuthScheme_addsS3ExpressAuthScheme() {
        S3ServiceClientConfiguration.Builder s3Config = new S3ServiceClientConfigurationBuilder();
        assertThat(s3Config.authSchemes()).isEmpty();

        S3_EXPRESS_PLUGIN.configureClient(s3Config);
        assertThat(s3Config.authSchemes().get(S3ExpressAuthScheme.SCHEME_ID)).isNotNull();
        assertThat(s3Config.authSchemes().get(S3ExpressAuthScheme.SCHEME_ID).signer()).isInstanceOf(DefaultS3ExpressHttpSigner.class);
    }

    @Test
    void s3Config_withExistingS3ExpressAuthScheme_doesNotOverrideAuthScheme() {
        S3ServiceClientConfiguration.Builder s3Config = new S3ServiceClientConfigurationBuilder()
            .putAuthScheme(new TestS3ExpressAuthScheme());
        assertThat(s3Config.authSchemes().get(S3ExpressAuthScheme.SCHEME_ID).signer()).isInstanceOf(AwsV4aHttpSigner.class);

        S3_EXPRESS_PLUGIN.configureClient(s3Config);
        assertThat(s3Config.authSchemes().get(S3ExpressAuthScheme.SCHEME_ID).signer()).isInstanceOf(AwsV4aHttpSigner.class);
    }

    @Test
    void s3Config_withDefaultS3AuthSchemeProvider_wrapsExistingProvider() {
        S3ServiceClientConfiguration.Builder s3Config = new S3ServiceClientConfigurationBuilder()
            .authSchemeProvider(S3AuthSchemeProvider.defaultProvider());
        assertThat(s3Config.authSchemeProvider()).isInstanceOf(DefaultS3AuthSchemeProvider.class);

        S3_EXPRESS_PLUGIN.configureClient(s3Config);
        assertThat(s3Config.authSchemeProvider()).isInstanceOf(S3ExpressAuthSchemeProvider.class);
        assertThat(getDelegateProvider(s3Config)).isInstanceOf(DefaultS3AuthSchemeProvider.class);
    }

    @Test
    void s3Config_withExistingModeledS3AuthSchemeProvider_wrapsExistingProvider() {
        S3ServiceClientConfiguration.Builder s3Config = new S3ServiceClientConfigurationBuilder()
            .authSchemeProvider(ModeledS3AuthSchemeProvider.create());
        assertThat(s3Config.authSchemeProvider()).isInstanceOf(ModeledS3AuthSchemeProvider.class);

        S3_EXPRESS_PLUGIN.configureClient(s3Config);
        assertThat(s3Config.authSchemeProvider()).isInstanceOf(S3ExpressAuthSchemeProvider.class);
        assertThat(getDelegateProvider(s3Config)).isInstanceOf(ModeledS3AuthSchemeProvider.class);
    }

    private S3AuthSchemeProvider getDelegateProvider(S3ServiceClientConfiguration.Builder s3Config) {
        S3ExpressAuthSchemeProvider configuredProvider = (S3ExpressAuthSchemeProvider) s3Config.authSchemeProvider();
        return configuredProvider.delegate();
    }

    private static class TestS3ExpressAuthScheme implements AuthScheme<AwsCredentialsIdentity> {
        @Override
        public String schemeId() {
            return S3ExpressAuthScheme.SCHEME_ID;
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return null;
        }

        @Override
        public HttpSigner<AwsCredentialsIdentity> signer() {
            return AwsV4aHttpSigner.create();
        }
    }
}
