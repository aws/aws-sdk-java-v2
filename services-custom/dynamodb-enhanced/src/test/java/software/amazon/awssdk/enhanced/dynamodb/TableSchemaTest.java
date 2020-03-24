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

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean;

public class TableSchemaTest {

    @Test
    public void builder_constructsStaticTableSchemaBuilder() {
        StaticTableSchema.Builder<FakeItem> builder = TableSchema.builder(FakeItem.class);
        assertThat(builder).isNotNull();
    }

    @Test
    public void fromBean_constructsBeanTableSchema() {
        BeanTableSchema<SimpleBean> beanBeanTableSchema = TableSchema.fromBean(SimpleBean.class);
        assertThat(beanBeanTableSchema).isNotNull();
    }
}
