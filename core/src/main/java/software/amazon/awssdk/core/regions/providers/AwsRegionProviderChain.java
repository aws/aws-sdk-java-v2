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

package software.amazon.awssdk.core.regions.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.auth.AwsCredentialsProviderChain;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.regions.Region;

/**
 * Composite {@link AwsRegionProvider} that sequentially delegates to a chain of providers looking
 * for region information.
 */
public class AwsRegionProviderChain implements AwsRegionProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsCredentialsProviderChain.class);

    private final List<AwsRegionProvider> providers;

    public AwsRegionProviderChain(AwsRegionProvider... providers) {
        this.providers = new ArrayList<>(providers.length);
        Collections.addAll(this.providers, providers);
    }

    @Override
    public Region getRegion() throws SdkClientException {
        for (AwsRegionProvider provider : providers) {
            try {
                final Region region = provider.getRegion();
                if (region != null) {
                    return region;
                }
            } catch (Exception e) {
                // Ignore any exceptions and move onto the next provider
                log.debug("Unable to load region from {}:{}", provider.toString(), e.getMessage());
            }
        }

        return null;
    }
}
