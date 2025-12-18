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

import java.util.Map;
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

public class AssignTypeInferringVisitor implements RuleExpressionVisitor<RuleType> {
    private final RuleRuntimeTypeMirror typeMirror;
    private final Map<String, RegistryInfo> registerInfoMap;

    public AssignTypeInferringVisitor(RuleRuntimeTypeMirror typeMirror, Map<String, RegistryInfo> registerInfoMap) {
        this.typeMirror = typeMirror;
        this.registerInfoMap = registerInfoMap;
    }

    @Override
    public RuleType visitLiteralBooleanExpression(LiteralBooleanExpression e) {
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitLiteralIntegerExpression(LiteralIntegerExpression e) {
        return RuleRuntimeTypeMirror.INTEGER;
    }

    @Override
    public RuleType visitLiteralStringExpression(LiteralStringExpression e) {
        return RuleRuntimeTypeMirror.STRING;
    }

    @Override
    public RuleType visitBooleanNotExpression(BooleanNotExpression e) {
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitBooleanAndExpression(BooleanAndExpression e) {
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitFunctionCallExpression(FunctionCallExpression e) {
        String fn = e.name();
        if ("not".equals(fn)) {
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        if ("isSet".equals(fn)) {
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        if ("isNotSet".equals(fn)) {
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        RuleFunctionMirror func = typeMirror.resolveFunction(e.name());
        return func.returns();
    }

    @Override
    public RuleType visitMethodCallExpression(MethodCallExpression e) {
        throw new IllegalStateException("Unexpected methodCallExpression");
    }

    @Override
    public RuleType visitVariableReferenceExpression(VariableReferenceExpression e) {
        RuleType type = registerInfoMap.get(e.variableName()).getRuleType();
        if (type == null) {
            // visit the assign condition for this
            registerInfoMap.get(e.variableName()).getRuleSetExpression().accept(this);
            type = registerInfoMap.get(e.variableName()).getRuleType();
            if (type == null) {
                throw new IllegalStateException("Unable to infer registry type information for `" + e.variableName() + "`");
            }
        }
        return type;
    }

    @Override
    public RuleType visitMemberAccessExpression(MemberAccessExpression e) {
        RuleType sourceType = e.source().accept(this);
        if (e.directIndex() && e.name() == null) {
            return sourceType;
        }
        return sourceType.property(e.name());
    }

    @Override
    public RuleType visitIndexedAccessExpression(IndexedAccessExpression e) {
        RuleType sourceType = e.source().accept(this);

        return sourceType.ruleTypeParam(); // get the list inner type
    }

    @Override
    public RuleType visitStringConcatExpression(StringConcatExpression e) {
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
            RuleType assignedType = v.accept(this);
            registerInfoMap.get(k).setRuleType(assignedType);
        }
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitRuleSetExpression(RuleSetExpression e) {
        if (e.conditions().size() != 1) {
            throw new IllegalStateException("Expected exactly one condition");
        }

        e.conditions().get(0).accept(this);
        return RuleRuntimeTypeMirror.VOID;
    }

    @Override
    public RuleType visitListExpression(ListExpression e) {
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
