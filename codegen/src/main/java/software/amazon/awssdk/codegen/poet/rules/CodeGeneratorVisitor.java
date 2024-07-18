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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.codegen.model.config.customization.KeyTypePair;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.poet.rules.RuleExpression.RuleExpressionKind;
import software.amazon.awssdk.endpoints.Endpoint;

public class CodeGeneratorVisitor extends WalkRuleExpressionVisitor {
    private final CodeBlock.Builder builder;
    private final RuleRuntimeTypeMirror typeMirror;
    private final SymbolTable symbolTable;
    private final Map<String, KeyTypePair> knownEndpointAttributes;

    public CodeGeneratorVisitor(RuleRuntimeTypeMirror typeMirror,
                                SymbolTable symbolTable,
                                Map<String, KeyTypePair> knownEndpointAttributes,
                                CodeBlock.Builder builder) {
        this.builder = builder;
        this.symbolTable = symbolTable;
        this.knownEndpointAttributes = knownEndpointAttributes;
        this.typeMirror = typeMirror;
    }

    @Override
    public Void visitLiteralBooleanExpression(LiteralBooleanExpression e) {
        builder.add(Boolean.toString(e.value()));
        return null;
    }

    @Override
    public Void visitLiteralIntegerExpression(LiteralIntegerExpression e) {
        builder.add(Integer.toString(e.value()));
        return null;
    }

    @Override
    public Void visitLiteralStringExpression(LiteralStringExpression e) {
        builder.add("$S", e.value());
        return null;
    }

    @Override
    public Void visitBooleanNotExpression(BooleanNotExpression e) {
        builder.add("!");
        e.expression().accept(this);
        return null;
    }

    @Override
    public Void visitBooleanAndExpression(BooleanAndExpression e) {
        List<RuleExpression> expressions = e.expressions();
        boolean isFirst = true;
        for (RuleExpression expr : expressions) {
            if (!isFirst) {
                builder.add(" && ");
            }
            expr.accept(this);
            isFirst = false;
        }
        return null;
    }

    @Override
    public Void visitFunctionCallExpression(FunctionCallExpression e) {
        String fn = e.name();
        if ("not".equals(fn)) {
            builder.add("!(");
            e.arguments().get(0).accept(this);
            builder.add(")");
            return null;
        }
        if ("isSet".equals(fn)) {
            e.arguments().get(0).accept(this);
            builder.add(" != null");
            return null;
        }
        if ("isNotSet".equals(fn)) {
            e.arguments().get(0).accept(this);
            builder.add(" == null");
            return null;
        }
        RuleFunctionMirror func = typeMirror.resolveFunction(e.name());
        builder.add("$T.$L(", func.containingType().type(), func.javaName());
        List<RuleExpression> args = e.arguments();
        boolean isFirst = true;
        for (RuleExpression arg : args) {
            if (!isFirst) {
                builder.add(", ");
            }
            arg.accept(this);
            isFirst = false;
        }
        builder.add(")");
        return null;
    }

    @Override
    public Void visitMethodCallExpression(MethodCallExpression e) {
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
        return null;
    }

    @Override
    public Void visitVariableReferenceExpression(VariableReferenceExpression e) {
        builder.add("$L", e.variableName());
        return null;
    }

    @Override
    public Void visitMemberAccessExpression(MemberAccessExpression e) {
        e.source().accept(this);
        if (regionNeedsConversionToString(e)) {
            builder.add(".$LId()", e.name());
        } else {
            builder.add(".$L()", e.name());
        }
        return null;
    }

    private boolean regionNeedsConversionToString(MemberAccessExpression e) {
        // It's only a Region type if it's a parameter, so see if it's a parameter.
        if (e.source().kind() != RuleExpressionKind.VARIABLE_REFERENCE) {
            return false;
        }

        if (!symbolTable.isParam(e.name())) {
            return false;
        }

        // It's a parameter. Is it a region type parameter? If so, we need to convert it.
        return symbolTable.builtInParamType(e.name()) == BuiltInParameter.AWS_REGION;
    }

    @Override
    public Void visitStringConcatExpression(StringConcatExpression e) {
        boolean isFirst = true;
        for (RuleExpression expr : e.expressions()) {
            if (!isFirst) {
                builder.add(" + ");
            }
            expr.accept(this);
            isFirst = false;
        }
        return null;
    }

    @Override
    public Void visitListExpression(ListExpression e) {
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
        return null;
    }

    @Override
    public Void visitRuleSetExpression(RuleSetExpression e) {
        conditionsPreamble(e);
        ErrorExpression error = e.error();
        if (error != null) {
            error.accept(this);
        }
        EndpointExpression endpoint = e.endpoint();
        if (endpoint != null) {
            endpoint.accept(this);
        }
        if (e.children() != null) {
            codegenTreeBody(e);
        }
        conditionsEpilogue(e);
        return null;
    }

    @Override
    public Void visitLetExpression(LetExpression expr) {
        for (String key : expr.bindings().keySet()) {
            RuleType type = symbolTable.locals().get(key);
            builder.addStatement("$T $L = null", type.javaType(), key);
        }
        builder.add("if (");
        boolean isFirst = true;
        for (Map.Entry<String, RuleExpression> kvp : expr.bindings().entrySet()) {
            String k = kvp.getKey();
            RuleExpression v = kvp.getValue();
            if (!isFirst) {
                builder.add(" && ");
            }
            builder.add("($L = ", k);
            v.accept(this);
            builder.add(") != null");
            isFirst = false;
        }
        builder.beginControlFlow(")");
        builder.add("locals = locals.toBuilder()");
        expr.bindings().forEach((k, v) -> {
            builder.add(".$1L($1L)", k);
        });
        builder.addStatement(".build()");
        return null;
    }

    private void conditionsPreamble(RuleSetExpression expr) {
        for (RuleExpression condition : expr.conditions()) {
            if (condition.kind() == RuleExpressionKind.LET) {
                condition.accept(this);
            } else {
                builder.add("if (");
                condition.accept(this);
                builder.beginControlFlow(")");
            }
        }
    }

    private void conditionsEpilogue(RuleSetExpression expr) {
        int blocksToClose = expr.conditions().size();
        for (int idx = 0; idx < blocksToClose; ++idx) {
            builder.endControlFlow();
        }
        if (!expr.conditions().isEmpty()) {
            builder.addStatement("return $T.carryOn()", typeMirror.rulesResult().type());
        }
    }

    private void codegenTreeBody(RuleSetExpression expr) {
        List<RuleSetExpression> children = expr.children();
        int size = children.size();
        for (int idx = 0; idx < size; ++idx) {
            RuleSetExpression child = children.get(idx);
            boolean isLast = idx == size - 1;
            if (isLast) {
                builder.addStatement("return $L(params, locals)",
                                     child.ruleId());
                continue;
            }
            boolean isFirst = idx == 0;
            if (isFirst) {
                builder.addStatement("$T result = $L(params, locals)",
                                     typeMirror.rulesResult().type(),
                                     child.ruleId());
            } else {
                builder.addStatement("result = $L(params, locals)",
                                     child.ruleId());
            }
            builder.beginControlFlow("if (result.isResolved())")
                   .addStatement("return result")
                   .endControlFlow();
        }

    }

    @Override
    public Void visitEndpointExpression(EndpointExpression e) {
        builder.add("return $T.endpoint(", typeMirror.rulesResult().type());
        builder.add("$T.builder().url($T.create(", Endpoint.class, URI.class);
        e.url().accept(this);
        builder.add("))");
        e.headers().accept(this);
        e.properties().accept(this);
        builder.add(".build()");
        builder.addStatement(")");
        return null;
    }

    @Override
    public Void visitPropertiesExpression(PropertiesExpression e) {
        Map<String, RuleExpression> properties = e.properties();
        properties.forEach((k, v) -> {
            if ("authSchemes".equals(k)) {
                addAuthSchemesBlock(v);
            } else if (knownEndpointAttributes.containsKey(k)) {
                addAttributeBlock(k, v);
            } else {
                throw new RuntimeException("unknown endpoint property: " + k);
            }
        });
        return null;
    }

    @Override
    public Void visitHeadersExpression(HeadersExpression e) {
        e.headers().forEach((k, v) -> {
            for (RuleExpression value : v.expressions()) {
                builder.add(".putHeader($S, ", k);
                value.accept(this);
                builder.add(")");
            }
        });
        return null;
    }

    @Override
    public Void visitErrorExpression(ErrorExpression e) {
        builder.add("return $T.error(", typeMirror.rulesResult().type());
        e.error().accept(this);
        builder.addStatement(")");
        return null;
    }

    private void addAuthSchemesBlock(RuleExpression e) {
        ListExpression expr = (ListExpression) e;
        builder.add(".putAttribute($T.AUTH_SCHEMES, ", AwsEndpointAttribute.class);
        builder.add("$T.asList(", Arrays.class);
        boolean isFirst = true;
        for (RuleExpression authSchemeExpr : expr.expressions()) {
            if (!isFirst) {
                builder.add(", ");
            }
            addAuthSchemesBody(authSchemeExpr);
            isFirst = false;
        }
        builder.add("))");
    }

    private void addAuthSchemesBody(RuleExpression e) {
        if (e.kind() != RuleExpressionKind.PROPERTIES) {
            throw new RuntimeException("Expecting properties, got: " + e);
        }
        PropertiesExpression expr = (PropertiesExpression) e;
        String name = stringValueOf(expr.properties().get("name"));
        builder.add("$T.builder()", authSchemeClass(name));
        expr.properties().forEach((k, v) -> {
            if (!"name".equals(k)) {
                builder.add(".$L(", k);
                v.accept(this);
                builder.add(")");
            }
        });
        builder.add(".build()");
    }

    private String stringValueOf(RuleExpression e) {
        if (e.kind() != RuleExpressionKind.STRING_VALUE) {
            throw new RuntimeException("Expecting string value, got: " + e);
        }
        LiteralStringExpression expr = (LiteralStringExpression) e;
        return expr.value();
    }

    private ClassName authSchemeClass(String name) {
        switch (name) {
            case "sigv4":
                return ClassName.get(SigV4AuthScheme.class);
            case "sigv4a":
                return ClassName.get(SigV4aAuthScheme.class);
            case "sigv4-s3express":
                return ClassName.get("software.amazon.awssdk.services.s3.endpoints.authscheme",
                                     "S3ExpressEndpointAuthScheme");
            default:
                throw new RuntimeException("Unknown auth scheme: " + name);
        }
    }

    private void addAttributeBlock(String k, RuleExpression v) {
        KeyTypePair keyType = knownEndpointAttributes.get(k);
        ClassConstant classConstant = parseClassConstant(keyType.getKey());
        builder.add(".putAttribute($T.$L, ", classConstant.className(), classConstant.fieldName());
        v.accept(this);
        builder.add(")");
    }


    public CodeBlock.Builder builder() {
        return builder;
    }

    // We assume that the value of the class constants follows the form
    // "<package-part0> ⋯ <package-partN>.<Class>.<CONSTANT>"
    private ClassConstant parseClassConstant(String value) {
        int lastDot = value.lastIndexOf('.');
        if (lastDot == -1) {
            throw new IllegalArgumentException("cannot parse class constant: " + value);
        }
        String fieldName = value.substring(lastDot + 1);
        String className = value.substring(0, lastDot);
        int classLastDot = className.lastIndexOf('.');
        if (classLastDot == -1) {
            throw new IllegalArgumentException("cannot parse class constant: " + value);
        }
        String simpleName = className.substring(classLastDot + 1);
        String packageName = className.substring(0, classLastDot);
        return new ClassConstant(ClassName.get(packageName, simpleName), fieldName);
    }


    /**
     * Helper class to represent a constant field within a fully qualified class.
     */
    static class ClassConstant {
        private final ClassName className;
        private final String fieldName;

        ClassConstant(ClassName className, String fieldName) {
            this.className = className;
            this.fieldName = fieldName;
        }

        public ClassName className() {
            return className;
        }

        public String fieldName() {
            return fieldName;
        }
    }
}
