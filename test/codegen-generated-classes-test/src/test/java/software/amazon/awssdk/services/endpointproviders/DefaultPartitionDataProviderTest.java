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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.DefaultPartitionDataProvider;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Partition;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.internal.ClassLoaderHelperTestBackdoor;

public class DefaultPartitionDataProviderTest {
    private static final String EMPTY_PARTITIONS_FILE =
        DefaultPartitionDataProviderTest.class.getResource("empty-partitions.json").getFile();
    private static final String ONLY_AWS_PARTITIONS_FILE =
        DefaultPartitionDataProviderTest.class.getResource("only-aws-partitions.json").getFile();

    private DefaultPartitionDataProvider provider;

    @BeforeEach
    public void setup() {
        provider = new DefaultPartitionDataProvider();
    }

    @Test
    public void loadPartitions_systemSettingOverrideHasHighestPriority() {
        runWithPartitionsOverrides(EMPTY_PARTITIONS_FILE, ONLY_AWS_PARTITIONS_FILE, () -> {
            assertThat(provider.loadPartitions().partitions()).isEmpty();
        });
    }

    @Test
    public void loadPartitions_classpathOverrideHasSecondPriority() {
        runWithPartitionsOverrides(null, ONLY_AWS_PARTITIONS_FILE, () -> {
            assertThat(provider.loadPartitions().partitions())
                .singleElement()
                .extracting(Partition::id)
                .isEqualTo("aws");
        });
    }

    @Test
    public void loadPartitions_returnsIncludedDataByDefault() {
        assertThat(provider.loadPartitions().partitions()).hasSizeGreaterThan(1);
    }

    private void runWithPartitionsOverrides(String systemSettingPartitionsFilePath,
                                            String classLoaderPartitionsFilePath,
                                            Runnable runnable) {
        EnvironmentVariableHelper.run(h -> {
            if (systemSettingPartitionsFilePath != null) {
                h.set(SdkSystemSetting.AWS_PARTITIONS_FILE, systemSettingPartitionsFilePath);
            }
            try {
                if (classLoaderPartitionsFilePath != null) {
                    Supplier<InputStream> partitionsContentSupplier =
                        () -> FunctionalUtils.invokeSafely(() -> Files.newInputStream(Paths.get(classLoaderPartitionsFilePath)));
                    PartitionsJsonAwareClassLoader overrideClassLoader =
                        new PartitionsJsonAwareClassLoader(partitionsContentSupplier);
                    ClassLoaderHelperTestBackdoor.addClassLoaderOverride(DefaultPartitionDataProvider.class,
                                                                         overrideClassLoader);
                }
                runnable.run();
            } finally {
                ClassLoaderHelperTestBackdoor.clearClassLoaderOverrides();
            }
        });
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

    private class PartitionsJsonAwareClassLoader extends ClassLoader {
        private final Supplier<InputStream> partitionContentLoader;

        private PartitionsJsonAwareClassLoader(Supplier<InputStream> partitionContentLoader) {
            this.partitionContentLoader = partitionContentLoader;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if ("software/amazon/awssdk/global/partitions.json".equals(name)) {
                return partitionContentLoader.get();
            }

            return super.getResourceAsStream(name);
        }
    }
}
