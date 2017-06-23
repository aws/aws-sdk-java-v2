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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.services.dynamodb.document.AttributeUpdate;
import software.amazon.awssdk.services.dynamodb.document.Expected;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Full parameter specification for the UpdateItem API.
 */
public class UpdateItemSpec extends AbstractSpecWithPrimaryKey<UpdateItemRequest> {
    private List<AttributeUpdate> attributes;
    private Collection<Expected> expected;

    private Map<String, String> nameMap;
    private Map<String, Object> valueMap;

    public UpdateItemSpec() {
        super(UpdateItemRequest.builder().build());
    }

    @Override
    public UpdateItemSpec withPrimaryKey(KeyAttribute... components) {
        super.withPrimaryKey(components);
        return this;
    }

    @Override
    public UpdateItemSpec withPrimaryKey(PrimaryKey primaryKey) {
        super.withPrimaryKey(primaryKey);
        return this;
    }

    @Override
    public UpdateItemSpec withPrimaryKey(String hashKeyName, Object hashKeyValue) {
        super.withPrimaryKey(hashKeyName, hashKeyValue);
        return this;
    }

    @Override
    public UpdateItemSpec withPrimaryKey(String hashKeyName, Object hashKeyValue,
                                         String rangeKeyName, Object rangeKeyValue) {
        super.withPrimaryKey(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue);
        return this;
    }

    public List<AttributeUpdate> getAttributeUpdate() {
        return attributes;
    }

    public UpdateItemSpec withAttributeUpdate(
            List<AttributeUpdate> attributeUpdates) {
        this.attributes = attributeUpdates;
        return this;
    }

    public UpdateItemSpec withAttributeUpdate(
            AttributeUpdate... attributeUpdates) {
        this.attributes = new ArrayList<AttributeUpdate>(Arrays.asList(attributeUpdates));
        return this;
    }

    public UpdateItemSpec addAttributeUpdate(AttributeUpdate attributeUpdate) {
        if (null == this.attributes) {
            this.attributes = new ArrayList<AttributeUpdate>();
        }
        this.attributes.add(attributeUpdate);
        return this;
    }

    public UpdateItemSpec clearAttributeUpdate() {
        this.attributes = null;
        return this;
    }

    public Collection<Expected> getExpected() {
        return expected;
    }

    public UpdateItemSpec withExpected(Expected... expected) {
        if (expected == null) {
            this.expected = null;
            return this;
        }
        return withExpected(Arrays.asList(expected));
    }

    public UpdateItemSpec withExpected(Collection<Expected> expected) {
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

    public String getUpdateExpression() {
        return getRequest().updateExpression();
    }

    public UpdateItemSpec withUpdateExpression(String updateExpression) {
        setRequest(getRequest().toBuilder().updateExpression(updateExpression).build());
        return this;
    }

    public String getConditionExpression() {
        return getRequest().conditionExpression();
    }

    public UpdateItemSpec withConditionExpression(String conditionExpression) {
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
    public UpdateItemSpec withNameMap(Map<String, String> nameMap) {
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
    public UpdateItemSpec valueMap(Map<String, Object> valueMap) {
        if (valueMap == null) {
            this.valueMap = null;
        } else {
            this.valueMap = Collections.unmodifiableMap(
                    new LinkedHashMap<String, Object>(valueMap));
        }
        return this;
    }

    public String getConditionalOperator() {
        return getRequest().conditionalOperator();
    }

    public String getReturnConsumedCapacity() {
        return getRequest().returnConsumedCapacity();
    }

    public UpdateItemSpec withReturnConsumedCapacity(
            String returnConsumedCapacity) {
        setRequest(getRequest().toBuilder().returnConsumedCapacity(returnConsumedCapacity).build());
        return this;
    }

    public UpdateItemSpec withReturnConsumedCapacity(
            ReturnConsumedCapacity returnConsumedCapacity) {
        setRequest(getRequest().toBuilder().returnConsumedCapacity(returnConsumedCapacity).build());
        return this;
    }

    public String getReturnItemCollectionMetrics() {
        return getRequest().returnItemCollectionMetrics();
    }

    public UpdateItemSpec withReturnItemCollectionMetrics(
            ReturnItemCollectionMetrics returnItemCollectionMetrics) {
        setRequest(getRequest().toBuilder().returnItemCollectionMetrics(returnItemCollectionMetrics).build());
        return this;
    }

    public UpdateItemSpec withReturnItemCollectionMetrics(
            String returnItemCollectionMetrics) {
        setRequest(getRequest().toBuilder().returnItemCollectionMetrics(returnItemCollectionMetrics).build());
        return this;
    }

    public String getReturnValues() {
        return getRequest().returnValues();
    }

    public UpdateItemSpec withReturnValues(ReturnValue returnValues) {
        setRequest(getRequest().toBuilder().returnValues(returnValues).build());
        return this;
    }

    public UpdateItemSpec withReturnValues(String returnValues) {
        setRequest(getRequest().toBuilder().returnValues(returnValues).build());
        return this;
    }

    @Override
    public UpdateItemSpec withProgressListener(ProgressListener progressListener) {
        setProgressListener(progressListener);
        return this;
    }

    @Override
    public UpdateItemSpec withRequestMetricCollector(
            RequestMetricCollector requestMetricCollector) {
        setRequestMetricCollector(requestMetricCollector);
        return this;
    }
}
