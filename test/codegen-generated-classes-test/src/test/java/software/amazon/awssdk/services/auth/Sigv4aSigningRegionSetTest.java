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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.multiauth.MultiauthClient;
import software.amazon.awssdk.services.multiauth.MultiauthClientBuilder;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

class Sigv4aSigningRegionSetTest {

    private final EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

    static Stream<Arguments> testCases() {
        return Stream.of(
            Arguments.of(new SuccessCase(
                null,
                null,
                null,
                null,
                setOf(),
                "No values set anywhere")),

            Arguments.of(new SuccessCase(
                null,
                null,
                null,
                "us-west-2",
                setOf("us-west-2"),
                "System Property value takes precedence")),

            Arguments.of(new SuccessCase(
                null,
                "us-west-2",
                null,
                null,
                setOf("us-west-2"),
                "Environment used when System Property null")),

            Arguments.of(new SuccessCase(
                null,
                null,
                "us-west-2",
                null,
                setOf("us-west-2"),
                "Config file used when others null")),

            Arguments.of(new SuccessCase(
                null,
                "us-east-1",
                "us-west-2",
                "us-west-1",
                setOf("us-west-1"),
                "System Property overrides Environment")),

            Arguments.of(new SuccessCase(
                null,
                null,
                "us-east-1",
                "us-west-2",
                setOf("us-west-2"),
                "System Property overrides Config File")),

            Arguments.of(new SuccessCase(
                null,
                "us-west-2",
                "us-east-1",
                null,
                setOf("us-west-2"),
                "Environment overrides Config File")),

            Arguments.of(new SuccessCase(
                null,
                "us-east-1",
                "us-west-2",
                "us-west-1",
                setOf("us-west-1"),
                "SystemProperty highest precedence")),

            Arguments.of(new SuccessCase(
                null,
                null,
                null,
                "*",
                setOf("*"),
                "Wildcard in System Property overrides specific value")),

            Arguments.of(new SuccessCase(
                null,
                "*",
                null,
                null,
                setOf("*"),
                "Specific Environment overrides wildcard")),

            Arguments.of(new SuccessCase(
                null,
                "*",
                "us-west-2",
                null,
                setOf("*"),
                "Wildcard in Environment overrides Config")),

            Arguments.of(new SuccessCase(
                null,
                null,
                "us-west-2,us-east-1",
                "us-west-1,us-east-2",
                setOf("us-west-1", "us-east-2"),
                "Multi-region System Property overrides Config")),

            Arguments.of(new SuccessCase(
                RegionSet.GLOBAL,
                "us-west-2,us-east-1",
                "us-west-4",
                "us-west-5",
                setOf("*"),
                "sigv4aSigningRegionSet set to GLOBAL value, takes highest precedence")),

            Arguments.of(new SuccessCase(
                RegionSet.create("us-west-3"),
                "us-west-2,us-east-1",
                "us-west-4",
                "us-west-5",
                setOf("us-west-3"),
                "sigv4aSigningRegionSet set to different value, takes highest precedence"))
        );
    }


    private static Set<String> setOf(String... s) {
        return new HashSet<>(Arrays.asList(s));
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
            MultiauthClientBuilder builder =
                MultiauthClient.builder()
                               .region(Region.US_WEST_2)
                               .credentialsProvider(AnonymousCredentialsProvider.create());
            if (testCase.regionSet != null) {
                builder.sigv4aSigningRegionSet(testCase.regionSet);
            }
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

            MultiauthClient client = builder.build();

            assertThatExceptionOfType(CaptureCompletedException.class)
                .isThrownBy(() -> client.multiAuthWithOnlySigv4aAndSigv4(b -> b.stringMember("test").build()));

            assertThat(interceptor.sigv4aSigningRegionSet())
                .containsExactlyInAnyOrderElementsOf(testCase.expectedValues);

        } finally {
            tearDown();
        }
    }

    public static class TestCase {
        private final RegionSet regionSet;
        private final String envVarSetting;
        private final String profileSetting;
        private final String systemPropSetting;
        private final Set<String> expectedValues;
        private final String caseName;

        public TestCase(RegionSet regionSet, String envVarSetting, String profileSetting, String systemPropSetting, Set<String> expectedValues,
                        String caseName) {
            this.regionSet = regionSet;
            this.envVarSetting = envVarSetting;
            this.profileSetting = profileSetting;
            this.systemPropSetting = systemPropSetting;
            this.expectedValues = expectedValues;
            this.caseName = caseName;
        }

        @Override
        public String toString() {
            return caseName;
        }
    }

    public static class SuccessCase extends TestCase {
        public SuccessCase(RegionSet regionSet, String envVarSetting, String profileSetting, String systemPropSetting, Set<String> expectedValues,
                           String caseName) {
            super(regionSet, envVarSetting, profileSetting, systemPropSetting, expectedValues, caseName);
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
    }
    public static class CaptureCompletedException extends RuntimeException {
    }
}
