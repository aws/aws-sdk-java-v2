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

package software.amazon.awssdk.http.auth.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static software.amazon.awssdk.http.auth.aws.RegionSetTest.Case.tc;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;

public class RegionSetTest {

    private static final List<Case<String>> stringCaseList = Arrays.asList(
        tc("*", "*", Arrays.asList("*")),
        tc("us-west-2", "us-west-2", Arrays.asList("us-west-2")),
        tc("us-east-1,us-west-2", "us-east-1,us-west-2", Arrays.asList("us-east-1", "us-west-2")),
        tc(" us-west-2 ", "us-west-2", Arrays.asList("us-west-2")),
        tc(" us-east-1, us-west-2  ", "us-east-1,us-west-2", Arrays.asList("us-east-1", "us-west-2")),
        tc(" a,b  ,c ,d ,e ,f ,  g ", "a,b,c,d,e,f,g", Arrays.asList("a", "b", "c", "d", "e", "f", "g"))
    );

    private static final List<String> stringFailList = Arrays.asList(
        null,
        "",
        " ",
        ", ,",
        ",,,"
    );

    private static final List<Case<Collection<String>>> collectionCaseList = Arrays.asList(
        tc(Arrays.asList("*"), "*", Arrays.asList("*")),
        tc(Arrays.asList("us-west-2"), "us-west-2", Arrays.asList("us-west-2")),
        tc(Arrays.asList("us-east-1", "us-west-2"), "us-east-1,us-west-2", Arrays.asList("us-east-1", "us-west-2")),
        tc(Arrays.asList(" us-west-2 "), "us-west-2", Arrays.asList("us-west-2")),
        tc(Arrays.asList(" us-east-1", " us-west-2  "), "us-east-1,us-west-2", Arrays.asList("us-east-1", "us-west-2")),
        tc(Arrays.asList(" a", "b  ", "c ", "d ", "e ", "f ", "  g "),
           "a,b,c,d,e,f,g",
           Arrays.asList("a", "b", "c", "d", "e", "f", "g")
        )
    );

    private static final List<Collection<String>> collectionFailList = Arrays.asList(
        null,
        Arrays.asList(),
        Arrays.asList(""),
        Arrays.asList(" "),
        Arrays.asList(" ", ""),
        Arrays.asList("", "", "")
    );

    private static List<Case<String>> stringCases() {
        return stringCaseList;
    }

    private static List<String> stringFailures() {
        return stringFailList;
    }

    private static List<Case<Collection<String>>> collectionCases() {
        return collectionCaseList;
    }

    private static List<Collection<String>> collectionFailures() {
        return collectionFailList;
    }

    @ParameterizedTest
    @MethodSource("stringCases")
    public void create_withStringInput_succeeds(Case<String> stringCase) {
        RegionSet regionSet = RegionSet.create(stringCase.input);
        assertEquals(stringCase.asString, regionSet.asString());
        assertIterableEquals(stringCase.asSet, regionSet.asSet());
    }

    @ParameterizedTest
    @MethodSource("stringFailures")
    public void create_withInvalidStringInput_throws(String input) {
        Exception ex = assertThrows(Exception.class, () -> RegionSet.create(input));
        if (ex instanceof NullPointerException) {
            assertThat(ex.getMessage()).contains("must not be");
        } else if (ex instanceof IllegalArgumentException) {
            assertThat(ex.getMessage()).contains("must not be");
        } else {
            fail();
        }
    }

    @ParameterizedTest
    @MethodSource("collectionCases")
    public void create_withCollectionInput_succeeds(Case<Collection<String>> collectionCase) {
        RegionSet regionSet = RegionSet.create(collectionCase.input);
        assertEquals(collectionCase.asString, regionSet.asString());
        assertIterableEquals(collectionCase.asSet, regionSet.asSet());
    }

    @ParameterizedTest
    @MethodSource("collectionFailures")
    public void create_withInvalidCollectionInput_throws(Collection<String> input) {
        Exception ex = assertThrows(Exception.class, () -> RegionSet.create(input));
        if (ex instanceof NullPointerException) {
            assertThat(ex.getMessage()).contains("must not be");
        } else if (ex instanceof IllegalArgumentException) {
            assertThat(ex.getMessage()).contains("must not be");
        } else {
            fail();
        }
    }

    static final class Case<T> {
        final T input;
        final String asString;
        final Collection<String> asSet;

        private Case(T input, String asString, Collection<String> asSet) {
            this.input = input;
            this.asString = asString;
            this.asSet = asSet;
        }

        static <T> Case<T> tc(T input, String asString, Collection<String> asSet) {
            return new Case<>(input, asString, asSet);
        }

        @Override
        public String toString() {
            return String.format("%s => %s :: %s", input, asString, asSet);
        }
    }
}
