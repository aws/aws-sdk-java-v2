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

package software.amazon.awssdk.codegen.poet.rules;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.jr.stree.JrsArray;
import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsNumber;
import com.fasterxml.jackson.jr.stree.JrsObject;
import com.fasterxml.jackson.jr.stree.JrsString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.rules.endpoints.ConditionModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility methods for parsing endpoint rules expressions.
 */
public final class ExpressionParser {

    private ExpressionParser() {
    }

    /**
     * Parses a rule set expression. Each expression consist of a list of conditions plus a body that can be either a tree node
     * grouping more rule set expressions or a leaf node which is either an endpoint or an error.
     */
    public static RuleSetExpression parseRuleSetExpression(RuleModel model) {
        RuleSetExpression.Builder builder = RuleSetExpression.builder();
        String type = model.getType();
        List<RuleExpression> conditions = groupExpressions(model.getConditions());
        builder.conditions(conditions);
        if ("error".equals(type)) {
            builder.error(new ErrorExpression(parseErrorExpression(model.getError())));
        } else if ("endpoint".equals(type)) {
            builder.endpoint(parseEndpointExpression(model.getEndpoint()));
        } else {
            Validate.isTrue("tree".equals(type),
                            "Unknown type, expected any of [error, endpoint, tree] but got: " + type);
            for (RuleModel child : model.getRules()) {
                builder.addChildren(parseRuleSetExpression(child));
            }
        }
        return builder.build();
    }

    /**
     * Takes the list of condition models from a rule and group them creating a single boolean "and" expression for each
     * consecutive condition without an assign and a single "let" expression for each consecutive condition with an assign.
     */
    private static List<RuleExpression> groupExpressions(List<ConditionModel> conditions) {
        List<RuleExpression> result = new ArrayList<>();
        BooleanAndExpression.Builder andBuilder = null;
        LetExpression.Builder letBuilder = null;
        Boolean readingAnd = null;
        for (ConditionModel model : conditions) {
            RuleExpression expression = parseConditionalExpression(model);
            String assign = model.getAssign();
            if (assign != null) {
                if (readingAnd == null) {
                    letBuilder = LetExpression.builder();
                } else if (Boolean.TRUE.equals(readingAnd)) {
                    result.add(andBuilder.build().simplify());
                    letBuilder = LetExpression.builder();
                    andBuilder = null;
                }
                readingAnd = false;
                letBuilder.putBinding(assign, expression);
            } else {
                if (readingAnd == null) {
                    andBuilder = BooleanAndExpression.builder();
                } else if (Boolean.FALSE.equals(readingAnd)) {
                    result.add(letBuilder.build().simplify());
                    andBuilder = BooleanAndExpression.builder();
                    letBuilder = null;
                }
                readingAnd = true;
                andBuilder.addExpression(expression);
            }
        }
        if (andBuilder != null) {
            result.add(andBuilder.build().simplify());
        }
        if (letBuilder != null) {
            result.add(letBuilder.build().simplify());
        }
        return result;
    }

    /**
     * Parses an error expression, e.g.,
     * <ul>
     *     <li><pre>"error": "Accelerate cannot be used with FIPS"</pre></li>
     *     <li><pre>"error": "Custom endpoint `{Endpoint}` was not a valid URI"</pre></li>
     * </ul>
     */
    private static RuleExpression parseErrorExpression(String error) {
        return parseStringValue(error);
    }

    private static RuleExpression parseConditionalExpression(ConditionModel model) {
        String fn = model.getFn();
        return getFunctionCallExpression(fn, model.getArgv());
    }

    /**
     * Parses a {@code TreeNode} into a {@code RuleExpression}.
     *
     * <p>Object values are parsed into either,
     *
     * <ul>
     *     <li>Function call expression, e.g., {@code {"fn": "isSet", "argv": [⋯]}}</li>
     *     <li>Variable reference expression, e.g., {@code {"ref": "Region"}}</li>
     *     <li>A properties expression (key value pairs) used for endpoint</li>
     * </ul>
     *
     * <p>A literal string, number, or, boolean are parsed into their corresponding literal expressions.
     *
     * <p>An array is parsed into a list expression.
     */
    private static RuleExpression parseExpressionFrom(TreeNode node) {
        if (node.isObject()) {
            JrsObject obj = (JrsObject) node;
            if (obj.get("fn") != null) {
                String fn = obj.get("fn").asText();
                return getFunctionCallExpression(fn, nodeArrayToList(node.get("argv")));
            }
            if (obj.get("ref") != null) {
                String ref = obj.get("ref").asText();
                return new VariableReferenceExpression(ref);
            }
            return parsePropertiesExpression(obj);
        }
        if (node.isValueNode()) {
            JsonToken token = node.asToken();
            switch (token) {
                case VALUE_STRING:
                    return parseStringValue(((JrsString) node).getValue());
                case VALUE_NUMBER_INT:
                    return new LiteralIntegerExpression(((JrsNumber) node).getValue().intValue());
                case VALUE_TRUE:
                case VALUE_FALSE:
                    return new LiteralBooleanExpression(((JrsBoolean) node).booleanValue());
                default:
                    throw new RuntimeException("Don't know how to create expression JSON type " + token);
            }
        }
        if (node.isArray()) {
            JrsArray array = (JrsArray) node;
            ListExpression.Builder builder = ListExpression.builder();
            for (int idx = 0; idx < array.size(); idx++) {
                builder.addExpression(parseExpressionFrom(array.get(idx)));
            }
            return builder.build();
        }
        throw new IllegalArgumentException("don't know how to convert from node: " + node);
    }

    /**
     * Parses a string value. A string value can be parsed into:
     *
     * <ul>
     *     <li>A literal string value, e.g., {@code "--x-s3"}</li>
     *     <li>A get attribute expression, e.g., {@code "{url#scheme}"}</li>
     *     <li>A get indexed attribute expression, e.g., {@code "resourceId[0]"}</li>
     *     <li>A string concatenation expression, e.g., {@code "https://{Bucket}.op-{outpostId}.{url#authority}"}</li>
     * </ul>
     */
    public static RuleExpression parseStringValue(String value) {
        if (value.indexOf('{') != -1 || value.indexOf('[') != -1) {
            Tokenizer tokenizer = new Tokenizer(value);
            RuleExpression expr;
            if (tokenizer.isReference()) {
                VariableReferenceExpression.Builder builder = VariableReferenceExpression.builder();
                tokenizer.consumeReferenceAccess(builder::variableName);
                expr = builder.build();
                if (tokenizer.atEof()) {
                    return expr;
                }
            } else if (tokenizer.isNamedAccess()) {
                MemberAccessExpression.Builder builder = MemberAccessExpression.builder();
                tokenizer.consumeNamedAccess((source, name) -> {
                    builder.source(new VariableReferenceExpression(source))
                           .name(name);
                });
                expr = builder.build();
                if (tokenizer.atEof()) {
                    return expr;
                }
            } else if (tokenizer.isIndexedAccess()) {
                IndexedAccessExpression.Builder indexedAccessBuilder = IndexedAccessExpression.builder();
                tokenizer.consumeIndexed((n, i) -> indexedAccessBuilder.source(new VariableReferenceExpression(n))
                                                                       .index(i));
                tokenizer.expectAtEof("indexed access");
                return indexedAccessBuilder.build();
            } else {
                expr = new LiteralStringExpression(tokenizer.next().value());
            }
            return parseStringConcat(tokenizer, expr);
        }
        return new LiteralStringExpression(value);
    }

    private static EndpointExpression parseEndpointExpression(EndpointModel model) {
        return EndpointExpression.builder()
                                 .url(parseExpressionFrom(model.getUrl()))
                                 .properties(parsePropertiesExpression(model.getProperties()))
                                 .headers(parseHeadersExpression(model.getHeaders()))
                                 .build();
    }

    private static HeadersExpression parseHeadersExpression(Map<String, List<TreeNode>> headers) {
        HeadersExpression.Builder builder = HeadersExpression.builder();
        if (headers != null) {
            headers.forEach((k, v) -> {
                ListExpression.Builder valueBuilder = ListExpression.builder();
                for (TreeNode node : v) {
                    valueBuilder.addExpression(parseExpressionFrom(node));
                }
                builder.putHeader(k, valueBuilder.build());
            });
        }
        return builder.build();
    }

    private static PropertiesExpression parsePropertiesExpression(Map<String, TreeNode> properties) {
        PropertiesExpression.Builder builder = PropertiesExpression.builder();
        if (properties != null) {
            properties.forEach((k, v) -> builder.putProperty(k, parseExpressionFrom(v)));
        }
        return builder.build();
    }

    private static RuleExpression parseStringConcat(Tokenizer tokenizer, RuleExpression expr) {
        StringConcatExpression.Builder concatBuilder = StringConcatExpression.builder();
        concatBuilder.addExpression(expr);
        while (!tokenizer.atEof()) {
            if (tokenizer.isReference()) {
                VariableReferenceExpression.Builder builder = VariableReferenceExpression.builder();
                tokenizer.consumeReferenceAccess(builder::variableName);
                concatBuilder.addExpression(builder.build());
            } else if (tokenizer.isNamedAccess()) {
                MemberAccessExpression.Builder builder = MemberAccessExpression.builder();
                tokenizer.consumeNamedAccess((source, name) -> {
                    builder.source(new VariableReferenceExpression(source))
                           .name(name);
                });
                concatBuilder.addExpression(builder.build());
            } else {
                concatBuilder.addExpression(new LiteralStringExpression(tokenizer.next().value()));
            }
        }
        return concatBuilder.build();
    }

    private static List<TreeNode> nodeArrayToList(TreeNode argv) {
        if (!argv.isArray()) {
            throw new IllegalArgumentException("expecting tree node array, got instead: " + argv.asToken());
        }
        JrsArray array = (JrsArray) argv;
        List<TreeNode> result = new ArrayList<>(array.size());
        for (int idx = 0; idx < array.size(); ++idx) {
            result.add(array.get(idx));
        }
        return result;
    }

    private static RuleExpression getFunctionCallExpression(String fn, List<TreeNode> argv) {
        if ("getAttr".equals(fn)) {
            return getAttrExpression(argv);
        }
        FunctionCallExpression.Builder builder = FunctionCallExpression.builder()
                                                                       .name(fn);
        for (TreeNode node : argv) {
            builder.addArgument(parseExpressionFrom(node));
        }
        return builder.build();
    }

    private static RuleExpression getAttrExpression(List<TreeNode> argv) {
        if (argv.size() != 2) {
            throw new IllegalArgumentException("getAttr expects two arguments");
        }
        TreeNode argv0 = argv.get(0);
        TreeNode argv1 = argv.get(1);
        RuleExpression variable = getReference(argv0);
        TreeNode nameNode = argv1;
        if (!(nameNode instanceof JrsString)) {
            throw new IllegalArgumentException("expecting node to be string, got instead starting token: " + nameNode.asToken());
        }
        MemberAccessExpression.Builder memberAccessBuilder = MemberAccessExpression.builder().source(variable);
        JrsString value = (JrsString) nameNode;
        Tokenizer tokenizer = new Tokenizer(value.getValue());
        if (tokenizer.isIndexedAccess()) {
            IndexedAccessExpression.Builder indexedAccessBuilder = IndexedAccessExpression.builder();
            tokenizer.consumeIndexed((n, i) -> indexedAccessBuilder.source(memberAccessBuilder.name(n).build())
                                                                   .index(i));
            tokenizer.expectAtEof("indexed access");
            return indexedAccessBuilder.build();
        }
        if (tokenizer.isIdentifier()) {
            tokenizer.consumeIdentifier(memberAccessBuilder::name);
            tokenizer.expectAtEof("member access");
            return memberAccessBuilder.build();
        }
        throw new IllegalArgumentException(
            String.format("Unexpected token parsing the second argument of getAttr expression: %s",
                          tokenizer.peek()));
    }

    private static RuleExpression getReference(TreeNode node) {
        if (!node.isObject()) {
            throw new IllegalArgumentException("expecting reference object, got instead: " + node);
        }
        JrsObject obj = (JrsObject) node;
        String reference = obj.get("ref").asText();
        return new VariableReferenceExpression(reference);
    }

    public static PropertiesExpression parsePropertiesExpression(JrsObject object) {
        PropertiesExpression.Builder builder = PropertiesExpression.builder();
        Iterator<String> fieldsIterator = object.fieldNames();
        while (fieldsIterator.hasNext()) {
            String name = fieldsIterator.next();
            builder.putProperty(name, parseExpressionFrom(object.get(name)));
        }
        return builder.build();
    }

}
