/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.pipeline;

import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;
import static software.amazon.awssdk.utils.FunctionalUtils.toFunction;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.HttpAsyncClientDependencies;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.HttpSyncClientDependencies;

/**
 * Builder for a {@link RequestPipeline}.
 *
 * @param <InputT>  Currently configured input type to the {@link RequestPipeline}.
 * @param <OutputT> Currently configured output type to the {@link RequestPipeline}.
 */
@Immutable
@SdkInternalApi
public class RequestPipelineBuilder<InputT, OutputT, D extends HttpClientDependencies> {

    private final Function<D, RequestPipeline<InputT, OutputT>> pipelineFactory;

    RequestPipelineBuilder(Function<D, RequestPipeline<InputT, OutputT>> pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    /**
     * Factory method to create a {@link RequestPipelineBuilder} with an initial pipeline stage. Stages in this pipeline will only
     * be able to accept an {@link HttpAsyncClientDependencies} or {@link HttpClientDependencies}.
     *
     * @param pipelineFactory Factory that can produce a {@link RequestPipeline}. Should take an {@link HttpClientDependencies}
     *                        object as the first parameter to the factory method.
     * @param <InputT>        Input type of pipeline
     * @param <OutputT>       Output type of pipeline.
     * @return RequestPipelineBuilder composed of that initial stage.
     * @see #build(HttpClientDependencies)
     */
    public static <InputT, OutputT> RequestPipelineBuilder<InputT, OutputT, HttpAsyncClientDependencies>
            firstAsync(Function<HttpAsyncClientDependencies, RequestPipeline<InputT, OutputT>> pipelineFactory) {
        return new RequestPipelineBuilder<>(pipelineFactory);
    }

    /**
     * Factory method to create a {@link RequestPipelineBuilder} with an initial pipeline stage.Stages in this pipeline will only
     * be able to accept an {@link HttpAsyncClientDependencies} or {@link HttpClientDependencies}.
     *
     * @param pipelineFactory Factory that can produce a {@link RequestPipeline}. Use this overload when the factory does not
     *                        need {@link HttpClientDependencies} and should instead take no arguments.
     * @param <InputT>        Input type of pipeline
     * @param <OutputT>       Output type of pipeline.
     * @return RequestPipelineBuilder composed of that initial stage.
     * @see #build(HttpClientDependencies)
     */
    public static <InputT, OutputT> RequestPipelineBuilder<InputT, OutputT, HttpAsyncClientDependencies>
            firstAsync(Supplier<RequestPipeline<InputT, OutputT>> pipelineFactory) {
        return new RequestPipelineBuilder<>(d -> pipelineFactory.get());
    }

    /**
     * Factory method to create a {@link RequestPipelineBuilder} with an initial pipeline stage. Stages in this pipeline will only
     * be able to accept an {@link HttpSyncClientDependencies} or {@link HttpClientDependencies}.
     *
     * @param pipelineFactory Factory that can produce a {@link RequestPipeline}. Should take an {@link HttpClientDependencies}
     *                        object as the first parameter to the factory method.
     * @param <InputT>        Input type of pipeline
     * @param <OutputT>       Output type of pipeline.
     * @return RequestPipelineBuilder composed of that initial stage.
     * @see #build(HttpClientDependencies)
     */
    public static <InputT, OutputT> RequestPipelineBuilder<InputT, OutputT, HttpSyncClientDependencies>
            firstSync(Function<HttpSyncClientDependencies, RequestPipeline<InputT, OutputT>> pipelineFactory) {
        return new RequestPipelineBuilder<>(pipelineFactory);
    }

    /**
     * Factory method to create a {@link RequestPipelineBuilder} with an initial pipeline stage. Stages in this pipeline will only
     * be able to accept an {@link HttpSyncClientDependencies} or {@link HttpClientDependencies}.
     *
     * @param pipelineFactory Factory that can produce a {@link RequestPipeline}. Use this overload when the factory does not
     *                        need {@link HttpClientDependencies} and should instead take no arguments.
     * @param <InputT>        Input type of pipeline
     * @param <OutputT>       Output type of pipeline.
     * @return RequestPipelineBuilder composed of that initial stage.
     * @see #build(HttpClientDependencies)
     */
    public static <InputT, OutputT> RequestPipelineBuilder<InputT, OutputT, HttpSyncClientDependencies>
            firstSync(Supplier<RequestPipeline<InputT, OutputT>> pipelineFactory) {
        return new RequestPipelineBuilder<>(d -> pipelineFactory.get());
    }

    /**
     * Factory method to chain the current {@link RequestPipelineBuilder} with another pipeline stage. The new stage's input type
     * must match the current stage's output type. The new stage may define a new output type (if it's transforming the type) or
     * may define the same output type as the current stage.
     *
     * @param pipelineFactory Factory that can produce a {@link RequestPipeline}. Should take an {@link HttpClientDependencies}
     *                        object as the first parameter to the factory method.
     * @param <NewOutputT>    New output type of pipeline
     * @return A new RequestPipelineBuilder composed of the previous stages and the new stage.
     * @see #build(HttpClientDependencies)
     */
    public <NewOutputT> RequestPipelineBuilder<InputT, NewOutputT, D> then(
            Function<D, RequestPipeline<OutputT, NewOutputT>> pipelineFactory) {
        return new RequestPipelineBuilder<>(r -> new ComposingRequestPipelineStage<>(this.pipelineFactory.apply(r),
                                                                                     pipelineFactory.apply(r)));
    }

    /**
     * Convert a synchronous {@link RequestPipeline} factory into a factory that produces a version of the RequestPipeline
     * that accepts a CompletableFuture and returns a CompletableFuture.
     *
     * @param pipelineFactory the delegate pipeline factory to wrap
     * @param <InputT>        the input type of the original {@link RequestPipeline}
     * @param <OutputT>       the return type of the original {@link RequestPipeline}
     * @return the wrapped {@link RequestPipeline} factory
     */
    @SuppressWarnings("unchecked")
    public static <InputT, OutputT>
            Function<HttpAsyncClientDependencies, RequestPipeline<CompletableFuture<InputT>, CompletableFuture<OutputT>>>
            async(Function<HttpAsyncClientDependencies, RequestPipeline<InputT, OutputT>> pipelineFactory) {
        return httpClientDependencies -> new AsyncRequestPipelineWrapper(pipelineFactory.apply(httpClientDependencies));
    }

    /**
     * A version of {@link #async(Function)} that takes a {@link Supplier}
     *
     * @see #async(Supplier)
     */
    public static <InputT, OutputT>
            Function<HttpAsyncClientDependencies, RequestPipeline<CompletableFuture<InputT>, CompletableFuture<OutputT>>>
            async(Supplier<RequestPipeline<InputT, OutputT>> pipelineFactory) {
        return async(toFunction(pipelineFactory));
    }

    /**
     * Factory method to chain the current {@link RequestPipelineBuilder} with another pipeline stage. The new stage's input type
     * must match the current stage's output type. The new stage may define a new output type (if it's transforming the type) or
     * may define the same output type as the current stage.
     *
     * @param pipelineFactory Factory that can produce a {@link RequestPipeline}. Use this overload when the factory does not
     *                        need {@link HttpClientDependencies} and should instead take no arguments.
     * @param <NewOutputT>    New output type of pipeline
     * @return A new RequestPipelineBuilder composed of the previous stages and the new stage.
     * @see #build(HttpClientDependencies)
     */
    public <NewOutputT> RequestPipelineBuilder<InputT, NewOutputT, D> then(
            Supplier<RequestPipeline<OutputT, NewOutputT>> pipelineFactory) {
        return new RequestPipelineBuilder<>(r -> new ComposingRequestPipelineStage<>(this.pipelineFactory.apply(r),
                                                                                     pipelineFactory.get()));
    }

    /**
     * Factory method to wrap the current {@link RequestPipelineBuilder} with another pipeline stage. The argument to wrap is a
     * factory that takes an {@link HttpClientDependencies} object and a inner {@link RequestPipeline} (the current one being
     * built) as arguments and produces a new {@link RequestPipeline} for the wrapper. The wrapper may have entirely different
     * input and output types, typically it will have the same however.
     *
     * @param wrappedFactory {@link BiFunction} factory that can produce a {@link RequestPipeline}. The arguments to the factory
     *                       will be {@link HttpClientDependencies} and an inner {@link RequestPipeline}.
     * @param <NewOutputT>   New output type of pipeline
     * @return A new RequestPipelineBuilder that wraps around the pipeline currently being constructed.
     * @see #build(HttpClientDependencies)
     */
    public <NewInputT, NewOutputT> RequestPipelineBuilder<NewInputT, NewOutputT, D> wrap(
            BiFunction<D, RequestPipeline<InputT, OutputT>,
                    RequestPipeline<NewInputT, NewOutputT>> wrappedFactory) {
        return new RequestPipelineBuilder<>(r -> wrappedFactory.apply(r, this.pipelineFactory.apply(r)));
    }

    /**
     * Factory method to wrap the current {@link RequestPipelineBuilder} with another pipeline stage. The argument to wrap is a
     * factory that takes an inner {@link RequestPipeline} (the current one being built) as an argument and produces a new {@link
     * RequestPipeline} for the wrapper. The wrapper may have entirely different input and output types, typically it will have
     * the same however.
     *
     * @param wrappedFactory {@link Function} factory that can produce a {@link RequestPipeline}. The argument to the factory
     *                       will be an inner {@link RequestPipeline}.
     * @param <NewOutputT>   New output type of pipeline
     * @return A new RequestPipelineBuilder that wraps around the pipeline currently being constructed.
     * @see #build(HttpClientDependencies)
     */
    public <NewInputT, NewOutputT> RequestPipelineBuilder<NewInputT, NewOutputT, D> wrap(
            Function<RequestPipeline<InputT, OutputT>,
                    RequestPipeline<NewInputT, NewOutputT>> wrappedFactory) {
        return new RequestPipelineBuilder<>(d -> wrappedFactory.apply(this.pipelineFactory.apply(d)));
    }

    /**
     * Construct the {@link RequestPipeline} with the currently configured stages.
     *
     * @param dependencies {@link HttpClientDependencies} to supply to factory methods that are interested in it.
     * @return Constructed {@link RequestPipeline}.
     * @see RequestPipeline#execute(Object, RequestExecutionContext)
     */
    public RequestPipeline<InputT, OutputT> build(D dependencies) {
        return pipelineFactory.apply(dependencies);
    }

    /**
     * Chains two {@link RequestPipeline}'s together.
     *
     * @param <InputT>  Input of first pipeline.
     * @param <MiddleT> Output of first pipeline and input to second.
     * @param <OutputT> Output of second pipeline.
     */
    private static class ComposingRequestPipelineStage<InputT, MiddleT, OutputT> implements RequestPipeline<InputT, OutputT> {

        private final RequestPipeline<InputT, MiddleT> first;
        private final RequestPipeline<MiddleT, OutputT> second;

        private ComposingRequestPipelineStage(RequestPipeline<InputT, MiddleT> first, RequestPipeline<MiddleT, OutputT> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public OutputT execute(InputT in, RequestExecutionContext context) throws Exception {
            return second.execute(first.execute(in, context), context);
        }
    }

    /**
     * Converts a synchronous {@link RequestPipeline} into one that accepts and returns a CompletableFuture
     *
     * @param <InputT>  The input type expected by the delegate RequestPipeline
     * @param <OutputT> The output type produced by the delegate RequestPipeline
     */
    private static class AsyncRequestPipelineWrapper<InputT, OutputT>
            implements RequestPipeline<CompletableFuture<InputT>, CompletableFuture<OutputT>> {

        private final RequestPipeline<InputT, OutputT> delegate;

        private AsyncRequestPipelineWrapper(RequestPipeline<InputT, OutputT> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<OutputT> execute(CompletableFuture<InputT> inputFuture, RequestExecutionContext context)
                throws Exception {
            return inputFuture.thenApply(safeFunction(input -> delegate.execute(input, context)));
        }
    }

}
