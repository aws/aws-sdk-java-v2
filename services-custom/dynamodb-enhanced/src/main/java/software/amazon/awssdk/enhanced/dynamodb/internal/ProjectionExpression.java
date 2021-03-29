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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.cleanAttributeName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * This class represents the concept of a projection expression, which allows the user to specify which specific attributes
 * should be returned when a table is queried. By default, all attribute names in a projection expression are replaced with
 * a cleaned placeholder version of itself, prefixed with <i>#AMZN_MAPPED</i>.
 * <p>
 * A ProjectionExpression can return a correctly formatted projection expression string
 * containing placeholder names (see {@link #projectionExpressionAsString()}), as well as the expression attribute names map which
 * contains the mapping from the placeholder attribute name to the actual attribute name (see
 * {@link #expressionAttributeNames()}).
 * <p>
 * <b>Resolving duplicates</b>
 * <ul>
 *     <li>If the input to the ProjectionExpression contains the same attribute name in more than one place, independent of
 *     nesting level, it will be mapped to a single placeholder</li>
 *     <li>If two attributes resolves to the same placeholder name, a disambiguator is added to the placeholder in order to
 *     make it unique.</li>
 * </ul>
 * <p>
 * <b>Placeholder conversion examples</b>
 * <ul>
 *     <li>'MyAttribute' maps to {@code #AMZN_MAPPED_MyAttribute}</li>
 *     <li>'MyAttribute' appears twice in input but maps to only one entry {@code #AMZN_MAPPED_MyAttribute}.</li>
 *     <li>'MyAttribute-1' maps to {@code #AMZN_MAPPED_MyAttribute_1}</li>
 *     <li>'MyAttribute-1' and 'MyAttribute.1' in the same input maps to {@code #AMZN_MAPPED_0_MyAttribute_1} and
 *     {@code #AMZN_MAPPED_1_MyAttribute_1}</li>
 * </ul>
 * <b>Projection expression usage example</b>
 * <pre>
 * {@code
 * List<NestedAttributeName> attributeNames = Arrays.asList(
 *     NestedAttributeName.create("MyAttribute")
 *     NestedAttributeName.create("MyAttribute.WithDot", "MyAttribute.03"),
 *     NestedAttributeName.create("MyAttribute:03, "MyAttribute")
 * );
 * ProjectionExpression projectionExpression = ProjectionExpression.create(attributeNames);
 * Map<String, String> expressionAttributeNames = projectionExpression.expressionAttributeNames();
 * Optional<String> projectionExpressionString = projectionExpression.projectionExpressionAsString();
 * }
 *
 * results in
 *
 * expressionAttributeNames: {
 *    #AMZN_MAPPED_MyAttribute : MyAttribute,
 *    #AMZN_MAPPED_MyAttribute_WithDot : MyAttribute.WithDot}
 *    #AMZN_MAPPED_0_MyAttribute_03 : MyAttribute.03}
 *    #AMZN_MAPPED_1_MyAttribute_03 : MyAttribute:03}
 * }
 * and
 *
 * projectionExpressionString: "#AMZN_MAPPED_MyAttribute,#AMZN_MAPPED_MyAttribute_WithDot.#AMZN_MAPPED_0_MyAttribute_03,
 *                              #AMZN_MAPPED_1_MyAttribute_03.#AMZN_MAPPED_MyAttribute"
 * </pre>
 * <p>
 * For more information, see <a href=
 * "https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ProjectionExpressions.html"
 * >Projection Expressions</a> in the <i>Amazon DynamoDB Developer Guide</i>.
 * </p>
 */
@SdkInternalApi
public class ProjectionExpression {

    private static final String AMZN_MAPPED = "#AMZN_MAPPED_";
    private static final UnaryOperator<String> PROJECTION_EXPRESSION_KEY_MAPPER = k -> AMZN_MAPPED + cleanAttributeName(k);

    private final Optional<String> projectionExpression;
    private final Map<String, String> expressionAttributeNames;

    private ProjectionExpression(List<NestedAttributeName> nestedAttributeNames) {
        this.expressionAttributeNames = createAttributePlaceholders(nestedAttributeNames);
        this.projectionExpression = buildProjectionExpression(nestedAttributeNames, this.expressionAttributeNames);
    }

    public static ProjectionExpression create(List<NestedAttributeName> nestedAttributeNames) {
        return new ProjectionExpression(nestedAttributeNames);
    }

    public Map<String, String> expressionAttributeNames() {
        return this.expressionAttributeNames;
    }

    public Optional<String> projectionExpressionAsString() {
        return this.projectionExpression;
    }

    private static Map<String, String> createAttributePlaceholders(List<NestedAttributeName> nestedAttributeNames) {
        if (CollectionUtils.isNullOrEmpty(nestedAttributeNames)) {
            return new HashMap<>();
        }

        Map<String, List<String>> placeholderToAttributeNames =
            nestedAttributeNames.stream()
                                .flatMap(n -> n.elements().stream())
                                .distinct()
                                .collect(Collectors.groupingBy(PROJECTION_EXPRESSION_KEY_MAPPER, Collectors.toList()));

        return placeholderToAttributeNames.entrySet()
                                          .stream()
                                          .flatMap(entry -> disambiguateNonUniquePlaceholderNames(entry.getKey(),
                                                                                                  entry.getValue()))
                                          .collect(Collectors.toMap(Pair::left, Pair::right));
    }

    private static Stream<Pair<String, String>> disambiguateNonUniquePlaceholderNames(String placeholder, List<String> values) {
        if (values.size() == 1) {
            return Stream.of(Pair.of(placeholder, values.get(0)));
        }
        return IntStream.range(0, values.size())
                        .mapToObj(index -> Pair.of(addDisambiguator(placeholder, index), values.get(index)));
    }

    private static String addDisambiguator(String placeholder, int index) {
        return AMZN_MAPPED + index + "_" + placeholder.substring(AMZN_MAPPED.length());
    }

    private static Optional<String> buildProjectionExpression(List<NestedAttributeName> nestedAttributeNames,
                                                              Map<String, String> expressionAttributeNames) {
        if (CollectionUtils.isNullOrEmpty(nestedAttributeNames)) {
            return Optional.empty();
        }

        Map<String, String> attributeToPlaceholderNames = CollectionUtils.inverseMap(expressionAttributeNames);

        return Optional.of(nestedAttributeNames.stream()
                                               .map(attributeName -> convertToNameExpression(attributeName,
                                                                                             attributeToPlaceholderNames))
                                               .distinct()
                                               .collect(Collectors.joining(",")));
    }

    private static String convertToNameExpression(NestedAttributeName nestedAttributeName,
                                                  Map<String, String> attributeToSanitizedMap) {
        return nestedAttributeName.elements()
                                  .stream()
                                  .map(attributeToSanitizedMap::get)
                                  .collect(Collectors.joining("."));
    }

}
