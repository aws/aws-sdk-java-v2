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

package software.amazon.awssdk.services.defaultendpointprovider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.defaultendpointprovider.endpoints.DefaultEndpointProviderEndpointProvider;

public class DefaultEndpointProviderTest {
    private static final DefaultEndpointProviderEndpointProvider DEFAULT_ENDPOINT_PROVIDER =
        DefaultEndpointProviderEndpointProvider.defaultProvider();

    @Test
    public void unknownRegion_resolvesToAwsPartition() {
        assertThat(resolveEndpoint("unknown-region", null))
            .isEqualTo("https://default-endpoint-provider.unknown-region.amazonaws.com");
    }

    @Test
    public void awsRegion_resolvesToAwsPartition() {
        assertThat(resolveEndpoint("us-west-2", null))
            .isEqualTo("https://default-endpoint-provider.us-west-2.amazonaws.com");
    }

    @Test
    public void cnRegion_resolvesToCnPartition() {
        assertThat(resolveEndpoint("cn-north-1", null))
            .isEqualTo("https://default-endpoint-provider.cn-north-1.amazonaws.com.cn");
    }

    @Test
    public void endpointOverride_resolvesToEndpointOverride() {
        assertThat(resolveEndpoint("unknown-region", "http://localhost:1234"))
            .isEqualTo("http://localhost:1234");
        assertThat(resolveEndpoint("us-west-2", "http://localhost:1234"))
            .isEqualTo("http://localhost:1234");
    }

    private String resolveEndpoint(String region, String endpointOverride) {
        return DEFAULT_ENDPOINT_PROVIDER.resolveEndpoint(e -> e.region(Region.of(region))
                                                               .endpoint(endpointOverride))
                                        .join()
                                        .url()
                                        .toString();
    }
}
