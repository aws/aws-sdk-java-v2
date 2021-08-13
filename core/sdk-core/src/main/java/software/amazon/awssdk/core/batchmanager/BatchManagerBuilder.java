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

package software.amazon.awssdk.core.batchmanager;

import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.internal.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.internal.batchmanager.BatchResponseMapper;

@SdkProtectedApi
public interface BatchManagerBuilder<RequestT, ResponseT, BatchResponseT, B> {

    /**
     * Defines overrides to the default BatchManager configuration that should be used.
     *
     * @param overrideConfiguration the override configuration.
     * @return a reference to this object so that method calls can be chained together.
     */
    B overrideConfiguration(BatchOverrideConfiguration overrideConfiguration);

    /**
     * Adds a {@link ScheduledExecutorService} to be used by the BatchManager to schedule periodic flushes of the underlying
     * buffers.
     * <p>
     * Creating a SqsBatchManager directly from a service client will use the service client's scheduled executor. If supplied by
     * the user, this ScheduledExecutorService must be closed by the caller when it is ready to be shut down.
     *
     * @param scheduledExecutor the provided scheduled executor.
     * @return a reference to this object so that method calls can be chained together.
     */
    B scheduledExecutor(ScheduledExecutorService scheduledExecutor);

    /**
     * Adds a function that defines how requests should be batched together into the appropriate batch response.
     *
     * @param batchFunction the provided function.
     * @return a reference to this object so that method calls can be chained together.
     */
    B batchFunction(BatchAndSend<RequestT, BatchResponseT> batchFunction);

    /**
     * Adds a function that defines how a batch response should be extracted and transformed into its individual responses.
     *
     * @param responseMapper the provided function.
     * @return a reference to this object so that method calls can be chained together.
     */
    B responseMapper(BatchResponseMapper<BatchResponseT, ResponseT> responseMapper);

    /**
     * Adds a function that calculates an appropriate batchKey from a given request.
     *
     * @param batchKeyMapper the provided function.
     * @return a reference to this object so that method calls can be chained together.
     */
    B batchKeyMapper(BatchKeyMapper<RequestT> batchKeyMapper);
}
