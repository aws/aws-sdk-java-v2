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

package software.amazon.awssdk.codegen.poet.rules2.bdd;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.poet.rules2.BooleanAndExpression;
import software.amazon.awssdk.codegen.poet.rules2.BooleanNotExpression;
import software.amazon.awssdk.codegen.poet.rules2.EndpointExpression;
import software.amazon.awssdk.codegen.poet.rules2.ErrorExpression;
import software.amazon.awssdk.codegen.poet.rules2.FunctionCallExpression;
import software.amazon.awssdk.codegen.poet.rules2.HeadersExpression;
import software.amazon.awssdk.codegen.poet.rules2.IndexedAccessExpression;
import software.amazon.awssdk.codegen.poet.rules2.LetExpression;
import software.amazon.awssdk.codegen.poet.rules2.ListExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralBooleanExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralIntegerExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralStringExpression;
import software.amazon.awssdk.codegen.poet.rules2.MemberAccessExpression;
import software.amazon.awssdk.codegen.poet.rules2.MethodCallExpression;
import software.amazon.awssdk.codegen.poet.rules2.PropertiesExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpressionVisitor;
import software.amazon.awssdk.codegen.poet.rules2.RuleFunctionMirror;
import software.amazon.awssdk.codegen.poet.rules2.RuleRuntimeTypeMirror;
import software.amazon.awssdk.codegen.poet.rules2.RuleSetExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleType;
import software.amazon.awssdk.codegen.poet.rules2.StringConcatExpression;
import software.amazon.awssdk.codegen.poet.rules2.VariableReferenceExpression;

public class ConditionFnCodeGeneratorVisitor implements RuleExpressionVisitor<RuleType> {
    private static final Logger log = LoggerFactory.getLogger(RuleExpressionVisitor.class);

    private final CodeBlock.Builder builder;
    private final RuleRuntimeTypeMirror typeMirror;
    private final Map<String, RegistryInfo> registerInfoMap;

    public ConditionFnCodeGeneratorVisitor(CodeBlock.Builder builder, RuleRuntimeTypeMirror typeMirror, Map<String, RegistryInfo> registerInfoMap) {
        this.builder = builder;
        this.typeMirror = typeMirror;
        this.registerInfoMap = registerInfoMap;
    }

    @Override
    public RuleType visitLiteralBooleanExpression(LiteralBooleanExpression e) {
        builder.add(Boolean.toString(e.value()));
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitLiteralIntegerExpression(LiteralIntegerExpression e) {
        builder.add(Integer.toString(e.value()));
        return RuleRuntimeTypeMirror.INTEGER;
    }

    @Override
    public RuleType visitLiteralStringExpression(LiteralStringExpression e) {
        builder.add("$S", e.value());
        return RuleRuntimeTypeMirror.STRING;
    }

    @Override
    public RuleType visitBooleanNotExpression(BooleanNotExpression e) {
        builder.add("!");
        e.expression().accept(this);
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitBooleanAndExpression(BooleanAndExpression e) {
        List<RuleExpression> expressions = e.expressions();
        boolean isFirst = true;
        for (RuleExpression expr : expressions) {
            if (!isFirst) {
                builder.add(" && ");
            }
            expr.accept(this);
            isFirst = false;
        }
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitFunctionCallExpression(FunctionCallExpression e) {
        String fn = e.name();
        if ("not".equals(fn)) {
            builder.add("!(");
            e.arguments().get(0).accept(this);
            builder.add(")");
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        if ("isSet".equals(fn)) {
            e.arguments().get(0).accept(this);
            builder.add(" != null");
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        if ("isNotSet".equals(fn)) {
            e.arguments().get(0).accept(this);
            builder.add(" == null");
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        RuleFunctionMirror func = typeMirror.resolveFunction(e.name());
        builder.add("$T.$L(", func.containingType().type(), func.javaName());
        List<RuleExpression> args = e.arguments();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                builder.add(", ");
            }
            args.get(i).accept(this);
        }
        builder.add(")");
        return func.returns();
    }

    @Override
    public RuleType visitMethodCallExpression(MethodCallExpression e) {
        e.source().accept(this);
        builder.add(".$L(", e.name());
        boolean isFirst = true;
        for (RuleExpression arg : e.arguments()) {
            if (!isFirst) {
                builder.add(", ");
            }
            arg.accept(this);
            isFirst = false;
        }
        builder.add(")");
        if ("equals".equals(e.name())) {
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        return e.type();
    }

    @Override
    public RuleType visitVariableReferenceExpression(VariableReferenceExpression e) {
        RegistryInfo registryInfo = registerInfoMap.get(e.variableName());
        builder.add("$L", registryInfo.getName());
        return registerInfoMap.get(e.variableName()).getRuleType();
    }

    @Override
    public RuleType visitMemberAccessExpression(MemberAccessExpression e) {
        RuleType sourceType = e.source().accept(this);
        if (!e.directIndex()) {
            builder.add(".$L()", e.name());
        }

        return sourceType.property(e.name());
    }

    @Override
    public RuleType visitIndexedAccessExpression(IndexedAccessExpression e) {
        RuleFunctionMirror func = typeMirror.resolveFunction("listAccess");
        builder.add("$T.$L(", func.containingType().type(), func.javaName());
        RuleType sourceType = e.source().accept(this);
        builder.add(", $L)", e.index());
        return sourceType.typeParam();
    }

    @Override
    public RuleType visitStringConcatExpression(StringConcatExpression e) {
        boolean isFirst = true;
        for (RuleExpression expr : e.expressions()) {
            if (!isFirst) {
                builder.add(" + ");
            }
            expr.accept(this);
            isFirst = false;
        }
        return RuleRuntimeTypeMirror.STRING;
    }

    @Override
    public RuleType visitLetExpression(LetExpression e) {
        if (e.bindings().size() != 1) {
            throw new IllegalStateException("Expected exactly one binding");
        }
        for (Map.Entry<String, RuleExpression> kvp : e.bindings().entrySet()) {
            String k = kvp.getKey();
            RuleExpression v = kvp.getValue();
            String registerName = registerInfoMap.get(k).getName();
            builder.add("$L = ", registerName);
            v.accept(this);
            builder.addStatement(""); // end the statement we started
            builder.addStatement("return $L != null", registerName);
        }
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitRuleSetExpression(RuleSetExpression e) {
        // generate the conditions - there may be multiple assigns (LET)
        // but there will be only one condition
        if (e.conditions().size() != 1) {
            throw new IllegalStateException("Expected exactly one condition");
        }

         RuleExpression condition = e.conditions().get(0);
        if (condition.kind() == RuleExpression.RuleExpressionKind.LET) {
            condition.accept(this); //lets are self contained
        } else {
            builder.add("return (");
            if (RuleExpression.RuleExpressionKind.VARIABLE_REFERENCE == condition.kind()) {
                VariableReferenceExpression varRef = (VariableReferenceExpression) condition;
                RegistryInfo registryInfo = registerInfoMap.get(varRef.variableName());
                // special case optimization: do not auto-box booleanEquals!
                if (registryInfo.isNullable() && RuleRuntimeTypeMirror.BOOLEAN.equals(registryInfo.getRuleType())) {
                    builder.add("Boolean.FALSE != $L", registryInfo.getName());
                    builder.addStatement(")");
                    return RuleRuntimeTypeMirror.BOOLEAN;
                }

            }
            RuleType type = condition.accept(this);
            if (type != null && !RuleRuntimeTypeMirror.BOOLEAN.equals(type)) {
                log.warn("Expected boolean, got {}.  Rewriting condition with a != null. Condition: `{}`", type, condition);
                builder.add(" != null");
            }
            builder.addStatement(")"); // finish the expression we started
        }

        return RuleRuntimeTypeMirror.VOID;
    }

    @Override
    public RuleType visitListExpression(ListExpression e) {
        builder.add("$T.asList(", Arrays.class);
        boolean isFirst = true;
        for (RuleExpression expr : e.expressions()) {
            if (!isFirst) {
                builder.add(", ");
            }
            expr.accept(this);
            isFirst = false;
        }
        builder.add(")");
        // TODO: this could potentially be another type
        return RuleRuntimeTypeMirror.LIST_OF_STRING;
    }

    @Override
    public RuleType visitEndpointExpression(EndpointExpression e) {
        throw new IllegalStateException("Unexpected EndpointExpression");
    }

    @Override
    public RuleType visitErrorExpression(ErrorExpression e) {
        throw new IllegalStateException("Unexpected ErrorExpression");
    }

    @Override
    public RuleType visitPropertiesExpression(PropertiesExpression e) {
        throw new IllegalStateException("Unexpected PropertiesExpression");
    }

    @Override
    public RuleType visitHeadersExpression(HeadersExpression e) {
        throw new IllegalStateException("Unexpected visitEndpointExpression");
    }
}
