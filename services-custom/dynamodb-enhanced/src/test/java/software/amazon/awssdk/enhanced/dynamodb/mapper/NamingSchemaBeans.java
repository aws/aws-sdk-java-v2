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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Beans used by the schema naming tests.
 */
final class NamingSchemaBeans {
    private NamingSchemaBeans() {
    }

    /**
     * A bean whose partition key is annotated and whose other attribute is a Java property name.
     */
    @DynamoDbBean
    public static class CustomerBean {
        private String id;
        private String customerId;

        @DynamoDbPartitionKey
        @DynamoDbAttribute("PK")
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }
    }

    /**
     * An immutable customer with the same names as {@link CustomerBean}.
     */
    @DynamoDbImmutable(builder = CustomerImmutable.Builder.class)
    public static final class CustomerImmutable {
        private final String id;
        private final String customerId;

        private CustomerImmutable(Builder builder) {
            this.id = builder.id;
            this.customerId = builder.customerId;
        }

        /**
         * Returns a new builder.
         */
        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        @DynamoDbAttribute("PK")
        public String id() {
            return id;
        }

        public String customerId() {
            return customerId;
        }

        /**
         * Builds a {@link CustomerImmutable}.
         */
        public static final class Builder {
            private String id;
            private String customerId;

            /**
             * Sets the partition key.
             */
            public Builder id(String id) {
                this.id = id;
                return this;
            }

            /**
             * Sets the customer id.
             */
            public Builder customerId(String customerId) {
                this.customerId = customerId;
                return this;
            }

            /**
             * Returns the immutable customer.
             */
            public CustomerImmutable build() {
                return new CustomerImmutable(this);
            }
        }
    }

    /**
     * A bean whose values may be null, including a list element and a map value.
     */
    @DynamoDbBean
    public static class NullableBean {
        private String id;
        private String customerId;
        private List<String> nicknames;
        private Map<String, String> labels;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public List<String> getNicknames() {
            return nicknames;
        }

        public void setNicknames(List<String> nicknames) {
            this.nicknames = nicknames;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }
    }

    /**
     * A bean with two flattened maps.
     * The same inner key may appear in each map without failing construction.
     */
    @DynamoDbBean
    public static class TwoMapBean {
        private String id;
        private Map<String, String> firstLabels = new HashMap<>();
        private Map<String, String> secondLabels = new HashMap<>();

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbFlatten
        public Map<String, String> getFirstLabels() {
            return firstLabels;
        }

        public void setFirstLabels(Map<String, String> firstLabels) {
            this.firstLabels = firstLabels;
        }

        @DynamoDbFlatten
        public Map<String, String> getSecondLabels() {
            return secondLabels;
        }

        public void setSecondLabels(Map<String, String> secondLabels) {
            this.secondLabels = secondLabels;
        }
    }

    /**
     * A bean whose annotated names skip the strategy.
     */
    @DynamoDbBean
    public static class AnnotatedBean {
        private String id;
        private String customerId;
        private String legacy;
        private String marker;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        @DynamoDbAttribute("___")
        public String getLegacy() {
            return legacy;
        }

        public void setLegacy(String legacy) {
            this.legacy = legacy;
        }

        @DynamoDbAttribute("HTTPRequest")
        public String getMarker() {
            return marker;
        }

        public void setMarker(String marker) {
            this.marker = marker;
        }
    }

    /**
     * A bean whose annotated name is a character the mapper already accepts.
     */
    @DynamoDbBean
    public static class StarAnnotatedBean {
        private String id;
        private String marker;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbAttribute("*")
        public String getMarker() {
            return marker;
        }

        public void setMarker(String marker) {
            this.marker = marker;
        }
    }

    /**
     * A mutable record used by static schemas.
     */
    public static class StaticCustomer {
        private String id;
        private String customerId;
        private String customerCode;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getCustomerCode() {
            return customerCode;
        }

        public void setCustomerCode(String customerCode) {
            this.customerCode = customerCode;
        }
    }

    /**
     * An immutable record used by static immutable schemas.
     */
    public static final class StaticImmutableCustomer {
        private final String id;
        private final String customerId;

        private StaticImmutableCustomer(Builder builder) {
            this.id = builder.id;
            this.customerId = builder.customerId;
        }

        /**
         * Returns a new builder.
         */
        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public String customerId() {
            return customerId;
        }

        /**
         * Builds a {@link StaticImmutableCustomer}.
         */
        public static final class Builder {
            private String id;
            private String customerId;

            /**
             * Sets the key.
             */
            public Builder id(String id) {
                this.id = id;
                return this;
            }

            /**
             * Sets the customer id.
             */
            public Builder customerId(String customerId) {
                this.customerId = customerId;
                return this;
            }

            /**
             * Returns the record.
             */
            public StaticImmutableCustomer build() {
                return new StaticImmutableCustomer(this);
            }
        }
    }
}
