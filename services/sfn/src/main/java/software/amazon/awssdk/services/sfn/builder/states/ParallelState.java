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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.sfn.builder.internal.Buildable;
import software.amazon.awssdk.services.sfn.builder.internal.PropertyName;

/**
 * State that allows for parallel execution of {@link Branch}s. A Parallel state causes the interpreter to execute each branch
 * starting with the state named in its “StartAt” field, as concurrently as possible, and wait until each branch terminates
 * (reaches a terminal state) before processing the Parallel state's “Next” field.
 *
 * @see <a href="https://states-language.net/spec.html#parallel-state">https://states-language.net/spec.html#parallel-state</a>
 */
@SdkPublicApi
public final class ParallelState extends TransitionState {

    @JsonProperty(PropertyName.COMMENT)
    private final String comment;

    @JsonProperty(PropertyName.BRANCHES)
    private final List<Branch> branches;

    @JsonProperty(PropertyName.INPUT_PATH)
    private final String inputPath;

    @JsonProperty(PropertyName.RESULT_PATH)
    private final String resultPath;

    @JsonProperty(PropertyName.OUTPUT_PATH)
    private final String outputPath;

    @JsonUnwrapped
    private final Transition transition;

    @JsonProperty(PropertyName.RETRY)
    private final List<Retrier> retriers;

    @JsonProperty(PropertyName.CATCH)
    private final List<Catcher> catchers;

    private ParallelState(Builder builder) {
        this.comment = builder.comment;
        this.branches = Buildable.Utils.build(builder.branches);
        this.inputPath = builder.inputPath;
        this.resultPath = builder.resultPath;
        this.outputPath = builder.outputPath;
        this.transition = builder.transition.build();
        this.retriers = Buildable.Utils.build(builder.retriers);
        this.catchers = Buildable.Utils.build(builder.catchers);
    }

    /**
     * @return Builder instance to construct a {@link ParallelState}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Type identifier of {@link ParallelState}.
     */
    @Override
    public String getType() {
        return "Parallel";
    }

    /**
     * @return The transition that will occur when all branches have executed successfully.
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
     * @return The branches of execution for this {@link ParallelState}.
     */
    public List<Branch> getBranches() {
        return branches;
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
     * Builder for a {@link ParallelState}.
     */
    public static final class Builder extends TransitionStateBuilder {

        @JsonProperty(PropertyName.COMMENT)
        private String comment;

        @JsonProperty(PropertyName.BRANCHES)
        private List<Branch.Builder> branches = new ArrayList<Branch.Builder>();

        @JsonProperty(PropertyName.INPUT_PATH)
        private String inputPath;

        @JsonProperty(PropertyName.RESULT_PATH)
        private String resultPath;

        @JsonProperty(PropertyName.OUTPUT_PATH)
        private String outputPath;

        private Transition.Builder transition = Transition.NULL_BUILDER;

        @JsonProperty(PropertyName.RETRY)
        private List<Retrier.Builder> retriers = new ArrayList<Retrier.Builder>();

        @JsonProperty(PropertyName.CATCH)
        private List<Catcher.Builder> catchers = new ArrayList<Catcher.Builder>();

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
         * REQUIRED. Adds a new branch of execution to this states branches. A parallel state must have at least one branch.
         *
         * @param branchBuilder Instance of {@link Branch.Builder}. Note that the {@link
         *                      Branch} object is not built until the {@link ParallelState} is built so any modifications on the
         *                      state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder branch(Branch.Builder branchBuilder) {
            this.branches.add(branchBuilder);
            return this;
        }

        /**
         * REQUIRED. Adds the branches of execution to this states branches. A parallel state must have at least one branch.
         *
         * @param branchBuilders Instances of {@link Branch.Builder}. Note that the {@link
         *                       Branch} object is not built until the {@link ParallelState} is built so any modifications on the
         *                       state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder branches(Branch.Builder... branchBuilders) {
            for (Branch.Builder branchBuilder : branchBuilders) {
                branch(branchBuilder);
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
         * REQUIRED. Sets the transition that will occur when all branches in this parallel
         * state have executed successfully.
         *
         * @param builder New transition.
         * @return This object for method chaining.
         */
        @Override
        public Builder transition(Transition.Builder builder) {
            this.transition = builder;
            return this;
        }

        /**
         * OPTIONAL. Adds the {@link Retrier}s to this states retries. If a single branch fails then the entire parallel state is
         * considered failed and eligible for retry.
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
         * OPTIONAL. Adds the {@link Retrier} to this states retries. If a single branch fails then the entire parallel state is
         * considered failed and eligible for retry.
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
         * OPTIONAL. Adds the {@link Catcher}s to this states catchers.  If a single branch fails then the entire parallel state
         * is considered failed and eligible to be caught.
         *
         * @param catcherBuilders Instances of {@link Catcher.Builder}. Note that the {@link
         *                        Catcher} object is not built until the {@link ParallelState} is built so any modifications on
         *                        the state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder catchers(Catcher.Builder... catcherBuilders) {
            for (Catcher.Builder catcherBuilder : catcherBuilders) {
                catcher(catcherBuilder);
            }
            return this;
        }

        /**
         * OPTIONAL. Adds the {@link Catcher} to this states catchers.  If a single branch fails then the entire parallel state
         * is
         * considered failed and eligible to be caught.
         *
         * @param catcherBuilder Instance of {@link Catcher.Builder}. Note that the {@link
         *                       Catcher} object is not built until the {@link ParallelState} is built so any modifications on
         *                       the
         *                       state builder will be reflected in this object.
         * @return This object for method chaining.
         */
        public Builder catcher(Catcher.Builder catcherBuilder) {
            this.catchers.add(catcherBuilder);
            return this;
        }

        /**
         * @return An immutable {@link ParallelState} object.
         */
        public ParallelState build() {
            return new ParallelState(this);
        }
    }
}
