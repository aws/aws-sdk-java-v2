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

package software.amazon.awssdk.codegen.jmespath;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.codegen.jmespath.component.Comparator;
import software.amazon.awssdk.codegen.jmespath.component.Expression;
import software.amazon.awssdk.codegen.jmespath.parser.JmesPathParser;

public class JmesPathParserTest {
    @Test
    public void testSubExpressionWithIdentifier() {
        Expression expression = JmesPathParser.parse("foo.bar");
        assertThat(expression.asSubExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asSubExpression().rightSubExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testSubExpressionWithMultiSelectList() {
        Expression expression = JmesPathParser.parse("foo.[bar]");
        assertThat(expression.asSubExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asSubExpression().rightSubExpression().asMultiSelectList().expressions()).hasSize(1);
        assertThat(expression.asSubExpression().rightSubExpression().asMultiSelectList().expressions().get(0).asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testSubExpressionWithMultiSelectHash() {
        Expression expression = JmesPathParser.parse("foo.{bar : baz}");
        assertThat(expression.asSubExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asSubExpression().rightSubExpression().asMultiSelectHash().keyValueExpressions()).hasSize(1);
        assertThat(expression.asSubExpression().rightSubExpression().asMultiSelectHash().keyValueExpressions().get(0).key()).isEqualTo("bar");
        assertThat(expression.asSubExpression().rightSubExpression().asMultiSelectHash().keyValueExpressions().get(0).value().asIdentifier()).isEqualTo("baz");
    }

    @Test
    public void testSubExpressionWithFunction() {
        Expression expression = JmesPathParser.parse("foo.length()");
        assertThat(expression.asSubExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asSubExpression().rightSubExpression().asFunctionExpression().function()).isEqualTo("length");
    }

    @Test
    public void testSubExpressionWithStar() {
        Expression expression = JmesPathParser.parse("foo.*");
        assertThat(expression.asSubExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asSubExpression().rightSubExpression().isWildcardExpression()).isTrue();
    }

    @Test
    public void testPipeExpression() {
        Expression expression = JmesPathParser.parse("foo | bar");
        assertThat(expression.asPipeExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asPipeExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testOrExpression() {
        Expression expression = JmesPathParser.parse("foo || bar");
        assertThat(expression.asOrExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asOrExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testAndExpression() {
        Expression expression = JmesPathParser.parse("foo && bar");
        assertThat(expression.asAndExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asAndExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testNotExpression() {
        Expression expression = JmesPathParser.parse("!foo");
        assertThat(expression.asNotExpression().expression().asIdentifier()).isEqualTo("foo");
    }

    @Test
    public void testParenExpression() {
        Expression expression = JmesPathParser.parse("(foo)");
        assertThat(expression.asParenExpression().expression().asIdentifier()).isEqualTo("foo");
    }

    @Test
    public void testWildcardExpression() {
        Expression expression = JmesPathParser.parse("*");
        assertThat(expression.isWildcardExpression()).isTrue();
    }

    @Test
    public void testIndexedExpressionWithoutLeftExpressionWithoutContents() {
        Expression expression = JmesPathParser.parse("[]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().isBracketSpecifierWithoutContents()).isTrue();
    }

    @Test
    public void testIndexedExpressionWithoutLeftExpressionWithNumberContents() {
        Expression expression = JmesPathParser.parse("[10]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asNumber()).isEqualTo(10);
    }

    @Test
    public void testIndexedExpressionWithoutLeftExpressionWithStarContents() {
        Expression expression = JmesPathParser.parse("[*]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().isWildcardExpression()).isTrue();
    }

    @Test
    public void testIndexedExpressionWithoutLeftExpressionWithQuestionMark() {
        Expression expression = JmesPathParser.parse("[?foo]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithQuestionMark().expression().asIdentifier()).isEqualTo("foo");
    }

    @Test
    public void testIndexedExpressionWithLeftExpressionWithoutContents() {
        Expression expression = JmesPathParser.parse("foo[]");
        assertThat(expression.asIndexExpression().expression()).hasValueSatisfying(e -> assertThat(e.asIdentifier()).isEqualTo("foo"));
        assertThat(expression.asIndexExpression().bracketSpecifier().isBracketSpecifierWithoutContents()).isTrue();
    }

    @Test
    public void testIndexedExpressionWithLeftExpressionWithContents() {
        Expression expression = JmesPathParser.parse("foo[10]");
        assertThat(expression.asIndexExpression().expression()).hasValueSatisfying(e -> assertThat(e.asIdentifier()).isEqualTo("foo"));
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asNumber()).isEqualTo(10);
    }

    @Test
    public void testIndexedExpressionWithLeftExpressionWithStarContents() {
        Expression expression = JmesPathParser.parse("foo[*]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).hasValueSatisfying(e -> assertThat(e.asIdentifier()).isEqualTo("foo"));
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().isWildcardExpression()).isTrue();
    }

    @Test
    public void testIndexedExpressionWithLeftExpressionWithQuestionMark() {
        Expression expression = JmesPathParser.parse("foo[?bar]");
        assertThat(expression.isIndexExpression()).isTrue();

        assertThat(expression.asIndexExpression().expression()).hasValueSatisfying(e -> assertThat(e.asIdentifier()).isEqualTo("foo"));
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithQuestionMark().expression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testMultiSelectList2() {
        Expression expression = JmesPathParser.parse("[foo, bar]");
        assertThat(expression.asMultiSelectList().expressions()).hasSize(2);
        assertThat(expression.asMultiSelectList().expressions().get(0).asIdentifier()).isEqualTo("foo");
        assertThat(expression.asMultiSelectList().expressions().get(1).asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testMultiSelectList3() {
        Expression expression = JmesPathParser.parse("[foo, bar, baz]");
        assertThat(expression.asMultiSelectList().expressions()).hasSize(3);
        assertThat(expression.asMultiSelectList().expressions().get(0).asIdentifier()).isEqualTo("foo");
        assertThat(expression.asMultiSelectList().expressions().get(1).asIdentifier()).isEqualTo("bar");
        assertThat(expression.asMultiSelectList().expressions().get(2).asIdentifier()).isEqualTo("baz");
    }

    @Test
    public void testNestedMultiSelectListsRight() {
        Expression expression = JmesPathParser.parse("[foo, [bar, baz]]");
        assertThat(expression.asMultiSelectList().expressions()).hasSize(2);
        assertThat(expression.asMultiSelectList().expressions().get(0).asIdentifier()).isEqualTo("foo");
        assertThat(expression.asMultiSelectList().expressions().get(1).asMultiSelectList().expressions()).hasSize(2);
        assertThat(expression.asMultiSelectList().expressions().get(1).asMultiSelectList().expressions().get(0).asIdentifier()).isEqualTo("bar");
        assertThat(expression.asMultiSelectList().expressions().get(1).asMultiSelectList().expressions().get(1).asIdentifier()).isEqualTo("baz");
    }

    @Test
    public void testNestedMultiSelectListsCenter() {
        Expression expression = JmesPathParser.parse("[foo, [bar, baz], bam]");
        assertThat(expression.asMultiSelectList().expressions()).hasSize(3);
        assertThat(expression.asMultiSelectList().expressions().get(0).asIdentifier()).isEqualTo("foo");
        assertThat(expression.asMultiSelectList().expressions().get(1).asMultiSelectList().expressions()).hasSize(2);
        assertThat(expression.asMultiSelectList().expressions().get(1).asMultiSelectList().expressions().get(0).asIdentifier()).isEqualTo("bar");
        assertThat(expression.asMultiSelectList().expressions().get(1).asMultiSelectList().expressions().get(1).asIdentifier()).isEqualTo("baz");
        assertThat(expression.asMultiSelectList().expressions().get(2).asIdentifier()).isEqualTo("bam");
    }

    @Test
    public void testNestedMultiSelectListsLeft() {
        Expression expression = JmesPathParser.parse("[[foo, bar], baz]");
        assertThat(expression.asMultiSelectList().expressions()).hasSize(2);
        assertThat(expression.asMultiSelectList().expressions().get(0).asMultiSelectList().expressions().get(0).asIdentifier()).isEqualTo("foo");
        assertThat(expression.asMultiSelectList().expressions().get(0).asMultiSelectList().expressions().get(1).asIdentifier()).isEqualTo("bar");
        assertThat(expression.asMultiSelectList().expressions().get(1).asIdentifier()).isEqualTo("baz");
    }

    @Test
    public void testMultiSelectHash2() {
        Expression expression = JmesPathParser.parse("{fooK : fooV, barK : barV}");
        assertThat(expression.asMultiSelectHash().keyValueExpressions()).hasSize(2);
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(0).key()).isEqualTo("fooK");
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(0).value().asIdentifier()).isEqualTo("fooV");
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(1).key()).isEqualTo("barK");
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(1).value().asIdentifier()).isEqualTo("barV");
    }

    @Test
    public void testMultiSelectHash3() {
        Expression expression = JmesPathParser.parse("{fooK : fooV, barK : barV, bazK : bazV}");
        assertThat(expression.asMultiSelectHash().keyValueExpressions()).hasSize(3);
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(0).key()).isEqualTo("fooK");
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(0).value().asIdentifier()).isEqualTo("fooV");
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(1).key()).isEqualTo("barK");
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(1).value().asIdentifier()).isEqualTo("barV");
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(2).key()).isEqualTo("bazK");
        assertThat(expression.asMultiSelectHash().keyValueExpressions().get(2).value().asIdentifier()).isEqualTo("bazV");
    }

    @Test
    public void testComparatorExpressionLT() {
        Expression expression = JmesPathParser.parse("foo < bar");
        assertThat(expression.asComparatorExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asComparatorExpression().comparator()).isEqualTo(Comparator.LESS_THAN);
        assertThat(expression.asComparatorExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testComparatorExpressionGT() {
        Expression expression = JmesPathParser.parse("foo > bar");
        assertThat(expression.asComparatorExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asComparatorExpression().comparator()).isEqualTo(Comparator.GREATER_THAN);
        assertThat(expression.asComparatorExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testComparatorExpressionLTE() {
        Expression expression = JmesPathParser.parse("foo <= bar");
        assertThat(expression.asComparatorExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asComparatorExpression().comparator()).isEqualTo(Comparator.LESS_THAN_OR_EQUAL);
        assertThat(expression.asComparatorExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testComparatorExpressionGTE() {
        Expression expression = JmesPathParser.parse("foo >= bar");
        assertThat(expression.asComparatorExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asComparatorExpression().comparator()).isEqualTo(Comparator.GREATER_THAN_OR_EQUAL);
        assertThat(expression.asComparatorExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testComparatorExpressionEQ() {
        Expression expression = JmesPathParser.parse("foo == bar");
        assertThat(expression.asComparatorExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asComparatorExpression().comparator()).isEqualTo(Comparator.EQUAL);
        assertThat(expression.asComparatorExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testComparatorExpressionNEQ() {
        Expression expression = JmesPathParser.parse("foo != bar");
        assertThat(expression.asComparatorExpression().leftExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asComparatorExpression().comparator()).isEqualTo(Comparator.NOT_EQUAL);
        assertThat(expression.asComparatorExpression().rightExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testSliceExpressionWithNoNumbers2() {
        Expression expression = JmesPathParser.parse("[:]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).isNotPresent();
    }

    @Test
    public void testSliceExpressionWithNoNumbers3() {
        Expression expression = JmesPathParser.parse("[::]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).isNotPresent();
    }

    @Test
    public void testSliceExpressionWithStartNumber2() {
        Expression expression = JmesPathParser.parse("[10:]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).hasValue(10);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).isNotPresent();
    }

    @Test
    public void testSliceExpressionWithStopNumber2() {
        Expression expression = JmesPathParser.parse("[:10]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).hasValue(10);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).isNotPresent();
    }

    @Test
    public void testSliceExpressionWithStartStopNumber2() {
        Expression expression = JmesPathParser.parse("[10:20]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).hasValue(10);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).hasValue(20);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).isNotPresent();
    }

    @Test
    public void testSliceExpressionWithStartNumber3() {
        Expression expression = JmesPathParser.parse("[10::]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).hasValue(10);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).isNotPresent();
    }

    @Test
    public void testSliceExpressionWithStopNumber3() {
        Expression expression = JmesPathParser.parse("[:10:]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).hasValue(10);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).isNotPresent();
    }

    @Test
    public void testSliceExpressionWithStartStopNumber3() {
        Expression expression = JmesPathParser.parse("[10:20:]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).hasValue(10);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).hasValue(20);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).isNotPresent();
    }

    @Test
    public void testSliceExpressionWithStartStopStepNumber3() {
        Expression expression = JmesPathParser.parse("[10:20:30]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).hasValue(10);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).hasValue(20);
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).hasValue(30);
    }

    @Test
    public void testSliceExpressionWithStepNumber3() {
        Expression expression = JmesPathParser.parse("[::30]");
        assertThat(expression.isIndexExpression()).isTrue();
        assertThat(expression.asIndexExpression().expression()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().start()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().stop()).isNotPresent();
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asSliceExpression().step()).hasValue(30);
    }

    @Test
    public void testFunction0Args() {
        Expression expression = JmesPathParser.parse("length()");
        assertThat(expression.asFunctionExpression().function()).isEqualTo("length");
        assertThat(expression.asFunctionExpression().functionArgs()).isEmpty();
    }

    @Test
    public void testFunction1ExpressionArg() {
        Expression expression = JmesPathParser.parse("length(foo)");
        assertThat(expression.asFunctionExpression().function()).isEqualTo("length");
        assertThat(expression.asFunctionExpression().functionArgs()).hasSize(1);
        assertThat(expression.asFunctionExpression().functionArgs().get(0).asExpression().asIdentifier()).isEqualTo("foo");
    }

    @Test
    public void testFunction2ExpressionArg() {
        Expression expression = JmesPathParser.parse("length(foo, bar)");
        assertThat(expression.asFunctionExpression().function()).isEqualTo("length");
        assertThat(expression.asFunctionExpression().functionArgs()).hasSize(2);
        assertThat(expression.asFunctionExpression().functionArgs().get(0).asExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asFunctionExpression().functionArgs().get(1).asExpression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testFunction3ExpressionArg() {
        Expression expression = JmesPathParser.parse("length(foo, bar, baz)");
        assertThat(expression.asFunctionExpression().function()).isEqualTo("length");
        assertThat(expression.asFunctionExpression().functionArgs()).hasSize(3);
        assertThat(expression.asFunctionExpression().functionArgs().get(0).asExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asFunctionExpression().functionArgs().get(1).asExpression().asIdentifier()).isEqualTo("bar");
        assertThat(expression.asFunctionExpression().functionArgs().get(2).asExpression().asIdentifier()).isEqualTo("baz");
    }

    @Test
    public void testFunction1ExpressionTypeArg() {
        Expression expression = JmesPathParser.parse("length(&foo)");
        assertThat(expression.asFunctionExpression().function()).isEqualTo("length");
        assertThat(expression.asFunctionExpression().functionArgs()).hasSize(1);
        assertThat(expression.asFunctionExpression().functionArgs().get(0).asExpressionType().expression().asIdentifier()).isEqualTo("foo");
    }

    @Test
    public void testFunction2ExpressionTypeArg() {
        Expression expression = JmesPathParser.parse("length(&foo, &bar)");
        assertThat(expression.asFunctionExpression().function()).isEqualTo("length");
        assertThat(expression.asFunctionExpression().functionArgs()).hasSize(2);
        assertThat(expression.asFunctionExpression().functionArgs().get(0).asExpressionType().expression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asFunctionExpression().functionArgs().get(1).asExpressionType().expression().asIdentifier()).isEqualTo("bar");
    }

    @Test
    public void testFunction3ExpressionTypeArg() {
        Expression expression = JmesPathParser.parse("length(&foo, &bar, &baz)");
        assertThat(expression.asFunctionExpression().function()).isEqualTo("length");
        assertThat(expression.asFunctionExpression().functionArgs()).hasSize(3);
        assertThat(expression.asFunctionExpression().functionArgs().get(0).asExpressionType().expression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asFunctionExpression().functionArgs().get(1).asExpressionType().expression().asIdentifier()).isEqualTo("bar");
        assertThat(expression.asFunctionExpression().functionArgs().get(2).asExpressionType().expression().asIdentifier()).isEqualTo("baz");
    }

    @Test
    public void testFunction3MixedExpressionTypeArg() {
        Expression expression = JmesPathParser.parse("length(foo, &bar, baz)");
        assertThat(expression.asFunctionExpression().function()).isEqualTo("length");
        assertThat(expression.asFunctionExpression().functionArgs()).hasSize(3);
        assertThat(expression.asFunctionExpression().functionArgs().get(0).asExpression().asIdentifier()).isEqualTo("foo");
        assertThat(expression.asFunctionExpression().functionArgs().get(1).asExpressionType().expression().asIdentifier()).isEqualTo("bar");
        assertThat(expression.asFunctionExpression().functionArgs().get(2).asExpression().asIdentifier()).isEqualTo("baz");
    }

    @Test
    public void testCurrentNode() {
        Expression expression = JmesPathParser.parse("@");
        assertThat(expression.isCurrentNode()).isTrue();
    }

    @Test
    public void testEmptyIdentifierUnquoted() {
        Expression expression = JmesPathParser.parse("foo_bar");
        assertThat(expression.asIdentifier()).isEqualTo("foo_bar");
    }

    @Test
    public void testIdentifierUnquoted() {
        Expression expression = JmesPathParser.parse("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
        assertThat(expression.asIdentifier()).isEqualTo("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
    }

    @Test
    public void testIdentifierQuoted() {
        Expression expression = JmesPathParser.parse("\"foo bar\"");
        assertThat(expression.asIdentifier()).isEqualTo("foo bar");
    }

    @Test
    public void testIdentifierQuotedWithEscapes() {
        Expression expression = JmesPathParser.parse("\"foo \\\" \\\\ \\/ \\b \\f \\n \\r \\t \\u0000 \\uffff bar\"");
        assertThat(expression.asIdentifier()).isEqualTo("foo \" \\ / \b \f \n \r \t \u0000 \uffff bar");
    }

    @Test
    public void testRawStringEmpty() {
        Expression expression = JmesPathParser.parse("''");
        assertThat(expression.asRawString()).isEmpty();
    }

    @Test
    public void testRawStringWithValue() {
        Expression expression = JmesPathParser.parse("'foo bar'");
        assertThat(expression.asRawString()).isEqualTo("foo bar");
    }

    @Test
    public void testRawStringWithEscapedSequences() {
        Expression expression = JmesPathParser.parse("'foo \\' \\\\ \\/ \\b \\f \\n \\r \\t \\u0000 \\uffff bar'");
        assertThat(expression.asRawString()).isEqualTo("foo \\' \\\\ \\/ \\b \\f \\n \\r \\t \\u0000 \\uffff bar");
    }

    @Test
    public void testNegativeNumber() {
        Expression expression = JmesPathParser.parse("[-10]");
        assertThat(expression.asIndexExpression().bracketSpecifier().asBracketSpecifierWithContents().asNumber()).isEqualTo(-10);
    }
}