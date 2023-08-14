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

package software.amazon.awssdk.policybuilder.iam.internal;

import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.policybuilder.iam.IamCondition;
import software.amazon.awssdk.policybuilder.iam.IamConditionKey;
import software.amazon.awssdk.policybuilder.iam.IamConditionOperator;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link IamCondition}.
 *
 * @see IamCondition#create
 */
@SdkInternalApi
public final class DefaultIamCondition implements IamCondition {
    @NotNull private final IamConditionOperator operator;
    @NotNull private final IamConditionKey key;
    @NotNull private final String value;

    private DefaultIamCondition(Builder builder) {
        this.operator = Validate.paramNotNull(builder.operator, "conditionOperator");
        this.key = Validate.paramNotNull(builder.key, "conditionKey");
        this.value = Validate.paramNotNull(builder.value, "conditionValue");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public IamConditionOperator operator() {
        return operator;
    }

    @Override
    public IamConditionKey key() {
        return key;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultIamCondition that = (DefaultIamCondition) o;

        if (!operator.equals(that.operator)) {
            return false;
        }
        if (!key.equals(that.key)) {
            return false;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = operator.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("IamCondition")
                       .add("operator", operator.value())
                       .add("key", key.value())
                       .add("value", value)
                       .build();
    }

    public static class Builder implements IamCondition.Builder {
        private IamConditionOperator operator;
        private IamConditionKey key;
        private String value;

        private Builder() {
        }

        private Builder(DefaultIamCondition condition) {
            this.operator = condition.operator;
            this.key = condition.key;
            this.value = condition.value;
        }
        
        @Override
        public IamCondition.Builder operator(IamConditionOperator operator) {
            this.operator = operator;
            return this;
        }

        @Override
        public IamCondition.Builder operator(String operator) {
            this.operator = operator == null ? null : IamConditionOperator.create(operator);
            return this;
        }

        @Override
        public IamCondition.Builder key(IamConditionKey key) {
            this.key = key;
            return this;
        }

        @Override
        public IamCondition.Builder key(String key) {
            this.key = key == null ? null : IamConditionKey.create(key);
            return this;
        }

        @Override
        public IamCondition.Builder value(String value) {
            this.value = value;
            return this;
        }

        @Override
        public IamCondition build() {
            return new DefaultIamCondition(this);
        }
    }
}
