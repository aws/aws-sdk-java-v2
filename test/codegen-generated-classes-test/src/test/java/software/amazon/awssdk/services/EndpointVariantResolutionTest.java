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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;

public class EndpointVariantResolutionTest {
    @Test
    public void dualstackEndpointResolution() {
        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();
        try {
            clientBuilder(interceptor).dualstackEnabled(true).build().allTypes();
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // Expected
        }

        assertThat(interceptor.endpoints())
            .singleElement()
            .isEqualTo("https://customresponsemetadata.us-west-2.api.aws/2016-03-11/allTypes");
    }

    @Test
    public void fipsEndpointResolution() {
        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();
        try {
            clientBuilder(interceptor).fipsEnabled(true).build().allTypes();
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // Expected
        }

        assertThat(interceptor.endpoints())
            .singleElement()
            .isEqualTo("https://customresponsemetadata-fips.us-west-2.amazonaws.com/2016-03-11/allTypes");
    }

    @Test
    public void dualstackFipsEndpointResolution() {
        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();
        try {
            clientBuilder(interceptor).dualstackEnabled(true).fipsEnabled(true).build().allTypes();
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // Expected
        }

        assertThat(interceptor.endpoints())
            .singleElement()
            .isEqualTo("https://customresponsemetadata-fips.us-west-2.api.aws/2016-03-11/allTypes");
    }

    private ProtocolRestJsonClientBuilder clientBuilder(EndpointCapturingInterceptor interceptor) {
        return ProtocolRestJsonClient.builder()
                                     .region(Region.US_WEST_2)
                                     .credentialsProvider(AnonymousCredentialsProvider.create())
                                     .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor));
    }
}
