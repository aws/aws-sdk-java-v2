package software.amazon.awssdk.enhanced.dynamodb;

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.ProjectionExpressionConvertor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;

public class ProjectionExpressionConvertorTest {

    public static final String MAPPED_INDICATOR = "#AMZN_MAPPED_";
    public static final String NESTING_SEPARATOR = ".";

    @Test
    public void testAttributeNameWithNoNestedAttributes() {
        final String keyName = "fieldKey";
        NestedAttributeName attributeName = NestedAttributeName.builder().elements(keyName).build();
        ProjectionExpressionConvertor expressionConvertor = ProjectionExpressionConvertor.create(Arrays.asList(attributeName));
        final Map<String, String> stringStringMap = expressionConvertor.convertToExpressionMap();
        final Optional<String> toNameExpression = expressionConvertor.convertToProjectionExpression();
        Map<String, String> expectedmap = new HashMap<>();
        expectedmap.put(MAPPED_INDICATOR + keyName, keyName);
        assertThat(stringStringMap).isEqualTo(expectedmap);
        assertThat(toNameExpression.get()).contains((MAPPED_INDICATOR + keyName));
    }

    @Test
    public void testAttributeNameWithNestedNestedAttributes() {
        final String keyName = "fieldKey";
        final String nestedAttribute = "levelOne";
        NestedAttributeName attributeName = NestedAttributeName.builder().addElements(keyName, nestedAttribute).build();
        ProjectionExpressionConvertor expressionConvertor = ProjectionExpressionConvertor.create(Arrays.asList(attributeName));
        final Map<String, String> stringStringMap = expressionConvertor.convertToExpressionMap();
        final Optional<String> toNameExpression = expressionConvertor.convertToProjectionExpression();
        Map<String, String> expectedmap = new HashMap<>();
        expectedmap.put(MAPPED_INDICATOR + keyName, keyName);
        expectedmap.put(MAPPED_INDICATOR + nestedAttribute, nestedAttribute);
        assertThat(stringStringMap).isEqualTo(expectedmap);
        assertThat(toNameExpression.get()).contains(MAPPED_INDICATOR + keyName + NESTING_SEPARATOR + MAPPED_INDICATOR + nestedAttribute);
    }

    @Test
    public void testAttributeNameWithNullAttributeName() {
        assertFails(() -> NestedAttributeName.builder().addElement(null).build());

    }

    @Test
    public void testAttributeNameWithNullElementsForNestingElement() {
        assertFails(() -> NestedAttributeName.builder()
                .elements("foo").addElement(null).build());
    }

    @Test
    public void toBuilder() {
        NestedAttributeName builtObject = NestedAttributeName.builder().addElement("foo").build();
        NestedAttributeName copiedObject = builtObject.toBuilder().build();
        assertThat(copiedObject).isEqualTo(builtObject);
    }
}
