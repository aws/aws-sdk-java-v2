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

package software.amazon.awssdk.codegen.poet.rules.bdd;

import com.squareup.javapoet.CodeBlock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.poet.rules.BooleanAndExpression;
import software.amazon.awssdk.codegen.poet.rules.BooleanNotExpression;
import software.amazon.awssdk.codegen.poet.rules.EndpointExpression;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.codegen.poet.rules.ErrorExpression;
import software.amazon.awssdk.codegen.poet.rules.FunctionCallExpression;
import software.amazon.awssdk.codegen.poet.rules.HeadersExpression;
import software.amazon.awssdk.codegen.poet.rules.IndexedAccessExpression;
import software.amazon.awssdk.codegen.poet.rules.LetExpression;
import software.amazon.awssdk.codegen.poet.rules.ListExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralBooleanExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralIntegerExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralStringExpression;
import software.amazon.awssdk.codegen.poet.rules.MemberAccessExpression;
import software.amazon.awssdk.codegen.poet.rules.MethodCallExpression;
import software.amazon.awssdk.codegen.poet.rules.PropertiesExpression;
import software.amazon.awssdk.codegen.poet.rules.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules.RuleExpressionVisitor;
import software.amazon.awssdk.codegen.poet.rules.RuleFunctionMirror;
import software.amazon.awssdk.codegen.poet.rules.RuleRuntimeTypeMirror;
import software.amazon.awssdk.codegen.poet.rules.RuleSetExpression;
import software.amazon.awssdk.codegen.poet.rules.RuleType;
import software.amazon.awssdk.codegen.poet.rules.StringConcatExpression;
import software.amazon.awssdk.codegen.poet.rules.VariableReferenceExpression;

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

        // Synthetic functions that are emitted as inline Java rather than a static call.
        // __substringEquals is not among them: it is a registered function and falls through below.
        if (BddPeepholeVisitor.isCustomEmitted(fn)) {
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
     * Emits inline Java for synthetic function calls that have no static-call equivalent, avoiding
     * the boxing and varargs allocation of the corresponding RulesFunctions calls.
     */
    private RuleType emitPeepholeOptimized(String fn, List<RuleExpression> args) {
        switch (fn) {
            case BddPeepholeVisitor.ITE:
                // __ite(cond, ifTrue, ifFalse) → (cond ? ifTrue : ifFalse)
                return emitIte(args);
            case BddPeepholeVisitor.COALESCE_BOOL:
                // __coalesceBoolean(expr, default) → (expr != null ? expr : default)
                return emitCoalesceBoolean(args);
            case BddPeepholeVisitor.IS_VALID_HOST_LABEL:
                // __isValidHostLabel(str, allowDots) → inline validation
                return emitIsValidHostLabel(args);
            default:
                throw new IllegalStateException("Unknown peephole function: " + fn);
        }
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
     * Emits a single-evaluation equivalent of {@code coalesce(expr, defaultValue)} for a boolean
     * default. Wrapper equality gives exact coalesce semantics with no branch and no boxing:
     *
     * <table border="1">
     *     <caption>Rewrite table</caption>
     *     <tr><th>rewrite</th><th>emitted</th><th>null</th><th>TRUE</th><th>FALSE</th></tr>
     *     <tr><td>{@code coalesce(x, false)}</td><td>{@code Boolean.TRUE.equals(x)}</td>
     *         <td>false</td><td>true</td><td>false</td></tr>
     *     <tr><td>{@code coalesce(x, true)}</td><td>{@code !Boolean.FALSE.equals(x)}</td>
     *         <td>true</td><td>true</td><td>false</td></tr>
     * </table>
     *
     * <p>The subject is emitted once. A ternary would emit it twice, which runs any non-trivial
     * operand (a nested rules function, for instance) twice per evaluation.
     */
    private RuleType emitCoalesceBoolean(List<RuleExpression> args) {
        boolean defaultValue = ((LiteralBooleanExpression) args.get(1)).value();
        if (defaultValue) {
            builder.add("!Boolean.FALSE.equals(");
        } else {
            builder.add("Boolean.TRUE.equals(");
        }
        args.get(0).accept(this);
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
            // Assign conditions succeed only when the assigned value is non-null. Skip the check
            // only where the value is provably non-null, otherwise emit it.
            if (isAlwaysNonNull(v)) {
                builder.addStatement("return true");
            } else {
                builder.addStatement("return $L != null", registerName);
            }
        }
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    /**
     * Returns true only if the expression is provably non-null at runtime.
     *
     * <p>Recognizes {@code __ite} whose two branches are both string literals, which
     * {@link #emitIte} emits as a ternary between them. {@code BddPeepholeVisitor.simplifyIte} does
     * not constrain the branches, and {@code RuleRuntimeTypeMirror} types them as {@code STRING}, so
     * a {@code {"ref": ...}} branch is legal input from the endpoint compiler and can be null. Both
     * branches are therefore checked here rather than assumed: eliding the register null-check for a
     * nullable branch would report an assign condition as satisfied with a null register, flipping a
     * BDD edge and resolving an endpoint the spec does not permit.
     */
    private static boolean isAlwaysNonNull(RuleExpression expr) {
        if (!(expr instanceof FunctionCallExpression)) {
            return false;
        }
        FunctionCallExpression fn = (FunctionCallExpression) expr;
        if (!BddPeepholeVisitor.ITE.equals(fn.name()) || fn.arguments().size() != 3) {
            return false;
        }
        return fn.arguments().get(1).kind() == RuleExpression.RuleExpressionKind.STRING_VALUE
               && fn.arguments().get(2).kind() == RuleExpression.RuleExpressionKind.STRING_VALUE;
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
