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

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchAndSend;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchConfiguration;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchKeyMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchManagerType;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchResponseMapper;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchingExecutionContext;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchingMap;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public abstract class AbstractBatchManager<RequestT, ResponseT, BatchResponseT> implements BatchManager<RequestT, ResponseT,
    BatchResponseT> {
    private final int maxBatchItems;
    private final Duration maxBatchOpenInMs;

    /**
     * A nested map that keeps track of customer requests and the corresponding responses to be completed. Requests are batched
     * together according to a batchKey that is calculated from the request by the service client.
     */
    private final BatchingMap<RequestT, ResponseT> requestsAndResponsesMaps;

    /**
     * Takes a list of identified requests in addition to a destination and batches the requests into a batch request. It then
     * sends the batch request and returns a CompletableFuture of the response.
     */
    private final BatchAndSend<RequestT, BatchResponseT> batchFunction;

    /**
     * Unpacks the batch response, then transforms individual entries to the appropriate response type. Each entry's batch ID is
     * mapped to the individual response entry.
     */
    private final BatchResponseMapper<BatchResponseT, ResponseT> responseMapper;

    /**
     * Takes a request and extracts a batchKey as determined by the caller.
     */
    private final BatchKeyMapper<RequestT> batchKeyMapper;

    /**
     * A scheduled executor that periodically schedules {@link #flushBuffer}.
     */
    private final ScheduledExecutorService scheduledExecutor;

    protected AbstractBatchManager(DefaultBuilder<RequestT, ResponseT, BatchResponseT> builder) {
        BatchConfiguration batchConfiguration = new BatchConfiguration(builder.overrideConfiguration);
        this.requestsAndResponsesMaps = new BatchingMap<>(batchConfiguration.maxBatchKeys(),
                                                          batchConfiguration.maxBufferSize());
        this.maxBatchItems = batchConfiguration.maxBatchItems();
        this.maxBatchOpenInMs = batchConfiguration.maxBatchOpenInMs();
        this.batchFunction = Validate.notNull(builder.batchFunction, "Null batchFunction");
        this.responseMapper = Validate.notNull(builder.responseMapper, "Null responseMapper");
        this.batchKeyMapper = Validate.notNull(builder.batchKeyMapper, "Null batchKeyMapper");
        this.scheduledExecutor = Validate.notNull(builder.scheduledExecutor, "Null scheduledExecutor");
    }

    public static <RequestT, ResponseT, BatchResponseT> Builder<RequestT, ResponseT, BatchResponseT> builder() {
        return new DefaultBuilder<>();
    }


    @Override
    public void close() {
        requestsAndResponsesMaps.forEach((batchKey, batchBuffer) -> {
            requestsAndResponsesMaps.cancelScheduledFlush(batchKey);
            Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests =
                requestsAndResponsesMaps.flushableRequests(batchKey, maxBatchItems);

            while (!flushableRequests.isEmpty()) {
                flushBuffer(batchKey, flushableRequests);
            }
        });
        requestsAndResponsesMaps.clear();
    }

    private void flushBuffer(String batchKey, Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests) {
    }

    public static final class DefaultBuilder<RequestT, ResponseT, BatchResponseT> implements Builder<RequestT, ResponseT,
        BatchResponseT> {

        private BatchOverrideConfiguration overrideConfiguration;
        private ScheduledExecutorService scheduledExecutor;
        private BatchAndSend<RequestT, BatchResponseT> batchFunction;
        private BatchResponseMapper<BatchResponseT, ResponseT> responseMapper;
        private BatchKeyMapper<RequestT> batchKeyMapper;
        private BatchManagerType batchManagerType;

        private DefaultBuilder() {
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> overrideConfiguration(BatchOverrideConfiguration
                                                                                              overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> batchFunction(BatchAndSend<RequestT, BatchResponseT>
                                                                              batchingFunction) {
            this.batchFunction = batchingFunction;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> responseMapper(
            BatchResponseMapper<BatchResponseT, ResponseT> responseMapper) {
            this.responseMapper = responseMapper;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> batchKeyMapper(BatchKeyMapper<RequestT> batchKeyMapper) {
            this.batchKeyMapper = batchKeyMapper;
            return this;
        }

        @Override
        public Builder<RequestT, ResponseT, BatchResponseT> batchManagerType(BatchManagerType batchManagerType) {
            this.batchManagerType = batchManagerType;
            return this;
        }

        public BatchManager<RequestT, ResponseT, BatchResponseT> build() {
            if (batchManagerType == BatchManagerType.RESPONSE) {
                return new ResponsesBatchManager<>(this);
            } else if (batchManagerType == BatchManagerType.REQUEST) {
                return new RequestsBatchManager<>(this);
            } else {
                throw new IllegalArgumentException("Type must be specified as either RESPONSE or REQUEST");
            }
        }

    }
}
