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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.IndexUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.IndexType;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticIndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata;

public class IndexUtilsTest {

    @Test
    public void splitSecondaryIndicesToLocalAndGlobalOnes_separateIndices() {
        Collection<IndexMetadata> indices = Arrays.asList(StaticIndexMetadata.builder()
                                                                                 .name("LocalIndex1")
                                                                                 .build(),
                                                              StaticIndexMetadata.builder()
                                                                                 .name("GlobalIndex1")
                                                                                 .partitionKey(StaticKeyAttributeMetadata.create(
                                                                                     "GlobalIndexPartitionKey",
                                                                                     AttributeValueType.N))
                                                                                 .build());

        Map<IndexType, List<IndexMetadata>> indexGroups = IndexUtils.splitSecondaryIndicesToLocalAndGlobalOnes(indices);

        assertThat(indexGroups.get(IndexType.LSI)).hasSize(1);
        assertThat(indexGroups.get(IndexType.LSI).get(0).name()).isEqualTo("LocalIndex1");
        assertThat(indexGroups.get(IndexType.GSI)).hasSize(1);
        assertThat(indexGroups.get(IndexType.GSI).get(0).name()).isEqualTo("GlobalIndex1");
    }
}