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
import com.fasterxml.jackson.jr.stree.JrsValue;
import com.squareup.javapoet.CodeBlock;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ConditionModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterDeprecatedModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.core.rules.Condition;
import software.amazon.awssdk.core.rules.EndpointResult;
import software.amazon.awssdk.core.rules.EndpointRuleset;
import software.amazon.awssdk.core.rules.Expr;
import software.amazon.awssdk.core.rules.FnNode;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.Parameter;
import software.amazon.awssdk.core.rules.ParameterType;
import software.amazon.awssdk.core.rules.Parameters;
import software.amazon.awssdk.core.rules.Rule;

public class RuleSetCreationSpec {
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final EndpointRuleSetModel ruleSetModel;

    public RuleSetCreationSpec(IntermediateModel intermediateModel) {
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
        this.ruleSetModel = intermediateModel.getEndpointRuleSetModel();
    }

    public CodeBlock ruleSetCreationExpr() {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.builder()", EndpointRuleset.class)
            .add(".version($S)", ruleSetModel.getVersion())
            .add(".serviceId($S)", ruleSetModel.getServiceId())
            .add(".parameters($L)", parameters(ruleSetModel.getParameters()));

        ruleSetModel.getRules().forEach(rm -> b.add(".addRule($L)", rule(rm)));

        b.add(".build()");
        return b.build();
    }

    private CodeBlock parameters(Map<String, ParameterModel> params) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.builder()", Parameters.class);

        params.forEach((name, model) -> {
            b.add(".addParameter($L)", parameter(name, model));
        });

        b.add(".build()");

        return b.build();
    }

    private CodeBlock parameter(String name, ParameterModel model) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.builder()", Parameter.class)
            .add(".name($S)", name)
            .add(".type($T.fromValue($S))", ParameterType.class, model.getType())
            .add(".required($L)", Boolean.TRUE.equals(model.isRequired()));

        if (model.getBuiltIn() != null) {
            b.add(".builtIn($S)", model.getBuiltIn());
        }

        if (model.getDocumentation() != null) {
            b.add(".documentation($S)", model.getDocumentation());
        }

        if (model.getDefault() != null) {
            TreeNode defaultValue = model.getDefault();
            JsonToken token = defaultValue.asToken();
            CodeBlock value;
            if (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE) {
                value = endpointRulesSpecUtils.valueCreationCode("boolean",
                                                                 CodeBlock.builder()
                                                                          .add("$L", ((JrsBoolean) defaultValue).booleanValue())
                                                                          .build());
            } else if (token == JsonToken.VALUE_STRING) {
                value = endpointRulesSpecUtils.valueCreationCode("string",
                                                                 CodeBlock.builder()
                                                                          .add("$S", ((JrsString) defaultValue).getValue())
                                                                          .build());
            } else {
                throw new RuntimeException("Can't set default value type " + token.name());
            }
            b.add(".defaultValue($L)", value);
        }

        if (model.getDeprecated() != null) {
            ParameterDeprecatedModel deprecated = model.getDeprecated();
            b.add(".deprecated(new $T($S, $S))", Parameter.Deprecated.class, deprecated.getMessage(), deprecated.getSince());
        }

        b.add(".build()");

        return b.build();
    }

    private CodeBlock rule(RuleModel model) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.builder()", Rule.class);

        model.getConditions().forEach(c -> b.add(".addCondition($L)", condition(c)));

        if ("error".equals(model.getType())) {
            b.add(".error($S)", model.getError());
        } else if ("tree".equals(model.getType())) {
            CodeBlock.Builder rulesArray = CodeBlock.builder()
                                                    .add("$T.asList(", Arrays.class);

            int nRules = model.getRules().size();
            for (int i = 0; i < nRules; ++i) {
                rulesArray.add(rule(model.getRules().get(i)));
                if (i + 1 < nRules) {
                    rulesArray.add(", ");
                }
            }
            rulesArray.add(")");

            b.add(".treeRule($L)", rulesArray.build());
        } else if ("endpoint".equals(model.getType())) {
            CodeBlock endpoint = endpoint(model.getEndpoint());
            b.add(".endpoint($L)", endpoint);
        }

        return b.build();
    }

    private CodeBlock endpoint(EndpointModel model) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.builder()", EndpointResult.class);

        TreeNode url = model.getUrl();
        b.add(".url($L)", expr(url));

        b.add(".build()");
        return b.build();
    }

    private CodeBlock condition(ConditionModel model) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.builder()", Condition.class)
            .add(".fn($L.validate())", fnNode(model));

        if (model.getAssign() != null) {
            b.add(".result($S)", model.getAssign());
        }

        b.add(".build()");

        return b.build();
    }

    private CodeBlock fnNode(ConditionModel model) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.builder()", FnNode.class)
            .add(".fn($S)", model.getFn())
            .add(".argv($T.asList(", Arrays.class);

        List<TreeNode> args = model.getArgv();
        for (int i = 0; i < args.size(); ++i) {
            b.add("$L", expr(args.get(i)));
            if (i + 1 < args.size()) {
                b.add(",");
            }
        }
        b.add("))");

        b.add(".build()");

        return b.build();
    }

    private CodeBlock expr(TreeNode n) {
        if (n.isValueNode()) {
            return valueExpr((JrsValue) n);
        }

        if (n.isObject()) {
            return objectExpr((JrsObject) n);
        }

        throw new RuntimeException("Don't know how to create expression from " + n);
    }

    private CodeBlock valueExpr(JrsValue n) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.of(", Expr.class);
        JsonToken token = n.asToken();
        switch (token) {
            case VALUE_STRING:
                b.add("$S", ((JrsString) n).getValue());
                break;
            case VALUE_NUMBER_INT:
                b.add("$L", ((JrsNumber) n).getValue().intValue());
                break;
            case VALUE_TRUE:
            case VALUE_FALSE:
                b.add("$L", ((JrsBoolean) n).booleanValue());
                break;
            default:
                throw new RuntimeException("Don't know how to create expression JSON type " + token);
        }

        b.add(")");

        return b.build();
    }

    private CodeBlock objectExpr(JrsObject n) {
        CodeBlock.Builder b = CodeBlock.builder();

        JrsValue ref = n.get("ref");
        JrsValue fn = n.get("fn");

        if (ref != null) {
            b.add("$T.ref($T.of($S))", Expr.class, Identifier.class, ref.asText());
        } else if (fn != null) {
            String name = fn.asText();
            CodeBlock.Builder fnNode = CodeBlock.builder();
            fnNode.add("$T.builder()", FnNode.class)
                  .add(".fn($S)", name);

            JrsArray argv = (JrsArray) n.get("argv");

            fnNode.add(".argv($T.asList(", Arrays.class);
            Iterator<JrsValue> iter = argv.elements();

            while (iter.hasNext()) {
                fnNode.add(expr(iter.next()));

                if (iter.hasNext()) {
                    fnNode.add(",");
                }
            }

            fnNode.add(")).build().validate()");

            b.add(fnNode.build());
        }

        return b.build();
    }
}
