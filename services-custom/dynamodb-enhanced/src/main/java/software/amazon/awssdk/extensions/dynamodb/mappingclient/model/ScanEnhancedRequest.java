/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkPublicApi
public final class ScanEnhancedRequest {

    private final Map<String, AttributeValue> exclusiveStartKey;
    private final Integer limit;
    private final Boolean consistentRead;
    private final Expression filterExpression;

    private ScanEnhancedRequest(Builder builder) {
        this.exclusiveStartKey = builder.exclusiveStartKey;
        this.limit = builder.limit;
        this.consistentRead = builder.consistentRead;
        this.filterExpression = builder.filterExpression;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().exclusiveStartKey(exclusiveStartKey)
                        .limit(limit)
                        .consistentRead(consistentRead)
                        .filterExpression(filterExpression);
    }

    public Map<String, AttributeValue> exclusiveStartKey() {
        return exclusiveStartKey;
    }

    public Integer limit() {
        return limit;
    }

    public Boolean consistentRead() {
        return consistentRead;
    }

    public Expression filterExpression() {
        return filterExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScanEnhancedRequest scan = (ScanEnhancedRequest) o;

        if (exclusiveStartKey != null ? ! exclusiveStartKey.equals(scan.exclusiveStartKey) :
            scan.exclusiveStartKey != null) {
            return false;
        }
        if (limit != null ? ! limit.equals(scan.limit) : scan.limit != null) {
            return false;
        }
        if (consistentRead != null ? ! consistentRead.equals(scan.consistentRead) : scan.consistentRead != null) {
            return false;
        }
        return filterExpression != null ? filterExpression.equals(scan.filterExpression) : scan.filterExpression == null;
    }

    @Override
    public int hashCode() {
        int result = exclusiveStartKey != null ? exclusiveStartKey.hashCode() : 0;
        result = 31 * result + (limit != null ? limit.hashCode() : 0);
        result = 31 * result + (consistentRead != null ? consistentRead.hashCode() : 0);
        result = 31 * result + (filterExpression != null ? filterExpression.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private Map<String, AttributeValue> exclusiveStartKey;
        private Integer limit;
        private Boolean consistentRead;
        private Expression filterExpression;

        private Builder() {
        }

        public Builder exclusiveStartKey(Map<String, AttributeValue> exclusiveStartKey) {
            this.exclusiveStartKey = exclusiveStartKey != null ? new HashMap<>(exclusiveStartKey) : null;
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        public Builder filterExpression(Expression filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        public ScanEnhancedRequest build() {
            return new ScanEnhancedRequest(this);
        }
    }
}
