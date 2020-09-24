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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;

/**
 * Wrapper method to get Projection Expression Name map and Projection Expressions from NestedAttributeNames.
 */
@SdkInternalApi
public class ProjectionExpressionConvertor {

    private static final String AMZN_MAPPED = "#AMZN_MAPPED_";
    private static final UnaryOperator<String> PROJECTION_EXPRESSION_KEY_MAPPER = k -> AMZN_MAPPED + cleanAttributeName(k);
    private final List<NestedAttributeName> nestedAttributeNames;

    private ProjectionExpressionConvertor(List<NestedAttributeName> nestedAttributeNames) {
        this.nestedAttributeNames = nestedAttributeNames;
    }

    public static ProjectionExpressionConvertor create(List<NestedAttributeName> nestedAttributeNames) {
        return new ProjectionExpressionConvertor(nestedAttributeNames);
    }
    
    private static Optional<Map<String, String>> convertToExpressionNameMap(NestedAttributeName attributeName) {
        List<String> nestedAttributeNames = attributeName.elements();
        if (nestedAttributeNames != null) {
            Map<String, String> resultNameMap = new LinkedHashMap<>();
            nestedAttributeNames.stream().forEach(nestedAttribute ->
                            resultNameMap.put(PROJECTION_EXPRESSION_KEY_MAPPER.apply(nestedAttribute), nestedAttribute));
            return Optional.of(resultNameMap);
        }
        return Optional.empty();
    }

    private static Optional<String> convertToNameExpression(NestedAttributeName nestedAttributeName) {

        String name = nestedAttributeName.elements().stream().findFirst().orElse(null);

        List<String> nestedAttributes = null;
        if (nestedAttributeName.elements().size() > 1) {
            nestedAttributes = nestedAttributeName.elements().subList(1, nestedAttributeName.elements().size());
        }
        if (name != null) {
            List<String> hashSeparatedNestedStringList =
                    new ArrayList<>(Arrays.asList(PROJECTION_EXPRESSION_KEY_MAPPER.apply(name)));
            if (nestedAttributes != null) {
                nestedAttributes.stream().forEach(hashSeparatedNestedStringList::add);
            }
            return Optional.of(String.join(".".concat(AMZN_MAPPED), hashSeparatedNestedStringList));
        }
        return Optional.empty();
    }

    public List<NestedAttributeName> nestedAttributeNames() {
        return nestedAttributeNames;
    }

    public Map<String, String> convertToExpressionMap() {
        Map<String, String> attributeNameMap = new LinkedHashMap<>();
        if (this.nestedAttributeNames() != null) {
            this.nestedAttributeNames().stream().forEach(attribs -> convertToExpressionNameMap(attribs)
                    .ifPresent(attributeNameMap::putAll));
        }
        return attributeNameMap;
    }

    public Optional<String> convertToProjectionExpression() {
        if (nestedAttributeNames != null) {
            List<String> expressionList = new ArrayList<>();
            this.nestedAttributeNames().stream().filter(Objects::nonNull)
                    .filter(item -> item.elements() != null && !item.elements().isEmpty())
                    .forEach(attributeName -> convertToNameExpression(attributeName)
                            .ifPresent(expressionList::add));
            String joinedExpression = String.join(",", expressionList.stream()
                    .distinct().collect(Collectors.toList()));
            return Optional.ofNullable(joinedExpression.isEmpty() ? null : joinedExpression);
        }
        return Optional.empty();
    }

}
