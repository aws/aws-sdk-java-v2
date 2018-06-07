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
import software.amazon.awssdk.services.stepfunctions.builder.states.ChoiceState;

/**
 * Represents the logical NOT of a single condition. May be used in a {@link ChoiceState}.
 *
 * @see <a href="https://states-language.net/spec.html#choice-state">https://states-language.net/spec.html#choice-state</a>
 * @see software.amazon.awssdk.services.stepfunctions.builder.states.Choice
 */
public final class NotCondition implements Condition {

    @JsonProperty(PropertyName.NOT)
    private final Condition condition;

    private NotCondition(Builder builder) {
        this.condition = builder.condition.build();
    }

    /**
     * @return Builder instance to construct a {@link NotCondition}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The condition being negated.
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Builder for a {@link NotCondition}.
     */
    public static final class Builder implements Condition.Builder {

        private Condition.Builder condition = NULL_BUILDER;

        private Builder() {
        }

        /**
         * Sets the condition to be negated. May be another composite condition or a simple condition.
         *
         * @param conditionBuilder Instance of {@link Condition.Builder}. Note that the {@link Condition} object is not built
         *     until the {@link NotCondition} is built so any modifications on the state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder condition(Condition.Builder conditionBuilder) {
            this.condition = conditionBuilder;
            return this;
        }

        /**
         * @return An immutable {@link NotCondition} object.
         */
        @Override
        public Condition build() {
            return new NotCondition(this);
        }
    }
}
