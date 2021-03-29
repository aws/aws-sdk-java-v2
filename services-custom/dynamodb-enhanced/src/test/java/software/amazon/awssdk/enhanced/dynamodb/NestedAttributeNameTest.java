package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class NestedAttributeNameTest {

    private static final String ATTRIBUTE_NAME = "attributeName";

    @Test
    public void testNullAttributeNames_fails() {
        assertThatThrownBy(() -> NestedAttributeName.builder().build())
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> NestedAttributeName.builder().addElement(null).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NestedAttributeName.builder().elements(ATTRIBUTE_NAME).addElement(null).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NestedAttributeName.create())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testEmptyAttributeNameList_fails() {
        assertThatThrownBy(() -> NestedAttributeName.builder().elements(Collections.emptyList()).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NestedAttributeName.create(Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testEmptyAttributeNames_allowed() {
        NestedAttributeName.builder().elements(Collections.singletonList("")).build();
        NestedAttributeName.create(Collections.singletonList(""));
    }

    @Test
    public void testEmptyAttributeNames_toString() {
        NestedAttributeName nestedAttribute = NestedAttributeName.create("Attribute1", "Attribute*2", "Attribute-3");
        assertThat(nestedAttribute.toString()).isEqualTo("Attribute1.Attribute*2.Attribute-3");
    }

    @Test
    public void toBuilder() {
        NestedAttributeName builtObject = NestedAttributeName.builder().addElement(ATTRIBUTE_NAME).build();
        NestedAttributeName copiedObject = builtObject.toBuilder().build();
        assertThat(copiedObject).isEqualTo(builtObject);
    }

}
