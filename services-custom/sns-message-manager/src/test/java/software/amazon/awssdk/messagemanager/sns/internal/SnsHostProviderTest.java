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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.endpoints.SnsEndpointParams;
import software.amazon.awssdk.services.sns.endpoints.SnsEndpointProvider;

public class SnsHostProviderTest {

    @ParameterizedTest
    @MethodSource("commonNameTestCases")
    void signingCertCommonName_returnsCorrectNameForRegion(CommonNameTestCase tc) {
        SnsHostProvider hostProvider = new SnsHostProvider(Region.of(tc.region));
        assertThat(hostProvider.signingCertCommonName()).isEqualTo(tc.expectedCommonName);
    }

    @Test
    void regionalEndpoint_delegatesToEndpointProvider() {
        SnsEndpointProvider mockProvider = mock(SnsEndpointProvider.class);
        SnsEndpointProvider realProvider = SnsEndpointProvider.defaultProvider();

        when(mockProvider.resolveEndpoint(any(SnsEndpointParams.class))).thenAnswer(
            i -> realProvider.resolveEndpoint(i.getArgument(0, SnsEndpointParams.class)));

        ArgumentCaptor<SnsEndpointParams> paramsCaptor = ArgumentCaptor.forClass(SnsEndpointParams.class);

        Region region = Region.US_WEST_2;
        SnsHostProvider hostProvider = new SnsHostProvider(region, mockProvider);
        hostProvider.regionalEndpoint();

        verify(mockProvider).resolveEndpoint(paramsCaptor.capture());
        assertThat(paramsCaptor.getValue().region()).isEqualTo(region);
    }

    private static Stream<CommonNameTestCase> commonNameTestCases() {
        return Stream.of(
          // gov regions
          new CommonNameTestCase("us-gov-west-1", "sns-us-gov-west-1.amazonaws.com"),
          new CommonNameTestCase("us-gov-east-1", "sns-us-gov-west-1.amazonaws.com"),

          // cn regions
          new CommonNameTestCase("cn-north-1", "sns-cn-north-1.amazonaws.com.cn"),
          new CommonNameTestCase("cn-northwest-1", "sns-cn-northwest-1.amazonaws.com.cn"),

          // opt-in regions
          new CommonNameTestCase("me-south-1", "sns-signing.me-south-1.amazonaws.com"),
          new CommonNameTestCase("ap-east-1", "sns-signing.ap-east-1.amazonaws.com"),
          new CommonNameTestCase("me-south-1", "sns-signing.me-south-1.amazonaws.com"),
          new CommonNameTestCase("ap-east-2", "sns-signing.ap-east-2.amazonaws.com"),
          new CommonNameTestCase("ap-southeast-5", "sns-signing.ap-southeast-5.amazonaws.com"),
          new CommonNameTestCase("ap-southeast-6", "sns-signing.ap-southeast-6.amazonaws.com"),
          new CommonNameTestCase("ap-southeast-7", "sns-signing.ap-southeast-7.amazonaws.com"),
          new CommonNameTestCase("mx-central-1", "sns-signing.mx-central-1.amazonaws.com"),

          // iso regions
          new CommonNameTestCase("us-iso-east-1", "sns-us-iso-east-1.c2s.ic.gov"),
          new CommonNameTestCase("us-isob-east-1", "sns-us-isob-east-1.sc2s.sgov.gov"),
          new CommonNameTestCase("us-isof-east-1", "sns-signing.us-isof-east-1.csp.hci.ic.gov"),
          new CommonNameTestCase("us-isof-south-1", "sns-signing.us-isof-south-1.csp.hci.ic.gov"),
          new CommonNameTestCase("eu-isoe-west-1", "sns-signing.eu-isoe-west-1.cloud.adc-e.uk"),

          //eusc
          new CommonNameTestCase("eusc-de-east-1", "sns-signing.eusc-de-east-1.amazonaws.eu"),

          // other regions
          new CommonNameTestCase("us-east-1", "sns.amazonaws.com"),
          new CommonNameTestCase("us-west-1", "sns.amazonaws.com"),

          // unknown regions
          new CommonNameTestCase("us-east-9", "sns.amazonaws.com"),
          new CommonNameTestCase("foo-bar-1", "sns.amazonaws.com"),
          new CommonNameTestCase("cn-northwest-9", "sns.amazonaws.com.cn")


        );
    }

    private static class CommonNameTestCase {
        private String region;
        private String expectedCommonName;

        CommonNameTestCase(String region, String expectedCommonName) {
            this.region = region;
            this.expectedCommonName = expectedCommonName;
        }

        @Override
        public String toString() {
            return region + " - " + expectedCommonName;
        }
    }
}
