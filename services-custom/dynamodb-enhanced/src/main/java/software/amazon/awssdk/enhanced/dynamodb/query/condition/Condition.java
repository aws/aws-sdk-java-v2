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

package software.amazon.awssdk.enhanced.dynamodb.query.condition;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Represents a filter condition for enhanced queries. Conditions can be combined with {@link #and(Condition)},
 * {@link #or(Condition)}, and {@link #not()}, and grouped with {@link #group(Condition)}.
 * <p>
 * Use the static factory methods to build conditions: {@link #eq(String, Object)}, {@link #gt(String, Object)},
 * {@link #gte(String, Object)}, {@link #lt(String, Object)}, {@link #lte(String, Object)},
 * {@link #between(String, Object, Object)}, {@link #contains(String, Object)}, {@link #beginsWith(String, String)}.
 * <p>
 * Implementation classes are package-private inner types; external code should interact only through the {@code Condition}
 * interface and its static/default methods.
 */
@SdkInternalApi
public interface Condition {

    /**
     * Combines this condition with another using logical AND.
     */
    default Condition and(Condition other) {
        return new And(this, other);
    }

    /**
     * Combines this condition with another using logical OR.
     */
    default Condition or(Condition other) {
        return new Or(this, other);
    }

    /**
     * Negates this condition.
     */
    default Condition not() {
        return new Not(this);
    }

    // ---- static factories -----------------------------------------------

    /**
     * Creates an equality condition: attribute = value.
     */
    static Condition eq(String attribute, Object value) {
        return new Comparator(attribute, "=", value);
    }

    /**
     * Creates a greater-than condition: attribute &gt; value.
     */
    static Condition gt(String attribute, Object value) {
        return new Comparator(attribute, ">", value);
    }

    /**
     * Creates a greater-than-or-equal condition: attribute &gt;= value.
     */
    static Condition gte(String attribute, Object value) {
        return new Comparator(attribute, ">=", value);
    }

    /**
     * Creates a less-than condition: attribute &lt; value.
     */
    static Condition lt(String attribute, Object value) {
        return new Comparator(attribute, "<", value);
    }

    /**
     * Creates a less-than-or-equal condition: attribute &lt;= value.
     */
    static Condition lte(String attribute, Object value) {
        return new Comparator(attribute, "<=", value);
    }

    /**
     * Creates a between condition: attribute BETWEEN from AND to.
     */
    static Condition between(String attribute, Object from, Object to) {
        return new Between(attribute, from, to);
    }

    /**
     * Creates a contains condition (for string or set attributes).
     */
    static Condition contains(String attribute, Object value) {
        return new Function(attribute, "contains", value);
    }

    /**
     * Creates a begins-with condition for string attributes.
     */
    static Condition beginsWith(String attribute, String prefix) {
        return new Function(attribute, "begins_with", prefix);
    }

    /**
     * Groups a condition in parentheses (for precedence in AND/OR trees).
     */
    static Condition group(Condition inner) {
        return new Group(inner);
    }

    // ---- inner implementation classes -----------------------------------

    /**
     * Comparison condition ({@code =}, {@code >}, {@code >=}, {@code <}, {@code <=}).
     */
    final class Comparator implements Condition {

        private final String attribute;
        private final String operator;
        private final Object value;

        Comparator(String attribute, String operator, Object value) {
            this.attribute = attribute;
            this.operator = operator;
            this.value = value;
        }

        String attribute() {
            return attribute;
        }

        String operator() {
            return operator;
        }

        Object value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Comparator that = (Comparator) o;
            return Objects.equals(attribute, that.attribute)
                   && Objects.equals(operator, that.operator)
                   && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(attribute);
            result = 31 * result + Objects.hashCode(operator);
            result = 31 * result + Objects.hashCode(value);
            return result;
        }
    }

    /**
     * Range condition: attribute BETWEEN from AND to.
     */
    final class Between implements Condition {

        private final String attribute;
        private final Object from;
        private final Object to;

        Between(String attribute, Object from, Object to) {
            this.attribute = attribute;
            this.from = from;
            this.to = to;
        }

        String attribute() {
            return attribute;
        }

        Object from() {
            return from;
        }

        Object to() {
            return to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Between that = (Between) o;
            return Objects.equals(attribute, that.attribute)
                   && Objects.equals(from, that.from)
                   && Objects.equals(to, that.to);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(attribute);
            result = 31 * result + Objects.hashCode(from);
            result = 31 * result + Objects.hashCode(to);
            return result;
        }
    }

    /**
     * Function-based condition ({@code contains}, {@code begins_with}).
     */
    final class Function implements Condition {

        private final String attribute;
        private final String function;
        private final Object value;

        Function(String attribute, String function, Object value) {
            this.attribute = attribute;
            this.function = function;
            this.value = value;
        }

        String attribute() {
            return attribute;
        }

        String function() {
            return function;
        }

        Object value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Function that = (Function) o;
            return Objects.equals(attribute, that.attribute)
                   && Objects.equals(function, that.function)
                   && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(attribute);
            result = 31 * result + Objects.hashCode(function);
            result = 31 * result + Objects.hashCode(value);
            return result;
        }
    }

    /**
     * Logical AND of two conditions.
     */
    final class And implements Condition {

        private final Condition left;
        private final Condition right;

        And(Condition left, Condition right) {
            this.left = left;
            this.right = right;
        }

        Condition left() {
            return left;
        }

        Condition right() {
            return right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            And that = (And) o;
            return Objects.equals(left, that.left)
                   && Objects.equals(right, that.right);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(left);
            result = 31 * result + Objects.hashCode(right);
            return result;
        }
    }

    /**
     * Logical OR of two conditions.
     */
    final class Or implements Condition {

        private final Condition left;
        private final Condition right;

        Or(Condition left, Condition right) {
            this.left = left;
            this.right = right;
        }

        Condition left() {
            return left;
        }

        Condition right() {
            return right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Or that = (Or) o;
            return Objects.equals(left, that.left)
                   && Objects.equals(right, that.right);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(left);
            result = 31 * result + Objects.hashCode(right);
            return result;
        }
    }

    /**
     * Negation of a condition. Double negation cancels out.
     */
    final class Not implements Condition {

        private final Condition inner;

        Not(Condition inner) {
            this.inner = inner;
        }

        Condition inner() {
            return inner;
        }

        @Override
        public Condition not() {
            return inner;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Not that = (Not) o;
            return Objects.equals(inner, that.inner);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(inner);
        }
    }

    /**
     * Grouping condition for precedence in AND/OR trees.
     */
    final class Group implements Condition {

        private final Condition inner;

        Group(Condition inner) {
            this.inner = inner;
        }

        Condition inner() {
            return inner;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Group that = (Group) o;
            return Objects.equals(inner, that.inner);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(inner);
        }
    }
}
