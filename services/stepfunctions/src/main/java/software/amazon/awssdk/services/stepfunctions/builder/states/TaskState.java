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
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.stepfunctions.builder.ErrorCode;
import software.amazon.awssdk.services.stepfunctions.builder.internal.Buildable;
import software.amazon.awssdk.services.stepfunctions.builder.internal.PropertyName;

/**
 * The Task State causes the interpreter to execute the work identified by the state’s “Resource” field.
 *
 * <p>Currently allowed resources include Lambda functions and States activities.</p>
 *
 * @see <a href="https://states-language.net/spec.html#task-state">https://states-language.net/spec.html#task-state</a>
 */
public final class TaskState extends TransitionState {

    @JsonProperty(PropertyName.RESOURCE)
    private final String resource;

    @JsonProperty(PropertyName.INPUT_PATH)
    private final String inputPath;

    @JsonProperty(PropertyName.RESULT_PATH)
    private final String resultPath;

    @JsonProperty(PropertyName.OUTPUT_PATH)
    private final String outputPath;

    @JsonProperty(PropertyName.COMMENT)
    private final String comment;

    @JsonProperty(PropertyName.TIMEOUT_SECONDS)
    private final Integer timeoutSeconds;

    @JsonProperty(PropertyName.HEARTBEAT_SECONDS)
    private final Integer heartbeatSeconds;

    @JsonUnwrapped
    private final Transition transition;

    @JsonProperty(PropertyName.RETRY)
    private final List<Retrier> retriers;

    @JsonProperty(PropertyName.CATCH)
    private final List<Catcher> catchers;

    private TaskState(Builder builder) {
        this.resource = builder.resource;
        this.inputPath = builder.inputPath;
        this.resultPath = builder.resultPath;
        this.outputPath = builder.outputPath;
        this.comment = builder.comment;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.heartbeatSeconds = builder.heartbeatSeconds;
        this.transition = builder.transition.build();
        this.retriers = Buildable.Utils.build(builder.retriers);
        this.catchers = Buildable.Utils.build(builder.catchers);
    }

    /**
     * @return Builder instance to construct a {@link TaskState}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Type identifier of {@link TaskState}.
     */
    @Override
    public String getType() {
        return "Task";
    }

    /**
     * @return URI of the resource to be executed by this task.
     */
    public String getResource() {
        return resource;
    }

    /**
     * @return The input path expression that may optionally transform the input to this state.
     */
    public String getInputPath() {
        return inputPath;
    }

    /**
     * @return The result path expression that may optionally combine or replace the state's raw input with it's result.
     */
    public String getResultPath() {
        return resultPath;
    }

    /**
     * @return The output path expression that may optionally transform the output to this state.
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * @return The transition that will occur when this task completes successfully.
     */
    public Transition getTransition() {
        return transition;
    }

    /**
     * @return Human readable description for the state.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return Timeout, in seconds, that a task is allowed to run.
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * @return Allowed time between "Heartbeats".
     */
    public Integer getHeartbeatSeconds() {
        return heartbeatSeconds;
    }

    /**
     * @return The list of {@link Retrier}s for this state.
     */
    public List<Retrier> getRetriers() {
        return retriers;
    }

    /**
     * @return The list of {@link Catcher}s for this state.
     */
    public List<Catcher> getCatchers() {
        return catchers;
    }

    @Override
    public <T> T accept(StateVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * Builder for a {@link TaskState}.
     */
    public static final class Builder extends TransitionStateBuilder {

        @JsonProperty(PropertyName.RETRY)
        private final List<Retrier.Builder> retriers = new ArrayList<Retrier.Builder>();
        @JsonProperty(PropertyName.CATCH)
        private final List<Catcher.Builder> catchers = new ArrayList<Catcher.Builder>();
        @JsonProperty(PropertyName.RESOURCE)
        private String resource;
        @JsonProperty(PropertyName.INPUT_PATH)
        private String inputPath;
        @JsonProperty(PropertyName.RESULT_PATH)
        private String resultPath;
        @JsonProperty(PropertyName.OUTPUT_PATH)
        private String outputPath;
        @JsonProperty(PropertyName.COMMENT)
        private String comment;
        @JsonProperty(PropertyName.TIMEOUT_SECONDS)
        private Integer timeoutSeconds;
        @JsonProperty(PropertyName.HEARTBEAT_SECONDS)
        private Integer heartbeatSeconds;
        private Transition.Builder transition = Transition.NULL_BUILDER;

        private Builder() {
        }

        /**
         * REQUIRED. Sets the resource to be executed by this task. Must be a URI that uniquely identifies the specific task to
         * execute. This may be the ARN of a Lambda function or a States Activity.
         *
         * @param resource URI of resource.
         * @return This object for method chaining.
         */
        public Builder resource(String resource) {
            this.resource = resource;
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
         * OPTIONAL. The value of “OutputPath” MUST be a path, which is applied to the state’s output after the application of
         * ResultPath, leading in the generation of the raw input for the next state. If not provided then the whole output is
         * used.
         *
         * @param outputPath New path value.
         * @return This object for method chaining.
         */
        public Builder outputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
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
         * OPTIONAL. Timeout, in seconds, that a task is allowed to run. If the task execution runs longer than this timeout the
         * execution fails with a {@link ErrorCode#TIMEOUT} error.
         *
         * @param timeoutSeconds Timeout value.
         * @return This object for method chaining.
         */
        public Builder timeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * OPTIONAL. Allowed time between "Heartbeats". If the task does not send "Heartbeats" within the timeout then execution
         * fails with a {@link ErrorCode#TIMEOUT}. If not set then no heartbeats are required. Heartbeats are a more granular
         * way
         * for a task to report it's progress to the state machine.
         *
         * @param heartbeatSeconds Heartbeat value.
         * @return This object for method chaining.
         */
        public Builder heartbeatSeconds(Integer heartbeatSeconds) {
            this.heartbeatSeconds = heartbeatSeconds;
            return this;
        }

        /**
         * REQUIRED. Sets the transition that will occur when the task completes successfully.
         *
         * @param transition New transition.
         * @return This object for method chaining.
         */
        public Builder transition(Transition.Builder transition) {
            this.transition = transition;
            return this;
        }

        /**
         * OPTIONAL. Adds the {@link Retrier}s to this states retriers. If the task exits abnormally (throws exception, times
         * out,
         * etc) it will be considered failed and eligible to be retried.
         *
         * @param retrierBuilders Instances of {@link Retrier.Builder}. Note that the {@link
         *                        Retrier} object is not built until the {@link ParallelState} is built so any modifications on
         *                        the state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder retriers(Retrier.Builder... retrierBuilders) {
            for (Retrier.Builder retrierBuilder : retrierBuilders) {
                retrier(retrierBuilder);
            }
            return this;
        }

        /**
         * OPTIONAL. Adds the {@link Retrier} to this states retriers. If the task exits abnormally (throws exception, times out,
         * etc) it will be considered failed and eligible to be retried.
         *
         * @param retrierBuilder Instance of {@link Retrier.Builder}. Note that the {@link
         *                       Retrier} object is not built until the {@link ParallelState} is built so any modifications on
         *                       the
         *                       state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder retrier(Retrier.Builder retrierBuilder) {
            this.retriers.add(retrierBuilder);
            return this;
        }

        /**
         * OPTIONAL. Adds the {@link Catcher}s to this states catchers. If the task exits abnormally (throws exception, times
         * out,
         * etc) it will be considered failed and eligible to be caught.
         *
         * @param catcherBuilders Instances of {@link Catcher.Builder}. Note that the {@link
         *                        Catcher} object is not built until the {@link TaskState} is built so any modifications on the
         *                        state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder catchers(Catcher.Builder... catcherBuilders) {
            for (Catcher.Builder catcherBuilder : catcherBuilders) {
                catcher(catcherBuilder);
            }
            return this;
        }

        /**
         * OPTIONAL. Adds the {@link Catcher} to this states catchers. If the task exits abnormally (throws exception, times out,
         * etc) it will be considered failed and eligible to be caught.
         *
         * @param catcherBuilder Instance of {@link Catcher.Builder}. Note that the {@link
         *                       Catcher} object is not built until the {@link TaskState} is built so any modifications on the
         *                       state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder catcher(Catcher.Builder catcherBuilder) {
            this.catchers.add(catcherBuilder);
            return this;
        }

        /**
         * @return An immutable {@link TaskState} object.
         */
        public TaskState build() {
            return new TaskState(this);
        }
    }
}
