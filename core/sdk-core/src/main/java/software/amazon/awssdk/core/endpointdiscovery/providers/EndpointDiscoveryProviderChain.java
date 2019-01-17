/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.endpointdiscovery.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class EndpointDiscoveryProviderChain implements EndpointDiscoveryProvider {

    private static final Logger log = LoggerFactory.getLogger(EndpointDiscoveryProviderChain.class);

    private final List<EndpointDiscoveryProvider> providers;

    public EndpointDiscoveryProviderChain(EndpointDiscoveryProvider... providers) {
        this.providers = new ArrayList<>(providers.length);
        Collections.addAll(this.providers, providers);
    }

    @Override
    public boolean resolveEndpointDiscovery() {
        for (EndpointDiscoveryProvider provider : providers) {
            try {
                return provider.resolveEndpointDiscovery();
            } catch (Exception e) {
                log.debug("Unable to load endpoint discovery from {}:{}", provider.toString(), e.getMessage());
            }
        }
        return false;
    }
}
