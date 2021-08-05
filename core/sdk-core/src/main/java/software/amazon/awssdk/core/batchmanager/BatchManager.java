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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.internal.batchmanager.DefaultBatchManager;
import software.amazon.awssdk.core.internal.waiters.DefaultWaiter;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterBuilder;
import software.amazon.awssdk.utils.SdkAutoCloseable;

// TODO: Add javadoc commentsgit
public interface BatchManager<RequestT, ResponseT, BatchResponseT> extends SdkAutoCloseable {

    CompletableFuture<ResponseT> sendRequest(RequestT request);

    void close();

    /**
     * Creates a newly initialized BatchManager builder object.
     *
     * @param <RequestT> the type of an outgoing request.
     * @param <ResponseT> the type of an outgoing response.
     * @param <BatchResponseT> the type of an outgoing batch response.
     */
    static <RequestT, ResponseT, BatchResponseT> DefaultBatchManager.Builder<RequestT, ResponseT, BatchResponseT> builder(
        Class<? extends RequestT> requestClass, Class<? extends ResponseT> responseClass,
        Class<? extends  BatchResponseT> batchResponseClass) {
        return DefaultBatchManager.builder();
    }

    /**
     * The BatchManager Builder
     * @param <RequestT> the type of an outgoing request.
     * @param <ResponseT> the type of an outgoing response.
     * @param <BatchResponseT> the type of an outgoing batch response.
     */
    interface Builder<RequestT, ResponseT, BatchResponseT> extends BatchManagerBuilder<RequestT, ResponseT, BatchResponseT,
        BatchManager.Builder<RequestT, ResponseT, BatchResponseT>> {

        /**
         * An immutable object that is created from the properties that have been set on the builder.
         * @return a reference to this object so that method calls can be chained together.
         */
        BatchManager<RequestT, ResponseT, BatchResponseT> build();
    }
}
