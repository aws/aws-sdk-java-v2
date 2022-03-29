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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.Objects;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Defines the elements returned by DynamoDB from a {@code DescribeTable} operation, such as
 * {@link DynamoDbTable#describeTable()} and {@link DynamoDbAsyncTable#describeTable()}
 */
@SdkPublicApi
@ThreadSafe
public final class DescribeTableEnhancedResponse {
    private final DescribeTableResponse response;

    private DescribeTableEnhancedResponse(Builder builder) {
        this.response = Validate.paramNotNull(builder.response, "response");
    }

    /**
     * The properties of the table.
     *
     * @return The properties of the table.
     */
    public TableDescription table() {
        return response.table();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DescribeTableEnhancedResponse that = (DescribeTableEnhancedResponse) o;

        return Objects.equals(response, that.response);
    }

    @Override
    public int hashCode() {
        return response != null ? response.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("DescribeTableEnhancedResponse")
                       .add("table", response.table())
                       .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static final class Builder {
        private DescribeTableResponse response;

        public Builder response(DescribeTableResponse response) {
            this.response = response;
            return this;
        }

        public DescribeTableEnhancedResponse build() {
            return new DescribeTableEnhancedResponse(this);
        }
    }
}
