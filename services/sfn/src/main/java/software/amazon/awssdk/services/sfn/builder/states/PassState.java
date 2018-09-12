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

package software.amazon.awssdk.services.sfn.builder.states;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sfn.builder.internal.PropertyName;

/**
 * The Pass State simply passes its input to its output, performing no work. Pass States are useful when constructing and
 * debugging state machines.
 *
 * <p>A Pass State MAY have a field named “Result”. If present, its value is treated as the output of a virtual task, and placed
 * as prescribed by the “ResultPath” field, if any, to be passed on to the next state.</p>
 *
 * @see <a href="https://states-language.net/spec.html#pass-state">https://states-language.net/spec.html#pass-state</a>
 */
@SdkPublicApi
public final class PassState extends TransitionState {

    /**
     * Disable Jackson specific features like annotations. Support only
     * basic POJO serialization to limit our coupling to Jackson.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(MapperFeature.USE_ANNOTATIONS)
            .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
            .disable(MapperFeature.AUTO_DETECT_FIELDS);

    @JsonProperty(PropertyName.COMMENT)
    private final String comment;

    @JsonProperty(PropertyName.RESULT)
    private final JsonNode result;

    @JsonProperty(PropertyName.INPUT_PATH)
    private final String inputPath;

    @JsonProperty(PropertyName.OUTPUT_PATH)
    private final String outputPath;

    @JsonProperty(PropertyName.RESULT_PATH)
    private final String resultPath;

    @JsonUnwrapped
    private final Transition transition;

    private PassState(Builder builder) {
        this.comment = builder.comment;
        this.result = builder.result;
        this.inputPath = builder.inputPath;
        this.outputPath = builder.outputPath;
        this.resultPath = builder.resultPath;
        this.transition = builder.transition.build();
    }

    /**
     * @return Builder instance to construct a {@link PassState}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Type identifier of {@link PassState}.
     */
    @Override
    public String getType() {
        return "Pass";
    }

    /**
     * @return Human readable description for the state.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return String containing JSON document of the state's "virtual" result.
     */
    @JsonIgnore
    public String getResult() {
        try {
            return result == null ? null : MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw SdkClientException.builder().message("Could not serialize result").cause(e).build();
        }
    }

    /**
     * @return The input path expression that may optionally transform the input to this state.
     */
    public String getInputPath() {
        return inputPath;
    }

    /**
     * @return The output path expression that may optionally transform the output to this state.
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * @return The result path expression that may optionally combine or replace the state's raw input with it's result.
     */
    public String getResultPath() {
        return resultPath;
    }

    /**
     * @return The {@link Transition} for this state.
     */
    public Transition getTransition() {
        return transition;
    }

    @Override
    public <T> T accept(StateVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * Builder for a {@link PassState}.
     */
    public static final class Builder extends TransitionStateBuilder {

        @JsonProperty(PropertyName.COMMENT)
        private String comment;

        @JsonProperty(PropertyName.RESULT)
        private JsonNode result;

        @JsonProperty(PropertyName.INPUT_PATH)
        private String inputPath;

        @JsonProperty(PropertyName.OUTPUT_PATH)
        private String outputPath;

        @JsonProperty(PropertyName.RESULT_PATH)
        private String resultPath;

        private Transition.Builder transition = Transition.NULL_BUILDER;

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
         * OPTIONAL. Sets the "virtual" result of the pass state. Must be a POJO that can be serialized into JSON.
         *
         * @param result Object that will be serialized into the JSON document representing this states result.
         * @return This object for method chaining.
         */
        public Builder result(Object result) {
            this.result = MAPPER.valueToTree(result);
            return this;
        }

        /**
         * OPTIONAL. Sets the "virtual" result of the pass state. Must be a valid JSON document.
         *
         * @param result JSON result represented as a string.
         * @return This object for method chaining.
         */
        public Builder result(String result) {
            try {
                this.result = MAPPER.readTree(result);
            } catch (IOException e) {
                throw SdkClientException.builder().message("Result must be a JSON document").cause(e).build();
            }
            return this;
        }

        /**
         * OPTIONAL. The value of “InputPath” MUST be a Path, which is applied to a State’s raw input to select some or all of
         * it;
         * that selection is used by the state. If not provided then the whole output from the previous state is used as input to
         * this state.
         *
         * @param inputPath New path value.
         * @return This object for method chaining.
         */
        public Builder inputPath(String inputPath) {
            this.inputPath = inputPath;
            return this;
        }

        /**
         * OPTIONAL. The value of “OutputPath” MUST be a path, which is applied to the state’s output after the application of
         * ResultPath,
         * leading in the generation of the raw input for the next state. If not provided then the whole output is used.
         *
         * @param outputPath New path value.
         * @return This object for method chaining.
         */
        public Builder outputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        /**
         * OPTIONAL. The value of “ResultPath” MUST be a Reference Path, which specifies the combination with or replacement of
         * the state’s result with its raw input. If not provided then the output completely replaces the input.
         *
         * @param resultPath New path value.
         * @return This object for method chaining.
         */
        public Builder resultPath(String resultPath) {
            this.resultPath = resultPath;
            return this;
        }

        /**
         * REQUIRED. Sets the transition that will occur when this state completes successfully.
         *
         * @param transition New transition.
         * @return This object for method chaining.
         */
        public Builder transition(Transition.Builder transition) {
            this.transition = transition;
            return this;
        }

        /**
         * @return An immutable {@link PassState} object.
         */
        public PassState build() {
            return new PassState(this);
        }
    }
}
