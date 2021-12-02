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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;

public class DefaultOperationContextTest {
    @Test
    public void createWithTableNameAndIndexName() {
        DefaultOperationContext context = DefaultOperationContext.create("table_name", "index_name");

        assertThat(context.tableName(), is("table_name"));
        assertThat(context.indexName(), is("index_name"));
    }

    @Test
    public void createWithTableName() {
        DefaultOperationContext context = DefaultOperationContext.create("table_name");

        assertThat(context.tableName(), is("table_name"));
        assertThat(context.indexName(), Matchers.is(TableMetadata.primaryIndexName()));
    }

}
