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

import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.util.EC2MetadataUtils;

/**
 * Attempts to load region information from the EC2 Metadata service. If the application is not
 * running on EC2 this provider will return null.
 */
public class InstanceProfileRegionProvider implements AwsRegionProvider {

    /**
     * Cache region as it will not change during the lifetime of the JVM.
     */
    private volatile String region;

    @Override
    public Region getRegion() throws SdkClientException {
        if (region == null) {
            synchronized (this) {
                if (region == null) {
                    this.region = tryDetectRegion();
                }
            }
        }

        return region == null ? null : Region.of(region);
    }

    private String tryDetectRegion() {
        try {
            return EC2MetadataUtils.getEC2InstanceRegion();
        } catch (SdkClientException sce) {
            LoggerFactory.getLogger(InstanceProfileRegionProvider.class)
                      .debug("Ignoring failure to retrieve the region: {}", sce.getMessage());
            return null;
        }
    }
}
