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

package software.amazon.awssdk.services.compiledendpointrules.endpoints.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Covers {@code RulesFunctions.awsPartition}'s handling of partition metadata that declares no {@code aws} partition.
 *
 * <p>{@code aws} is the fallback for a region that matches neither a declared region name nor a partition's region
 * pattern, so metadata without it is only incomplete for those regions. The regions it does describe must keep
 * resolving, and only the unmatched ones may fail. Pinned here because the natural way to precompute the fallback -
 * validating it while loading - fails every region instead, and reads as a tidier implementation.
 *
 * <p>Reachable in production through either partitions-file override, not through the shipped default: the default
 * metadata is generated into {@code LazyPartitionsContainer} at build time from {@code partitions.json.resource} and
 * always declares {@code aws}.
 */
class RulesFunctionsAwsPartitionTest {
    private static final String NO_AWS_PARTITIONS_FILE =
        RulesFunctionsAwsPartitionTest.class.getResource(
            "/software/amazon/awssdk/services/endpointproviders/no-aws-partitions.json").getFile();

    private final EnvironmentVariableHelper environment = new EnvironmentVariableHelper();

    /**
     * {@code RulesFunctions} caches the loaded metadata in a static, so the cache has to be cleared around any test
     * that swaps the metadata out. Without this the result would depend on whether some earlier test in the same JVM
     * had already resolved a partition.
     */
    @BeforeEach
    void clearCacheBefore() throws Exception {
        resetPartitionDataCache();
    }

    @AfterEach
    void clearCacheAfter() throws Exception {
        environment.reset();
        resetPartitionDataCache();
    }

    @Test
    void metadataWithoutAws_regionDeclaredByAnotherPartition_stillResolves() {
        environment.set(SdkSystemSetting.AWS_PARTITIONS_FILE, NO_AWS_PARTITIONS_FILE);

        assertThat(RulesFunctions.awsPartition("cn-north-1").name()).isEqualTo("aws-cn");
    }

    @Test
    void metadataWithoutAws_regionMatchingAnotherPartitionsPattern_stillResolves() {
        environment.set(SdkSystemSetting.AWS_PARTITIONS_FILE, NO_AWS_PARTITIONS_FILE);

        // Not declared in the fixture's regions, so this can only resolve through the regionRegex.
        assertThat(RulesFunctions.awsPartition("cn-northwest-1").name()).isEqualTo("aws-cn");
    }

    /**
     * The one case the missing partition actually breaks. The message has to name the region and the override
     * mechanisms, because at this point that is the only information available about why the fallback was needed.
     */
    @Test
    void metadataWithoutAws_unmatchedRegion_throwsNamingTheRegionAndTheCause() {
        environment.set(SdkSystemSetting.AWS_PARTITIONS_FILE, NO_AWS_PARTITIONS_FILE);

        assertThatThrownBy(() -> RulesFunctions.awsPartition("us-east-1"))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("us-east-1")
            .hasMessageContaining("'aws' partition")
            .hasMessageContaining("aws.partitionsFile");
    }

    @Test
    void defaultMetadata_unmatchedRegion_fallsBackToAws() {
        assertThat(RulesFunctions.awsPartition("not-a-real-region-1").name()).isEqualTo("aws");
    }

    @Test
    void defaultMetadata_knownRegionResolvesToItsOwnPartition() {
        assertThat(RulesFunctions.awsPartition("us-west-2").name()).isEqualTo("aws");
        assertThat(RulesFunctions.awsPartition("cn-north-1").name()).isEqualTo("aws-cn");
        assertThat(RulesFunctions.awsPartition("us-gov-west-1").name()).isEqualTo("aws-us-gov");
    }

    private static void resetPartitionDataCache() throws Exception {
        Field partitionDataField = RulesFunctions.class.getDeclaredField("PARTITION_DATA");
        partitionDataField.setAccessible(true);
        Object lazyValue = partitionDataField.get(null);

        Field valueField = lazyValue.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(lazyValue, null);

        Field initializedField = lazyValue.getClass().getDeclaredField("initialized");
        initializedField.setAccessible(true);
        initializedField.setBoolean(lazyValue, false);
    }
}
