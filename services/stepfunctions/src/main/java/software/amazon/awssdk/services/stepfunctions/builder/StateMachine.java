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

package software.amazon.awssdk.services.stepfunctions.builder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.stepfunctions.builder.internal.Buildable;
import software.amazon.awssdk.services.stepfunctions.builder.internal.DateModule;
import software.amazon.awssdk.services.stepfunctions.builder.internal.PropertyName;
import software.amazon.awssdk.services.stepfunctions.builder.internal.validation.StateMachineValidator;
import software.amazon.awssdk.services.stepfunctions.builder.states.State;

/**
 * Represents a StepFunctions state machine. A state machine must have at least one state.
 *
 * @see <a href="https://states-language.net/spec.html#toplevelfields">https://states-language.net/spec.html#toplevelfields</a>
 */
public final class StateMachine {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .registerModule(DateModule.INSTANCE);

    @JsonProperty(PropertyName.COMMENT)
    private final String comment;

    @JsonProperty(PropertyName.START_AT)
    private final String startAt;

    @JsonProperty(PropertyName.TIMEOUT_SECONDS)
    private final Integer timeoutSeconds;

    @JsonProperty(PropertyName.STATES)
    private final Map<String, State> states;

    private StateMachine(Builder builder) {
        this.comment = builder.comment;
        this.startAt = builder.startAt;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.states = Buildable.Utils.build(builder.states);
    }

    /**
     * Deserializes a JSON representation of a state machine into a {@link StateMachine.Builder} .
     *
     * @param json JSON representing State machine.
     * @return Mutable {@link StateMachine.Builder} deserialized from JSON representation.
     */
    public static StateMachine.Builder fromJson(String json) {
        try {
            return MAPPER.readValue(json, StateMachine.Builder.class);
        } catch (IOException e) {
            throw new SdkClientException(
                    String.format("Could not deserialize state machine.\n%s", json), e);
        }
    }

    /**
     * @return Builder instance to construct a {@link StateMachine}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Human readable description for the state machine.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return Name of the state to start execution at.
     */
    public String getStartAt() {
        return startAt;
    }

    /**
     * @return Timeout, in seconds, that a state machine is allowed to run. If the machine execution runs longer than this timeout
     *     the execution fails with a {@link ErrorCode#TIMEOUT} error
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * @return The states for this state machine.
     */
    public Map<String, State> getStates() {
        return states;
    }

    /**
     * @return Compact JSON representation of this StateMachine. May be provided to API calls (see {@link
     *     software.amazon.awssdk.services.stepfunctions.AWSStepFunctions}).
     */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new SdkClientException("Could not serialize state machine.", e);
        }
    }

    /**
     * @return Formatted JSON representation of this StateMachine. May be provided to API calls (see {@link
     *     software.amazon.awssdk.services.stepfunctions.AWSStepFunctions}).
     */
    public String toPrettyJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new SdkClientException("Could not serialize state machine.", e);
        }
    }

    /**
     * Builder for a {@link StateMachine}.
     */
    public static final class Builder {

        @JsonProperty(PropertyName.STATES)
        private final Map<String, State.Builder> states = new LinkedHashMap<String, State.Builder>();
        @JsonProperty(PropertyName.COMMENT)
        private String comment;
        @JsonProperty(PropertyName.START_AT)
        private String startAt;
        @JsonProperty(PropertyName.TIMEOUT_SECONDS)
        private Integer timeoutSeconds;

        private Builder() {
        }

        /**
         * OPTIONAL. Human readable description for the state machine.
         *
         * @param comment New comment.
         * @return This object for method chaining.
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * REQUIRED. Name of the state to start execution at. Must match a state name provided via {@link #state(String,
         * State.Builder)}.
         *
         * @param startAt Name of starting state.
         * @return This object for method chaining.
         */
        public Builder startAt(String startAt) {
            this.startAt = startAt;
            return this;
        }

        /**
         * OPTIONAL. Timeout, in seconds, that a state machine is allowed to run. If the machine execution runs longer than this
         * timeout the execution fails with a {@link ErrorCode#TIMEOUT} error
         *
         * @param timeoutSeconds Timeout value.
         * @return This object for method chaining.
         */
        public Builder timeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * REQUIRED. Adds a new state to the state machine. A state machine MUST have at least one state.
         *
         * @param stateName    Name of the state
         * @param stateBuilder Instance of {@link State.Builder}. Note that the {@link State}
         *                     object is not built until the {@link StateMachine} is built so any modifications on the state
         *                     builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder state(String stateName, State.Builder stateBuilder) {
            this.states.put(stateName, stateBuilder);
            return this;
        }

        /**
         * @return An immutable {@link StateMachine} object that can be transformed to JSON via {@link StateMachine#toJson()}.
         */
        public StateMachine build() {
            return new StateMachineValidator(new StateMachine(this)).validate();
        }
    }
}
