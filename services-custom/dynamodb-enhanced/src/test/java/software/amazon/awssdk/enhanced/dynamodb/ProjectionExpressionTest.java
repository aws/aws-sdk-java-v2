package software.amazon.awssdk.enhanced.dynamodb;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.ProjectionExpression;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectionExpressionTest {

    private static final String NESTING_SEPARATOR = ".";
    private static final String PROJ_EXP_SEPARATOR = ",";

    private static final Map<String, String> EXPECTED_ATTRIBUTE_NAMES = new HashMap<>();

    static {
        EXPECTED_ATTRIBUTE_NAMES.put("#AMZN_MAPPED_attribute", "attribute");
        EXPECTED_ATTRIBUTE_NAMES.put("#AMZN_MAPPED_Attribute", "Attribute");
        EXPECTED_ATTRIBUTE_NAMES.put("#AMZN_MAPPED_firstiteminlist[0]", "firstiteminlist[0]");
        EXPECTED_ATTRIBUTE_NAMES.put("#AMZN_MAPPED_March_2021", "March-2021");
        EXPECTED_ATTRIBUTE_NAMES.put("#AMZN_MAPPED_Why_Make_This_An_Attribute_Name", "Why.Make-This*An:Attribute:Name");
    }

    @Test
    public void nullInputSetsStateCorrectly() {
        ProjectionExpression projectionExpression = ProjectionExpression.create(null);

        assertThat(projectionExpression.expressionAttributeNames()).isEmpty();
        assertThat(projectionExpression.projectionExpressionAsString()).isEmpty();
    }

    @Test
    public void emptyInputSetsStateCorrectly() {
        ProjectionExpression projectionExpression = ProjectionExpression.create(new ArrayList<>());

        assertThat(projectionExpression.expressionAttributeNames()).isEmpty();
        assertThat(projectionExpression.projectionExpressionAsString()).isEmpty();
    }

    @Test
    public void severalTopLevelAttributes_handledCorrectly() {
        List<NestedAttributeName> attributeNames = EXPECTED_ATTRIBUTE_NAMES.values()
                                                                           .stream()
                                                                           .map(NestedAttributeName::create)
                                                                           .collect(Collectors.toList());

        String expectedProjectionExpression = EXPECTED_ATTRIBUTE_NAMES.keySet()
                                                                      .stream()
                                                                      .collect(Collectors.joining(PROJ_EXP_SEPARATOR));

        assertProjectionExpression(attributeNames, EXPECTED_ATTRIBUTE_NAMES, expectedProjectionExpression);
    }

    @Test
    public void severalNestedAttributes_handledCorrectly() {
        List<NestedAttributeName> attributeNames = Arrays.asList(NestedAttributeName.create(
            EXPECTED_ATTRIBUTE_NAMES.values()
                                    .stream()
                                    .collect(Collectors.toList())));

        String expectedProjectionExpression = EXPECTED_ATTRIBUTE_NAMES.keySet()
                                                                      .stream()
                                                                      .collect(Collectors.joining(NESTING_SEPARATOR));

        assertProjectionExpression(attributeNames, EXPECTED_ATTRIBUTE_NAMES, expectedProjectionExpression);
    }

    @Test
    public void nonUniqueAttributeNames_AreCollapsed() {
        Map<String, String> expectedAttributeNames = new HashMap<>();
        expectedAttributeNames.put("#AMZN_MAPPED_attribute", "attribute");

        List<NestedAttributeName> attributeNames = Arrays.asList(
            NestedAttributeName.create("attribute", "attribute"),
            NestedAttributeName.create("attribute")
        );

        String expectedProjectionExpression = "#AMZN_MAPPED_attribute.#AMZN_MAPPED_attribute,#AMZN_MAPPED_attribute";

        assertProjectionExpression(attributeNames, expectedAttributeNames, expectedProjectionExpression);
    }

    @Test
    public void nonUniquePlaceholders_AreDisambiguated() {
        Map<String, String> expectedAttributeNames = new HashMap<>();
        expectedAttributeNames.put("#AMZN_MAPPED_0_attribute_03", "attribute-03");
        expectedAttributeNames.put("#AMZN_MAPPED_1_attribute_03", "attribute.03");
        expectedAttributeNames.put("#AMZN_MAPPED_2_attribute_03", "attribute:03");

        List<NestedAttributeName> attributeNames = Arrays.asList(
            NestedAttributeName.create("attribute-03", "attribute.03"),
            NestedAttributeName.create("attribute:03")
        );

        String expectedProjectionExpression = "#AMZN_MAPPED_0_attribute_03.#AMZN_MAPPED_1_attribute_03,"
                                              + "#AMZN_MAPPED_2_attribute_03";

        assertProjectionExpression(attributeNames, expectedAttributeNames, expectedProjectionExpression);
    }

    private void assertProjectionExpression(List<NestedAttributeName> attributeNames,
                                            Map<String, String> expectedAttributeNames,
                                            String expectedProjectionExpression) {
        ProjectionExpression projectionExpression = ProjectionExpression.create(attributeNames);

        Map<String, String> expressionAttributeNames = projectionExpression.expressionAttributeNames();
        Optional<String> projectionExpressionString = projectionExpression.projectionExpressionAsString();

        assertThat(projectionExpressionString.get()).isEqualTo(expectedProjectionExpression);
        assertThat(expressionAttributeNames).isEqualTo(expectedAttributeNames);
    }
}
