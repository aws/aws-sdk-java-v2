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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class OperationContextTest {
    @Test
    public void createWithTableNameAndIndexName() {
        OperationContext context = OperationContext.create("table_name", "index_name");

        assertThat(context.tableName(), is("table_name"));
        assertThat(context.indexName(), is("index_name"));
    }

    @Test
    public void createWithTableName() {
        OperationContext context = OperationContext.create("table_name");

        assertThat(context.tableName(), is("table_name"));
        assertThat(context.indexName(), is(TableMetadata.primaryIndexName()));
    }

}
