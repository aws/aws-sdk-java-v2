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

import com.squareup.javapoet.ClassName;
import org.junit.Before;
import org.junit.Test;

public class JmesPathAcceptorGeneratorTest {
    private JmesPathAcceptorGenerator acceptorGenerator;

    @Before
    public void setup() {
        acceptorGenerator = new JmesPathAcceptorGenerator(ClassName.get("software.amazon.awssdk.codegen", "WaitersRuntime"));
    }

    @Test
    public void testAutoScalingComplexExpression() {
        testConversion("contains(AutoScalingGroups[].[length(Instances[?LifecycleState=='InService']) >= MinSize][], `false`)",
                       "input.field(\"AutoScalingGroups\").flatten().multiSelectList(x0 -> x0.field(\"Instances\").filter(x1 -> "
                       + "x1.field(\"LifecycleState\").compare(\"==\", x1.constant(\"InService\"))).length().compare(\">=\", "
                       + "x0.field(\"MinSize\"))).flatten().contains(input.constant(false))");
    }

    @Test
    public void testEcsComplexExpression() {
        testConversion("length(services[?!(length(deployments) == `1` && runningCount == desiredCount)]) == `0`",
                       "input.field(\"services\").filter(x0 -> x0.constant(x0.field(\"deployments\").length().compare(\"==\", "
                       + "x0.constant(1)).and(x0.field(\"runningCount\").compare(\"==\", x0.field(\"desiredCount\"))).not()))"
                       + ".length().compare(\"==\", input.constant(0))");
    }

    @Test
    public void testSubExpressionWithIdentifier() {
        testConversion("foo.bar", "input.field(\"foo\").field(\"bar\")");
    }

    @Test
    public void testSubExpressionWithMultiSelectList() {
        testConversion("foo.[bar]", "input.field(\"foo\").multiSelectList(x0 -> x0.field(\"bar\"))");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubExpressionWithMultiSelectHash() {
        testConversion("foo.{bar : baz}", "");
    }

    @Test
    public void testSubExpressionWithFunction() {
        testConversion("length(foo)", "input.field(\"foo\").length()");
    }

    @Test
    public void testSubExpressionWithStar() {
        testConversion("foo.*", "input.field(\"foo\").wildcard()");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPipeExpression() {
        testConversion("foo | bar", "");
    }

    @Test
    public void testOrExpression() {
        testConversion("foo || bar", "input.field(\"foo\").or(input.field(\"bar\"))");
    }

    @Test
    public void testAndExpression() {
        testConversion("foo && bar", "input.field(\"foo\").and(input.field(\"bar\"))");
    }

    @Test
    public void testNotExpression() {
        testConversion("!foo", "input.constant(input.field(\"foo\").not())");
    }

    @Test
    public void testParenExpression() {
        testConversion("(foo)", "input.field(\"foo\")");
    }

    @Test
    public void testWildcardExpression() {
        testConversion("*", "input.wildcard()");
    }

    @Test
    public void testIndexedExpressionWithoutLeftExpressionWithoutContents() {
        testConversion("[]", "input.flatten()");
    }

    @Test
    public void testIndexedExpressionWithoutLeftExpressionWithNumberContents() {
        testConversion("[10]", "input.index(10)");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIndexedExpressionWithoutLeftExpressionWithStarContents() {
        testConversion("[*]", "");
    }

    @Test
    public void testIndexedExpressionWithoutLeftExpressionWithQuestionMark() {
        testConversion("[?foo]", "input.filter(x0 -> x0.field(\"foo\"))");
    }

    @Test
    public void testIndexedExpressionWithLeftExpressionWithoutContents() {
        testConversion("foo[]", "input.field(\"foo\").flatten()");
    }

    @Test
    public void testIndexedExpressionWithLeftExpressionWithContents() {
        testConversion("foo[10]", "input.field(\"foo\").index(10)");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIndexedExpressionWithLeftExpressionWithStarContents() {
        testConversion("foo[*]", "");
    }

    @Test
    public void testIndexedExpressionWithLeftExpressionWithQuestionMark() {
        testConversion("foo[?bar]", "input.field(\"foo\").filter(x0 -> x0.field(\"bar\"))");
    }

    @Test
    public void testMultiSelectList2() {
        testConversion("[foo, bar]", "input.multiSelectList(x0 -> x0.field(\"foo\"), x1 -> x1.field(\"bar\"))");
    }

    @Test
    public void testMultiSelectList3() {
        testConversion("[foo, bar, baz]",
                       "input.multiSelectList(x0 -> x0.field(\"foo\"), x1 -> x1.field(\"bar\"), x2 -> x2.field(\"baz\"))");
    }

    @Test
    public void testNestedMultiSelectListsRight() {
        testConversion("[foo, [bar, baz]]",
                       "input.multiSelectList(x0 -> x0.field(\"foo\"), "
                       + "x1 -> x1.multiSelectList(x2 -> x2.field(\"bar\"), x3 -> x3.field(\"baz\")))");
    }

    @Test
    public void testNestedMultiSelectListsCenter() {
        testConversion("[foo, [bar, baz], bam]",
                       "input.multiSelectList(x0 -> x0.field(\"foo\"), "
                       + "x1 -> x1.multiSelectList(x2 -> x2.field(\"bar\"), x3 -> x3.field(\"baz\")), "
                       + "x4 -> x4.field(\"bam\"))");
    }

    @Test
    public void testNestedMultiSelectListsLeft() {
        testConversion("[[foo, bar], baz]", "input.multiSelectList(x0 -> x0.multiSelectList(x1 -> x1.field(\"foo\"), "
                                            + "x2 -> x2.field(\"bar\")), "
                                            + "x3 -> x3.field(\"baz\"))");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMultiSelectHash2() {
        testConversion("{fooK : fooV, barK : barV}", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMultiSelectHash3() {
        testConversion("{fooK : fooV, barK : barV, bazK : bazV}", "");
    }

    @Test
    public void testComparatorExpressionLT() {
        testConversion("foo < bar", "input.field(\"foo\").compare(\"<\", input.field(\"bar\"))");
    }

    @Test
    public void testComparatorExpressionGT() {
        testConversion("foo > bar", "input.field(\"foo\").compare(\">\", input.field(\"bar\"))");
    }

    @Test
    public void testComparatorExpressionLTE() {
        testConversion("foo <= bar", "input.field(\"foo\").compare(\"<=\", input.field(\"bar\"))");
    }

    @Test
    public void testComparatorExpressionGTE() {
        testConversion("foo >= bar", "input.field(\"foo\").compare(\">=\", input.field(\"bar\"))");
    }

    @Test
    public void testComparatorExpressionEQ() {
        testConversion("foo == bar", "input.field(\"foo\").compare(\"==\", input.field(\"bar\"))");
    }

    @Test
    public void testComparatorExpressionNEQ() {
        testConversion("foo != bar", "input.field(\"foo\").compare(\"!=\", input.field(\"bar\"))");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithNoNumbers2() {
        testConversion("[:]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithNoNumbers3() {
        testConversion("[::]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithStartNumber2() {
        testConversion("[10:]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithStopNumber2() {
        testConversion("[:10]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithStartStopNumber2() {
        testConversion("[10:20]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithStartNumber3() {
        testConversion("[10::]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithStopNumber3() {
        testConversion("[:10:]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithStartStopNumber3() {
        testConversion("[10:20:]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithStartStopStepNumber3() {
        testConversion("[10:20:30]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSliceExpressionWithStepNumber3() {
        testConversion("[::30]", "");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCurrentNode() {
        testConversion("@", "");
    }

    @Test
    public void testEmptyIdentifierUnquoted() {
        testConversion("foo_bar", "input.field(\"foo_bar\")");
    }

    @Test
    public void testIdentifierUnquoted() {
        testConversion("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_",
                       "input.field(\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_\")");
    }

    @Test
    public void testIdentifierQuoted() {
        testConversion("\"foo bar\"", "input.field(\"foo bar\")");
    }

    @Test
    public void testRawStringEmpty() {
        testConversion("''", "input.constant(\"\")");
    }

    @Test
    public void testRawStringWithValue() {
        testConversion("'foo bar'", "input.constant(\"foo bar\")");
    }

    @Test
    public void testNegativeNumber() {
        testConversion("foo[-10]", "input.field(\"foo\").index(-10)");
    }

    private void testConversion(String jmesPathString, String expectedCode) {
        assertThat(acceptorGenerator.interpret(jmesPathString, "input").toString()).isEqualTo((expectedCode));
    }

}