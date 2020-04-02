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

package software.amazon.awssdk.core.waiters;

import java.util.concurrent.Future;

public interface Waiter<InputT> {

    /**
     * Polls synchronously until it is determined that the resource
     * transitioned into the desired state or not.
     *
     * @param waiterParameters Custom provided parameters. Includes request and
     *                         optional custom polling strategy
     * @throws WaiterUnrecoverableException If the resource transitions into a failure/unexpected state.
     * @throws WaiterTimedOutException      If the resource doesn't transition into the desired state
     *                                      even after a certain number of retries.
     */
    void run(WaiterParameters<InputT> waiterParameters)
            throws WaiterTimedOutException, WaiterUnrecoverableException;

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
    Future<Void> runAsync(WaiterParameters<InputT> waiterParameters, WaiterHandler callback)
            throws WaiterTimedOutException, WaiterUnrecoverableException;
}
