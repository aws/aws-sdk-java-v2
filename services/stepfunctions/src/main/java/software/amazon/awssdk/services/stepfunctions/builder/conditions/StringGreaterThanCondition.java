/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.stepfunctions.builder.conditions;

import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.services.stepfunctions.builder.internal.PropertyName;

/**
 * Binary condition for String greater than comparison.
 *
 * @see <a href="https://states-language.net/spec.html#choice-state">https://states-language.net/spec.html#choice-state</a>
 * @see software.amazon.awssdk.services.stepfunctions.builder.states.Choice
 */
public final class StringGreaterThanCondition implements BinaryCondition<String> {

    @JsonProperty(PropertyName.VARIABLE)
    private final String variable;

    @JsonProperty(PropertyName.STRING_GREATER_THAN)
    private final String expectedValue;

    private StringGreaterThanCondition(Builder builder) {
        this.variable = builder.variable;
        this.expectedValue = builder.expectedValue;
    }

    /**
     * @return Builder instance to construct a {@link StringGreaterThanCondition}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The JSONPath expression that determines which piece of the input document is used for the comparison.
     */
    @Override
    public String getVariable() {
        return variable;
    }

    /**
     * @return The expected value for this condition.
     */
    @Override
    public String getExpectedValue() {
        return expectedValue;
    }

    /**
     * Builder for a {@link StringGreaterThanCondition}.
     */
    public static final class Builder extends BinaryStringConditionBuilder {

        @JsonProperty(PropertyName.VARIABLE)
        private String variable;

        @JsonProperty(PropertyName.STRING_GREATER_THAN)
        private String expectedValue;

        private Builder() {
        }

        /**
         * Sets the JSONPath expression that determines which piece of the input document is used for the comparison.
         *
         * @param variable Reference path.
         * @return This object for method chaining.
         */
        @Override
        public Builder variable(String variable) {
            this.variable = variable;
            return this;
        }

        /**
         * Sets the expected value for this condition.
         *
         * @param expectedValue Expected value.
         * @return This object for method chaining.
         */
        public Builder expectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        @Override
        String type() {
            return PropertyName.STRING_GREATER_THAN;
        }

        /**
         * @return An immutable {@link StringGreaterThanCondition} object.
         */
        @Override
        public StringGreaterThanCondition build() {
            return new StringGreaterThanCondition(this);
        }
    }
}
