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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Validate;

public class DisableS3ExpressAuthTest {
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @BeforeEach
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_S3_DISABLE_EXPRESS_SESSION_AUTH.property());
    }

    // description, clientConfig, systemProperty, envVar, profileFileValue, expected)
    public static List<Arguments> disableS3ExpressAuthConfiguration() {
        return Arrays.asList(
            Arguments.of("No configuration", null, null, null, null, false),
            Arguments.of("Client config sets disable", true, null, null, null, true),
            Arguments.of("Client config sets disable false", false, null, null, null, false),
            Arguments.of("System property sets disable", null, "true", null, null, true),
            Arguments.of("System property sets disable false", null, "false", null, null, false),
            Arguments.of("Env var sets disable", null, null, "true", null, true),
            Arguments.of("Env var sets disable false", null, null, "false", null, false),
            Arguments.of("Profile file param sets disable", null, null, null, "/s3_express_profile_true.tst", true),
            Arguments.of("Profile file param sets disable false", null, null, null, "/s3_express_profile_false.tst", false),
            Arguments.of("Client config overrides system property - disable", true, "false", null, null, true),
            Arguments.of("Client config overrides system property - disable false", false, "true", null, null, false),
            Arguments.of("Client config overrides env var - disable", true, null, "false", null, true),
            Arguments.of("Client config overrides env var - disable false", false, null, "true", null, false),
            Arguments.of("Client config overrides profile file property - disable", true, null, null, "/s3_express_profile_true.tst", true),
            Arguments.of("Client config overrides profile file property - disable false", false, null, null, "/s3_express_profile_false.tst", false)
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("disableS3ExpressAuthConfiguration")
    public void disableS3ExpressAuth(String description, Boolean clientConfig, String systemProperty, String envVarValue,
                                     String profileFileValue, boolean expected) {

        if (envVarValue != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_S3_DISABLE_EXPRESS_SESSION_AUTH.environmentVariable(),
                                            envVarValue);
        }

        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_S3_DISABLE_EXPRESS_SESSION_AUTH.property(), systemProperty);
        }

        S3EndpointProvider mockProvider = mock(S3EndpointProvider.class);
        when(mockProvider.resolveEndpoint(any(S3EndpointParams.class))).thenThrow(new RuntimeException("boom"));

        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                                                  .region(Region.US_EAST_1)
                                                  .credentialsProvider(CREDENTIALS_PROVIDER)
                                                  .endpointProvider(mockProvider);

        if (clientConfig != null) {
            s3ClientBuilder.disableS3ExpressSessionAuth(clientConfig);
        }

        if (profileFileValue != null) {
            String diskLocationForFile = diskLocationForConfig(profileFileValue);
            Validate.isTrue(Files.isReadable(Paths.get(diskLocationForFile)), diskLocationForFile + " is not readable.");

            ProfileFile file = ProfileFile.builder()
                                          .content(Paths.get(diskLocationForFile))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();

            s3ClientBuilder.overrideConfiguration(o -> o.defaultProfileFile(file).defaultProfileName("s3Express_auth").build());
        }

        S3Client s3Client = s3ClientBuilder.build();

        assertThatThrownBy(() -> s3Client.putObject(r -> r.bucket("b").key("k"), RequestBody.fromString("t")))
            .hasMessageContaining("boom");

        ArgumentCaptor<S3EndpointParams> endpointParamsCaptor = ArgumentCaptor.forClass(S3EndpointParams.class);
        verify(mockProvider).resolveEndpoint(endpointParamsCaptor.capture());

        S3EndpointParams resolvedEndpointParams = endpointParamsCaptor.getValue();
        assertThat(resolvedEndpointParams.disableS3ExpressSessionAuth()).isNotNull().isEqualTo(expected);
    }

    private String diskLocationForConfig(String configFileName) {
        return getClass().getResource(configFileName).getFile();
    }
}
