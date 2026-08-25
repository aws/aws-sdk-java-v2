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

import java.util.List;
import software.amazon.awssdk.codegen.poet.rules2.FunctionCallExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralBooleanExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralIntegerExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralStringExpression;
import software.amazon.awssdk.codegen.poet.rules2.MethodCallExpression;
import software.amazon.awssdk.codegen.poet.rules2.RewriteRuleExpressionVisitor;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleRuntimeTypeMirror;

/**
 * BDD-only peephole optimization pass. Rewrites endpoint rule expressions into synthetic function
 * calls that the BDD code generators emit as allocation-free native Java.
 *
 * <p>This runs before {@link software.amazon.awssdk.codegen.poet.rules2.PrepareForCodegenVisitor} on
 * the BDD path only, so the tree-based rules code generation is unaffected.
 *
 * <p>Optimizations applied:
 * <ul>
 *     <li>{@code stringEquals(coalesce(substring(str, 0, N, false), ""), literal)} →
 *         {@code __startsWith(str, literal)} when {@code N == literal.length()}</li>
 *     <li>{@code stringEquals(coalesce(substring(str, 0, N, true), ""), literal)} →
 *         {@code __endsWith(str, literal)} when {@code N == literal.length()}</li>
 *     <li>{@code stringEquals(coalesce(substring(str, X, Y, reverse), ""), literal)} →
 *         {@code __regionMatches(str, offset, literal)} for interior substring checks</li>
 *     <li>{@code stringEquals(left, right)} → {@code left.equals(right)} to avoid
 *         {@code RulesFunctions.stringEquals} dispatch</li>
 *     <li>{@code coalesce(boolExpr, boolLiteral)} → {@code __coalesceBoolean(expr, default)}</li>
 *     <li>{@code ite(cond, ifTrue, ifFalse)} → {@code __ite(cond, ifTrue, ifFalse)}</li>
 *     <li>{@code isValidHostLabel(str, boolLiteral)} → {@code __isValidHostLabel(str, allowDots)}</li>
 * </ul>
 *
 * <p>Synthetic function names are prefixed with {@code __} so they cannot collide with endpoint
 * rule standard library function names.
 */
public final class BddPeepholeVisitor extends RewriteRuleExpressionVisitor {
    public static final String STARTS_WITH = "__startsWith";
    public static final String ENDS_WITH = "__endsWith";
    public static final String REGION_MATCHES = "__regionMatches";
    public static final String ITE = "__ite";
    public static final String COALESCE_BOOL = "__coalesceBoolean";
    public static final String IS_VALID_HOST_LABEL = "__isValidHostLabel";

    @Override
    public RuleExpression visitFunctionCallExpression(FunctionCallExpression e) {
        e = (FunctionCallExpression) super.visitFunctionCallExpression(e);
        switch (e.name()) {
            case "stringEquals":
                return simplifyStringEquals(e);
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

    /**
     * Rewrites {@code stringEquals} either into a substring peephole (startsWith / endsWith /
     * regionMatches) or into a direct {@code .equals()} method call.
     */
    private RuleExpression simplifyStringEquals(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        if (args.size() != 2) {
            return e;
        }
        RuleExpression left = args.get(0);
        RuleExpression right = args.get(1);

        RuleExpression peephole = trySubstringPeephole(left, right);
        if (peephole != null) {
            return peephole;
        }
        peephole = trySubstringPeephole(right, left);
        if (peephole != null) {
            return peephole;
        }

        // Put a string constant on the left when present so the emitted .equals() is null-safe.
        if (right.kind() == RuleExpression.RuleExpressionKind.STRING_VALUE) {
            return methodCallEquals(right, left);
        }
        return methodCallEquals(left, right);
    }

    private RuleExpression methodCallEquals(RuleExpression source, RuleExpression argument) {
        return MethodCallExpression.builder()
                                   .name("equals")
                                   .source(source)
                                   .addArgument(argument)
                                   .build();
    }

    /**
     * Matches {@code coalesce(substring(str, start, stop, reverse), "")} compared against a string
     * literal, returning the corresponding synthetic expression, or null when the pattern does not
     * apply.
     */
    private RuleExpression trySubstringPeephole(RuleExpression coalesceCandidate, RuleExpression literalCandidate) {
        if (literalCandidate.kind() != RuleExpression.RuleExpressionKind.STRING_VALUE) {
            return null;
        }
        String literal = ((LiteralStringExpression) literalCandidate).value();

        FunctionCallExpression coalesceExpr = extractCoalesceWithEmptyDefault(coalesceCandidate);
        if (coalesceExpr == null) {
            return null;
        }

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
        if (!(subArgs.get(1) instanceof LiteralIntegerExpression)
            || !(subArgs.get(2) instanceof LiteralIntegerExpression)
            || !(subArgs.get(3) instanceof LiteralBooleanExpression)) {
            return null;
        }

        RuleExpression strExpr = subArgs.get(0);
        int startIdx = ((LiteralIntegerExpression) subArgs.get(1)).value();
        int stopIdx = ((LiteralIntegerExpression) subArgs.get(2)).value();
        boolean reverse = ((LiteralBooleanExpression) subArgs.get(3)).value();

        // The comparison can only ever be true when the lengths line up.
        if (literal.length() != stopIdx - startIdx) {
            return null;
        }

        if (startIdx == 0) {
            return booleanFunction(reverse ? ENDS_WITH : STARTS_WITH, strExpr, new LiteralStringExpression(literal));
        }

        // Interior match. For reverse the real offset is str.length() - stopIdx, which is unknown at
        // codegen time, so it is encoded as a negative offset for the generator to resolve.
        int offset = reverse ? -stopIdx : startIdx;
        return FunctionCallExpression.builder()
                                     .name(REGION_MATCHES)
                                     .type(RuleRuntimeTypeMirror.BOOLEAN)
                                     .addArgument(strExpr)
                                     .addArgument(new LiteralIntegerExpression(offset))
                                     .addArgument(new LiteralStringExpression(literal))
                                     .build();
    }

    /**
     * Returns the {@code coalesce(expr, "")} call when the candidate matches that shape, else null.
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
        if (!"".equals(((LiteralStringExpression) defaultArg).value())) {
            return null;
        }
        return fn;
    }

    /**
     * Rewrites {@code coalesce(boolExpr, boolLiteral)} so the generator can emit
     * {@code expr != null ? expr : default} instead of a varargs call that boxes its arguments.
     */
    private RuleExpression simplifyCoalesce(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        if (args.size() != 2 || args.get(1).kind() != RuleExpression.RuleExpressionKind.BOOLEAN_VALUE) {
            return e;
        }
        return booleanFunction(COALESCE_BOOL, args.get(0), args.get(1));
    }

    /**
     * Rewrites {@code ite(condition, ifTrue, ifFalse)} so the generator can emit a native ternary.
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
     * Rewrites {@code isValidHostLabel(str, allowDots)} when {@code allowDots} is a compile-time
     * constant, letting the generator call the specialized runtime helper that skips the branch.
     */
    private RuleExpression simplifyIsValidHostLabel(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        if (args.size() != 2 || !(args.get(1) instanceof LiteralBooleanExpression)) {
            return e;
        }
        return booleanFunction(IS_VALID_HOST_LABEL, args.get(0), args.get(1));
    }

    private RuleExpression booleanFunction(String name, RuleExpression arg0, RuleExpression arg1) {
        return FunctionCallExpression.builder()
                                     .name(name)
                                     .type(RuleRuntimeTypeMirror.BOOLEAN)
                                     .addArgument(arg0)
                                     .addArgument(arg1)
                                     .build();
    }
}
