/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import software.amazon.awssdk.jmespath.InvalidTypeException;
import software.amazon.awssdk.jmespath.JmesPathAndExpression;
import software.amazon.awssdk.jmespath.JmesPathContainsFunction;
import software.amazon.awssdk.jmespath.JmesPathExpression;
import software.amazon.awssdk.jmespath.JmesPathField;
import software.amazon.awssdk.jmespath.JmesPathFilter;
import software.amazon.awssdk.jmespath.JmesPathFlatten;
import software.amazon.awssdk.jmespath.JmesPathIdentity;
import software.amazon.awssdk.jmespath.JmesPathLengthFunction;
import software.amazon.awssdk.jmespath.JmesPathLiteral;
import software.amazon.awssdk.jmespath.JmesPathMultiSelectList;
import software.amazon.awssdk.jmespath.JmesPathNotExpression;
import software.amazon.awssdk.jmespath.JmesPathProjection;
import software.amazon.awssdk.jmespath.JmesPathSubExpression;
import software.amazon.awssdk.jmespath.JmesPathValueProjection;
import software.amazon.awssdk.jmespath.OpEquals;
import software.amazon.awssdk.jmespath.OpGreaterThan;
import software.amazon.awssdk.jmespath.OpGreaterThanOrEqualTo;
import software.amazon.awssdk.jmespath.OpLessThan;
import software.amazon.awssdk.jmespath.OpLessThanOrEqualTo;
import software.amazon.awssdk.jmespath.OpNotEquals;


public class AstJsonToAstJava {

    public static JmesPathExpression fromAstJsonToAstJava(JsonNode jsonNode) throws InvalidTypeException {

        switch (jsonNode.get("type").asText()) {
            case "subexpression":
                return jsonToSubExpression(jsonNode);
            case "field":
                return jsonToField(jsonNode);
            case "projection":
                return jsonToProjection(jsonNode);
            case "flatten":
                return jsonToFlatten(jsonNode);
            case "value_projection":
                return jsonToValueProjection(jsonNode);
            case "filter_projection":
                return jsonToFilterProjection(jsonNode);
            case "and_expression":
                return jsonToAndExpression(jsonNode);
            case "not_expression":
                return jsonToNotExpression(jsonNode);
            case "multi_select_list":
                return jsonToMultiSelectList(jsonNode);
            case "comparator":
                return jsonToComparator(jsonNode);
            case "pipe":
                //Pipe expression is similar to a sub-expression with a distinction that
                // a sub-expression restricts the type of expression that can be on rhs
                return jsonToSubExpression(jsonNode);
            case "identity":
                return new JmesPathIdentity();
            case "function_expression":
                return jsonToFunctionExpression(jsonNode);
            case "literal":
                return jsonToLiteral(jsonNode);
            default:
                throw new InvalidTypeException("JsonNode type not found");
        }
    }

    private static JmesPathExpression jsonToLiteral(JsonNode jsonNode) {
        return new JmesPathLiteral(jsonNode.get("value"));
    }

    private static JmesPathExpression jsonToField(JsonNode jsonNode) {
        return new JmesPathField(jsonNode.get("value").asText());
    }

    private static JmesPathExpression jsonToFlatten(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        return new JmesPathFlatten(args.get(0));
    }

    private static JmesPathExpression jsonToFunctionExpression(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        switch (jsonNode.get("value").asText()) {
            case "length":
                return new JmesPathLengthFunction(args);
            case "contains":
                return new JmesPathContainsFunction(args);
            default:
                return null;
        }
    }

    private static JmesPathExpression jsonToFilterProjection(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        return new JmesPathFilter(args.get(0), args.get(1), args.get(2));
    }

    private static JmesPathExpression jsonToValueProjection(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        return new JmesPathValueProjection(args.get(0), args.get(1));
    }

    private static JmesPathExpression jsonToProjection(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        return new JmesPathProjection(args.get(0), args.get(1));
    }

    private static JmesPathExpression jsonToSubExpression(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        return new JmesPathSubExpression(args);
    }

    private static JmesPathExpression jsonToComparator(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        switch (jsonNode.get("value").asText()) {
            case "eq":
                return new OpEquals(args.get(0), args.get(1));
            case "ne":
                return new OpNotEquals(args.get(0), args.get(1));
            case "gt":
                return new OpGreaterThan(args.get(0), args.get(1));
            case "lt":
                return new OpLessThan(args.get(0), args.get(1));
            case "gte":
                return new OpGreaterThanOrEqualTo(args.get(0), args.get(1));
            case "lte":
                return new OpLessThanOrEqualTo(args.get(0), args.get(1));
            default:
                throw new InvalidTypeException("Didn't match any of the expected comparator type");
        }
    }

    private static JmesPathExpression jsonToAndExpression(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        return new JmesPathAndExpression(args.get(0), args.get(1));
    }

    private static JmesPathExpression jsonToNotExpression(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        return new JmesPathNotExpression(args.get(0));
    }

    private static JmesPathExpression jsonToMultiSelectList(JsonNode jsonNode) {
        List<JmesPathExpression> args = getChildren(jsonNode);
        return new JmesPathMultiSelectList(args);
    }

    private static List<JmesPathExpression> getChildren(JsonNode jsonNode) {
        if (jsonNode.get("children").size() < 1) {
            throw new RuntimeException("Expected one or more arguments");
        }
        Iterator<JsonNode> children = jsonNode.get("children").elements();
        final List<JmesPathExpression> childrenList = new ArrayList<>();
        while (children.hasNext()) {
            childrenList.add(fromAstJsonToAstJava(children.next()));
        }
        return childrenList;
    }

}
