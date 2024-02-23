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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ExpressionTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void join_correctlyWrapsExpressions() {
        Expression expression1 = Expression.builder().expression("one").build();
        Expression expression2 = Expression.builder().expression("two").build();
        Expression expression3 = Expression.builder().expression("three").build();

        Expression coalescedExpression = Expression.join(Expression.join(expression1, expression2, " AND "),
                                                         expression3, " AND ");

        String expectedExpression = "((one) AND (two)) AND (three)";
        assertThat(coalescedExpression.expression(), is(expectedExpression));
    }

    @Test
    public void joinExpressions_correctlyJoins() {
        String result = Expression.joinExpressions("one", "two", " AND ");
        assertThat(result, is("(one) AND (two)"));
    }

    @Test
    public void joinNames_correctlyJoins() {
        Map<String, String> names1 = new HashMap<>();
        names1.put("one", "1");
        names1.put("two", "2");
        Map<String, String> names2 = new HashMap<>();
        names2.put("three", "3");
        names2.put("four", "4");

        Map<String, String> result = Expression.joinNames(names1, names2);

        assertThat(result.size(), is(4));
        assertThat(result, hasEntry("one", "1"));
        assertThat(result, hasEntry("two", "2"));
        assertThat(result, hasEntry("three", "3"));
        assertThat(result, hasEntry("four", "4"));
    }

    @Test
    public void joinNames_correctlyJoinsEmpty() {
        Map<String, String> names1 = new HashMap<>();
        names1.put("one", "1");
        names1.put("two", "2");
        Map<String, String> names2 = new HashMap<>();
        names2.put("three", "3");
        names2.put("four", "4");

        Map<String, String> result = Expression.joinNames(names1, null);
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry("one", "1"));
        assertThat(result, hasEntry("two", "2"));

        result = Expression.joinNames(null, names2);
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry("three", "3"));
        assertThat(result, hasEntry("four", "4"));

        result = Expression.joinNames(names1, Collections.emptyMap());
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry("one", "1"));
        assertThat(result, hasEntry("two", "2"));

        result = Expression.joinNames(Collections.emptyMap(), names2);
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry("three", "3"));
        assertThat(result, hasEntry("four", "4"));
    }

    @Test
    public void joinNames_conflictingKey() {
        Map<String, String> names1 = new HashMap<>();
        names1.put("one", "1");
        names1.put("two", "2");
        Map<String, String> names2 = new HashMap<>();
        names2.put("three", "3");
        names2.put("two", "4");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("two");
        Expression.joinNames(names1, names2);
    }

    @Test
    public void joinValues_correctlyJoins() {
        Map<String, AttributeValue> values1 = new HashMap<>();
        values1.put("one", EnhancedAttributeValue.fromString("1").toAttributeValue());
        values1.put("two", EnhancedAttributeValue.fromString("2").toAttributeValue());
        Map<String, AttributeValue> values2 = new HashMap<>();
        values2.put("three", EnhancedAttributeValue.fromString("3").toAttributeValue());
        values2.put("four", EnhancedAttributeValue.fromString("4").toAttributeValue());

        Map<String, AttributeValue> result = Expression.joinValues(values1, values2);

        assertThat(result.size(), is(4));
        assertThat(result, hasEntry("one", EnhancedAttributeValue.fromString("1").toAttributeValue()));
        assertThat(result, hasEntry("two", EnhancedAttributeValue.fromString("2").toAttributeValue()));
        assertThat(result, hasEntry("three", EnhancedAttributeValue.fromString("3").toAttributeValue()));
        assertThat(result, hasEntry("four", EnhancedAttributeValue.fromString("4").toAttributeValue()));
    }

    @Test
    public void joinValues_conflictingKey() {
        Map<String, AttributeValue> values1 = new HashMap<>();
        values1.put("one", EnhancedAttributeValue.fromString("1").toAttributeValue());
        values1.put("two", EnhancedAttributeValue.fromString("2").toAttributeValue());
        Map<String, AttributeValue> values2 = new HashMap<>();
        values2.put("three", EnhancedAttributeValue.fromString("3").toAttributeValue());
        values2.put("two", EnhancedAttributeValue.fromString("4").toAttributeValue());

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("two");
        Expression.joinValues(values1, values2);
    }

    @Test
    public void join_correctlyJoins() {
        Map<String, String> names1 = new HashMap<>();
        names1.put("one", "1");
        Map<String, AttributeValue> values1 = new HashMap<>();
        values1.put("one", EnhancedAttributeValue.fromString("1").toAttributeValue());
        Expression expression1 = Expression.builder()
                                           .expression("one")
                                           .expressionNames(names1)
                                           .expressionValues(values1)
                                           .build();

        Map<String, String> names2 = new HashMap<>();
        names2.put("two", "2");
        Map<String, AttributeValue> values2 = new HashMap<>();
        values2.put("two", EnhancedAttributeValue.fromString("2").toAttributeValue());
        Expression expression2 = Expression.builder()
                                           .expression("two")
                                           .expressionNames(names2)
                                           .expressionValues(values2)
                                           .build();

        Map<String, String> names3 = new HashMap<>();
        names3.put("three", "3");
        Map<String, AttributeValue> values3 = new HashMap<>();
        values3.put("three", EnhancedAttributeValue.fromString("3").toAttributeValue());
        Expression expression3 = Expression.builder()
                                           .expression("three")
                                           .expressionNames(names3)
                                           .expressionValues(values3)
                                           .build();

        Expression joinedExpression = Expression.join(Expression.AND, expression1, expression2, expression3);

        String expectedExpression = "(one) AND (two) AND (three)";
        assertThat(joinedExpression.expression(), is(expectedExpression));

        final Map<String, String> names = joinedExpression.expressionNames();
        assertThat(names.size(), is(3));
        assertThat(names, hasEntry("one", "1"));
        assertThat(names, hasEntry("two", "2"));
        assertThat(names, hasEntry("three", "3"));

        final Map<String, AttributeValue> values = joinedExpression.expressionValues();
        assertThat(values.size(), is(3));
        assertThat(values, hasEntry("one", AttributeValue.fromS("1")));
        assertThat(values, hasEntry("two", AttributeValue.fromS("2")));
        assertThat(values, hasEntry("three", AttributeValue.fromS("3")));
    }

    @Test
    public void join_conflictingKey() {
        Map<String, String> names1 = new HashMap<>();
        names1.put("one", "1");
        Map<String, AttributeValue> values1 = new HashMap<>();
        values1.put("one", EnhancedAttributeValue.fromString("1").toAttributeValue());
        Expression expression1 = Expression.builder()
                                           .expression("one")
                                           .expressionNames(names1)
                                           .expressionValues(values1)
                                           .build();

        Map<String, String> names2 = new HashMap<>();
        names2.put("one", "2");
        Map<String, AttributeValue> values2 = new HashMap<>();
        values2.put("one", EnhancedAttributeValue.fromString("2").toAttributeValue());
        Expression expression2 = Expression.builder()
                                           .expression("two")
                                           .expressionNames(names2)
                                           .expressionValues(values2)
                                           .build();

        Map<String, String> names3 = new HashMap<>();
        names3.put("one", "3");
        Map<String, AttributeValue> values3 = new HashMap<>();
        values3.put("one", EnhancedAttributeValue.fromString("3").toAttributeValue());
        Expression expression3 = Expression.builder()
                                           .expression("three")
                                           .expressionNames(names3)
                                           .expressionValues(values3)
                                           .build();

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("on");
        Expression.join(Expression.AND, expression1, expression2, expression3);
    }

    @Test
    public void and_correctlyJoins() {
        Map<String, String> names1 = new HashMap<>();
        names1.put("one", "1");
        Map<String, AttributeValue> values1 = new HashMap<>();
        values1.put("one", EnhancedAttributeValue.fromString("1").toAttributeValue());
        Expression expression1 = Expression.builder()
                                           .expression("one")
                                           .expressionNames(names1)
                                           .expressionValues(values1)
                                           .build();

        Map<String, String> names2 = new HashMap<>();
        names2.put("two", "2");
        Map<String, AttributeValue> values2 = new HashMap<>();
        values2.put("two", EnhancedAttributeValue.fromString("2").toAttributeValue());
        Expression expression2 = Expression.builder()
                                           .expression("two")
                                           .expressionNames(names2)
                                           .expressionValues(values2)
                                           .build();

        Map<String, String> names3 = new HashMap<>();
        names3.put("three", "3");
        Map<String, AttributeValue> values3 = new HashMap<>();
        values3.put("three", EnhancedAttributeValue.fromString("3").toAttributeValue());
        Expression expression3 = Expression.builder()
                                           .expression("three")
                                           .expressionNames(names3)
                                           .expressionValues(values3)
                                           .build();

        Expression joinedExpression = expression1.and(expression2, expression3);

        String expectedExpression = "(one) AND (two) AND (three)";
        assertThat(joinedExpression.expression(), is(expectedExpression));

        final Map<String, String> names = joinedExpression.expressionNames();
        assertThat(names.size(), is(3));
        assertThat(names, hasEntry("one", "1"));
        assertThat(names, hasEntry("two", "2"));
        assertThat(names, hasEntry("three", "3"));

        final Map<String, AttributeValue> values = joinedExpression.expressionValues();
        assertThat(values.size(), is(3));
        assertThat(values, hasEntry("one", AttributeValue.fromS("1")));
        assertThat(values, hasEntry("two", AttributeValue.fromS("2")));
        assertThat(values, hasEntry("three", AttributeValue.fromS("3")));
    }

    @Test
    public void or_correctlyJoins() {
        Map<String, String> names1 = new HashMap<>();
        names1.put("one", "1");
        Map<String, AttributeValue> values1 = new HashMap<>();
        values1.put("one", EnhancedAttributeValue.fromString("1").toAttributeValue());
        Expression expression1 = Expression.builder()
                                           .expression("one")
                                           .expressionNames(names1)
                                           .expressionValues(values1)
                                           .build();

        Map<String, String> names2 = new HashMap<>();
        names2.put("two", "2");
        Map<String, AttributeValue> values2 = new HashMap<>();
        values2.put("two", EnhancedAttributeValue.fromString("2").toAttributeValue());
        Expression expression2 = Expression.builder()
                                           .expression("two")
                                           .expressionNames(names2)
                                           .expressionValues(values2)
                                           .build();

        Map<String, String> names3 = new HashMap<>();
        names3.put("three", "3");
        Map<String, AttributeValue> values3 = new HashMap<>();
        values3.put("three", EnhancedAttributeValue.fromString("3").toAttributeValue());
        Expression expression3 = Expression.builder()
                                           .expression("three")
                                           .expressionNames(names3)
                                           .expressionValues(values3)
                                           .build();

        Expression joinedExpression = expression1.or(expression2, expression3);

        String expectedExpression = "(one) OR (two) OR (three)";
        assertThat(joinedExpression.expression(), is(expectedExpression));

        final Map<String, String> names = joinedExpression.expressionNames();
        assertThat(names.size(), is(3));
        assertThat(names, hasEntry("one", "1"));
        assertThat(names, hasEntry("two", "2"));
        assertThat(names, hasEntry("three", "3"));

        final Map<String, AttributeValue> values = joinedExpression.expressionValues();
        assertThat(values.size(), is(3));
        assertThat(values, hasEntry("one", AttributeValue.fromS("1")));
        assertThat(values, hasEntry("two", AttributeValue.fromS("2")));
        assertThat(values, hasEntry("three", AttributeValue.fromS("3")));
    }

    @Test
    public void compounded_expressions_correctlyJoins() {
        Map<String, String> names1 = new HashMap<>();
        names1.put("one", "1");
        Map<String, AttributeValue> values1 = new HashMap<>();
        values1.put("one", EnhancedAttributeValue.fromString("1").toAttributeValue());
        Expression expression1 = Expression.builder()
                                           .expression("one")
                                           .expressionNames(names1)
                                           .expressionValues(values1)
                                           .build();

        Map<String, String> names2 = new HashMap<>();
        names2.put("two", "2");
        Map<String, AttributeValue> values2 = new HashMap<>();
        values2.put("two", EnhancedAttributeValue.fromString("2").toAttributeValue());
        Expression expression2 = Expression.builder()
                                           .expression("two")
                                           .expressionNames(names2)
                                           .expressionValues(values2)
                                           .build();

        Map<String, String> names3 = new HashMap<>();
        names3.put("three", "3");
        Map<String, AttributeValue> values3 = new HashMap<>();
        values3.put("three", EnhancedAttributeValue.fromString("3").toAttributeValue());
        Expression expression3 = Expression.builder()
                                           .expression("three")
                                           .expressionNames(names3)
                                           .expressionValues(values3)
                                           .build();

        Expression joinedExpression = expression1.and(expression2.or(expression3));

        String expectedExpression = "(one) AND ((two) OR (three))";
        assertThat(joinedExpression.expression(), is(expectedExpression));

        final Map<String, String> names = joinedExpression.expressionNames();
        assertThat(names.size(), is(3));
        assertThat(names, hasEntry("one", "1"));
        assertThat(names, hasEntry("two", "2"));
        assertThat(names, hasEntry("three", "3"));

        final Map<String, AttributeValue> values = joinedExpression.expressionValues();
        assertThat(values.size(), is(3));
        assertThat(values, hasEntry("one", AttributeValue.fromS("1")));
        assertThat(values, hasEntry("two", AttributeValue.fromS("2")));
        assertThat(values, hasEntry("three", AttributeValue.fromS("3")));
    }

}
