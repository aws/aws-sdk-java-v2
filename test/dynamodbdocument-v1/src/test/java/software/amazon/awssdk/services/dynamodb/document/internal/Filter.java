/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document.internal;

import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;

/**
 * Abstract base class for both query filters and scan filters.
 */
public abstract class Filter<T extends Filter<T>> {
    private final String attribute;
    private ComparisonOperator op;
    private Object[] values;

    /**
     * Create a filter for the specified top-level attribute.
     *
     * @param attrName
     *            attribute name
     */
    protected Filter(String attrName) {
        InternalUtils.checkInvalidAttrName(attrName);
        this.attribute = attrName;
    }

    /** Returns the attribute name. */
    public String getAttribute() {
        return attribute;
    }

    public ComparisonOperator getComparisonOperator() {
        return op;
    }

    public Object[] values() {
        return values == null ? null : values.clone();
    }

    @SuppressWarnings("unchecked")
    protected T values(Object... values) {
        this.values = values.clone();
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    private T withComparisonOperator(ComparisonOperator op) {
        this.op = op;
        return (T) this;
    }

    /**
     * Creates and returns a condition of the range key being equal to the given
     * value.
     */
    public T eq(Object val) {
        return withComparisonOperator(ComparisonOperator.EQ).values(val);
    }

    public T ne(Object val) {
        return withComparisonOperator(ComparisonOperator.NE).values(val);
    }

    /**
     * Expects the attribute be an existing attribute.
     */
    public T exists() {
        return withComparisonOperator(ComparisonOperator.NOT_NULL);
    }

    /**
     * Expects the attribute be non-existing.
     */
    public T notExist() {
        return withComparisonOperator(ComparisonOperator.NULL);
    }

    public T contains(Object val) {
        return withComparisonOperator(ComparisonOperator.CONTAINS).values(val);
    }

    public T notContains(Object val) {
        return withComparisonOperator(ComparisonOperator.NOT_CONTAINS).values(val);
    }

    /**
     * Creates and returns a condition of the range key with a value that begins
     * with the given value.
     */
    public T beginsWith(String val) {
        return withComparisonOperator(ComparisonOperator.BEGINS_WITH).values(val);
    }

    public T in(Object... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("values must not be null or empty.");
        }

        return withComparisonOperator(ComparisonOperator.IN).values(values);
    }

    /**
     * Creates and returns a condition of the range key that has a value between
     * the given values.
     */
    public T between(Object low, Object hi) {
        return withComparisonOperator(ComparisonOperator.BETWEEN).values(low, hi);
    }

    /**
     * Creates and returns a condition of the range key being greater than or
     * equal to the given value.
     */
    public T ge(Object val) {
        return withComparisonOperator(ComparisonOperator.GE).values(val);
    }

    /**
     * Creates and returns a condition of the range key being greater than the
     * given value.
     */
    public T gt(Object val) {
        return withComparisonOperator(ComparisonOperator.GT).values(val);
    }

    /**
     * Creates and returns a condition of the range key being less than or equal
     * to the given value.
     */
    public T le(Object val) {
        return withComparisonOperator(ComparisonOperator.LE).values(val);
    }

    /**
     * Creates and returns a condition of the range key being less than the
     * given value.
     */
    public T lt(Object val) {
        return withComparisonOperator(ComparisonOperator.LT).values(val);
    }
}
