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

package software.amazon.awssdk.services.stepfunctions.builder.states;

import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.services.stepfunctions.builder.internal.PropertyName;

/**
 * Terminal state that terminates the state machine and marks it as a failure.
 *
 * @see <a href="https://states-language.net/spec.html#fail-state">https://states-language.net/spec.html#fail-state</a>
 */
public final class FailState implements State {

    @JsonProperty(PropertyName.COMMENT)
    private final String comment;

    @JsonProperty(PropertyName.ERROR)
    private final String error;

    @JsonProperty(PropertyName.CAUSE)
    private final String cause;

    private FailState(Builder builder) {
        this.comment = builder.comment;
        this.error = builder.error;
        this.cause = builder.cause;
    }

    /**
     * @return Builder instance to construct a {@link FailState}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Type identifier of {@link FailState}.
     */
    @Override
    public String getType() {
        return "Fail";
    }

    /**
     * @return Human readable description for the state.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return Error code that can be referenced in {@link Retrier}s or {@link Catcher}s and can also be used for diagnostic
     *     purposes.
     */
    public String getError() {
        return error;
    }

    /**
     * @return Human readable message describing the failure. Used for diagnostic purposes only.
     */
    public String getCause() {
        return cause;
    }

    /**
     * {@link FailState} is always a terminal state.
     *
     * @return True.
     */
    @Override
    public boolean isTerminalState() {
        return true;
    }

    @Override
    public <T> T accept(StateVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * Builder for a {@link FailState}.
     */
    public static final class Builder implements State.Builder {

        @JsonProperty(PropertyName.COMMENT)
        private String comment;

        @JsonProperty(PropertyName.ERROR)
        private String error;

        @JsonProperty(PropertyName.CAUSE)
        private String cause;

        private Builder() {
        }

        /**
         * OPTIONAL. Human readable description for the state.
         *
         * @param comment New comment.
         * @return This object for method chaining.
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * REQUIRED. Error code that can be referenced in {@link Retrier}s or {@link Catcher}s and can also be used for
         * diagnostic
         * purposes.
         *
         * @param error Error code value.
         * @return This object for method chaining.
         */
        public Builder error(String error) {
            this.error = error;
            return this;
        }

        /**
         * REQUIRED. Human readable message describing the failure. Used for diagnostic purposes only.
         *
         * @param cause Cause description.
         * @return This object for method chaining.
         */
        public Builder cause(String cause) {
            this.cause = cause;
            return this;
        }

        /**
         * @return An immutable {@link FailState} object.
         */
        public FailState build() {
            return new FailState(this);
        }
    }
}
