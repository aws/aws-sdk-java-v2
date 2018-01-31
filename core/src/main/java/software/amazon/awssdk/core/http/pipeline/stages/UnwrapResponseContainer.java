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

package software.amazon.awssdk.core.http.pipeline.stages;

import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.http.pipeline.RequestPipeline;

/**
 * Unwrap a {@link Response} container to just the POJO result. If we've gotten this far
 * then the Response container can only be a success response, otherwise the exception would have
 * been thrown out of the pipeline.
 *
 * @param <OutputT> POJO result type.
 */
public class UnwrapResponseContainer<OutputT> implements RequestPipeline<Response<OutputT>, OutputT> {
    @Override
    public OutputT execute(Response<OutputT> input, RequestExecutionContext context) throws Exception {
        return input.getAwsResponse();
    }
}
