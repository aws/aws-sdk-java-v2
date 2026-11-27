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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Nested, flattened, and recursive beans used by the naming tests.
 */
final class NamingNestedBeans {
    private NamingNestedBeans() {
    }

    /**
     * A discovered child bean.
     * {@code code} keeps the annotated name.
     */
    @DynamoDbBean
    public static class CityBean {
        private String cityName;
        private String code;

        public String getCityName() {
            return cityName;
        }

        public void setCityName(String cityName) {
            this.cityName = cityName;
        }

        @DynamoDbAttribute("CITY")
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    /**
     * A discovered immutable child.
     */
    @DynamoDbImmutable(builder = CityImmutable.Builder.class)
    public static final class CityImmutable {
        private final String cityName;
        private final String code;

        private CityImmutable(Builder builder) {
            this.cityName = builder.cityName;
            this.code = builder.code;
        }

        /**
         * Returns a new builder.
         */
        public static Builder builder() {
            return new Builder();
        }

        public String cityName() {
            return cityName;
        }

        @DynamoDbAttribute("CITY")
        public String code() {
            return code;
        }

        /**
         * Builds a {@link CityImmutable}.
         */
        public static final class Builder {
            private String cityName;
            private String code;

            /**
             * Sets the city name.
             */
            public Builder cityName(String cityName) {
                this.cityName = cityName;
                return this;
            }

            /**
             * Sets the annotated code.
             */
            public Builder code(String code) {
                this.code = code;
                return this;
            }

            /**
             * Returns the city.
             */
            public CityImmutable build() {
                return new CityImmutable(this);
            }
        }
    }

    /**
     * A bean parent with discovered children, a list, a map, and a flattened child.
     */
    @DynamoDbBean
    public static class ParentBean {
        private String id;
        private CityBean homeCity;
        private CityImmutable cityRecord;
        private List<CityBean> cityList;
        private Map<String, CityBean> cityMap;
        private CityBean flatCity;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public CityBean getHomeCity() {
            return homeCity;
        }

        public void setHomeCity(CityBean homeCity) {
            this.homeCity = homeCity;
        }

        public CityImmutable getCityRecord() {
            return cityRecord;
        }

        public void setCityRecord(CityImmutable cityRecord) {
            this.cityRecord = cityRecord;
        }

        public List<CityBean> getCityList() {
            return cityList;
        }

        public void setCityList(List<CityBean> cityList) {
            this.cityList = cityList;
        }

        public Map<String, CityBean> getCityMap() {
            return cityMap;
        }

        public void setCityMap(Map<String, CityBean> cityMap) {
            this.cityMap = cityMap;
        }

        @DynamoDbFlatten
        public CityBean getFlatCity() {
            return flatCity;
        }

        public void setFlatCity(CityBean flatCity) {
            this.flatCity = flatCity;
        }
    }

    /**
     * An immutable parent with a discovered bean child and a flattened bean.
     */
    @DynamoDbImmutable(builder = ParentImmutable.Builder.class)
    public static final class ParentImmutable {
        private final String id;
        private final CityBean homeCity;
        private final CityBean flatCity;

        private ParentImmutable(Builder builder) {
            this.id = builder.id;
            this.homeCity = builder.homeCity;
            this.flatCity = builder.flatCity;
        }

        /**
         * Returns a new builder.
         */
        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public CityBean homeCity() {
            return homeCity;
        }

        @DynamoDbFlatten
        public CityBean flatCity() {
            return flatCity;
        }

        /**
         * Builds a {@link ParentImmutable}.
         */
        public static final class Builder {
            private String id;
            private CityBean homeCity;
            private CityBean flatCity;

            /**
             * Sets the key.
             */
            public Builder id(String id) {
                this.id = id;
                return this;
            }

            /**
             * Sets the nested city.
             */
            public Builder homeCity(CityBean homeCity) {
                this.homeCity = homeCity;
                return this;
            }

            /**
             * Sets the flattened city.
             */
            public Builder flatCity(CityBean flatCity) {
                this.flatCity = flatCity;
                return this;
            }

            /**
             * Returns the parent.
             */
            public ParentImmutable build() {
                return new ParentImmutable(this);
            }
        }
    }

    /**
     * A bean that contains itself.
     */
    @DynamoDbBean
    public static class RecursiveBean {
        private String id;
        private String cityName;
        private RecursiveBean child;
        private List<RecursiveBean> children;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCityName() {
            return cityName;
        }

        public void setCityName(String cityName) {
            this.cityName = cityName;
        }

        public RecursiveBean getChild() {
            return child;
        }

        public void setChild(RecursiveBean child) {
            this.child = child;
        }

        public List<RecursiveBean> getChildren() {
            return children;
        }

        public void setChildren(List<RecursiveBean> children) {
            this.children = children;
        }
    }

    /**
     * An immutable class that contains itself.
     */
    @DynamoDbImmutable(builder = RecursiveImmutable.Builder.class)
    public static final class RecursiveImmutable {
        private final String id;
        private final String cityName;
        private final RecursiveImmutable child;

        private RecursiveImmutable(Builder builder) {
            this.id = builder.id;
            this.cityName = builder.cityName;
            this.child = builder.child;
        }

        /**
         * Returns a new builder.
         */
        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public String cityName() {
            return cityName;
        }

        public RecursiveImmutable child() {
            return child;
        }

        /**
         * Builds a {@link RecursiveImmutable}.
         */
        public static final class Builder {
            private String id;
            private String cityName;
            private RecursiveImmutable child;

            /**
             * Sets the key.
             */
            public Builder id(String id) {
                this.id = id;
                return this;
            }

            /**
             * Sets the city name.
             */
            public Builder cityName(String cityName) {
                this.cityName = cityName;
                return this;
            }

            /**
             * Sets the nested record.
             */
            public Builder child(RecursiveImmutable child) {
                this.child = child;
                return this;
            }

            /**
             * Returns the record.
             */
            public RecursiveImmutable build() {
                return new RecursiveImmutable(this);
            }
        }
    }

    /**
     * The outer type of a cycle through another bean.
     */
    @DynamoDbBean
    public static class OuterCycle {
        private String id;
        private InnerCycle inner;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public InnerCycle getInner() {
            return inner;
        }

        public void setInner(InnerCycle inner) {
            this.inner = inner;
        }
    }

    /**
     * The inner type of a cycle back to {@link OuterCycle}.
     */
    @DynamoDbBean
    public static class InnerCycle {
        private String cityName;
        private OuterCycle outer;

        public String getCityName() {
            return cityName;
        }

        public void setCityName(String cityName) {
            this.cityName = cityName;
        }

        public OuterCycle getOuter() {
            return outer;
        }

        public void setOuter(OuterCycle outer) {
            this.outer = outer;
        }
    }

    /**
     * A record whose static schema is supplied to a parent.
     */
    public static class SuppliedCity {
        private String cityName;

        public String getCityName() {
            return cityName;
        }

        public void setCityName(String cityName) {
            this.cityName = cityName;
        }
    }

    /**
     * A static parent that can hold a supplied child.
     */
    public static class SuppliedParent {
        private String customerId;
        private String orderId;
        private SuppliedCity cityRecord;
        private SuppliedCity flatCity;

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public SuppliedCity getCityRecord() {
            return cityRecord;
        }

        public void setCityRecord(SuppliedCity cityRecord) {
            this.cityRecord = cityRecord;
        }

        public SuppliedCity getFlatCity() {
            return flatCity;
        }

        public void setFlatCity(SuppliedCity flatCity) {
            this.flatCity = flatCity;
        }
    }
}
