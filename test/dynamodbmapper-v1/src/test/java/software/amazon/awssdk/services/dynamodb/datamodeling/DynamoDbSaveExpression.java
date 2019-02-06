/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;

/**
 * Enables adding options to a save operation.
 * For example, you may want to save only if an attribute has a particular value.
 * @see DynamoDbMapper#save(Object, DynamoDbSaveExpression)
 */
public class DynamoDbSaveExpression {

    /** Optional expected attributes. */
    private Map<String, ExpectedAttributeValue> expectedAttributes;

    /**
     * The logical operator on the expected value conditions of this save
     * operation.
     */
    private String conditionalOperator;

    /**
     * Gets the map of attribute names to expected attribute values to check on save.
     *
     * @return The map of attribute names to expected attribute value conditions to check on save
     */
    public Map<String, ExpectedAttributeValue> getExpected() {
        return expectedAttributes;
    }

    /**
     * Sets the expected condition to the map of attribute names to expected attribute values given.
     *
     * @param expectedAttributes
     *            The map of attribute names to expected attribute value conditions to check on save
     */
    public void setExpected(Map<String, ExpectedAttributeValue> expectedAttributes) {
        this.expectedAttributes = expectedAttributes;
    }

    /**
     * Sets the expected condition to the map of attribute names to expected
     * attribute values given and returns a pointer to this object for
     * method-chaining.
     *
     * @param expectedAttributes
     *            The map of attribute names to expected attribute value
     *            conditions to check on save
     */
    public DynamoDbSaveExpression withExpected(Map<String, ExpectedAttributeValue> expectedAttributes) {
        setExpected(expectedAttributes);
        return this;
    }

    /**
     * Adds one entry to the expected conditions and returns a pointer to this
     * object for method-chaining.
     *
     * @param attributeName
     *            The name of the attribute.
     * @param expected
     *            The expected attribute value.
     */
    public DynamoDbSaveExpression withExpectedEntry(String attributeName, ExpectedAttributeValue expected) {
        if (expectedAttributes == null) {
            expectedAttributes = new HashMap<String, ExpectedAttributeValue>();
        }
        expectedAttributes.put(attributeName, expected);
        return this;
    }

    /**
     * Returns the logical operator on the expected value conditions of this save
     * operation.
     */
    public String getConditionalOperator() {
        return conditionalOperator;
    }

    /**
     * Sets the logical operator on the expected value conditions of this save
     * operation.
     */
    public void setConditionalOperator(String conditionalOperator) {
        this.conditionalOperator = conditionalOperator;
    }

    /**
     * Sets the logical operator on the expected value conditions of this save
     * operation.
     */
    public void setConditionalOperator(ConditionalOperator conditionalOperator) {
        setConditionalOperator(conditionalOperator.toString());
    }

    /**
     * Sets the logical operator on the expected value conditions of this save
     * operation and returns a pointer to this object for method-chaining.
     */
    public DynamoDbSaveExpression withConditionalOperator(String conditionalOperator) {
        setConditionalOperator(conditionalOperator);
        return this;
    }

    /**
     * Sets the logical operator on the expected value conditions of this save
     * operation and returns a pointer to this object for method-chaining.
     */
    public DynamoDbSaveExpression withConditionalOperator(ConditionalOperator conditionalOperator) {
        return withConditionalOperator(conditionalOperator.toString());
    }
}
