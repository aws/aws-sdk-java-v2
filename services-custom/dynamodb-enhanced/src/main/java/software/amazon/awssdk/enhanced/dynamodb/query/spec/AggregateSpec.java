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

package software.amazon.awssdk.enhanced.dynamodb.query.spec;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.AggregationFunction;

/**
 * Specification for a single aggregation: function, source attribute, and output name.
 */
@SdkInternalApi
public final class AggregateSpec {
    private final AggregationFunction function;
    private final String attribute;
    private final String outputName;

    private AggregateSpec(Builder b) {
        this.function = Objects.requireNonNull(b.function, "function");
        this.attribute = Objects.requireNonNull(b.attribute, "attribute");
        this.outputName = Objects.requireNonNull(b.outputName, "outputName");
    }

    public AggregationFunction function() {
        return function;
    }

    public String attribute() {
        return attribute;
    }

    public String outputName() {
        return outputName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AggregationFunction function;
        private String attribute;
        private String outputName;

        private Builder() {
        }

        public Builder function(AggregationFunction function) {
            this.function = function;
            return this;
        }

        public Builder attribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        public Builder outputName(String outputName) {
            this.outputName = outputName;
            return this;
        }

        public AggregateSpec build() {
            return new AggregateSpec(this);
        }
    }
}
