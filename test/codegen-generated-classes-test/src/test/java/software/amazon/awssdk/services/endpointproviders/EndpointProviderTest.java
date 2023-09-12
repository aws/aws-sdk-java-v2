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

package software.amazon.awssdk.services.endpointproviders;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;

public class EndpointProviderTest {
    @Test
    public void resolveEndpoint_requiredParamNotPresent_throws() {
        assertThatThrownBy(() -> RestJsonEndpointProvidersEndpointProvider.defaultProvider()
            .resolveEndpoint(r -> {}))
            .hasMessageContaining("must not be null");
    }

    @Test
    public void resolveEndpoint_optionalParamNotPresent_doesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                               RestJsonEndpointProvidersEndpointProvider.defaultProvider()
                                                                        .resolveEndpoint(r -> r.useFips(null)
                                                                                               .region(Region.of("us-mars-1"))));
    }
}
