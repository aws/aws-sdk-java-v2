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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.document.converter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.core.SdkBytes;

public class CustomClass {
    public String foo() {
        return foo;
    }

    public Set<String> stringSet() {
        return stringSet;
    }

    public SdkBytes binary() {
        return binary;
    }

    public Set<byte[]> binarySet() {
        return binarySet;
    }

    public boolean aBoolean() {
        return aBoolean;
    }

    public Set<Boolean> booleanSet() {
        return booleanSet;
    }

    public Long longNumber() {
        return longNumber;
    }

    public Set<Long> longSet() {
        return longSet;
    }

    public BigDecimal bigDecimal() {
        return bigDecimal;
    }

    public Set<BigDecimal> bigDecimalSet() {
        return bigDecimalSet;
    }

    public List<CustomClass> customClassList() {
        return customClassList;
    }

    public List<Instant> instantList() {
        return instantList;
    }

    public Map<String, CustomClass> customClassMap() {
        return customClassMap;
    }

    public CustomClass innerCustomClass() {
        return innerCustomClass;
    }

    private final String foo;
    private final Set<String> stringSet;
    private final SdkBytes binary;
    private final Set<byte[]> binarySet;
    private final boolean aBoolean;
    private final Set<Boolean> booleanSet;
    private final Long longNumber;
    private final Set<Long> longSet;
    private final BigDecimal bigDecimal;
    private final Set<BigDecimal> bigDecimalSet;
    private final List<CustomClass> customClassList;
    private final List<Instant> instantList;
    private final Map<String, CustomClass> customClassMap;
    private final CustomClass innerCustomClass;

    public static Builder builder(){
        return new Builder();
    }

    public CustomClass(Builder builder) {
        this.foo = builder.foo;
        this.stringSet = builder.stringSet;
        this.binary = builder.binary;
        this.binarySet = builder.binarySet;
        this.aBoolean = builder.aBoolean;
        this.booleanSet = builder.booleanSet;
        this.longNumber = builder.longNumber;
        this.longSet = builder.longSet;
        this.bigDecimal = builder.bigDecimal;
        this.bigDecimalSet = builder.bigDecimalSet;
        this.customClassList = builder.customClassList;
        this.instantList = builder.instantList;
        this.customClassMap = builder.customClassMap;
        this.innerCustomClass = builder.innerCustomClass;
    }

    public static final class Builder {
        private String foo;
        private Set<String> stringSet;
        private SdkBytes binary;
        private Set<byte []> binarySet;
        private boolean aBoolean;
        private Set<Boolean> booleanSet;
        private Long longNumber;
        private Set<Long> longSet;
        private BigDecimal bigDecimal;
        private Set<BigDecimal> bigDecimalSet;
        private List<CustomClass> customClassList;
        private List<Instant> instantList;
        private Map<String, CustomClass> customClassMap;
        private CustomClass innerCustomClass;

        private Builder() {
        }


        public Builder foo(String foo) {
            this.foo = foo;
            return this;
        }

        public Builder stringSet(Set<String> stringSet) {
            this.stringSet = stringSet;
            return this;
        }

        public Builder binary(SdkBytes binary) {
            this.binary = binary;
            return this;
        }

        public Builder binarySet(Set<byte[]> binarySet) {
            this.binarySet = binarySet;
            return this;
        }

        public Builder aBoolean(boolean aBoolean) {
            this.aBoolean = aBoolean;
            return this;
        }

        public Builder booleanSet(Set<Boolean> booleanSet) {
            this.booleanSet = booleanSet;
            return this;
        }

        public Builder longNumber(Long longNumber) {
            this.longNumber = longNumber;
            return this;
        }

        public Builder longSet(Set<Long> longSet) {
            this.longSet = longSet;
            return this;
        }

        public Builder bigDecimal(BigDecimal bigDecimal) {
            this.bigDecimal = bigDecimal;
            return this;
        }

        public Builder bigDecimalSet(Set<BigDecimal> bigDecimalSet) {
            this.bigDecimalSet = bigDecimalSet;
            return this;
        }

        public Builder customClassList(List<CustomClass> customClassList) {
            this.customClassList = customClassList;
            return this;
        }

        public Builder instantList(List<Instant> instantList) {
            this.instantList = instantList;
            return this;
        }

        public Builder customClassMap(Map<String, CustomClass> customClassMap) {
            this.customClassMap = customClassMap;
            return this;
        }

        public Builder innerCustomClass(CustomClass innerCustomClass) {
            this.innerCustomClass = innerCustomClass;
            return this;
        }

        public CustomClass build() {
            return new CustomClass(this);
        }
    }


}
