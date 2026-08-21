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

import java.util.List;

/**
 * Visitor that rewrites expressions in preparation for codegen.
 *
 * <p>Includes peephole optimizations that eliminate allocations for common patterns:
 * <ul>
 *     <li>{@code stringEquals(coalesce(substring(str, 0, N, true), ""), literal)} →
 *         {@code __endsWith(str, literal)} when N == literal.length()</li>
 *     <li>{@code stringEquals(coalesce(substring(str, 0, N, false), ""), literal)} →
 *         {@code __startsWith(str, literal)} when N == literal.length()</li>
 *     <li>{@code stringEquals(coalesce(substring(str, X, Y, reverse), ""), literal)} →
 *         {@code __regionMatches(str, offset, literal)} for interior substring checks</li>
 *     <li>{@code coalesce(boolExpr, false/true)} → {@code __coalesceBoolean(expr, default)}</li>
 *     <li>{@code ite(cond, ifTrue, ifFalse)} → {@code __ite(cond, ifTrue, ifFalse)}</li>
 * </ul>
 *
 * <p>Synthetic function names prefixed with {@code __} are recognized by code generator visitors
 * and emitted as allocation-free native Java code.
 */
public final class PrepareForCodegenVisitor extends RewriteRuleExpressionVisitor {

    // Synthetic function names for peephole-optimized expressions
    public static final String STARTS_WITH = "__startsWith";
    public static final String ENDS_WITH = "__endsWith";
    public static final String REGION_MATCHES = "__regionMatches";
    public static final String ITE = "__ite";
    public static final String COALESCE_BOOL = "__coalesceBoolean";
    public static final String IS_VALID_HOST_LABEL = "__isValidHostLabel";

    public PrepareForCodegenVisitor() {
    }

    @Override
    public RuleExpression visitBooleanNotExpression(BooleanNotExpression e) {
        e = (BooleanNotExpression) super.visitBooleanNotExpression(e);
        RuleExpression arg = e.expression();
        if (arg instanceof FunctionCallExpression) {
            FunctionCallExpression functionCall = (FunctionCallExpression) arg;
            if ("isSet".equals(functionCall.name())) {
                return functionCall.toBuilder()
                                   .name("isNotSet")
                                   .build();
            }
        }
        return e;
    }

    @Override
    public RuleExpression visitFunctionCallExpression(FunctionCallExpression e) {
        e = (FunctionCallExpression) super.visitFunctionCallExpression(e);
        String fn = e.name();
        switch (fn) {
            case "booleanEquals":
                return simplifyBooleanEquals(e);
            case "stringEquals":
                return simplifyStringEquals(e);
            case "not":
                return simplifyNotExpression(e);
            case "coalesce":
                return simplifyCoalesce(e);
            case "ite":
                return simplifyIte(e);
            case "isValidHostLabel":
                return simplifyIsValidHostLabel(e);
            default:
                return e;
        }
    }

    @Override
    public RuleExpression visitIndexedAccessExpression(IndexedAccessExpression e) {
        e = (IndexedAccessExpression) super.visitIndexedAccessExpression(e);
        return FunctionCallExpression
            .builder()
            .name("listAccess")
            .type(e.type())
            .addArgument(e.source())
            .addArgument(new LiteralIntegerExpression(e.index()))
            .build();
    }

    /**
     * Transforms the following expressions:
     * <ul>
     *     <li>{@code booleanEquals(left, TRUE)} transforms to {@code left}</li>
     *     <li>{@code booleanEquals(TRUE, right)} transforms to {@code right}</li>
     *     <li>{@code booleanEquals(left, FALSE)} transforms to {@code (not left)}</li>
     *     <li>{@code booleanEquals(FALSE, right)} transforms to {@code (not right)}</li>
     * </ul>
     */
    private RuleExpression simplifyBooleanEquals(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        RuleExpression left = args.get(0).accept(this);
        RuleExpression right = args.get(1).accept(this);
        if (left.kind() == RuleExpression.RuleExpressionKind.BOOLEAN_VALUE) {
            LiteralBooleanExpression leftAsBoolean = (LiteralBooleanExpression) left;
            if (leftAsBoolean.value()) {
                return right;
            }
            return BooleanNotExpression
                .builder()
                .expression(right)
                .build();
        }
        if (right.kind() == RuleExpression.RuleExpressionKind.BOOLEAN_VALUE) {
            LiteralBooleanExpression rightAsBoolean = (LiteralBooleanExpression) right;
            if (rightAsBoolean.value()) {
                return left;
            }
            return BooleanNotExpression
                .builder()
                .expression(left)
                .build();
        }
        return MethodCallExpression.builder()
                                   .name("equals")
                                   .source(left)
                                   .addArgument(right)
                                   .build();
    }

    /**
     * Transforms stringEquals with peephole optimizations for substring patterns:
     * <ul>
     *     <li>{@code stringEquals(coalesce(substring(str, 0, N, true), ""), literal)} →
     *         {@code __endsWith(str, literal)} when N == literal.length()</li>
     *     <li>{@code stringEquals(coalesce(substring(str, 0, N, false), ""), literal)} →
     *         {@code __startsWith(str, literal)} when N == literal.length()</li>
     *     <li>{@code stringEquals(coalesce(substring(str, X, Y, reverse), ""), literal)} →
     *         {@code __regionMatches(str, offset, literal)} for interior checks</li>
     *     <li>Otherwise: {@code stringEquals(left, right)} → {@code "literal".equals(other)}</li>
     * </ul>
     */
    private RuleExpression simplifyStringEquals(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        RuleExpression left = args.get(0);
        RuleExpression right = args.get(1);

        // Try peephole: stringEquals(coalesce(substring(...), ""), literal)
        RuleExpression peephole = trySubstringPeephole(left, right);
        if (peephole != null) {
            return peephole;
        }
        peephole = trySubstringPeephole(right, left);
        if (peephole != null) {
            return peephole;
        }

        // Default: convert to method call form — always emit .equals() to avoid RulesFunctions.stringEquals dispatch
        if (right.kind() == RuleExpression.RuleExpressionKind.STRING_VALUE) {
            // Put constant on left for null-safety: "literal".equals(other)
            return MethodCallExpression.builder()
                                       .name("equals")
                                       .source(right)
                                       .addArgument(left)
                                       .build();

        }
        if (left.kind() == RuleExpression.RuleExpressionKind.STRING_VALUE) {
            return MethodCallExpression.builder()
                                       .name("equals")
                                       .source(left)
                                       .addArgument(right)
                                       .build();
        }
        // Neither side is a constant — still emit left.equals(right) to avoid RulesFunctions dispatch
        return MethodCallExpression.builder()
                                   .name("equals")
                                   .source(left)
                                   .addArgument(right)
                                   .build();
    }

    /**
     * Attempts to match the pattern: coalesce(substring(str, startIdx, stopIdx, reverse), "") compared to a literal.
     * Returns a synthetic peephole expression if the pattern matches, null otherwise.
     *
     * <p>Recognized patterns:
     * <ul>
     *     <li>substring(str, 0, N, false) == literal (N == literal.length) → __startsWith(str, literal)</li>
     *     <li>substring(str, 0, N, true) == literal (N == literal.length) → __endsWith(str, literal)</li>
     *     <li>substring(str, X, Y, false) == literal (Y-X == literal.length) → __regionMatches(str, X, literal)</li>
     *     <li>substring(str, X, Y, true) == literal (Y-X == literal.length) →
     *         __regionMatches(str, len-Y, literal) but since we don't know len at codegen time,
     *         we emit __regionMatches with a negative offset to signal reverse</li>
     * </ul>
     */
    private RuleExpression trySubstringPeephole(RuleExpression coalesceCandidate, RuleExpression literalCandidate) {
        if (literalCandidate.kind() != RuleExpression.RuleExpressionKind.STRING_VALUE) {
            return null;
        }
        String literal = ((LiteralStringExpression) literalCandidate).value();

        // Match coalesce(substring(...), "")
        FunctionCallExpression coalesceExpr = extractCoalesceWithEmptyDefault(coalesceCandidate);
        if (coalesceExpr == null) {
            return null;
        }

        // The first argument of coalesce should be substring(str, start, stop, reverse)
        RuleExpression substringCandidate = coalesceExpr.arguments().get(0);
        if (!(substringCandidate instanceof FunctionCallExpression)) {
            return null;
        }
        FunctionCallExpression substringExpr = (FunctionCallExpression) substringCandidate;
        if (!"substring".equals(substringExpr.name())) {
            return null;
        }

        List<RuleExpression> subArgs = substringExpr.arguments();
        if (subArgs.size() != 4) {
            return null;
        }

        RuleExpression strExpr = subArgs.get(0);
        if (!(subArgs.get(1) instanceof LiteralIntegerExpression)
            || !(subArgs.get(2) instanceof LiteralIntegerExpression)
            || !(subArgs.get(3) instanceof LiteralBooleanExpression)) {
            return null;
        }

        int startIdx = ((LiteralIntegerExpression) subArgs.get(1)).value();
        int stopIdx = ((LiteralIntegerExpression) subArgs.get(2)).value();
        boolean reverse = ((LiteralBooleanExpression) subArgs.get(3)).value();
        int substringLen = stopIdx - startIdx;

        // Validate: the literal length must match the substring length
        if (literal.length() != substringLen) {
            return null;
        }

        // Pattern: substring(str, 0, N, false) == literal → startsWith
        if (startIdx == 0 && !reverse) {
            return FunctionCallExpression.builder()
                .name(STARTS_WITH)
                .type(RuleRuntimeTypeMirror.BOOLEAN)
                .addArgument(strExpr)
                .addArgument(new LiteralStringExpression(literal))
                .build();
        }

        // Pattern: substring(str, 0, N, true) == literal → endsWith
        if (startIdx == 0 && reverse) {
            return FunctionCallExpression.builder()
                .name(ENDS_WITH)
                .type(RuleRuntimeTypeMirror.BOOLEAN)
                .addArgument(strExpr)
                .addArgument(new LiteralStringExpression(literal))
                .build();
        }

        // Pattern: substring(str, X, Y, false) == literal → regionMatches at offset X
        if (!reverse) {
            return FunctionCallExpression.builder()
                .name(REGION_MATCHES)
                .type(RuleRuntimeTypeMirror.BOOLEAN)
                .addArgument(strExpr)
                .addArgument(new LiteralIntegerExpression(startIdx))
                .addArgument(new LiteralStringExpression(literal))
                .build();
        }

        // Pattern: substring(str, X, Y, true) == literal → regionMatches from end
        // reverse=true means: actual offset = str.length() - stopIdx
        // We encode this as negative offset: -(stopIdx) to signal "from end"
        return FunctionCallExpression.builder()
            .name(REGION_MATCHES)
            .type(RuleRuntimeTypeMirror.BOOLEAN)
            .addArgument(strExpr)
            .addArgument(new LiteralIntegerExpression(-stopIdx))
            .addArgument(new LiteralStringExpression(literal))
            .build();
    }

    /**
     * Extracts a coalesce(expr, "") call, returning the coalesce expression if it matches,
     * null otherwise. The second argument must be the empty string literal.
     */
    private FunctionCallExpression extractCoalesceWithEmptyDefault(RuleExpression candidate) {
        if (!(candidate instanceof FunctionCallExpression)) {
            return null;
        }
        FunctionCallExpression fn = (FunctionCallExpression) candidate;
        if (!"coalesce".equals(fn.name())) {
            return null;
        }
        List<RuleExpression> args = fn.arguments();
        if (args.size() != 2) {
            return null;
        }
        RuleExpression defaultArg = args.get(1);
        if (defaultArg.kind() != RuleExpression.RuleExpressionKind.STRING_VALUE) {
            return null;
        }
        String defaultVal = ((LiteralStringExpression) defaultArg).value();
        if (!"".equals(defaultVal)) {
            return null;
        }
        return fn;
    }

    /**
     * Rewrites {@code coalesce(boolExpr, boolLiteral)} to synthetic {@code __coalesceBoolean(expr, default)}
     * to avoid the varargs allocation and boxing overhead at runtime.
     *
     * <p>Generates: {@code expr != null ? expr : defaultValue}
     */
    private RuleExpression simplifyCoalesce(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        if (args.size() != 2) {
            return e;
        }
        RuleExpression defaultArg = args.get(1);

        // coalesce(boolExpr, boolLiteral) → __coalesceBoolean(expr, default)
        if (defaultArg.kind() == RuleExpression.RuleExpressionKind.BOOLEAN_VALUE) {
            return FunctionCallExpression.builder()
                .name(COALESCE_BOOL)
                .type(RuleRuntimeTypeMirror.BOOLEAN)
                .addArgument(args.get(0))
                .addArgument(defaultArg)
                .build();
        }

        return e;
    }

    /**
     * Rewrites {@code ite(condition, ifTrue, ifFalse)} to synthetic {@code __ite(condition, ifTrue, ifFalse)}
     * so code generators emit a native ternary instead of a method call.
     *
     * <p>Generates: {@code condition ? ifTrue : ifFalse}
     */
    private RuleExpression simplifyIte(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        if (args.size() != 3) {
            return e;
        }
        return FunctionCallExpression.builder()
            .name(ITE)
            .type(RuleRuntimeTypeMirror.STRING)
            .addArgument(args.get(0))
            .addArgument(args.get(1))
            .addArgument(args.get(2))
            .build();
    }

    /**
     * Transforms the following expression
     * <ul>
     *     <li>{@code not(isSet(getAttr(source, name)))} to {@code isNotSet(getAttr(source, name))} which can be later
     *     transformed into {@code getAttr(source, name) == null}</li>
     * </ul>
     */
    private RuleExpression simplifyNotExpression(FunctionCallExpression e) {
        RuleExpression arg = e.arguments().get(0);
        if (arg instanceof FunctionCallExpression) {
            FunctionCallExpression inner = (FunctionCallExpression) arg;
            if ("isSet".equals(inner.name())) {
                return inner.toBuilder()
                            .name("isNotSet")
                            .build();
            }
        }
        return e;
    }

    /**
     * Rewrites {@code isValidHostLabel(str, allowDots)} to synthetic {@code __isValidHostLabel(str, allowDots)}
     * so code generators can inline the host label validation check directly, avoiding the RulesFunctions
     * method dispatch overhead.
     *
     * <p>For allowDots=false: checks length 1-63, all chars [a-z0-9-], not starting/ending with '-'
     * <p>For allowDots=true: splits on '.', validates each segment as above
     */
    private RuleExpression simplifyIsValidHostLabel(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        if (args.size() != 2) {
            return e;
        }
        // Only inline when allowDots is a known boolean literal
        if (!(args.get(1) instanceof LiteralBooleanExpression)) {
            return e;
        }
        return FunctionCallExpression.builder()
            .name(IS_VALID_HOST_LABEL)
            .type(RuleRuntimeTypeMirror.BOOLEAN)
            .addArgument(args.get(0))
            .addArgument(args.get(1))
            .build();
    }
}
