package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Tests that endpoint resolution using service-specific endpoint overrides in environment variables, system properties,
 * and profile configuration functions correctly.
 */
@RunWith(Parameterized.class)
public class EndpointSharedConfigTest {
    private static final String GLOBAL_ENV_VAR = "AWS_ENDPOINT_URL";
    private static final String GLOBAL_SYS_PROP = "aws.endpointUrl";
    private static final String SERVICE_ENV_VAR = "AWS_ENDPOINT_URL_AMAZONPROTOCOLRESTJSON";
    private static final String SERVICE_SYS_PROP = "aws.endpointUrlProtocolRestJson";

    @Parameterized.Parameter
    public TestCase testCase;

    @Test
    public void resolvesCorrectEndpoint() {
        Map<String, String> systemPropertiesBeforeTest = new HashMap<>();
        systemPropertiesBeforeTest.put(GLOBAL_SYS_PROP, System.getProperty(GLOBAL_SYS_PROP));
        systemPropertiesBeforeTest.put(SERVICE_SYS_PROP, System.getProperty(SERVICE_SYS_PROP));

        EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

        try {
            ProtocolRestJsonClientBuilder builder =
                ProtocolRestJsonClient.builder()
                                      .region(Region.US_WEST_2)
                                      .credentialsProvider(AnonymousCredentialsProvider.create());

            if (testCase.clientSetting != null) {
                builder.endpointOverride(URI.create(testCase.clientSetting));
            }

            if (testCase.globalEnvVarSetting != null) {
                helper.set(GLOBAL_ENV_VAR, testCase.globalEnvVarSetting);
            }

            if (testCase.serviceEnvVarSetting != null) {
                helper.set(SERVICE_ENV_VAR, testCase.serviceEnvVarSetting);
            }

            if (testCase.globalSystemPropSetting != null) {
                System.setProperty(GLOBAL_SYS_PROP, testCase.globalSystemPropSetting);
            }

            if (testCase.serviceSystemPropSetting != null) {
                System.setProperty(SERVICE_SYS_PROP, testCase.serviceSystemPropSetting);
            }

            StringBuilder profileFileContent = new StringBuilder();
            profileFileContent.append("[default]\n");
            if (testCase.globalProfileSetting != null) {
                profileFileContent.append("endpoint_url = ").append(testCase.globalProfileSetting).append("\n");
            }
            if (testCase.serviceProfileSetting != null) {
                profileFileContent.append("amazonprotocolrestjson =\n")
                                  .append("  endpoint_url = ").append(testCase.serviceProfileSetting).append("\n");
            }

            ProfileFile profileFile =
                ProfileFile.builder()
                           .type(ProfileFile.Type.CONFIGURATION)
                           .content(profileFileContent.toString())
                           .build();

            EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();

            builder.overrideConfiguration(c -> c.defaultProfileFile(profileFile)
                                                .defaultProfileName("default")
                                                .addExecutionInterceptor(interceptor));

            ProtocolRestJsonClient client = builder.build();

            try {
                client.allTypes();
            } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
                // Expected
            }

            assertThat(interceptor.endpoints())
                .singleElement()
                .isEqualTo(testCase.expectedEndpoint + "/2016-03-11/allTypes");
        } finally {
            systemPropertiesBeforeTest.forEach((k, v) -> {
                if (v != null) {
                    System.setProperty(k, v);
                } else {
                    System.clearProperty(k);
                }
            });
            helper.reset();
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<TestCase> testCases() {
        List<String> settingNames =
            Arrays.asList("Client",
                          "Service system property",
                          "Global system property",
                          "Service environment variable",
                          "Global environment variable",
                          "Service profile file",
                          "Global profile file");

        boolean[][] settingCombinations = getSettingCombinations(settingNames.size());

        List<TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < settingCombinations.length; i++) {
            boolean[] settings = settingCombinations[i];
            testCases.add(createCase(settingNames, settings, i));
        }

        return testCases;
    }

    private static TestCase createCase(List<String> settingNames,
                                       boolean[] settings,
                                       int caseIndex) {
        List<String> falseSettings = new ArrayList<>();
        String firstTrueSetting = null;
        List<String> lowerPrioritySettings = new ArrayList<>();
        Integer expectedEndpointIndex = null;

        for (int j = 0; j < settings.length; j++) {
            String settingName = settingNames.get(j);
            if (settings[j] && firstTrueSetting == null) {
                firstTrueSetting = settingName;
                expectedEndpointIndex = j;
            } else if (firstTrueSetting == null) {
                falseSettings.add(settingName);
            } else {
                lowerPrioritySettings.add(settingName);
            }
        }

        // Create case name
        String caseName;
        if (firstTrueSetting == null) {
            caseName = "(" + caseIndex + ") Defaults are used.";
        } else {
            caseName = "(" + caseIndex + ") " + firstTrueSetting + " setting should be used";
            if (!falseSettings.isEmpty()) {
                caseName += ", because " + falseSettings + " setting(s) were not set";
            }
            if (!lowerPrioritySettings.isEmpty()) {
                caseName += ". " + lowerPrioritySettings + " setting(s) should be ignored";
            }
            caseName += ".";
        }

        return new TestCase(settings, expectedEndpointIndex, caseName);
    }

    public static void printArrayOfArrays(boolean[][] arrays) {
        for (boolean[] array : arrays) {
            System.out.println(arrayToString(array));
        }
    }

    private static String arrayToString(boolean[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static boolean[][] getSettingCombinations(int numSettings) {
        int numCombinations = 1 << numSettings;
        boolean[][] settingCombinations = new boolean[numCombinations][numSettings];
        for (int combination = 0; combination < numCombinations; combination++) {
            for (int settingIndex = 0; settingIndex < numSettings; settingIndex++) {
                int settingBit = 1 << settingIndex;
                settingCombinations[combination][settingIndex] = (combination & settingBit) > 0;
            }
        }
        return settingCombinations;
    }

    public static class TestCase {
        private static final String DEFAULT_ENDPOINT = "https://customresponsemetadata.us-west-2.amazonaws.com";
        private static final List<String> SETTING_ENDPOINTS =
            Arrays.asList("https://client-endpoint.com",
                          "https://service-system-property-endpoint.com",
                          "https://global-system-property-endpoint.com",
                          "https://service-env-var-endpoint.com",
                          "https://global-env-var-endpoint.com",
                          "https://service-profile-endpoint.com",
                          "https://global-profile-endpoint.com");

        private final String clientSetting;
        private final String serviceSystemPropSetting;
        private final String globalSystemPropSetting;
        private final String serviceEnvVarSetting;
        private final String globalEnvVarSetting;
        private final String serviceProfileSetting;
        private final String globalProfileSetting;
        private final String caseName;
        private final String expectedEndpoint;

        public TestCase(boolean[] settings, Integer expectedEndpointIndex, String caseName) {
            this(endpoint(settings, 0), endpoint(settings, 1), endpoint(settings, 2), endpoint(settings, 3),
                 endpoint(settings, 4), endpoint(settings, 5), endpoint(settings, 6),
                 endpointForIndex(expectedEndpointIndex), caseName);
        }

        private static String endpoint(boolean[] settings, int i) {
            if (settings[i]) {
                return SETTING_ENDPOINTS.get(i);
            }
            return null;
        }

        private static String endpointForIndex(Integer expectedEndpointIndex) {
            return expectedEndpointIndex == null ? DEFAULT_ENDPOINT : SETTING_ENDPOINTS.get(expectedEndpointIndex);
        }

        private TestCase(String clientSetting,
                         String serviceSystemPropSetting,
                         String globalSystemPropSetting,
                         String serviceEnvVarSetting,
                         String globalEnvVarSetting,
                         String serviceProfileSetting,
                         String globalProfileSetting,
                         String expectedEndpoint,
                         String caseName) {
            this.clientSetting = clientSetting;
            this.serviceSystemPropSetting = serviceSystemPropSetting;
            this.globalSystemPropSetting = globalSystemPropSetting;
            this.serviceEnvVarSetting = serviceEnvVarSetting;
            this.globalEnvVarSetting = globalEnvVarSetting;
            this.serviceProfileSetting = serviceProfileSetting;
            this.globalProfileSetting = globalProfileSetting;
            this.expectedEndpoint = expectedEndpoint;
            this.caseName = caseName;
        }

        @Override
        public String toString() {
            return caseName;
        }
    }
}
