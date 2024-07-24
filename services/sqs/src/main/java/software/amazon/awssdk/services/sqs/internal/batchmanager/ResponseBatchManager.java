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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchAndSend;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchKeyMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchResponseMapper;


// TODO:  This class will be implemented in future PR , for now just added it to show how we have separate Batch managers for
// Request And Response
@SdkInternalApi
public final class ResponseBatchManager<RequestT, ResponseT, BatchResponseT>
    implements BatchManager<RequestT, ResponseT, BatchResponseT> {


    private ResponseBatchManager(DefaultBuilder<RequestT, ResponseT, BatchResponseT> builder) {
        // TODO:  Add implementation logic for sending a request in separate PR
    }

    public static <ResponseT, BatchResponseT, RequestT> Builder<RequestT, ResponseT, BatchResponseT> builder() {
        return new DefaultBuilder<>();

    }


    @Override
    public CompletableFuture<ResponseT> batchRequest(RequestT request) {
        // TODO:  Add implementation logic for sending a request in separate PR
        return null;
    }

    @Override
    public void close() {
        // TODO:  Add implementation logic for sending a request in separate PR
    }

    public static final class DefaultBuilder<RequestT, ResponseT, BatchResponseT> implements Builder<RequestT, ResponseT,
        BatchResponseT> {

        public ResponseBatchManager<RequestT, ResponseT,
            BatchResponseT> build() {
            return new ResponseBatchManager<>(this);
        }

        @Override
        public Builder<RequestT, ResponseT,
            BatchResponseT> overrideConfiguration(BatchOverrideConfiguration overrideConfiguration) {
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT,
            BatchResponseT> batchFunction(BatchAndSend<RequestT, BatchResponseT> batchFunction) {
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT,
            BatchResponseT> responseMapper(BatchResponseMapper<BatchResponseT, ResponseT> responseMapper) {
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> batchKeyMapper(BatchKeyMapper<RequestT> batchKeyMapper) {
            return this;
        }
    }
}
