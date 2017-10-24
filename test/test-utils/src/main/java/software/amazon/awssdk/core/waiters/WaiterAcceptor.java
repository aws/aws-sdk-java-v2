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

import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public abstract class WaiterAcceptor<OutputT, ErrorT extends RuntimeException> {

    /**
     * Default method definition that matches the response
     * state with the expected state defined by the acceptor.
     * Overriden by each acceptor definition of matches.
     *
     * @param output Response got by the execution of the operation
     * @return False by default.
     *     When overriden, returns True if it matches, False
     *     otherwise
     */
    public boolean matches(OutputT output) {
        return false;
    }

    /**
     * Default method definition that matches the exception
     * with the expected state defined by the acceptor.
     * Overriden by each acceptor definition of matches.
     *
     * @param output Exception thrown by the execution of the operation
     * @return False by default.
     *     When overriden, returns True if it matches, False otherwise
     */
    public boolean matches(ErrorT output) {
        return false;
    }

    /**
     * Abstract method to fetch the corresponding state
     *
     * @return Corresponding state of the resource
     */
    public abstract WaiterState getState();
}


