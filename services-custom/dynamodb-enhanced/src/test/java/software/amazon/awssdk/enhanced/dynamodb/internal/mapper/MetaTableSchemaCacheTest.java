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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;

public class MetaTableSchemaCacheTest {
    private final MetaTableSchemaCache metaTableSchemaCache = new MetaTableSchemaCache();

    @Test
    public void createAndGetSingleEntry() {
        MetaTableSchema<FakeItem> metaTableSchema = metaTableSchemaCache.getOrCreate(FakeItem.class);
        assertThat(metaTableSchema).isNotNull();

        assertThat(metaTableSchemaCache.get(FakeItem.class)).hasValue(metaTableSchema);
    }

    @Test
    public void getKeyNotInMap() {
        assertThat(metaTableSchemaCache.get(FakeItem.class)).isNotPresent();
    }

    @Test
    public void createReturnsExistingObject() {
        MetaTableSchema<FakeItem> metaTableSchema = metaTableSchemaCache.getOrCreate(FakeItem.class);
        assertThat(metaTableSchema).isNotNull();

        assertThat(metaTableSchemaCache.getOrCreate(FakeItem.class)).isSameAs(metaTableSchema);
    }
}