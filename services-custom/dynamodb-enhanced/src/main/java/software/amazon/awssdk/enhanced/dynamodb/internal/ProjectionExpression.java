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

import java.util.Collections;
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
    // Placeholder keys are built from base attribute names only (list-index suffixes already removed).
    private static final UnaryOperator<String> PROJECTION_EXPRESSION_KEY_MAPPER = k -> AMZN_MAPPED + cleanAttributeName(k);

    private final Optional<String> projectionExpressionAsString;
    private final Map<String, String> expressionAttributeNames;

    private ProjectionExpression(List<NestedAttributeName> nestedAttributeNames) {
        this.expressionAttributeNames = createAttributePlaceholders(nestedAttributeNames);
        this.projectionExpressionAsString = buildProjectionExpression(nestedAttributeNames, this.expressionAttributeNames);
    }

    public static ProjectionExpression create(List<NestedAttributeName> nestedAttributeNames) {
        return new ProjectionExpression(nestedAttributeNames);
    }

    public Map<String, String> expressionAttributeNames() {
        return this.expressionAttributeNames;
    }

    public Optional<String> projectionExpressionAsString() {
        return this.projectionExpressionAsString;
    }

    /**
     * Creates a map of modified attribute/placeholder name -> real attribute name based on what is essentially a list of list of
     * attribute names. Duplicates are removed from the list of attribute names and then the names are transformed
     * into DDB-compatible 'placeholders' using the supplied function, resulting in a
     * map of placeholder name -> list of original attribute names that resolved to that placeholder.
     * If different original attribute names end up having the same placeholder name, a disambiguator is added to those
     * placeholders to make them unique and the number of map entries expand with the length of that list; however this is
     * a rare use-case and normally it's a 1:1 relation.
     * <p>
     * Trailing list-dereference suffixes such as {@code [0]} or {@code [1][2]} are stripped before placeholder generation
     * so ExpressionAttributeNames map only the base attribute name. The suffixes are re-attached when building the
     * projection expression string.
     */
    private static Map<String, String> createAttributePlaceholders(List<NestedAttributeName> nestedAttributeNames) {
        if (CollectionUtils.isNullOrEmpty(nestedAttributeNames)) {
            return new HashMap<>();
        }

        Map<String, List<String>> placeholderToAttributeNames =
            nestedAttributeNames.stream()
                                .flatMap(n -> n.elements().stream())
                                .map(element -> parseTrailingListDereferences(element).left())
                                .distinct()
                                .collect(Collectors.groupingBy(PROJECTION_EXPRESSION_KEY_MAPPER, Collectors.toList()));

        return Collections.unmodifiableMap(
            placeholderToAttributeNames.entrySet()
                                       .stream()
                                       .flatMap(entry -> disambiguateNonUniquePlaceholderNames(entry.getKey(),
                                                                                               entry.getValue()))
                                       .collect(Collectors.toMap(Pair::left, Pair::right)));
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

    /**
     * The projection expression contains only placeholder names, and is based on the list if nested attribute names, which
     * are converted into string representations with each attribute name replaced by its placeholder name as specified
     * in the expressionAttributeNames map. Because we need to find the placeholder value of an attribute, the
     * expressionAttributeNames map must be reversed before doing a lookup.
     */
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
                                  .map(element -> {
                                      // left = base attribute name; right = trailing list-index suffix (e.g. "[1][2]", or "").
                                      Pair<String, String> baseNameAndSuffix = parseTrailingListDereferences(element);
                                      return attributeToSanitizedMap.get(baseNameAndSuffix.left())
                                             + baseNameAndSuffix.right();
                                  })
                                  .collect(Collectors.joining("."));
    }

    /**
     * Parses a path element into its base attribute name and any trailing list-dereference suffix such as {@code [0]}
     * or {@code [1][2]}.
     *
     * @return a {@link Pair} where {@link Pair#left()} is the base attribute name and {@link Pair#right()} is the
     *         list-dereference suffix (empty when the element has no trailing list indexes)
     */
    private static Pair<String, String> parseTrailingListDereferences(String attributeName) {
        int suffixStart = trailingListDereferenceStart(attributeName);
        if (suffixStart == -1) {
            return Pair.of(attributeName, "");
        }
        return Pair.of(attributeName.substring(0, suffixStart), attributeName.substring(suffixStart));
    }

    /**
     * Finds the start index of a trailing sequence of list dereferences such as {@code [0]} or {@code [1][2]}.
     * Returns -1 if the name does not end in a valid list-dereference suffix.
     */
    private static int trailingListDereferenceStart(String attributeName) {
        int position = attributeName.length();
        int suffixStart = position;

        while (position > 0 && attributeName.charAt(position - 1) == ']') {
            int openBracketIndex = attributeName.lastIndexOf('[', position - 1);
            if (openBracketIndex == -1 || openBracketIndex == position - 1 ||
                !containsOnlyDigits(attributeName, openBracketIndex + 1, position - 1)) {
                return -1;
            }
            suffixStart = openBracketIndex;
            position = openBracketIndex;
        }

        return suffixStart == attributeName.length() ? -1 : suffixStart;
    }

    private static boolean containsOnlyDigits(String value, int beginIndex, int endIndex) {
        for (int i = beginIndex; i < endIndex; i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return beginIndex < endIndex;
    }

}
