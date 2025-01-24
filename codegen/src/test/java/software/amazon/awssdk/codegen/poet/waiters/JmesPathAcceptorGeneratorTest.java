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

package software.amazon.awssdk.codegen.poet.waiters;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.squareup.javapoet.ClassName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JmesPathAcceptorGeneratorTest {
    private JmesPathAcceptorGenerator acceptorGenerator;

    @BeforeEach
    public void setup() {
        acceptorGenerator = new JmesPathAcceptorGenerator(ClassName.get("software.amazon.awssdk.codegen", "JmesPathRuntime"));
    }

    @Test
    void testAutoScalingComplexExpression() {
        testConversion("contains(AutoScalingGroups[].[length(Instances[?LifecycleState=='InService']) >= MinSize][], `false`)",
                       "input.field(\"AutoScalingGroups\").flatten().multiSelectList(x0 -> x0.field(\"Instances\").filter(x1 -> "
                       + "x1.field(\"LifecycleState\").compare(\"==\", x1.constant(\"InService\"))).length().compare(\">=\", "
                       + "x0.field(\"MinSize\"))).flatten().contains(input.constant(false))");
    }

    @Test
    void testEcsComplexExpression() {
        testConversion("length(services[?!(length(deployments) == `1` && runningCount == desiredCount)]) == `0`",
                       "input.field(\"services\").filter(x0 -> x0.constant(x0.field(\"deployments\")"
                       + ".length().compare(\"==\", x0.constant(new java.math.BigDecimal(\"1\")))"
                       + ".and(x0.field(\"runningCount\").compare(\"==\", x0.field(\"desiredCount\")))"
                       + ".not())).length().compare(\"==\", input.constant(new java.math.BigDecimal(\"0\")))");
    }

    @Test
    void testSubExpressionWithIdentifier() {
        testConversion("foo.bar", "input.field(\"foo\").field(\"bar\")");
    }

    @Test
    void testSubExpressionWithMultiSelectList() {
        testConversion("foo.[bar]", "input.field(\"foo\").multiSelectList(x0 -> x0.field(\"bar\"))");
    }

    @Test
    void testSubExpressionWithMultiSelectHash() {
        assertThatThrownBy(() -> testConversion("foo.{bar : baz}", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSubExpressionWithFunction() {
        testConversion("length(foo)", "input.field(\"foo\").length()");
    }

    @Test
    void testSubExpressionWithStar() {
        testConversion("foo.*", "input.field(\"foo\").wildcard()");
    }

    @Test
    void testPipeExpression() {
        assertThatThrownBy(() -> testConversion("foo | bar", ""))
            .isInstanceOf(UnsupportedOperationException.class);;
    }

    @Test
    void testOrExpression() {
        testConversion("foo || bar", "input.field(\"foo\").or(input.field(\"bar\"))");
    }

    @Test
    void testAndExpression() {
        testConversion("foo && bar", "input.field(\"foo\").and(input.field(\"bar\"))");
    }

    @Test
    void testNotExpression() {
        testConversion("!foo", "input.constant(input.field(\"foo\").not())");
    }

    @Test
    void testParenExpression() {
        testConversion("(foo)", "input.field(\"foo\")");
    }

    @Test
    void testWildcardExpression() {
        testConversion("*", "input.wildcard()");
    }

    @Test
    void testIndexedExpressionWithoutLeftExpressionWithoutContents() {
        testConversion("[]", "input.flatten()");
    }

    @Test
    void testIndexedExpressionWithoutLeftExpressionWithNumberContents() {
        testConversion("[10]", "input.index(10)");
    }

    @Test
    void testIndexedExpressionWithoutLeftExpressionWithStarContents() {
        testConversion("[*]", "input.wildcard()");
    }

    @Test
    void testIndexedExpressionWithoutLeftExpressionWithQuestionMark() {
        testConversion("[?foo]", "input.filter(x0 -> x0.field(\"foo\"))");
    }

    @Test
    void testIndexedExpressionWithLeftExpressionWithoutContents() {
        testConversion("foo[]", "input.field(\"foo\").flatten()");
    }

    @Test
    void testIndexedExpressionWithLeftExpressionWithContents() {
        testConversion("foo[10]", "input.field(\"foo\").index(10)");
    }

    @Test
    void testIndexedExpressionWithLeftExpressionWithStarContents() {
        testConversion("foo[*]", "input.field(\"foo\").wildcard()");
    }

    @Test
    void testIndexedExpressionWithLeftExpressionWithStarContents_sub() {
        testConversion("foo[*].bar", "input.field(\"foo\").wildcard().field(\"bar\")");
    }

    @Test
    void testIndexedExpressionWithLeftExpressionWithQuestionMark() {
        testConversion("foo[?bar]", "input.field(\"foo\").filter(x0 -> x0.field(\"bar\"))");
    }

    @Test
    void testMultiSelectList2() {
        testConversion("[foo, bar]", "input.multiSelectList(x0 -> x0.field(\"foo\"), x1 -> x1.field(\"bar\"))");
    }

    @Test
    void testMultiSelectList3() {
        testConversion("[foo, bar, baz]",
                       "input.multiSelectList(x0 -> x0.field(\"foo\"), x1 -> x1.field(\"bar\"), x2 -> x2.field(\"baz\"))");
    }

    @Test
    void testNestedMultiSelectListsRight() {
        testConversion("[foo, [bar, baz]]",
                       "input.multiSelectList(x0 -> x0.field(\"foo\"), "
                       + "x1 -> x1.multiSelectList(x2 -> x2.field(\"bar\"), x3 -> x3.field(\"baz\")))");
    }

    @Test
    void testNestedMultiSelectListsCenter() {
        testConversion("[foo, [bar, baz], bam]",
                       "input.multiSelectList(x0 -> x0.field(\"foo\"), "
                       + "x1 -> x1.multiSelectList(x2 -> x2.field(\"bar\"), x3 -> x3.field(\"baz\")), "
                       + "x4 -> x4.field(\"bam\"))");
    }

    @Test
    void testNestedMultiSelectListsLeft() {
        testConversion("[[foo, bar], baz]", "input.multiSelectList(x0 -> x0.multiSelectList(x1 -> x1.field(\"foo\"), "
                                            + "x2 -> x2.field(\"bar\")), "
                                            + "x3 -> x3.field(\"baz\"))");
    }

    @Test
    void testMultiSelectHash2() {
        assertThatThrownBy(() -> testConversion("{fooK : fooV, barK : barV}", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testMultiSelectHash3() {
        assertThatThrownBy(() -> testConversion("{fooK : fooV, barK : barV, bazK : bazV}", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testComparatorExpressionLT() {
        testConversion("foo < bar", "input.field(\"foo\").compare(\"<\", input.field(\"bar\"))");
    }

    @Test
    void testComparatorExpressionGT() {
        testConversion("foo > bar", "input.field(\"foo\").compare(\">\", input.field(\"bar\"))");
    }

    @Test
    void testComparatorExpressionLTE() {
        testConversion("foo <= bar", "input.field(\"foo\").compare(\"<=\", input.field(\"bar\"))");
    }

    @Test
    void testComparatorExpressionGTE() {
        testConversion("foo >= bar", "input.field(\"foo\").compare(\">=\", input.field(\"bar\"))");
    }

    @Test
    void testComparatorExpressionEQ() {
        testConversion("foo == bar", "input.field(\"foo\").compare(\"==\", input.field(\"bar\"))");
    }

    @Test
    void testComparatorExpressionNEQ() {
        testConversion("foo != bar", "input.field(\"foo\").compare(\"!=\", input.field(\"bar\"))");
    }

    @Test
    void testKeysExpression() {
        testConversion("keys(foo)", "input.field(\"foo\").keys()");
    }

    @Test
    void testSliceExpressionWithNoNumbers2() {
        assertThatThrownBy(() -> testConversion("[:]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithNoNumbers3() {
        assertThatThrownBy(() -> testConversion("[::]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithStartNumber2() {
        assertThatThrownBy(() -> testConversion("[10:]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithStopNumber2() {
        assertThatThrownBy(() -> testConversion("[:10]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithStartStopNumber2() {
        assertThatThrownBy(() -> testConversion("[10:20]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithStartNumber3() {
        assertThatThrownBy(() -> testConversion("[10::]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithStopNumber3() {
        assertThatThrownBy(() -> testConversion("[:10:]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithStartStopNumber3() {
        assertThatThrownBy(() -> testConversion("[10:20:]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithStartStopStepNumber3() {
        assertThatThrownBy(() -> testConversion("[10:20:30]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSliceExpressionWithStepNumber3() {
        assertThatThrownBy(() -> testConversion("[::30]", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testCurrentNode() {
        assertThatThrownBy(() -> testConversion("@", ""))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testEmptyIdentifierUnquoted() {
        testConversion("foo_bar", "input.field(\"foo_bar\")");
    }

    @Test
    void testIdentifierUnquoted() {
        testConversion("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_",
                       "input.field(\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_\")");
    }

    @Test
    void testIdentifierQuoted() {
        testConversion("\"foo bar\"", "input.field(\"foo bar\")");
    }

    @Test
    void testRawStringEmpty() {
        testConversion("''", "input.constant(\"\")");
    }

    @Test
    void testRawStringWithValue() {
        testConversion("'foo bar'", "input.constant(\"foo bar\")");
    }

    @Test
    void testNegativeNumber() {
        testConversion("foo[-10]", "input.field(\"foo\").index(-10)");
    }

    @ParameterizedTest
    @CsvSource({
        "42,                    42",
        "127,                   127",
        "32767,                 32767",
        "9223372036854775807,   9223372036854775807",
        "42.5,                  42.5",
        "1.7976931348623157E308, 1.7976931348623157E308",
        "-42,                   -42",
        "0,                     0",
        "1e-10,                 1e-10"
    })
    void testNumericValues(String input, String expectedNumber) {
        String jmesPath = String.format("value == `%s`", input);
        String expected = String.format("input.field(\"value\").compare(\"==\", input.constant(new java.math.BigDecimal(\"%s\")))", expectedNumber);
        testConversion(jmesPath, expected);
    }

    private void testConversion(String jmesPathString, String expectedCode) {
        assertThat(acceptorGenerator.interpret(jmesPathString, "input").toString()).isEqualTo((expectedCode));
    }

}