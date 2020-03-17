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

package software.amazon.awssdk.core.internal.http.pipeline;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Pipeline stage that takes in a mutable {@link SdkHttpFullRequest.Builder} and returns the same builder. Useful
 * for long chains of mutating stages where going to and from builder each stage is inefficient.
 */
@SdkInternalApi
public interface MutableRequestToRequestPipeline
        extends RequestPipeline<SdkHttpFullRequest.Builder, SdkHttpFullRequest.Builder> {
}
