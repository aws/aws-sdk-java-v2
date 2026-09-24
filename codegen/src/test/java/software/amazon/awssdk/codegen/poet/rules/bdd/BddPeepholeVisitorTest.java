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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.rules.FunctionCallExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralBooleanExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralIntegerExpression;
import software.amazon.awssdk.codegen.poet.rules.LiteralStringExpression;
import software.amazon.awssdk.codegen.poet.rules.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules.RuleRuntimeTypeMirror;
import software.amazon.awssdk.codegen.poet.rules.VariableReferenceExpression;

/**
 * Covers the substring peephole matcher in {@link BddPeepholeVisitor}: when it rewrites
 * {@code stringEquals(coalesce(substring(..), ""), literal)} into
 * {@code RulesFunctions.substringEquals}, and when it must leave the expression alone.
 */
class BddPeepholeVisitorTest {

    @Test
    void substringComparisonIsRewrittenToSubstringEquals() {
        RuleExpression result = rewrite(substringEqualsExpr(0, 6, true, "--x-s3"));

        assertThat(result).isInstanceOf(FunctionCallExpression.class);
        FunctionCallExpression fn = (FunctionCallExpression) result;
        assertThat(fn.name()).isEqualTo(RuleRuntimeTypeMirror.SUBSTRING_EQUALS_FN);
        assertThat(fn.arguments()).hasSize(5);
        assertThat(((LiteralIntegerExpression) fn.arguments().get(1)).value()).isEqualTo(0);
        assertThat(((LiteralIntegerExpression) fn.arguments().get(2)).value()).isEqualTo(6);
        assertThat(((LiteralBooleanExpression) fn.arguments().get(3)).value()).isTrue();
        assertThat(((LiteralStringExpression) fn.arguments().get(4)).value()).isEqualTo("--x-s3");
    }

    @Test
    void forwardInteriorComparisonIsRewritten() {
        FunctionCallExpression fn = (FunctionCallExpression) rewrite(substringEqualsExpr(16, 18, false, "--"));

        assertThat(fn.name()).isEqualTo(RuleRuntimeTypeMirror.SUBSTRING_EQUALS_FN);
        assertThat(((LiteralIntegerExpression) fn.arguments().get(1)).value()).isEqualTo(16);
        assertThat(((LiteralIntegerExpression) fn.arguments().get(2)).value()).isEqualTo(18);
        assertThat(((LiteralBooleanExpression) fn.arguments().get(3)).value()).isFalse();
    }

    @Test
    void lengthMismatchIsNotRewritten() {
        // The window is 6 wide but the literal is 4, so the comparison can never be true. Leave it to
        // the general path rather than emitting a call whose arguments contradict each other.
        assertThat(isSubstringEqualsCall(rewrite(substringEqualsExpr(0, 6, true, "arn:")))).isFalse();
    }

    /**
     * {@code startIndex == stopIndex} makes the spec's {@code substring} return null, so the coalesce
     * yields {@code ""} and comparing against an empty literal is true even for a null input.
     * {@code substringEquals} returns false there, so this must not be rewritten. Paired with
     * {@code RulesFunctionsSubstringEqualsTest.degenerateEmptyWindowDivergesFromTheSpecAndIsGuardedInCodegen}
     * in the s3 module, which pins the runtime side of the same divergence.
     */
    @Test
    void emptyLiteralIsNotRewritten() {
        assertThat(isSubstringEqualsCall(rewrite(substringEqualsExpr(3, 3, false, "")))).isFalse();
    }

    /**
     * A {@code stringEquals} that is not a substring comparison must be left intact for
     * {@link software.amazon.awssdk.codegen.poet.rules.PrepareForCodegenVisitor}, which only rewrites
     * to {@code constant.equals(other)} when one side is a string constant.
     *
     * <p>Rewriting it here would emit {@code left.equals(right)} for two nullable operands, and the
     * spec's {@code stringEquals} returns false for a null operand rather than throwing. The S3 BDD
     * reaches this with {@code stringEquals(region, bucketArn.region())} among others.
     */
    @Test
    void plainStringEqualsIsLeftForPrepareForCodegen() {
        RuleExpression expr = FunctionCallExpression
            .builder()
            .name("stringEquals")
            .type(RuleRuntimeTypeMirror.BOOLEAN)
            .addArgument(VariableReferenceExpression.builder().variableName("region").build())
            .addArgument(VariableReferenceExpression.builder().variableName("arnRegion").build())
            .build();

        RuleExpression result = rewrite(expr);

        assertThat(result).isInstanceOf(FunctionCallExpression.class);
        assertThat(((FunctionCallExpression) result).name()).isEqualTo("stringEquals");
    }

    /**
     * Same for a constant operand: the peephole must not pre-empt PrepareForCodegenVisitor, which
     * already places the constant on the receiver side.
     */
    @Test
    void constantOperandStringEqualsIsAlsoLeftForPrepareForCodegen() {
        RuleExpression expr = FunctionCallExpression
            .builder()
            .name("stringEquals")
            .type(RuleRuntimeTypeMirror.BOOLEAN)
            .addArgument(VariableReferenceExpression.builder().variableName("region").build())
            .addArgument(new LiteralStringExpression("us-east-1"))
            .build();

        assertThat(((FunctionCallExpression) rewrite(expr)).name()).isEqualTo("stringEquals");
    }

    private static RuleExpression rewrite(RuleExpression expr) {
        return expr.accept(new BddPeepholeVisitor());
    }

    /**
     * When the substring peephole declines, {@code simplifyStringEquals} still rewrites the comparison
     * into a {@code MethodCallExpression} for {@code left.equals(right)}, so the result is not
     * necessarily a {@link FunctionCallExpression} at all.
     */
    private static boolean isSubstringEqualsCall(RuleExpression expr) {
        return expr instanceof FunctionCallExpression
               && RuleRuntimeTypeMirror.SUBSTRING_EQUALS_FN.equals(((FunctionCallExpression) expr).name());
    }

    /**
     * Builds {@code stringEquals(coalesce(substring(Bucket, start, stop, reverse), ""), literal)}.
     */
    private static RuleExpression substringEqualsExpr(int start, int stop, boolean reverse, String literal) {
        FunctionCallExpression substring =
            FunctionCallExpression.builder()
                                  .name("substring")
                                  .type(RuleRuntimeTypeMirror.STRING)
                                  .addArgument(VariableReferenceExpression.builder().variableName("Bucket").build())
                                  .addArgument(new LiteralIntegerExpression(start))
                                  .addArgument(new LiteralIntegerExpression(stop))
                                  .addArgument(new LiteralBooleanExpression(reverse))
                                  .build();

        FunctionCallExpression coalesce =
            FunctionCallExpression.builder()
                                  .name("coalesce")
                                  .type(RuleRuntimeTypeMirror.STRING)
                                  .addArgument(substring)
                                  .addArgument(new LiteralStringExpression(""))
                                  .build();

        return FunctionCallExpression.builder()
                                     .name("stringEquals")
                                     .type(RuleRuntimeTypeMirror.BOOLEAN)
                                     .addArgument(coalesce)
                                     .addArgument(new LiteralStringExpression(literal))
                                     .build();
    }
}
