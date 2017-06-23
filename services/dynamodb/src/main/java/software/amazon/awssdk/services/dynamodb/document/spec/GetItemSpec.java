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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.services.dynamodb.document.KeyAttribute;
import software.amazon.awssdk.services.dynamodb.document.PrimaryKey;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

/**
 * Full parameter specification for the GetItem API.
 */
public class GetItemSpec extends AbstractSpecWithPrimaryKey<GetItemRequest> {
    private Map<String, String> nameMap;

    public GetItemSpec() {
        super(GetItemRequest.builder().build());
    }

    @Override
    public GetItemSpec withPrimaryKey(KeyAttribute... components) {
        super.withPrimaryKey(components);
        return this;
    }

    @Override
    public GetItemSpec withPrimaryKey(PrimaryKey primaryKey) {
        super.withPrimaryKey(primaryKey);
        return this;
    }

    @Override
    public GetItemSpec withPrimaryKey(String hashKeyName, Object hashKeyValue) {
        super.withPrimaryKey(hashKeyName, hashKeyValue);
        return this;
    }

    @Override
    public GetItemSpec withPrimaryKey(String hashKeyName, Object hashKeyValue,
                                      String rangeKeyName, Object rangeKeyValue) {
        super.withPrimaryKey(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue);
        return this;
    }

    public String getReturnConsumedCapacity() {
        return getRequest().returnConsumedCapacity();
    }

    public GetItemSpec withReturnConsumedCapacity(ReturnConsumedCapacity capacity) {
        setRequest(getRequest().toBuilder().returnConsumedCapacity(capacity).build());
        return this;
    }

    public List<String> getAttributesToGet() {
        return getRequest().attributesToGet();
    }

    public GetItemSpec withAttributesToGet(String... attrNames) {
        if (attrNames == null) {
            setRequest(getRequest().toBuilder().attributesToGet((String[]) null).build());
        } else {
            setRequest(getRequest().toBuilder().attributesToGet(Arrays.asList(attrNames)).build());
        }
        return this;
    }

    public Boolean isConsistentRead() {
        return getRequest().consistentRead();
    }

    public GetItemSpec withConsistentRead(boolean consistentRead) {
        setRequest(getRequest().toBuilder().consistentRead(consistentRead).build());
        return this;
    }

    public String getProjectionExpression() {
        return getRequest().projectionExpression();
    }

    /**
     * When a projection expression is specified, the corresponding name-map can
     * optionally be specified via { {@link #withNameMap(Map)}. (Note
     * attributes-to-get must not be specified if a projection expression has
     * been specified.)
     */
    public GetItemSpec withProjectionExpression(String projectionExpression) {
        setRequest(getRequest().toBuilder().projectionExpression(projectionExpression).build());
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
    public GetItemSpec withNameMap(Map<String, String> nameMap) {
        if (nameMap == null) {
            this.nameMap = null;
        } else {
            this.nameMap = Collections.unmodifiableMap(
                    new LinkedHashMap<String, String>(nameMap));
        }
        return this;
    }

    @Override
    public GetItemSpec withProgressListener(ProgressListener progressListener) {
        setProgressListener(progressListener);
        return this;
    }

    @Override
    public GetItemSpec withRequestMetricCollector(
            RequestMetricCollector requestMetricCollector) {
        setRequestMetricCollector(requestMetricCollector);
        return this;
    }
}
