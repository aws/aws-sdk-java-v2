/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document.spec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.Item;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

/**
 * Full parameter specification for the PutItem API.
 */
public class PutItemSpec extends AbstractSpec<PutItemRequest> {
    private Item item;
    private Collection<Expected> expected;
    private Map<String, String> nameMap;
    private Map<String, Object> valueMap;

    public PutItemSpec() {
        super(PutItemRequest.builder().build());
    }

    public Item getItem() {
        return item;
    }

    public PutItemSpec withItem(Item item) {
        this.item = item;
        return this;
    }

    public Collection<Expected> getExpected() {
        return expected;
    }

    public PutItemSpec withExpected(Expected... expected) {
        if (expected == null) {
            this.expected = null;
            return this;
        }
        return withExpected(Arrays.asList(expected));
    }

    public PutItemSpec withExpected(Collection<Expected> expected) {
        if (expected == null) {
            this.expected = null;
            return this;
        }
        Set<String> names = new LinkedHashSet<String>();
        for (Expected e : expected) {
            names.add(e.getAttribute());
        }
        if (names.size() != expected.size()) {
            throw new IllegalArgumentException(
                    "attribute names must not duplicate in the list of expected");
        }
        this.expected = Collections.unmodifiableCollection(expected);
        return this;
    }

    public String getConditionExpression() {
        return getRequest().conditionExpression();
    }

    public PutItemSpec withConditionExpression(String conditionExpression) {
        setRequest(getRequest().toBuilder().conditionExpression(conditionExpression).build());
        return this;
    }

    public Map<String, String> nameMap() {
        return nameMap;
    }

    /**
     * Applicable only when an expression has been specified.
     * Used to specify the actual values for the attribute-name placeholders,
     * where the value in the map can either be string for simple attribute
     * name, or a JSON path expression.
     */
    public PutItemSpec withNameMap(Map<String, String> nameMap) {
        if (nameMap == null) {
            this.nameMap = null;
        } else {
            this.nameMap = Collections.unmodifiableMap(
                    new LinkedHashMap<String, String>(nameMap));
        }
        return this;
    }

    public Map<String, Object> valueMap() {
        return valueMap;
    }

    /**
     * Applicable only when an expression has been specified. Used to
     * specify the actual values for the attribute-value placeholders.
     */
    public PutItemSpec valueMap(Map<String, Object> valueMap) {
        if (valueMap == null) {
            this.valueMap = null;
        } else {
            this.valueMap = Collections.unmodifiableMap(
                    new LinkedHashMap<String, Object>(valueMap));
        }
        return this;
    }

    public String getConditionalOperator() {
        return getRequest().conditionalOperatorString();
    }

    public PutItemSpec withConditionalOperator(
            ConditionalOperator conditionalOperator) {
        setRequest(getRequest().toBuilder().conditionalOperator(conditionalOperator).build());
        return this;
    }

    public String getReturnConsumedCapacity() {
        return getRequest().returnConsumedCapacityString();
    }

    public PutItemSpec withReturnConsumedCapacity(
            ReturnConsumedCapacity returnConsumedCapacity) {
        setRequest(getRequest().toBuilder().returnConsumedCapacity(returnConsumedCapacity).build());
        return this;
    }

    public String getReturnItemCollectionMetrics() {
        return getRequest().returnItemCollectionMetricsString();
    }

    public PutItemSpec withReturnItemCollectionMetrics(
            ReturnItemCollectionMetrics returnItemCollectionMetrics) {
        setRequest(getRequest().toBuilder()
                .returnItemCollectionMetrics(returnItemCollectionMetrics)
                .build());
        return this;
    }

    public String getReturnValues() {
        return getRequest().returnValuesString();
    }

    public PutItemSpec withReturnValues(ReturnValue returnValues) {
        setRequest(getRequest().toBuilder().returnValues(returnValues).build());
        return this;
    }
}
