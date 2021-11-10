package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.Validate;

@RunWith(Parameterized.class)
public class FipsEndpointTest {
    @Parameterized.Parameter
    public TestCase testCase;

    @Test
    public void resolvesCorrectEndpoint() {
        String systemPropertyBeforeTest = System.getProperty(SdkSystemSetting.AWS_USE_FIPS_ENDPOINT.property());
        EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

        try {
            ProtocolRestJsonClientBuilder builder =
                ProtocolRestJsonClient.builder()
                                      .region(Region.US_WEST_2)
                                      .credentialsProvider(AnonymousCredentialsProvider.create());

            if (testCase.clientSetting != null) {
                builder.fipsEnabled(testCase.clientSetting);
            }

            if (testCase.systemPropSetting != null) {
                System.setProperty(SdkSystemSetting.AWS_USE_FIPS_ENDPOINT.property(), testCase.systemPropSetting);
            }

            if (testCase.envVarSetting != null) {
                helper.set(SdkSystemSetting.AWS_USE_FIPS_ENDPOINT.environmentVariable(), testCase.envVarSetting);
            }

            ProfileFile.Builder profileFile = ProfileFile.builder().type(ProfileFile.Type.CONFIGURATION);

            if (testCase.profileSetting != null) {
                profileFile.content(new StringInputStream("[default]\n" +
                                                          ProfileProperty.USE_FIPS_ENDPOINT + " = " + testCase.profileSetting));
            } else {
                profileFile.content(new StringInputStream(""));
            }

            EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();

            builder.overrideConfiguration(c -> c.defaultProfileFile(profileFile.build())
                                                .defaultProfileName("default")
                                                .addExecutionInterceptor(interceptor));

            if (testCase instanceof SuccessCase) {
                ProtocolRestJsonClient client = builder.build();

                try {
                    client.allTypes();
                } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
                    // Expected
                }

                boolean expectedFipsEnabled = ((SuccessCase) testCase).expectedValue;
                String expectedEndpoint = expectedFipsEnabled
                                          ? "https://customresponsemetadata-fips.us-west-2.amazonaws.com/2016-03-11/allTypes"
                                          : "https://customresponsemetadata.us-west-2.amazonaws.com/2016-03-11/allTypes";
                assertThat(interceptor.endpoints()).singleElement().isEqualTo(expectedEndpoint);
            } else {
                FailureCase failure = Validate.isInstanceOf(FailureCase.class, testCase, "Unexpected test case type.");
                assertThatThrownBy(builder::build).hasMessageContaining(failure.exceptionMessage);
            }

        } finally {
            if (systemPropertyBeforeTest != null) {
                System.setProperty(SdkSystemSetting.AWS_USE_FIPS_ENDPOINT.property(), systemPropertyBeforeTest);
            } else {
                System.clearProperty(SdkSystemSetting.AWS_USE_FIPS_ENDPOINT.property());
            }
            helper.reset();
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<TestCase> testCases() {
        return Arrays.asList(new SuccessCase(true, "false", "false", "false", true, "Client highest priority (true)"),
                             new SuccessCase(false, "true", "true", "true", false, "Client highest priority (false)"),
                             new SuccessCase(null, "true", "false", "false", true, "System property second priority (true)"),
                             new SuccessCase(null, "false", "true", "true", false, "System property second priority (false)"),
                             new SuccessCase(null, null, "true", "false", true, "Env var third priority (true)"),
                             new SuccessCase(null, null, "false", "true", false, "Env var third priority (false)"),
                             new SuccessCase(null, null, null, "true", true, "Profile last priority (true)"),
                             new SuccessCase(null, null, null, "false", false, "Profile last priority (false)"),
                             new SuccessCase(null, null, null, null, false, "Default is false."),
                             new SuccessCase(null, "tRuE", null, null, true, "System property is not case sensitive."),
                             new SuccessCase(null, null, "tRuE", null, true, "Env var is not case sensitive."),
                             new SuccessCase(null, null, null, "tRuE", true, "Profile property is not case sensitive."),
                             new FailureCase(null, "FOO", null, null, "FOO", "Invalid system property values fail."),
                             new FailureCase(null, null, "FOO", null, "FOO", "Invalid env var values fail."),
                             new FailureCase(null, null, null, "FOO", "FOO", "Invalid profile values fail."));
    }

    public static class TestCase {
        private final Boolean clientSetting;
        private final String envVarSetting;
        private final String systemPropSetting;
        private final String profileSetting;
        private final String caseName;

        public TestCase(Boolean clientSetting, String systemPropSetting, String envVarSetting, String profileSetting,
                        String caseName) {
            this.clientSetting = clientSetting;
            this.envVarSetting = envVarSetting;
            this.systemPropSetting = systemPropSetting;
            this.profileSetting = profileSetting;
            this.caseName = caseName;
        }

        @Override
        public String toString() {
            return caseName;
        }
    }

    public static class SuccessCase extends TestCase {
        private final boolean expectedValue;

        public SuccessCase(Boolean clientSetting,
                           String systemPropSetting,
                           String envVarSetting,
                           String profileSetting,
                           boolean expectedValue,
                           String caseName) {
            super(clientSetting, systemPropSetting, envVarSetting, profileSetting, caseName);
            this.expectedValue = expectedValue;
        }
    }

    private static class FailureCase extends TestCase {
        private final String exceptionMessage;

        public FailureCase(Boolean clientSetting,
                           String systemPropSetting,
                           String envVarSetting,
                           String profileSetting,
                           String exceptionMessage,
                           String caseName) {
            super(clientSetting, systemPropSetting, envVarSetting, profileSetting, caseName);
            this.exceptionMessage = exceptionMessage;
        }
    }
}
