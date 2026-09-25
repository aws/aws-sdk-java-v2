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

package software.amazon.awssdk.services.compiledendpointrules.endpoints.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Covers {@code RulesFunctions.split}'s handling of arguments the endpoint rules specification forbids.
 *
 * <p>None of {@code value}, {@code delimiter} or {@code limit} is optional, so reaching this function with a bad one
 * means the rule set or the generated provider is wrong. Each therefore has to fail as a named
 * {@link SdkClientException} that a reader can act on, rather than as whatever the first dereference happens to
 * produce. The four cases below previously produced four different outcomes, three of them unhelpful and one of them
 * dangerous.
 *
 * <p>The valid cases are pinned alongside them, because the natural way to add the validation - after the
 * {@code limit == 1} shortcut, where the arguments are first used - leaves two of the violations undetected.
 */
class RulesFunctionsSplitTest {

    // ---- value: the rules type checker requires it to be set ----

    /**
     * Covers every limit branch. {@code limit == 1} is the one that matters most: it returns before {@code value} is
     * dereferenced, so a null used to come back as {@code [null]} with no error at all. Downstream that reads as an
     * unset value, which sends the rule down a different branch instead of failing.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5})
    void nullValue_throwsSdkClientException(int limit) {
        assertThatThrownBy(() -> RulesFunctions.split(null, ",", limit))
            .isInstanceOf(SdkClientException.class)
            .isNotInstanceOf(NullPointerException.class)
            .hasMessageContaining("null value");
    }

    // ---- delimiter: "must not be null or empty" ----

    /**
     * At {@code limit == 1} the delimiter is never read, so a null one used to be accepted silently.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5})
    void nullDelimiter_throwsSdkClientException(int limit) {
        assertThatThrownBy(() -> RulesFunctions.split("a,b", null, limit))
            .isInstanceOf(SdkClientException.class)
            .isNotInstanceOf(NullPointerException.class)
            .hasMessageContaining("null delimiter");
    }

    /**
     * An empty delimiter with a limit produced silent nonsense - {@code split("abc", "", 4)} returned
     * {@code ["", "", "", "abc"]}.
     */
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 5})
    void emptyDelimiterWithLimit_throwsSdkClientException(int limit) {
        assertThatThrownBy(() -> RulesFunctions.split("abc", "", limit))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("empty delimiter");
    }

    /**
     * The case that made this worth fixing rather than tidying. {@code indexOf("")} matches at the current position
     * every time, so with no limit the loop appended empty strings until the heap was exhausted -
     * {@link OutOfMemoryError} on a 256 MB heap, which takes more than the request down with it.
     *
     * <p>The timeout is the assertion as much as the exception type is: a regression here hangs rather than fails.
     */
    @Test
    @Timeout(10)
    void emptyDelimiterUnlimited_throwsInsteadOfExhaustingTheHeap() {
        assertThatThrownBy(() -> RulesFunctions.split("abc", "", 0))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("empty delimiter");
    }

    /**
     * An empty value short-circuits before the delimiter is read, so this pins that validation still happens.
     */
    @Test
    void emptyValueWithBadDelimiter_stillThrows() {
        assertThatThrownBy(() -> RulesFunctions.split("", null, 0))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("null delimiter");
        assertThatThrownBy(() -> RulesFunctions.split("", "", 0))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("empty delimiter");
    }

    // ---- limit: "must not be negative" ----

    /**
     * Previously an {@code IllegalArgumentException: Illegal Capacity: -1} from {@code ArrayList}'s constructor, which
     * names neither split nor the limit.
     */
    @ParameterizedTest
    @ValueSource(ints = {-1, -2, -100, Integer.MIN_VALUE})
    void negativeLimit_throwsSdkClientExceptionNamingTheLimit(int limit) {
        assertThatThrownBy(() -> RulesFunctions.split("a,b", ",", limit))
            .isInstanceOf(SdkClientException.class)
            .isNotInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negative limit")
            .hasMessageContaining(String.valueOf(limit));
    }

    // ---- valid input keeps working ----

    /**
     * Stated by the specification: "For empty input strings, the function returns an array containing a single empty
     * string." Not an empty array, which is what a reader tends to assume.
     */
    @Test
    void emptyValue_returnsSingleEmptyString() {
        assertThat(RulesFunctions.split("", ",", 0)).containsExactly("");
        assertThat(RulesFunctions.split("", ",", 1)).containsExactly("");
        assertThat(RulesFunctions.split("", ",", 3)).containsExactly("");
    }

    @Test
    void limitOne_returnsWholeValueUnsplit() {
        assertThat(RulesFunctions.split("a,b,c", ",", 1)).containsExactly("a,b,c");
    }

    @Test
    void limitZero_splitsOnEveryOccurrence() {
        assertThat(RulesFunctions.split("a,b,c", ",", 0)).containsExactly("a", "b", "c");
    }

    @Test
    void limitBoundsThePartsAndKeepsTheRemainderWhole() {
        assertThat(RulesFunctions.split("a,b,c,d", ",", 2)).containsExactly("a", "b,c,d");
        assertThat(RulesFunctions.split("a,b,c,d", ",", 3)).containsExactly("a", "b", "c,d");
        assertThat(RulesFunctions.split("a,b,c,d", ",", 99)).containsExactly("a", "b", "c", "d");
    }

    @Test
    void delimiterAbsent_returnsTheWholeValue() {
        assertThat(RulesFunctions.split("abc", ",", 0)).containsExactly("abc");
    }

    /**
     * The shape the S3 rules use: {@code split(Bucket, "--", 0)} feeding an index read, for S3 Express bucket names.
     */
    @Test
    void multiCharacterDelimiter_splitsOnTheWholeDelimiter() {
        assertThat(RulesFunctions.split("mybucket--usw2-az1--x-s3", "--", 0))
            .containsExactly("mybucket", "usw2-az1", "x-s3");
        assertThat(RulesFunctions.split("a-b", "--", 0)).containsExactly("a-b");
    }

    @Test
    void delimitersAtTheEdgesProduceEmptyParts() {
        assertThat(RulesFunctions.split(",a,", ",", 0)).containsExactly("", "a", "");
        assertThat(RulesFunctions.split("a,,b", ",", 0)).containsExactly("a", "", "b");
    }

    @Test
    void valueEqualToTheDelimiter_producesTwoEmptyParts() {
        assertThat(RulesFunctions.split(",", ",", 0)).containsExactly("", "");
    }

    /**
     * A long unlimited split, to exercise the growth path of the list now that {@code limit == 0} seeds a capacity
     * instead of starting empty.
     */
    @Test
    void unlimitedSplitOfManyParts() {
        int parts = 200;
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < parts; i++) {
            if (i > 0) {
                value.append(',');
            }
            value.append('p').append(i);
        }

        assertThat(RulesFunctions.split(value.toString(), ",", 0))
            .hasSize(parts)
            .startsWith("p0", "p1")
            .endsWith("p" + (parts - 1));
    }

    @Test
    void resultIsTheDocumentedOrderForAKnownCase() {
        assertThat(RulesFunctions.split("a.b.c", ".", 0)).isEqualTo(Arrays.asList("a", "b", "c"));
        assertThat(RulesFunctions.split("solo", ".", 0)).isEqualTo(Collections.singletonList("solo"));
    }
}
