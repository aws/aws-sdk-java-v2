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

package software.amazon.awssdk.enhanced.dynamodb.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.ConditionEvaluator;

/**
 * Unit tests for the {@link Condition} DSL: primitive conditions (eq, gt, between, etc.), combinators (and, or, not), and tree
 * conditions using {@link Condition#group(Condition)} for precedence.
 * <p>
 * These tests do not use DynamoDB; they assert that condition objects are built correctly and that combined conditions are
 * non-null and can be used in a spec.
 */
public class ConditionTest {

    /**
     * Asserts that {@link Condition#eq(String, Object)} returns a non-null condition and that chaining
     * {@link Condition#and(Condition)} produces a combined condition.
     */
    @Test
    public void eqAndAndCombination() {
        Condition c = Condition.eq("status", "ACTIVE").and(Condition.gt("value", 0));
        assertThat(c).isNotNull();
    }

    /**
     * Asserts that {@link Condition#or(Condition)} combines two conditions and returns non-null.
     */
    @Test
    public void orCombination() {
        Condition c = Condition.eq("a", 1).or(Condition.eq("b", 2));
        assertThat(c).isNotNull();
    }

    /**
     * Asserts that {@link Condition#not()} returns a negated condition (non-null).
     */
    @Test
    public void not() {
        Condition c = Condition.eq("x", 1).not();
        assertThat(c).isNotNull();
    }

    /**
     * Asserts that {@link Condition#group(Condition)} wraps a condition for precedence and that a tree expression (e.g. (a=1 AND
     * b>0) OR c=2) can be built. Verifies the grouped condition is non-null and distinct from the inner condition.
     */
    @Test
    public void groupForPrecedence() {
        Condition inner = Condition.eq("a", 1).and(Condition.gt("b", 0));
        Condition grouped = Condition.group(inner);
        assertThat(grouped).isNotNull();
        Condition tree = grouped.or(Condition.eq("c", 2));
        assertThat(tree).isNotNull();
    }

    /**
     * Asserts that all primitive factory methods return non-null conditions: eq, gt, gte, lt, lte, between, contains,
     * beginsWith.
     */
    @Test
    public void primitiveConditions() {
        assertThat(Condition.eq("k", "v")).isNotNull();
        assertThat(Condition.gt("k", 1)).isNotNull();
        assertThat(Condition.gte("k", 1)).isNotNull();
        assertThat(Condition.lt("k", 1)).isNotNull();
        assertThat(Condition.lte("k", 1)).isNotNull();
        assertThat(Condition.between("k", 0, 10)).isNotNull();
        assertThat(Condition.contains("k", "sub")).isNotNull();
        assertThat(Condition.beginsWith("k", "pre")).isNotNull();
    }

    /**
     * Asserts that equality is consistent for the same condition built twice (eq with same attribute and value).
     */
    @Test
    public void eqConditionsEqual() {
        Condition c1 = Condition.eq("id", 42);
        Condition c2 = Condition.eq("id", 42);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }

    /**
     * Asserts that and() and or() produce equal combinations when given the same operands.
     */
    @Test
    public void andOrEquality() {
        Condition left = Condition.eq("a", 1);
        Condition right = Condition.eq("b", 2);
        Condition and1 = left.and(right);
        Condition and2 = Condition.eq("a", 1).and(Condition.eq("b", 2));
        assertThat(and1).isEqualTo(and2);
        Condition or1 = left.or(right);
        Condition or2 = Condition.eq("a", 1).or(Condition.eq("b", 2));
        assertThat(or1).isEqualTo(or2);
    }

    // --- Nested attribute (dot-path) evaluation tests ---

    private Map<String, Object> itemWithNestedAddress() {
        Map<String, Object> address = new HashMap<>();
        address.put("city", "Seattle");
        address.put("zip", "98101");
        address.put("state", "WA");
        Map<String, Object> item = new HashMap<>();
        item.put("customerId", "c1");
        item.put("address", address);
        return item;
    }

    private Map<String, Object> itemWithDeepNesting() {
        Map<String, Object> geo = new HashMap<>();
        geo.put("lat", 47.6);
        geo.put("lng", -122.3);
        Map<String, Object> address = new HashMap<>();
        address.put("city", "Seattle");
        address.put("geo", geo);
        Map<String, Object> item = new HashMap<>();
        item.put("address", address);
        return item;
    }

    @Test
    public void nestedAttribute_eqMatch() {
        Map<String, Object> item = itemWithNestedAddress();
        assertThat(ConditionEvaluator.evaluate(Condition.eq("address.city", "Seattle"), item)).isTrue();
    }

    @Test
    public void nestedAttribute_eqNoMatch() {
        Map<String, Object> item = itemWithNestedAddress();
        assertThat(ConditionEvaluator.evaluate(Condition.eq("address.city", "Portland"), item)).isFalse();
    }

    @Test
    public void nestedAttribute_gtOnString() {
        Map<String, Object> item = itemWithNestedAddress();
        assertThat(ConditionEvaluator.evaluate(Condition.gt("address.state", "TX"), item)).isTrue();
        assertThat(ConditionEvaluator.evaluate(Condition.gt("address.state", "WA"), item)).isFalse();
    }

    @Test
    public void nestedAttribute_betweenOnZip() {
        Map<String, Object> item = itemWithNestedAddress();
        assertThat(ConditionEvaluator.evaluate(Condition.between("address.zip", "90000", "99999"), item)).isTrue();
        assertThat(ConditionEvaluator.evaluate(Condition.between("address.zip", "10000", "20000"), item)).isFalse();
    }

    @Test
    public void nestedAttribute_contains() {
        Map<String, Object> item = itemWithNestedAddress();
        assertThat(ConditionEvaluator.evaluate(Condition.contains("address.city", "att"), item)).isTrue();
        assertThat(ConditionEvaluator.evaluate(Condition.contains("address.city", "xyz"), item)).isFalse();
    }

    @Test
    public void nestedAttribute_beginsWith() {
        Map<String, Object> item = itemWithNestedAddress();
        assertThat(ConditionEvaluator.evaluate(Condition.beginsWith("address.city", "Sea"), item)).isTrue();
        assertThat(ConditionEvaluator.evaluate(Condition.beginsWith("address.city", "Por"), item)).isFalse();
    }

    @Test
    public void nestedAttribute_deepPath() {
        Map<String, Object> item = itemWithDeepNesting();
        assertThat(ConditionEvaluator.evaluate(Condition.gt("address.geo.lat", 40.0), item)).isTrue();
        assertThat(ConditionEvaluator.evaluate(Condition.lt("address.geo.lng", -100.0), item)).isTrue();
    }

    @Test
    public void nestedAttribute_missingIntermediateReturnsNull() {
        Map<String, Object> item = new HashMap<>();
        item.put("customerId", "c1");
        assertThat(ConditionEvaluator.evaluate(Condition.eq("address.city", "Seattle"), item)).isFalse();
    }

    @Test
    public void nestedAttribute_intermediateNotMapReturnsNull() {
        Map<String, Object> item = new HashMap<>();
        item.put("address", "plain-string-not-a-map");
        assertThat(ConditionEvaluator.evaluate(Condition.eq("address.city", "Seattle"), item)).isFalse();
    }

    @Test
    public void nestedAttribute_andCombination() {
        Map<String, Object> item = itemWithNestedAddress();
        Condition c = Condition.eq("address.city", "Seattle").and(Condition.eq("address.state", "WA"));
        assertThat(ConditionEvaluator.evaluate(c, item)).isTrue();

        Condition noMatch = Condition.eq("address.city", "Seattle").and(Condition.eq("address.state", "CA"));
        assertThat(ConditionEvaluator.evaluate(noMatch, item)).isFalse();
    }

    @Test
    public void nestedAttribute_orCombination() {
        Map<String, Object> item = itemWithNestedAddress();
        Condition c = Condition.eq("address.city", "Portland").or(Condition.eq("address.state", "WA"));
        assertThat(ConditionEvaluator.evaluate(c, item)).isTrue();
    }

    @Test
    public void nestedAttribute_mixedWithFlatAttribute() {
        Map<String, Object> item = itemWithNestedAddress();
        Condition c = Condition.eq("customerId", "c1").and(Condition.eq("address.city", "Seattle"));
        assertThat(ConditionEvaluator.evaluate(c, item)).isTrue();
    }

    @Test
    public void nestedAttribute_combinedMapView() {
        Map<String, Object> primary = new HashMap<>();
        primary.put("customerId", "c1");

        Map<String, Object> address = new HashMap<>();
        address.put("city", "Seattle");
        Map<String, Object> secondary = new HashMap<>();
        secondary.put("address", address);

        Condition c = Condition.eq("address.city", "Seattle");
        assertThat(ConditionEvaluator.evaluate(c, primary, secondary)).isTrue();
    }

    @Test
    public void resolveAttribute_flatKeyUnchanged() {
        Map<String, Object> item = new HashMap<>();
        item.put("name", "Alice");
        assertThat(ConditionEvaluator.resolveAttribute(item, "name")).isEqualTo("Alice");
    }

    @Test
    public void resolveAttribute_nullItemReturnsNull() {
        assertThat(ConditionEvaluator.resolveAttribute(null, "name")).isNull();
    }

    @Test
    public void resolveAttribute_nullAttributeReturnsNull() {
        Map<String, Object> item = new HashMap<>();
        assertThat(ConditionEvaluator.resolveAttribute(item, null)).isNull();
    }
}
