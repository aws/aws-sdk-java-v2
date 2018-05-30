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

package software.amazon.awssdk.core.internal.http.pipeline;

import java.util.function.BiFunction;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;

/**
 * Represents a series of transformations when executing a SDK request.
 *
 * @param <InputT>  Source type, provided as input param to pipeline.
 * @param <OutputT> Output type returned by the pipeline.
 */
public interface RequestPipeline<InputT, OutputT> {

    /**
     * Execute the pipeline with the given input.
     *
     * @param input   Input to pipeline.
     * @param context Context containing both request dependencies, and a container for any mutable state that must be shared
     *                between stages.
     * @return Output of pipeline.
     * @throws Exception If any error occurs. This will be thrown out of the pipeline, if exceptions must be handled see
     *                   {@link RequestPipelineBuilder#wrap(BiFunction)}.
     */
    OutputT execute(InputT input, RequestExecutionContext context) throws Exception;
}
