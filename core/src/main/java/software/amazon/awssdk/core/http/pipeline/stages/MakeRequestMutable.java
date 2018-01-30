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
import software.amazon.awssdk.core.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;

public class MakeRequestMutable implements RequestPipeline<SdkHttpFullRequest, SdkHttpFullRequest.Builder> {
    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        return input.toBuilder();
    }
}
