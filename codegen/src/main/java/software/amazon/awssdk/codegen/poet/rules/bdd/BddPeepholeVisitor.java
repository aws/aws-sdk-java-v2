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

import java.util.List;
import software.amazon.awssdk.codegen.poet.rules.FunctionCallExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralBooleanExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralIntegerExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralStringExpression;
import software.amazon.awssdk.codegen.poet.rules.RewriteRuleExpressionVisitor;
import software.amazon.awssdk.codegen.poet.rules.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules.RuleRuntimeTypeMirror;

/**
 * BDD-only peephole optimization pass. Rewrites endpoint rule expressions into synthetic function
 * calls that the BDD code generators emit as allocation-free native Java.
 *
 * <p>This runs before {@link software.amazon.awssdk.codegen.poet.rules.PrepareForCodegenVisitor} on
 * the BDD path only, so the tree-based rules code generation is unaffected.
 *
 * <p>Optimizations applied:
 * <ul>
 *     <li>{@code stringEquals(coalesce(substring(str, X, Y, reverse), ""), literal)} →
 *         {@code RulesFunctions.substringEquals(str, X, Y, reverse, literal)}, which compares in
 *         place instead of allocating the substring and the coalesce varargs array. The helper
 *         reproduces {@code substring}'s semantics exactly, including its rejection of non-ASCII
 *         input, so the rewrite cannot change which branch a rule takes.</li>
 *     <li>{@code coalesce(boolExpr, boolLiteral)} → {@code __coalesceBoolean(expr, default)}</li>
 *     <li>{@code ite(cond, ifTrue, ifFalse)} → {@code __ite(cond, ifTrue, ifFalse)}</li>
 *     <li>{@code isValidHostLabel(str, boolLiteral)} → {@code __isValidHostLabel(str, allowDots)}</li>
 * </ul>
 *
 * <p>Synthetic function names are prefixed with {@code __} so they cannot collide with endpoint
 * rule standard library function names. Those in {@link #isCustomEmitted(String)} are emitted as
 * inline Java by the code generators; {@code __substringEquals} is instead registered as a real
 * function in {@link RuleRuntimeTypeMirror} and emitted by the ordinary static-call path.
 */
public final class BddPeepholeVisitor extends RewriteRuleExpressionVisitor {
    public static final String ITE = "__ite";
    public static final String COALESCE_BOOL = "__coalesceBoolean";
    public static final String IS_VALID_HOST_LABEL = "__isValidHostLabel";

    /**
     * Returns true for synthetic functions that the BDD code generator emits as inline Java rather
     * than as a plain static call. {@link RuleRuntimeTypeMirror#SUBSTRING_EQUALS_FN} is deliberately
     * absent: it is registered as a real function and so needs no custom emitter.
     */
    public static boolean isCustomEmitted(String functionName) {
        return ITE.equals(functionName)
               || COALESCE_BOOL.equals(functionName)
               || IS_VALID_HOST_LABEL.equals(functionName);
    }

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
     * Rewrites {@code stringEquals} into a {@code substringEquals} call when either side is a
     * {@code coalesce(substring(..), "")}. Any other {@code stringEquals} is returned unchanged for
     * {@link software.amazon.awssdk.codegen.poet.rules.PrepareForCodegenVisitor} to handle.
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

        // Leave the plain comparison alone. PrepareForCodegenVisitor runs immediately after and
        // rewrites it to constant.equals(other) only when one side is a string constant, keeping the
        // null-safe RulesFunctions.stringEquals call otherwise. Rewriting it here would emit
        // left.equals(right) even when both sides are nullable at runtime, which turns the
        // spec-mandated false into a NullPointerException.
        return e;
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

        // Degenerate case: startIdx == stopIdx makes the spec's substring return null, so the
        // coalesce yields "" and the comparison against an empty literal is true even for a null
        // input. substringEquals cannot express that, so leave it to the general path.
        if (literal.isEmpty()) {
            return null;
        }

        return FunctionCallExpression.builder()
                                     .name(RuleRuntimeTypeMirror.SUBSTRING_EQUALS_FN)
                                     .type(RuleRuntimeTypeMirror.BOOLEAN)
                                     .addArgument(strExpr)
                                     .addArgument(new LiteralIntegerExpression(startIdx))
                                     .addArgument(new LiteralIntegerExpression(stopIdx))
                                     .addArgument(new LiteralBooleanExpression(reverse))
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
