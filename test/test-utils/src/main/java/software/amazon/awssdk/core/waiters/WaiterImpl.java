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

package software.amazon.awssdk.core.waiters;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public class WaiterImpl<InputT, OutputT, ErrorT extends RuntimeException> implements Waiter<InputT> {

    /**
     * Represents the operation function
     */
    private final SdkFunction<InputT, OutputT> sdkFunction;

    /**
     * List of acceptors
     */
    private final List<WaiterAcceptor<OutputT, ErrorT>> acceptors;

    /**
     * Represents the default polling strategy
     */
    private final PollingStrategy defaultPollingStrategy;

    private final ExecutorService executorService;

    /**
     * Constructs a new waiter with the given internal parameters
     *
     * @param waiterBuilder Takes in default parameters and builds a
     *                      basic waiter. Excludes request and custom
     *                      polling strategy parameters.
     */
    @SdkProtectedApi
    public WaiterImpl(WaiterBuilder<InputT, OutputT, ErrorT> waiterBuilder) {
        this.sdkFunction = Validate.paramNotNull(waiterBuilder.getSdkFunction(), "sdkFunction");
        this.acceptors = Validate.paramNotNull(waiterBuilder.getAcceptor(), "acceptors");
        this.defaultPollingStrategy = Validate.paramNotNull(waiterBuilder.getDefaultPollingStrategy(), "defaultPollingStrategy");
        this.executorService = Validate.paramNotNull(waiterBuilder.getExecutorService(), "executorService");
    }

    /**
     * Polls synchronously until it is determined that the resource
     * transitioned into the desired state or not.
     *
     * @param waiterParameters Custom provided parameters. Includes request and
     *                         optional custom polling strategy
     * @throws AmazonServiceException       If the service exception thrown doesn't match any of the expected
     *                                      exceptions, it's re-thrown.
     * @throws WaiterUnrecoverableException If the resource transitions into a failure/unexpected state.
     * @throws WaiterTimedOutException      If the resource doesn't transition into the desired state
     *                                      even after a certain number of retries.
     */
    public void run(WaiterParameters<InputT> waiterParameters)
            throws WaiterTimedOutException, WaiterUnrecoverableException {

        Validate.paramNotNull(waiterParameters, "waiterParameters");
        @SuppressWarnings("unchecked")
        InputT request = Validate.paramNotNull(waiterParameters.getRequest(), "request");
        WaiterExecution<InputT, OutputT, ErrorT> waiterExecution = new WaiterExecutionBuilder<InputT, OutputT, ErrorT>()
                .withRequest(request)
                .withPollingStrategy(waiterParameters.getPollingStrategy() != null ? waiterParameters.getPollingStrategy()
                                                                                   : defaultPollingStrategy)
                .withAcceptors(acceptors)
                .withSdkFunction(sdkFunction)
                .build();

        waiterExecution.pollResource();

    }

    /**
     * Polls asynchronously until it is determined that the resource
     * transitioned into the desired state or not. Includes additional
     * callback.
     *
     * @param waiterParameters Custom provided parameters. Includes request and
     *                         optional custom polling strategy
     * @param callback         Custom callback
     * @return Future object that holds the result of an asynchronous
     *     computation of waiter
     */
    public Future<Void> runAsync(final WaiterParameters<InputT> waiterParameters, final WaiterHandler callback)
            throws WaiterTimedOutException, WaiterUnrecoverableException {

        return executorService.submit(() -> {
            try {
                run(waiterParameters);
                callback.onWaitSuccess(waiterParameters.getRequest());
            } catch (Exception ex) {
                callback.onWaitFailure(ex);

                throw ex;
            }
            return null;
        });

    }
}
