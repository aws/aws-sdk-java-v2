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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean;

@RunWith(MockitoJUnitRunner.class)
public class StaticSubtypeTest {
    private static final TableSchema<SimpleBean> SIMPLE_BEAN_TABLE_SCHEMA = TableSchema.fromClass(SimpleBean.class);

    private abstract static class AbstractItem {
    }

    @Test
    public void testValidSubtype() {
        StaticSubtype<SimpleBean> staticSubtype =
            StaticSubtype.builder(SimpleBean.class)
                         .name("customer")
                         .tableSchema(SIMPLE_BEAN_TABLE_SCHEMA)
                         .build();

        assertThat(staticSubtype.name()).isEqualTo("customer");
        assertThat(staticSubtype.tableSchema()).isEqualTo(SIMPLE_BEAN_TABLE_SCHEMA);
    }

    @Test
    public void testInvalidSubtype_withMissingNames_throwsException() {
        assertThatThrownBy(StaticSubtype.builder(SimpleBean.class)
                                        .tableSchema(SIMPLE_BEAN_TABLE_SCHEMA)::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("A subtype must have one name associated with it. "
                        + "[subtypeClass = \"software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean\"]");
    }

    @Test
    public void testInvalidSubtype_withMissingTableSchema_throwsException() {
        assertThatThrownBy(StaticSubtype.builder(SimpleBean.class)
                                        .name("customer")::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("A subtype must have a tableSchema associated with it. "
                        + "[subtypeClass = \"software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean\"]");
    }


    @Test
    public void testInvalidSubtype_withAbstractTableSchema_throwsException() {
        TableSchema<AbstractItem> tableSchema = StaticTableSchema.builder(AbstractItem.class).build();

        assertThatThrownBy(StaticSubtype.builder(AbstractItem.class)
                                        .tableSchema(tableSchema)
                                        .name("customer")::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A subtype may not be constructed with an abstract TableSchema. An abstract TableSchema is a TableSchema "
                        + "that does not know how to construct new objects of its type. "
                        + "[subtypeClass = \"software.amazon.awssdk.enhanced.dynamodb.mapper.StaticSubtypeTest$AbstractItem\"]");
    }
}
