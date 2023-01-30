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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.DefaultPartitionDataProvider;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Partition;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Partitions;

public class DefaultPartitionDataProviderTest {
    private DefaultPartitionDataProvider provider;

    @BeforeEach
    public void setup() {
        provider = new DefaultPartitionDataProvider();
    }

    @Test
    public void loadPartitions_returnsData() {
        Partitions partitions = provider.loadPartitions();
        assertThat(partitions.partitions()).isNotEmpty();
    }

    @Test
    public void loadPartitions_partitionsContainsValidData() {
        Partition awsPartition = provider.loadPartitions()
                                                .partitions()
                                                .stream().filter(e -> e.id().equals("aws"))
                                                .findFirst()
                                                .orElseThrow(
                                                    () -> new RuntimeException("could not find aws partition"));

        assertThat(awsPartition.regions()).containsKey("us-west-2");
    }
}
