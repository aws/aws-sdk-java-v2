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

package software.amazon.awssdk.services.s3.endpoints.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code RulesFunctions.substringEquals} is emitted by the BDD endpoint codegen peephole in place of
 * {@code stringEquals(coalesce(substring(value, start, stop, reverse), ""), literal)}. It must be
 * indistinguishable from that composition, otherwise a rule takes a different branch than the
 * endpoint spec requires and we resolve a wrong endpoint.
 *
 * <p>The interesting case is non-ASCII input: the spec's {@code substring} returns null when *any*
 * character in the whole input is outside the 7-bit ASCII range, which makes the comparison false
 * even when the characters at the compared positions match exactly.
 */
class RulesFunctionsSubstringEqualsTest {

    /**
     * The composition that {@code substringEquals} replaces. Written out in full so the test is
     * checking against the spec functions rather than against a restatement of the optimization.
     */
    private static boolean reference(String value, int startIndex, int stopIndex, boolean reverse, String literal) {
        return RulesFunctions.stringEquals(
            RulesFunctions.coalesce(RulesFunctions.substring(value, startIndex, stopIndex, reverse), ""),
            literal);
    }

    @ParameterizedTest(name = "substringEquals({0}, {1}, {2}, {3}, {4})")
    @MethodSource("nonEmptyLiteralCases")
    void matchesSpecComposition(String value, int startIndex, int stopIndex, boolean reverse, String literal) {
        assertThat(RulesFunctions.substringEquals(value, startIndex, stopIndex, reverse, literal))
            .as("substringEquals must agree with stringEquals(coalesce(substring(..), \"\"), literal)")
            .isEqualTo(reference(value, startIndex, stopIndex, reverse, literal));
    }

    /**
     * An empty literal is the sole case where the two are allowed to differ, because
     * {@code coalesce(null, "")} makes the spec composition true whenever {@code substring} returns
     * null. Asserting the direction of every divergence keeps that carve-out honest: if the helper
     * ever diverged for a non-empty literal it would show up in
     * {@link #matchesSpecComposition}, and if a *new* kind of divergence appeared here it would show
     * up as a failure rather than as a silently skipped case.
     *
     * <p>Codegen never emits this shape - see {@code BddPeepholeVisitorTest.emptyLiteralIsNotRewritten}.
     */
    @ParameterizedTest(name = "substringEquals({0}, {1}, {2}, {3}, \"\")")
    @MethodSource("emptyLiteralCases")
    void emptyLiteralDivergesOnlyBySpecReturningTrue(String value, int startIndex, int stopIndex, boolean reverse) {
        boolean actual = RulesFunctions.substringEquals(value, startIndex, stopIndex, reverse, "");
        boolean expected = reference(value, startIndex, stopIndex, reverse, "");

        assertThat(actual).as("substringEquals must never be true for an empty literal").isFalse();
        if (actual != expected) {
            assertThat(expected)
                .as("the only permitted divergence is the spec composition returning true")
                .isTrue();
        }
    }

    private static List<Arguments> nonEmptyLiteralCases() {
        return cases(false);
    }

    private static List<Arguments> emptyLiteralCases() {
        List<Arguments> args = new ArrayList<>();
        for (Arguments a : cases(true)) {
            Object[] g = a.get();
            // Drop the trailing literal; the test method supplies it.
            args.add(Arguments.of(g[0], g[1], g[2], g[3]));
        }
        return args;
    }

    private static List<Arguments> cases(boolean emptyLiteralOnly) {
        // (startIndex, stopIndex, reverse, literal) shapes that codegen actually emits for S3,
        // plus a forward interior match which the S3 ruleset does not currently use.
        int[][] windows = {
            {0, 4}, {0, 6}, {0, 7}, {16, 18}, {14, 16}, {1, 3}, {3, 3}
        };
        List<String> values = Arrays.asList(
            null,
            "",
            "a",
            "ab",
            "arn:",
            "arn:aws:s3:::mybucket",
            "mybucket--x-s3",
            "mybucket--xa-s3",
            "my-bucket-name",
            "mybucket--abcd-ab1--x-s3",
            // Non-ASCII: the compared window is pure ASCII and matches, but the spec still rejects
            // the whole input, so every one of these must come out false.
            "arn\u00e9:aws:s3:::b",
            "arn:aws:s3:::b\u00fc",
            "mybucket\u00fc--x-s3",
            "\u00fcmybucket--x-s3",
            "mybucket--x-s3\u00fc",
            // Multi-byte beyond Latin-1, and a surrogate pair.
            "mybucket\u4e2d--x-s3",
            "mybucket\ud83d\ude00--x-s3"
        );
        List<String> literals = emptyLiteralOnly
                                ? Arrays.asList("")
                                : Arrays.asList("arn:", "--x-s3", "--xa-s3", "--", "rn", "zz");

        List<Arguments> args = new ArrayList<>();
        for (int[] window : windows) {
            for (String value : values) {
                for (String literal : literals) {
                    for (boolean reverse : new boolean[] {false, true}) {
                        args.add(Arguments.of(value, window[0], window[1], reverse, literal));
                    }
                }
            }
        }
        return args;
    }

    /**
     * Spot checks in the direction that matters, so a regression is legible rather than just a
     * differential mismatch somewhere in the matrix above.
     */
    @Test
    void nonAsciiInputIsRejectedEvenWhenTheComparedWindowMatches() {
        // Pure-ASCII directory bucket: this is an S3 Express bucket.
        assertThat(RulesFunctions.substringEquals("mybucket--x-s3", 0, 6, true, "--x-s3")).isTrue();

        // Same suffix, but a non-ASCII character elsewhere in the name. The spec's substring returns
        // null, so this is NOT an S3 Express bucket and must not be routed as one.
        assertThat(RulesFunctions.substringEquals("mybuck\u00e9t--x-s3", 0, 6, true, "--x-s3")).isFalse();
        assertThat(reference("mybuck\u00e9t--x-s3", 0, 6, true, "--x-s3")).isFalse();

        // Likewise for the ARN prefix check.
        assertThat(RulesFunctions.substringEquals("arn:aws:s3:::b", 0, 4, false, "arn:")).isTrue();
        assertThat(RulesFunctions.substringEquals("arn:aws:s3:::b\u00fc", 0, 4, false, "arn:")).isFalse();
        assertThat(reference("arn:aws:s3:::b\u00fc", 0, 4, false, "arn:")).isFalse();
    }

    @Test
    void nullAndTooShortInputsAreFalse() {
        assertThat(RulesFunctions.substringEquals(null, 0, 4, false, "arn:")).isFalse();
        assertThat(RulesFunctions.substringEquals("arn", 0, 4, false, "arn:")).isFalse();
        assertThat(RulesFunctions.substringEquals("", 0, 4, false, "arn:")).isFalse();
    }

    @Test
    void literalLengthMustMatchTheWindow() {
        // Guards against a caller passing a literal that is not stopIndex - startIndex long. A
        // shorter or longer literal can never equal the substring.
        assertThat(RulesFunctions.substringEquals("arn:aws", 0, 4, false, "arn")).isFalse();
        assertThat(RulesFunctions.substringEquals("arn:aws", 0, 4, false, "arn:a")).isFalse();
    }

    /**
     * The one case where {@code substringEquals} is deliberately not equivalent, and therefore the
     * one case codegen must not rewrite. {@code startIndex == stopIndex} makes the spec's
     * {@code substring} return null, the coalesce turns that into {@code ""}, and comparing {@code ""}
     * to an empty literal is true - even for a null input. See
     * {@code BddPeepholeVisitorTest.emptyLiteralIsNotRewritten}, which pins the codegen guard.
     */
    @Test
    void degenerateEmptyWindowDivergesFromTheSpecAndIsGuardedInCodegen() {
        assertThat(reference(null, 3, 3, false, "")).isTrue();
        assertThat(reference("anything", 3, 3, false, "")).isTrue();

        assertThat(RulesFunctions.substringEquals(null, 3, 3, false, "")).isFalse();
        assertThat(RulesFunctions.substringEquals("anything", 3, 3, false, "")).isFalse();
    }
}
