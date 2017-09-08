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

package software.amazon.awssdk.waiters;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.util.ValidationUtils;

@SdkInternalApi
@ReviewBeforeRelease("Clean up remaining waiter code.")
class CompositeAcceptor<OutputT> {

    /**
     * List of acceptors defined for each waiter
     */
    private List<WaiterAcceptor<OutputT>> acceptors = new ArrayList<WaiterAcceptor<OutputT>>();

    /**
     * Constructs a new Composite Acceptor with the given list of acceptors.
     * Throws an assertion exception if the acceptor list is empty or null
     *
     * @param acceptors List of acceptors defined for each waiter. It shouldn't
     *                  be null or empty
     */
    CompositeAcceptor(List<WaiterAcceptor<OutputT>> acceptors) {
        this.acceptors = ValidationUtils.assertNotEmpty(acceptors, "acceptors");
    }

    /**
     * @return List of acceptors defined for each waiter
     */
    public List<WaiterAcceptor<OutputT>> getAcceptors() {
        return this.acceptors;
    }

    /**
     * Compares the response against each response acceptor and returns
     * the state of the acceptor it matches on. If none is matched, returns
     * retry state by default
     *
     * @param response Response object got by executing the specified
     *                 waiter operation
     * @return (Enum) Corresponding waiter state defined by the acceptor or
     *     retry state if none matched
     */
    public WaiterState accepts(OutputT response) {
        for (WaiterAcceptor<OutputT> acceptor : acceptors) {
            if (acceptor.matches(response)) {
                return acceptor.getState();
            }
        }
        return WaiterState.RETRY;

    }

    /**
     * Compares the exception thrown against each exception acceptor and
     * returns the state of the acceptor it matches on. If none is
     * matched, it rethrows the exception to the caller
     *
     * @param exception Exception thrown by executing the specified
     *                  waiter operation
     * @return (Enum) Corresponding waiter state defined by the acceptor or
     *     rethrows the exception back to the caller if none matched
     */
    public WaiterState accepts(AmazonServiceException exception) throws AmazonServiceException {
        for (WaiterAcceptor<OutputT> acceptor : acceptors) {
            if (acceptor.matches(exception)) {
                return acceptor.getState();
            }
        }
        throw exception;
    }
}
