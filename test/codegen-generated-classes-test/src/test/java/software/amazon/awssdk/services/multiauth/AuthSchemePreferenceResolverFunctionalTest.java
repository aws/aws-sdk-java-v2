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

package software.amazon.awssdk.services.multiauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.multiauth.auth.scheme.MultiauthAuthSchemeProvider;
import software.amazon.awssdk.services.multiauth.model.MultiAuthWithOnlySigv4AAndSigv4Request;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

public class AuthSchemePreferenceResolverFunctionalTest {
    private final EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

    @AfterEach
    void tearDown() {
        System.clearProperty(SdkSystemSetting.AWS_AUTH_SCHEME_PREFERENCE.property());
        helper.reset();
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void resolvesAuthSchemePreference(TestCase testCase) {
        try {
            MultiauthClientBuilder builder =
                MultiauthClient.builder()
                               .region(Region.US_WEST_2)
                               .credentialsProvider(AnonymousCredentialsProvider.create());

            builder.putAuthScheme(authScheme("aws.auth#sigv4a", new SkipCrtNoOpSigner()));

            if (testCase.clientSetting != null) {
                builder.authSchemeProvider(MultiauthAuthSchemeProvider.builder().preferredAuthSchemes(testCase.clientSetting).build());
            }

            if (testCase.systemPropSetting != null) {
                System.setProperty(SdkSystemSetting.AWS_AUTH_SCHEME_PREFERENCE.property(), testCase.systemPropSetting);
            }

            if (testCase.envVarSetting != null) {
                helper.set(SdkSystemSetting.AWS_AUTH_SCHEME_PREFERENCE.environmentVariable(), testCase.envVarSetting);
            }

            ProfileFile.Builder profileFile = ProfileFile.builder().type(ProfileFile.Type.CONFIGURATION);

            if (testCase.profileSetting != null) {
                profileFile.content(new StringInputStream("[default]\n" +
                                                          ProfileProperty.AUTH_SCHEME_PREFERENCE + " = " + testCase.profileSetting));
            } else {
                profileFile.content(new StringInputStream(""));
            }

            AutSchemeCapturingInterceptor interceptor = new AutSchemeCapturingInterceptor();

            builder.overrideConfiguration(c -> c.defaultProfileFile(profileFile.build())
                                                .defaultProfileName("default")
                                                .addExecutionInterceptor(interceptor));

            MultiauthClient client = builder.build();

            assertThatThrownBy(() ->
                                   client.multiAuthWithOnlySigv4aAndSigv4(MultiAuthWithOnlySigv4AAndSigv4Request.builder().build())
            ).isInstanceOf(AutSchemeCapturingInterceptor.CaptureException.class);

            assertThat(interceptor.authScheme()).isEqualTo(testCase.resolvedAuthScheme);
        } finally {
            tearDown();
        }
    }

    private static AuthScheme<?> authScheme(String schemeId, HttpSigner<AwsCredentialsIdentity> signer) {
        return new AuthScheme<AwsCredentialsIdentity>() {
            @Override
            public String schemeId() {
                return schemeId;
            }

            @Override
            public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
                return providers.identityProvider(AwsCredentialsIdentity.class);
            }

            @Override
            public HttpSigner<AwsCredentialsIdentity> signer() {
                return signer;
            }
        };
    }

    static Stream<Arguments> testCases() {
        return Stream.of(
            Arguments.of(new TestCase(
                null,
                null,
                null,
                Arrays.asList("sigv4", "noauth"),
                "sigv4",
                "Client config is used when set")),

            Arguments.of(new TestCase(
                null,
                null,
                "sigv4,sigv4a,bearer",
                null,
                "sigv4",
                "System property value is used")),

            Arguments.of(new TestCase(
                null,
                "sigv4a,sigv4,bearer",
                null,
                null,
                "sigv4a",
                "Environment variable is used when other properties is null")),

            Arguments.of(new TestCase(
                "bearer,sigv4,sigv4a",
                null,
                null,
                null,
                "sigv4",
                "Profile setting is used when others are null")),

            Arguments.of(new TestCase(
                "",
                null,
                null,
                null,
                "sigv4a",
                "Profile setting is used when explicit empty string is supplied")),


            Arguments.of(new TestCase(
                "bearer,sigv4,sigv4a",
                "sigv4a,sigv4,bearer",
                "sigv4,sigv4a,bearer",
                null,
                "sigv4",
                "JVM system property has precedence over env var and profile")),

            Arguments.of(new TestCase(
                "bearer,sigv4,sigv4a",
                "sigv4,sigv4a,bearer",
                "sigv4,sigv4a,bearer",
                Arrays.asList("sigv4a", "noauth", "bearer"),
                "sigv4a",
                "Client config has highest precedence"))
        );
    }

    public static class TestCase {
        private final String profileSetting;
        private final String envVarSetting;
        private final String systemPropSetting;
        private final List<String> clientSetting;
        private final String resolvedAuthScheme;
        private final String caseName;

        public TestCase(String profileSetting, String envVarSetting, String systemPropSetting, List<String> clientSetting,
                        String resolvedAuthScheme, String caseName) {
            this.profileSetting = profileSetting;
            this.envVarSetting = envVarSetting;
            this.systemPropSetting = systemPropSetting;
            this.clientSetting = clientSetting;
            this.resolvedAuthScheme = resolvedAuthScheme;

            this.caseName = caseName;
        }

        @Override
        public String toString() {
            return caseName;
        }
    }

    public static class AutSchemeCapturingInterceptor implements ExecutionInterceptor {
        private final AtomicReference<String> authScheme = new AtomicReference<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SelectedAuthScheme<?> scheme = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            String schemeId = scheme.authSchemeOption().schemeId();
            authScheme.set(schemeId.replace("aws.auth#", ""));
            throw new CaptureException();
        }


        public String authScheme() {
            return this.authScheme.get();
        }

        public static class CaptureException extends RuntimeException {
        }
    }

    public static class SkipCrtNoOpSigner implements HttpSigner<AwsCredentialsIdentity> {

        @Override
        public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
            return SignedRequest
                .builder()
                .request(request.request())
                .build();
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(
            AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
            return CompletableFuture.completedFuture(
                AsyncSignedRequest.builder()
                                  .request(request.request())
                                  .build()
            );
        }
    }
}
