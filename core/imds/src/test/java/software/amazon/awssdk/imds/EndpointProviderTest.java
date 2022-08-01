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

package software.amazon.awssdk.imds;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.imds.internal.EndpointProvider;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Test Class to test the endpoint resolution functionality.
 */
public class EndpointProviderTest {

    @Rule
    private EnvironmentVariableHelper settingsHelper = new EnvironmentVariableHelper();

    private static Stream<Arguments> provideEndpointAndEndpointModes() {
        return Stream.of(
             Arguments.of(null, EndpointMode.IPV4,null,false,false,"http://169.254.169.254"),
             Arguments.of(null, EndpointMode.IPV6,null,false,false,"http://[fd00:ec2::254]"),
             Arguments.of(null, EndpointMode.IPV6,"http://169.254.169.254",false,true,"http://169.254.169.254"),
             Arguments.of(null, EndpointMode.IPV4,"http://[fd00:ec2::254]",true,true,"http://[fd00:ec2::254]"),
             Arguments.of(null, null,null,true,false,"http://169.254.169.254"),
             Arguments.of(URI.create("http://169.254.169.254"), null,null,true,false,"http://169.254.169.254"),
             Arguments.of(URI.create("http://[fd00:ec2::254]"), null,null,true,false,"http://[fd00:ec2::254]")
        );
    }

    @ParameterizedTest
    @MethodSource("provideEndpointAndEndpointModes")
    void validateResolveEndpoint(URI builderEndpoint, EndpointMode endpointMode, String envEndpoint, boolean setConfigFile,
                                 boolean setEnvVariable,
                                 String expectedValue) throws URISyntaxException {

        if(setConfigFile) {
            String testFile = "/profileconfig/test-profiles.tst";
            settingsHelper.set(ProfileFileSystemSetting.AWS_PROFILE, "test");
            settingsHelper.set(ProfileFileSystemSetting.AWS_CONFIG_FILE,
                               Paths.get(getClass().getResource(testFile).toURI()).toString());
        }

        EndpointProvider endpointProvider = EndpointProvider.builder().build();
        if(setEnvVariable) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), envEndpoint);
        }
        String endpoint = endpointProvider.resolveEndpoint(builderEndpoint,endpointMode);
        assertThat(endpoint).isEqualTo(expectedValue);
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    private static Stream<Arguments> provideEndpointModes() {
        return Stream.of(
            Arguments.of(null,null,false,false,EndpointMode.IPV4),
            Arguments.of(EndpointMode.IPV4,"ipv6",true,true,EndpointMode.IPV4),
            Arguments.of(EndpointMode.IPV6,"ipv4",false,true,EndpointMode.IPV6),
            Arguments.of(null,"ipv4",true,true,EndpointMode.IPV4),
            Arguments.of(null,"ipv6",false,true,EndpointMode.IPV6),
            Arguments.of(null,null,true,false,EndpointMode.IPV4)
        );
    }

    @ParameterizedTest
    @MethodSource("provideEndpointModes")
     void endpointModeCheck(EndpointMode builderEndpointMode,String envEndpointMode,
                                        boolean setConfigFile,boolean setEnvVariable,EndpointMode expectedValue) throws URISyntaxException {

        if(setConfigFile) {
        String testFile = "/profileconfig/test-profiles.tst";
        settingsHelper.set(ProfileFileSystemSetting.AWS_PROFILE, "test");
        settingsHelper.set(ProfileFileSystemSetting.AWS_CONFIG_FILE,
                           Paths.get(getClass().getResource(testFile).toURI()).toString());
        }

        EndpointProvider endpointProvider = EndpointProvider.builder().build();
        if(setEnvVariable) {
         System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property(), envEndpointMode);
        }
        EndpointMode endpointMode = endpointProvider.resolveEndpointMode(builderEndpointMode);
        assertThat(endpointMode).isEqualTo(expectedValue);
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property());
    }

}
