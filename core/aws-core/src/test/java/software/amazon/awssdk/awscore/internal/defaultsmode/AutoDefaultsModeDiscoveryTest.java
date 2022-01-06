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

package software.amazon.awssdk.awscore.internal.defaultsmode;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.JavaSystemSetting;

@RunWith(Parameterized.class)
public class AutoDefaultsModeDiscoveryTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    @Parameterized.Parameter
    public TestData testData;

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] {

            // Mobile
            new TestData().clientRegion(Region.US_EAST_1)
                          .javaVendorProperty("The Android Project")
                          .awsExecutionEnvVar("AWS_Lambda_java8")
                          .awsRegionEnvVar("us-east-1")
                .expectedResolvedMode(DefaultsMode.MOBILE),

            // Region available from AWS execution environment
            new TestData().clientRegion(Region.US_EAST_1)
                          .awsExecutionEnvVar("AWS_Lambda_java8")
                          .awsRegionEnvVar("us-east-1")
                .expectedResolvedMode(DefaultsMode.IN_REGION),

            // Region available from AWS execution environment
            new TestData().clientRegion(Region.US_EAST_1)
                          .awsExecutionEnvVar("AWS_Lambda_java8")
                          .awsDefaultRegionEnvVar("us-west-2")
                .expectedResolvedMode(DefaultsMode.CROSS_REGION),

            // ImdsV2 available, in-region
            new TestData().clientRegion(Region.US_EAST_1)
                          .awsDefaultRegionEnvVar("us-west-2")
                          .ec2MetadataConfig(new Ec2MetadataConfig().region("us-east-1")
                                                                    .imdsAvailable(true))
                .expectedResolvedMode(DefaultsMode.IN_REGION),

            // ImdsV2 available, cross-region
            new TestData().clientRegion(Region.US_EAST_1)
                          .awsDefaultRegionEnvVar("us-west-2")
                          .ec2MetadataConfig(new Ec2MetadataConfig().region("us-west-2")
                                                                    .imdsAvailable(true)
                                                                    .ec2MetadataDisabledEnvVar("false"))
                .expectedResolvedMode(DefaultsMode.CROSS_REGION),

            // Imdsv2 disabled, should not query ImdsV2 and use fallback mode
            new TestData().clientRegion(Region.US_EAST_1)
                          .awsDefaultRegionEnvVar("us-west-2")
                          .ec2MetadataConfig(new Ec2MetadataConfig().region("us-west-2")
                                                                    .imdsAvailable(true)
                                                                    .ec2MetadataDisabledEnvVar("true"))
                .expectedResolvedMode(DefaultsMode.STANDARD),

            // Imdsv2 not available, should use fallback mode.
            new TestData().clientRegion(Region.US_EAST_1)
                          .awsDefaultRegionEnvVar("us-west-2")
                          .ec2MetadataConfig(new Ec2MetadataConfig().imdsAvailable(false))
                .expectedResolvedMode(DefaultsMode.STANDARD),
            });
    }

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Before
    public void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                           "http://localhost:" + wireMock.port());
    }

    @After
    public void cleanUp() {
        EC2MetadataUtils.clearCache();
        wireMock.resetAll();
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(JavaSystemSetting.JAVA_VENDOR.property());
    }

    @Test
    public void differentCombinationOfConfigs_shouldResolveCorrectly() throws Exception {
        if (testData.javaVendorProperty != null) {
            System.setProperty(JavaSystemSetting.JAVA_VENDOR.property(), testData.javaVendorProperty);
        }

        if (testData.awsExecutionEnvVar != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EXECUTION_ENV.environmentVariable(),
                                            testData.awsExecutionEnvVar);
        } else {
            ENVIRONMENT_VARIABLE_HELPER.remove(SdkSystemSetting.AWS_EXECUTION_ENV.environmentVariable());
        }

        if (testData.awsRegionEnvVar != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_REGION.environmentVariable(), testData.awsRegionEnvVar);
        } else {
            ENVIRONMENT_VARIABLE_HELPER.remove(SdkSystemSetting.AWS_REGION.environmentVariable());
        }

        if (testData.awsDefaultRegionEnvVar != null) {
            ENVIRONMENT_VARIABLE_HELPER.set("AWS_DEFAULT_REGION", testData.awsDefaultRegionEnvVar);
        } else {
            ENVIRONMENT_VARIABLE_HELPER.remove("AWS_DEFAULT_REGION");
        }

        if (testData.ec2MetadataConfig != null) {
            if (testData.ec2MetadataConfig.ec2MetadataDisabledEnvVar != null) {
                ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.environmentVariable(),
                                                testData.ec2MetadataConfig.ec2MetadataDisabledEnvVar);
            }

            if (testData.ec2MetadataConfig.imdsAvailable) {
                stubSuccessfulResponse(testData.ec2MetadataConfig.region);
            }
        }

        Callable<DefaultsMode> result = () -> new AutoDefaultsModeDiscovery().discover(testData.clientRegion);
        assertThat(result.call()).isEqualTo(testData.expectedResolvedMode);
    }

    public void stubSuccessfulResponse(String region) {
        stubFor(put("/latest/api/token")
                    .willReturn(aResponse().withStatus(200).withBody("token")));

        stubFor(get("/latest/meta-data/placement/region")
                    .willReturn(aResponse().withStatus(200).withBody(region)));
    }

    private static final class TestData {
        private Region clientRegion;
        private String javaVendorProperty;
        private String awsExecutionEnvVar;
        private String awsRegionEnvVar;
        private String awsDefaultRegionEnvVar;
        private Ec2MetadataConfig ec2MetadataConfig;
        private DefaultsMode expectedResolvedMode;

        public TestData clientRegion(Region clientRegion) {
            this.clientRegion = clientRegion;
            return this;
        }

        public TestData javaVendorProperty(String javaVendorProperty) {
            this.javaVendorProperty = javaVendorProperty;
            return this;
        }

        public TestData awsExecutionEnvVar(String awsExecutionEnvVar) {
            this.awsExecutionEnvVar = awsExecutionEnvVar;
            return this;
        }

        public TestData awsRegionEnvVar(String awsRegionEnvVar) {
            this.awsRegionEnvVar = awsRegionEnvVar;
            return this;
        }

        public TestData awsDefaultRegionEnvVar(String awsDefaultRegionEnvVar) {
            this.awsDefaultRegionEnvVar = awsDefaultRegionEnvVar;
            return this;
        }

        public TestData ec2MetadataConfig(Ec2MetadataConfig ec2MetadataConfig) {
            this.ec2MetadataConfig = ec2MetadataConfig;
            return this;
        }

        public TestData expectedResolvedMode(DefaultsMode expectedResolvedMode) {
            this.expectedResolvedMode = expectedResolvedMode;
            return this;
        }
    }

    private static final class Ec2MetadataConfig {
        private boolean imdsAvailable;
        private String region;
        private String ec2MetadataDisabledEnvVar;

        public Ec2MetadataConfig imdsAvailable(boolean imdsAvailable) {
            this.imdsAvailable = imdsAvailable;
            return this;
        }

        public Ec2MetadataConfig region(String region) {
            this.region = region;
            return this;
        }

        public Ec2MetadataConfig ec2MetadataDisabledEnvVar(String ec2MetadataDisabledEnvVar) {
            this.ec2MetadataDisabledEnvVar = ec2MetadataDisabledEnvVar;
            return this;
        }
    }
}