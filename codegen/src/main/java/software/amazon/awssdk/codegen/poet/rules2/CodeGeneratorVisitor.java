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

package software.amazon.awssdk.codegen.poet.rules2;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.codegen.model.config.customization.KeyTypePair;
import software.amazon.awssdk.endpoints.Endpoint;

public class CodeGeneratorVisitor extends WalkRuleExpressionVisitor {
    private final CodeBlock.Builder builder;
    private final RuleRuntimeTypeMirror typeMirror;
    private final SymbolTable symbolTable;
    private final Map<String, KeyTypePair> knownEndpointAttributes;
    private final Map<String, ComputeScopeTree.Scope> ruleIdToScope;

    public CodeGeneratorVisitor(RuleRuntimeTypeMirror typeMirror,
                                SymbolTable symbolTable,
                                Map<String, KeyTypePair> knownEndpointAttributes,
                                Map<String, ComputeScopeTree.Scope> ruleIdToScope,
                                CodeBlock.Builder builder) {
        this.builder = builder;
        this.symbolTable = symbolTable;
        this.knownEndpointAttributes = knownEndpointAttributes;
        this.ruleIdToScope = ruleIdToScope;
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
        if (!e.directIndex()) {
            builder.add(".$L()", e.name());
        }

        return null;
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
        for (Map.Entry<String, RuleExpression> kvp : expr.bindings().entrySet()) {
            String k = kvp.getKey();
            RuleExpression v = kvp.getValue();
            RuleType type = symbolTable.locals().get(k);
            builder.add("$T $L = ", type.javaType(), k);
            v.accept(this);
            builder.addStatement("");
            builder.beginControlFlow("if ($L != null)", k);
        }
        return null;
    }

    private void conditionsPreamble(RuleSetExpression expr) {
        for (RuleExpression condition : expr.conditions()) {
            if (condition.kind() == RuleExpression.RuleExpressionKind.LET) {
                condition.accept(this);
            } else {
                builder.add("if (");
                condition.accept(this);
                builder.beginControlFlow(")");
            }
        }
    }

    private void conditionsEpilogue(RuleSetExpression expr) {
        for (RuleExpression condition : expr.conditions()) {
            if (condition.kind() == RuleExpression.RuleExpressionKind.LET) {
                LetExpression let = (LetExpression) condition;
                for (int x = 0; x < let.bindings().size(); x++) {
                    builder.endControlFlow();
                }
            } else {
                builder.endControlFlow();
            }
        }
        if (needsReturn(expr)) {
            builder.addStatement("return $T.carryOn()", typeMirror.rulesResult().type());
        }
    }

    private boolean needsReturn(RuleSetExpression expr) {
        // If the expression can be inlined, then it doesn't live in
        // its own method, no return at the end required
        if (canBeInlined(expr)) {
            return false;
        }
        // If the expression has conditions all be be wrapped in
        // if-blocks, thus at the end of the method we need to return
        // carryOn()
        if (!expr.conditions().isEmpty()) {
            return true;
        }
        // If the expression doesn't have any conditions, and doesn't
        // have any children then we need to return carryOn(). This
        // case SHOULD NOT happen but we assume below that there are
        // children, thus adding the test here.
        if (expr.children().isEmpty()) {
            return true;
        }
        // We have children, check the last one.
        int size = expr.children().size();
        RuleSetExpression child = expr.children().get(size - 1);
        // If a tree then we don't need a return.
        if (child.isTree()) {
            return false;
        }
        // The child is not a tree, so it was inlined. Check if it
        // does have any conditions, if it so, its body will be inside
        // a block already so we need to return after it.
        return !child.conditions().isEmpty();
    }

    private void codegenTreeBody(RuleSetExpression expr) {
        List<RuleSetExpression> children = expr.children();
        int size = children.size();
        boolean isFirst = true;
        for (int idx = 0; idx < size; ++idx) {
            RuleSetExpression child = children.get(idx);
            if (canBeInlined(child)) {
                child.accept(this);
                continue;
            }
            boolean isLast = idx == size - 1;
            if (isLast) {
                builder.addStatement("return $L($L)",
                                     child.ruleId(),
                                     callParams(child.ruleId()));
                continue;
            }

            if (isFirst) {
                isFirst = false;
                builder.addStatement("$T result = $L($L)",
                                     typeMirror.rulesResult().type(),
                                     child.ruleId(),
                                     callParams(child.ruleId()));
            } else {
                builder.addStatement("result = $L($L)",
                                     child.ruleId(),
                                     callParams(child.ruleId()));
            }
            builder.beginControlFlow("if (result.isResolved())")
                   .addStatement("return result")
                   .endControlFlow();
        }
    }

    private boolean canBeInlined(RuleSetExpression child) {
        return !child.isTree();
    }

    private String callParams(String ruleId) {
        ComputeScopeTree.Scope scope = ruleIdToScope.get(ruleId);
        String args = scope.usesLocals().stream()
                           .filter(a -> !scope.defines().contains(a))
                           .collect(Collectors.joining(", "));
        if (args.isEmpty()) {
            return "params";
        }
        return "params, " + args;
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
        if (e.kind() != RuleExpression.RuleExpressionKind.PROPERTIES) {
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
        if (e.kind() != RuleExpression.RuleExpressionKind.STRING_VALUE) {
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
