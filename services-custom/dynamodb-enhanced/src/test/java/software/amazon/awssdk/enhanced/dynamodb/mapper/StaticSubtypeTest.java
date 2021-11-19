package software.amazon.awssdk.enhanced.dynamodb.mapper;

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.AbstractBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.AbstractImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StaticSubtypeTest {
    private static final TableSchema<SimpleBean> SIMPLE_BEAN_TABLE_SCHEMA = TableSchema.fromClass(SimpleBean.class);

    private static abstract class AbstractItem {
    }

    @Test
    public void validSubtype() {
        StaticSubtype<SimpleBean> staticSubtype =
                StaticSubtype.builder(SimpleBean.class)
                             .names("one", "two")
                             .tableSchema(SIMPLE_BEAN_TABLE_SCHEMA)
                             .build();

        assertThat(staticSubtype.names()).containsExactly("one", "two");
        assertThat(staticSubtype.tableSchema()).isEqualTo(SIMPLE_BEAN_TABLE_SCHEMA);
    }

    @Test
    public void validSubtype_nameCollection() {
        StaticSubtype<SimpleBean> staticSubtype =
                StaticSubtype.builder(SimpleBean.class)
                             .names(Arrays.asList("one", "two"))
                             .tableSchema(SIMPLE_BEAN_TABLE_SCHEMA)
                             .build();

        assertThat(staticSubtype.names()).containsExactly("one", "two");
        assertThat(staticSubtype.tableSchema()).isEqualTo(SIMPLE_BEAN_TABLE_SCHEMA);
    }

    @Test
    public void invalidSubtype_missingNames() {
        assertThatThrownBy(() -> StaticSubtype.builder(SimpleBean.class)
                                              .names("one", "two")
                                              .build()).isInstanceOf(NullPointerException.class)
                                                       .hasMessageContaining("tableSchema")
                                                       .hasMessageContaining("SimpleBean");
    }

    @Test
    public void invalidSubtype_missingTableSchema() {
        assertThatThrownBy(() -> StaticSubtype.builder(SimpleBean.class)
                                              .tableSchema(SIMPLE_BEAN_TABLE_SCHEMA)
                                              .build()).isInstanceOf(NullPointerException.class)
                                                       .hasMessageContaining("subtype must have one or more names")
                                                       .hasMessageContaining("SimpleBean");
    }

    @Test
    public void invalidSubtype_abstractTableSchema() {
        TableSchema<AbstractItem> tableSchema = StaticTableSchema.builder(AbstractItem.class).build();

        assertThatThrownBy(() -> StaticSubtype.builder(AbstractItem.class)
                                              .tableSchema(tableSchema)
                                              .names("one", "two")
                                              .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abstract TableSchema")
                .hasMessageContaining("AbstractItem");
    }
}