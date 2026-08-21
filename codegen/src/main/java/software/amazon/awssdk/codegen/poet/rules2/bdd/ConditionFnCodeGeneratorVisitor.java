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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
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
import software.amazon.awssdk.codegen.poet.rules2.PrepareForCodegenVisitor;
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
    private static final Logger log = LoggerFactory.getLogger(ConditionFnCodeGeneratorVisitor.class);
    private final CodeBlock.Builder builder;
    private final RuleRuntimeTypeMirror typeMirror;
    private final Map<String, RegistryInfo> registerInfoMap;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public ConditionFnCodeGeneratorVisitor(CodeBlock.Builder builder, RuleRuntimeTypeMirror typeMirror,
                                           Map<String, RegistryInfo> registerInfoMap,
                                           EndpointRulesSpecUtils endpointRulesSpecUtils) {
        this.builder = builder;
        this.typeMirror = typeMirror;
        this.registerInfoMap = registerInfoMap;
        this.endpointRulesSpecUtils = endpointRulesSpecUtils;
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

        // Peephole-optimized synthetic functions
        if (fn.startsWith("__")) {
            return emitPeepholeOptimized(fn, e.arguments());
        }

        RuleFunctionMirror func = typeMirror.resolveFunction(e.name());
        builder.add("$T.$L(", func.containingType().type(), func.javaName());
        List<RuleExpression> args = e.arguments();
        RuleType lastArgType = RuleRuntimeTypeMirror.VOID;
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                builder.add(", ");
            }
            lastArgType = args.get(i).accept(this);
        }
        builder.add(")");
        if ("coalesce".equals(fn)) {
            // special case type inference for coalesce
            return lastArgType;
        }
        return func.returns();
    }

    /**
     * Emits peephole-optimized native Java code for synthetic function calls.
     * These avoid allocations by using String.startsWith/endsWith/regionMatches
     * and inline ternary expressions instead of calling RulesFunctions.
     */
    private RuleType emitPeepholeOptimized(String fn, List<RuleExpression> args) {
        switch (fn) {
            case PrepareForCodegenVisitor.STARTS_WITH:
                // __startsWith(str, literal) → str != null && str.startsWith("literal")
                return emitStartsWith(args);
            case PrepareForCodegenVisitor.ENDS_WITH:
                // __endsWith(str, literal) → str != null && str.endsWith("literal")
                return emitEndsWith(args);
            case PrepareForCodegenVisitor.REGION_MATCHES:
                // __regionMatches(str, offset, literal) → null-safe regionMatches
                return emitRegionMatches(args);
            case PrepareForCodegenVisitor.ITE:
                // __ite(cond, ifTrue, ifFalse) → (cond ? ifTrue : ifFalse)
                return emitIte(args);
            case PrepareForCodegenVisitor.COALESCE_BOOL:
                // __coalesceBoolean(expr, default) → (expr != null ? expr : default)
                return emitCoalesceBoolean(args);
            case PrepareForCodegenVisitor.IS_VALID_HOST_LABEL:
                // __isValidHostLabel(str, allowDots) → inline validation
                return emitIsValidHostLabel(args);
            default:
                throw new IllegalStateException("Unknown peephole function: " + fn);
        }
    }

    /**
     * Emits: {@code (str != null && str.startsWith("literal"))}
     */
    private RuleType emitStartsWith(List<RuleExpression> args) {
        builder.add("(");
        args.get(0).accept(this);
        builder.add(" != null && ");
        args.get(0).accept(this);
        builder.add(".startsWith(");
        args.get(1).accept(this);
        builder.add("))");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    /**
     * Emits: {@code (str != null && str.endsWith("literal"))}
     */
    private RuleType emitEndsWith(List<RuleExpression> args) {
        builder.add("(");
        args.get(0).accept(this);
        builder.add(" != null && ");
        args.get(0).accept(this);
        builder.add(".endsWith(");
        args.get(1).accept(this);
        builder.add("))");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    /**
     * Emits a regionMatches check. Offset can be negative to indicate "from end" (reverse=true).
     * <ul>
     *     <li>Positive offset: {@code (str != null && str.length() >= (offset + litLen)
     *         && str.regionMatches(offset, "literal", 0, litLen))}</li>
     *     <li>Negative offset (value = -stopIdx): {@code (str != null && str.length() >= stopIdx
     *         && str.regionMatches(str.length() - stopIdx, "literal", 0, litLen))}</li>
     * </ul>
     */
    private RuleType emitRegionMatches(List<RuleExpression> args) {
        RuleExpression strExpr = args.get(0);
        int offset = ((LiteralIntegerExpression) args.get(1)).value();
        String literal = ((LiteralStringExpression) args.get(2)).value();
        int litLen = literal.length();

        builder.add("(");
        strExpr.accept(this);
        builder.add(" != null && ");

        if (offset >= 0) {
            // Forward: str.length() >= (offset + litLen) && str.regionMatches(offset, literal, 0, litLen)
            strExpr.accept(this);
            builder.add(".length() >= $L && ", offset + litLen);
            strExpr.accept(this);
            builder.add(".regionMatches(");
            builder.add("$L, $S, 0, $L", offset, literal, litLen);
            builder.add("))");
        } else {
            // Reverse: offset encodes -(stopIdx). Actual position = str.length() - stopIdx
            int stopIdx = -offset;
            strExpr.accept(this);
            builder.add(".length() >= $L && ", stopIdx);
            strExpr.accept(this);
            builder.add(".regionMatches(");
            strExpr.accept(this);
            builder.add(".length() - $L, $S, 0, $L", stopIdx, literal, litLen);
            builder.add("))");
        }
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    /**
     * Emits: {@code (cond ? ifTrue : ifFalse)}
     */
    private RuleType emitIte(List<RuleExpression> args) {
        builder.add("(");
        args.get(0).accept(this);
        builder.add(" ? ");
        args.get(1).accept(this);
        builder.add(" : ");
        args.get(2).accept(this);
        builder.add(")");
        return RuleRuntimeTypeMirror.STRING;
    }

    /**
     * Emits: {@code (expr != null ? expr : defaultValue)}
     */
    private RuleType emitCoalesceBoolean(List<RuleExpression> args) {
        builder.add("(");
        args.get(0).accept(this);
        builder.add(" != null ? ");
        args.get(0).accept(this);
        builder.add(" : ");
        args.get(1).accept(this);
        builder.add(")");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    /**
     * Emits: {@code RulesFunctions.isValidHostLabelSingle(str)} or
     * {@code RulesFunctions.isValidHostLabelMulti(str)} depending on the allowDots constant.
     * Avoids the boolean parameter dispatch branch at runtime.
     */
    private RuleType emitIsValidHostLabel(List<RuleExpression> args) {
        boolean allowDots = ((LiteralBooleanExpression) args.get(1)).value();
        RuleFunctionMirror func = typeMirror.resolveFunction("isValidHostLabel");
        builder.add("$T.$L(", func.containingType().type(),
                    allowDots ? "isValidHostLabelMulti" : "isValidHostLabelSingle");
        args.get(0).accept(this);
        builder.add(")");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitMethodCallExpression(MethodCallExpression e) {
        // Wrap compound expressions (string concat) in parens to ensure correct binding
        boolean needsParens = e.source().kind() == RuleExpression.RuleExpressionKind.STRING_CONCAT;
        if (needsParens) {
            builder.add("(");
        }
        e.source().accept(this);
        if (needsParens) {
            builder.add(")");
        }
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
        if (registryInfo.isNonRegionParam()) {
            builder.add("params.$L()", endpointRulesSpecUtils.paramMethodName(registryInfo.getNonRegionParamKey()));
        } else {
            builder.add("$L", registryInfo.getName());
        }
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
            // __ite always returns non-null (it's a ternary between two string literals),
            // so skip the dead null-check and just return true
            if (isAlwaysNonNull(v)) {
                builder.addStatement("return true");
            } else {
                builder.addStatement("return $L != null", registerName);
            }
        }
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    /**
     * Returns true if the expression is guaranteed to produce a non-null value at runtime.
     * Currently recognizes __ite (inline ternary between two string literals).
     */
    private static boolean isAlwaysNonNull(RuleExpression expr) {
        if (!(expr instanceof FunctionCallExpression)) {
            return false;
        }
        FunctionCallExpression fn = (FunctionCallExpression) expr;
        return PrepareForCodegenVisitor.ITE.equals(fn.name());
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
            condition.accept(this); // lets are self contained
        } else {
            builder.add("return (");
            if (RuleExpression.RuleExpressionKind.VARIABLE_REFERENCE == condition.kind()) {
                VariableReferenceExpression varRef = (VariableReferenceExpression) condition;
                RegistryInfo registryInfo = registerInfoMap.get(varRef.variableName());
                // special case optimization: do not auto-box booleanEquals!
                if (registryInfo.isNullable() && RuleRuntimeTypeMirror.BOOLEAN.equals(registryInfo.getRuleType())) {
                    builder.add("Boolean.TRUE.equals($L)", registryInfo.getName());
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
        throw new IllegalStateException("Unexpected HeadersExpression");
    }
}
