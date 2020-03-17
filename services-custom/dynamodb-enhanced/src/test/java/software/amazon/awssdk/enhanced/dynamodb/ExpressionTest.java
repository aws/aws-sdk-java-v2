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

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
}
