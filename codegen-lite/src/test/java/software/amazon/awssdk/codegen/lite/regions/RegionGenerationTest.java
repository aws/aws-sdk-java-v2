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
package software.amazon.awssdk.codegen.lite.regions;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.lite.PoetMatchers.generatesTo;

import java.io.File;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.codegen.lite.regions.model.Partition;
import software.amazon.awssdk.codegen.lite.regions.model.Partitions;

public class RegionGenerationTest {

    private static final String ENDPOINTS = "/software/amazon/awssdk/codegen/lite/test-endpoints.json";
    private static final String SERVICE_METADATA_BASE = "software.amazon.awssdk.regions.servicemetadata";
    private static final String REGION_METADATA_BASE = "software.amazon.awssdk.regions.regionmetadata";
    private static final String PARTITION_METADATA_BASE = "software.amazon.awssdk.regions.partitionmetadata";
    private static final String REGION_BASE = "software.amazon.awssdk.regions";

    private File endpoints;
    private Partitions partitions;

    @Before
    public void before() throws Exception {
        this.endpoints = Paths.get(getClass().getResource(ENDPOINTS).toURI()).toFile();
        this.partitions = RegionMetadataLoader.build(endpoints);
    }

    @Test
    public void regionClass() {
        RegionGenerator regions = new RegionGenerator(partitions, REGION_BASE);
        assertThat(regions, generatesTo("regions.java"));
    }

    @Test
    public void regionMetadataClass()  {
        Partition partition = partitions.getPartitions().get(0);
        RegionMetadataGenerator metadataGenerator = new RegionMetadataGenerator(partition,
                                                                                "us-east-1",
                                                                                "US East (N. Virginia)",
                                                                                REGION_METADATA_BASE,
                                                                                REGION_BASE);

        assertThat(metadataGenerator, generatesTo("us-east-1.java"));
    }

    @Test
    public void regionMetadataProviderClass() {
        RegionMetadataProviderGenerator providerGenerator = new RegionMetadataProviderGenerator(partitions,
                                                                                                REGION_METADATA_BASE,
                                                                                                REGION_BASE);
        assertThat(providerGenerator, generatesTo("region-metadata-provider.java"));
    }

    @Test
    public void serviceMetadataClass() {
        ServiceMetadataGenerator serviceMetadataGenerator = new ServiceMetadataGenerator(partitions,
                                                                                         "s3",
                                                                                         SERVICE_METADATA_BASE,
                                                                                         REGION_BASE);

        assertThat(serviceMetadataGenerator, generatesTo("s3-service-metadata.java"));
    }

    @Test
    public void serviceWithOverriddenPartitionsMetadataClass() {
        ServiceMetadataGenerator serviceMetadataGenerator = new ServiceMetadataGenerator(partitions,
                                                                                         "sts",
                                                                                         SERVICE_METADATA_BASE,
                                                                                         REGION_BASE);

        assertThat(serviceMetadataGenerator, generatesTo("sts-service-metadata.java"));
    }

    @Test
    public void serviceMetadataProviderClass() {
        ServiceMetadataProviderGenerator serviceMetadataProviderGenerator = new ServiceMetadataProviderGenerator(partitions,
                                                                                                                 SERVICE_METADATA_BASE,
                                                                                                                 REGION_BASE);

        assertThat(serviceMetadataProviderGenerator, generatesTo("service-metadata-provider.java"));
    }

    @Test
    public void partitionMetadataClass() {
        PartitionMetadataGenerator partitionMetadataGenerator = new PartitionMetadataGenerator(partitions.getPartitions().get(0),
                                                                              PARTITION_METADATA_BASE,
                                                                              REGION_BASE);

        assertThat(partitionMetadataGenerator, generatesTo("partition-metadata.java"));
    }

    @Test
    public void partitionMetadataProviderClass() {
        PartitionMetadataProviderGenerator partitionMetadataProviderGenerator =
            new PartitionMetadataProviderGenerator(partitions, PARTITION_METADATA_BASE, REGION_BASE);

        assertThat(partitionMetadataProviderGenerator, generatesTo("partition-metadata-provider.java"));
    }
}
