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

package software.amazon.awssdk.regions.providers;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;

/**
 * Attempts to load region information from the EC2 Metadata service. If the application is not
 * running on EC2 this provider will thrown an exception.
 *
 * <P>
 * If {@link SdkSystemSetting#AWS_EC2_METADATA_DISABLED} is set to true, it will not try to load
 * region from EC2 metadata service and will return null.
 */
@SdkProtectedApi
public final class InstanceProfileRegionProvider implements AwsRegionProvider {

    /**
     * Cache region as it will not change during the lifetime of the JVM.
     */
    private volatile String region;

    @Override
    public Region getRegion() throws SdkClientException {
        if (SdkSystemSetting.AWS_EC2_METADATA_DISABLED.getBooleanValueOrThrow()) {
            throw new SdkClientException("EC2 Metadata is disabled. Unable to retrieve region information from EC2 Metadata "
                                         + "service.");
        }

        if (region == null) {
            synchronized (this) {
                if (region == null) {
                    this.region = tryDetectRegion();
                }
            }
        }

        if (region == null) {
            throw new SdkClientException("Unable to retrieve region information from EC2 Metadata service. "
                                         + "Please make sure the application is running on EC2.");
        }

        return Region.of(region);
    }

    private String tryDetectRegion() {
        return EC2MetadataUtils.getEC2InstanceRegion();
    }
}
