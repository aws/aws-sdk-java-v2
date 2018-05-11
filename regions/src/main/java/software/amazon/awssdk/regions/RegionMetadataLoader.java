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

package software.amazon.awssdk.regions;

import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.internal.PartitionMetadataProvider;
import software.amazon.awssdk.regions.internal.model.Partitions;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Loads all the partition files into memory.
 */
@SdkInternalApi
public final class RegionMetadataLoader {

    private static volatile PartitionMetadataProvider provider;

    /**
     * class path from where all partition files are loaded.
     */
    private static final String PARTITIONS_RESOURCE_PATH =
            "software/amazon/awssdk/regions/internal/region/endpoints.json";

    /**
     * override class path from where all partition files are loaded.
     */
    private static final String PARTITIONS_OVERRIDE_RESOURCE_PATH =
            "software/amazon/awssdk/regions/partitions/override/endpoints.json";

    /**
     * classloader to to be used for loading the partitions.
     */
    private static final ClassLoader CLASS_LOADER = RegionMetadataLoader.class.getClassLoader();

    private RegionMetadataLoader() {
    }

    /**
     * Loads the partition files from the {@link #PARTITIONS_OVERRIDE_RESOURCE_PATH}. If no files are present, then
     * loads the partition files from the {@link #PARTITIONS_RESOURCE_PATH}
     * <p/>
     * Builds the {@link RegionMetadata} from the partition files.
     */
    protected static RegionMetadata getRegionMetadata(Region region) {
        if (provider == null) {
            build();
        }

        return provider.getRegionMetadata(region);
    }

    protected static ServiceMetadata getServiceMetadata(String serviceEndpointPrefix) {
        if (provider == null) {
            build();
        }

        return provider.getServiceMetadata(serviceEndpointPrefix);
    }

    protected static void build() {

        InputStream stream = CLASS_LOADER
                .getResourceAsStream(PARTITIONS_OVERRIDE_RESOURCE_PATH);

        if (stream != null) {
            provider = new PartitionMetadataProvider(
                    loadPartitionFromStream(stream, PARTITIONS_OVERRIDE_RESOURCE_PATH).getPartitions());
        } else {
            stream = CLASS_LOADER.getResourceAsStream(PARTITIONS_RESOURCE_PATH);
            if (stream == null) {
                throw new SdkClientException("Unable to load partition metadata from " + PARTITIONS_RESOURCE_PATH);
            }
            provider = new PartitionMetadataProvider(loadPartitionFromStream(stream, PARTITIONS_RESOURCE_PATH).getPartitions());
        }
    }

    private static Partitions loadPartitionFromStream(InputStream stream, String location) {

        try {
            return JSON.std.with(JSON.Feature.FAIL_ON_UNKNOWN_BEAN_PROPERTY)
                           .with(JSON.Feature.USE_IS_GETTERS)
                           .beanFrom(Partitions.class, stream);

        } catch (IOException | RuntimeException e) {
            throw new SdkClientException("Error while loading partitions " +
                                         "file from " + location, e);
        } finally {
            IoUtils.closeQuietly(stream, null);
        }
    }
}
