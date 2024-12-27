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

package software.amazon.awssdk.services.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigClient;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigClientBuilder;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

class Sigv4aSigningRegionSetTest {

    private EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

    static Stream<Arguments> testCases() {

        //TODO: ClientBuilder option test cases will be added after we add regionSet option in clientBuilder in new PR.
        return Stream.of(
            Arguments.of(new SuccessCase(null,
                                         null,
                                         null,
                                         Collections.emptySet(),
                                         "No values set anywhere")),

            Arguments.of(new SuccessCase("us-west-2",
                                         null,
                                         null,
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("us-west-2"))),
                                         "System Property value takes precedence")),

            Arguments.of(new SuccessCase(null,
                                         "us-west-2",
                                         null,
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("us-west-2"))),
                                         "Environment used when System Property null")),

            Arguments.of(new SuccessCase(null,
                                         null,
                                         "us-west-2",
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("us-west-2"))),
                                         "Config file used when others null")),

            Arguments.of(new SuccessCase("us-west-2",
                                         "us-east-1", null,
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("us-west-2")))
                , "System Property overrides Environment")),

            Arguments.of(new SuccessCase("us-west-2",
                                         null,
                                         "us-east-1",
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("us-west-2"))),
                                         "System Property overrides Config File")),

            Arguments.of(new SuccessCase(null,
                                         "us-west-2",
                                         "us-east-1",
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("us-west-2"))),
                                         "Environment overrides Config File")),

            Arguments.of(new SuccessCase("us-west-2",
                                         "us-east-1",
                                         "us-north-1",
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("us-west-2"))),
                                         "SystemProperty highest precedence")),

            Arguments.of(new SuccessCase("*",
                                         "us-west-2",
                                         null,
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("*"))),
                                         "Wildcard in System Property overrides specific value")),

            Arguments.of(new SuccessCase("us-west-2",
                                         "*",
                                         null,
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("us-west-2"))),
                                         "Specific Environment overrides wildcard")),

            Arguments.of(new SuccessCase(null,
                                         "*",
                                         "us-west-2",
                                         Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("*")))
                , "Wildcard in Environment overrides Config")),

            Arguments.of(new SuccessCase("us-west-1,us-east-1",
                                         null, "us-west-2",
                                         Collections.unmodifiableSet(new HashSet<>(Arrays.asList("us-west-1", "us-east-1"))),
                                         "Multi-region System Property overrides Config")),

            Arguments.of(new SuccessCase(null,
                                         "us-west-1,us-east-1",
                                         "us-west-2",
                                         Collections.unmodifiableSet(new HashSet<>(Arrays.asList("us-west-1", "us-east-1"))),
                                         "Multi-region Environment overrides Config"))
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET.property());
        helper.reset();
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void resolvesSigv4aSigningRegionSet(TestCase testCase) {
        try {
            ProtocolRestJsonWithConfigClientBuilder builder =
                ProtocolRestJsonWithConfigClient.builder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(AnonymousCredentialsProvider.create());
            if (testCase.systemPropSetting != null) {
                System.setProperty(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET.property(), testCase.systemPropSetting);
            }
            if (testCase.envVarSetting != null) {
                helper.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET.environmentVariable(), testCase.envVarSetting);
            }
            ProfileFile.Builder profileFile = ProfileFile.builder().type(ProfileFile.Type.CONFIGURATION);

            if (testCase.profileSetting != null) {
                profileFile.content(new StringInputStream("[default]\n" +
                                                          ProfileProperty.SIGV4A_SIGNING_REGION_SET + " = " + testCase.profileSetting));
            } else {
                profileFile.content(new StringInputStream(""));
            }

            EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();

            builder.overrideConfiguration(c -> c.defaultProfileFile(profileFile.build())
                                                .defaultProfileName("default")
                                                .addExecutionInterceptor(interceptor));

            ProtocolRestJsonWithConfigClient client = builder.build();

            try {
                client.allTypes();
            } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
                // Expected
            }
            assertThat(interceptor.sigv4aSigningRegionSet())
                .containsExactlyInAnyOrderElementsOf(testCase.expectedValues);

        } finally {
            tearDown();
        }
    }

    public static class TestCase {
        private final String systemPropSetting;
        private final String envVarSetting;
        private final String profileSetting;
        private final Set<String> expectedValues;
        private final String caseName;

        public TestCase(String systemPropSetting, String envVarSetting, String profileSetting, Set<String> expectedValues,
                        String caseName) {
            this.systemPropSetting = systemPropSetting;
            this.envVarSetting = envVarSetting;
            this.profileSetting = profileSetting;
            this.expectedValues = expectedValues;
            this.caseName = caseName;
        }

        @Override
        public String toString() {
            return caseName;
        }
    }

    public static class SuccessCase extends TestCase {
        public SuccessCase(String systemPropSetting, String envVarSetting, String profileSetting, Set<String> expectedValues,
                           String caseName) {
            super(systemPropSetting, envVarSetting, profileSetting, expectedValues, caseName);
        }
    }

    public static class EndpointCapturingInterceptor implements ExecutionInterceptor {
        private Set<String> sigv4aSigningRegionSet = Collections.emptySet();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            sigv4aSigningRegionSet =
                executionAttributes.getOptionalAttribute(AwsExecutionAttribute.AWS_SIGV4A_SIGNING_REGION_SET)
                                   .orElse(Collections.emptySet());
            throw new CaptureCompletedException();
        }

        public Set<String> sigv4aSigningRegionSet() {
            return Collections.unmodifiableSet(sigv4aSigningRegionSet);
        }

        public void reset() {
            sigv4aSigningRegionSet = Collections.emptySet();
        }

        public static class CaptureCompletedException extends RuntimeException {
        }
    }
}
