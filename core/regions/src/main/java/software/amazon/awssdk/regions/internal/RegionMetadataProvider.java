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

package software.amazon.awssdk.regions.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;

@SdkInternalApi
public interface RegionMetadataProvider {

    /**
     * Returns the region with the name given, if it exists in the metadata
     * or if it can be derived from the metadata.
     * Otherwise, returns null.
     *
     * @param region the region to search for
     * @return the corresponding region, if it exists or derived.
     */
    RegionMetadata getRegionMetadata(Region region);
}
