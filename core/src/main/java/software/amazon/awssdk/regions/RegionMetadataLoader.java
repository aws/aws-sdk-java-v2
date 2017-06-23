/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.internal.region.PartitionMetadataProvider;
import software.amazon.awssdk.internal.region.model.Partitions;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Loads all the partition files into memory.
 */
@SdkInternalApi
public class RegionMetadataLoader {

    private static volatile PartitionMetadataProvider provider;

    /**
     * class path from where all partition files are loaded.
     */
    private static final String PARTITIONS_RESOURCE_PATH =
            "software/amazon/awssdk/internal/region/endpoints.json";

    /**
     * override class path from where all partition files are loaded.
     */
    private static final String PARTITIONS_OVERRIDE_RESOURCE_PATH =
            "software/amazon/awssdk/partitions/override/endpoints.json";

    /**
     * Jackson object mapper that is used for parsing the partition files.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
            .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * classloader to to be used for loading the partitions.
     */
    private static final ClassLoader CLASS_LOADER = RegionMetadataLoader.class.getClassLoader();

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

            return MAPPER.readValue(stream, Partitions.class);

        } catch (IOException e) {
            throw new SdkClientException("Error while loading partitions " +
                                         "file from " + location, e);
        } finally {
            IoUtils.closeQuietly(stream, null);
        }
    }
}
