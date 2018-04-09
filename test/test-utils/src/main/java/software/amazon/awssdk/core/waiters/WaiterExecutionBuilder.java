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

package software.amazon.awssdk.core.waiters;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class WaiterExecutionBuilder<InputT, OutputT, ErrorT extends RuntimeException> {

    private SdkFunction<InputT, OutputT> sdkFunction;

    private InputT request;

    private PollingStrategy pollingStrategy;

    private List<WaiterAcceptor<OutputT, ErrorT>> acceptors = new ArrayList<>();

    public WaiterExecutionBuilder<InputT, OutputT, ErrorT> withSdkFunction(SdkFunction sdkFunction) {
        this.sdkFunction = sdkFunction;
        return this;
    }

    public WaiterExecutionBuilder<InputT, OutputT, ErrorT> withRequest(InputT request) {
        this.request = request;
        return this;
    }


    public WaiterExecutionBuilder<InputT, OutputT, ErrorT> withPollingStrategy(PollingStrategy pollingStrategy) {
        this.pollingStrategy = pollingStrategy;
        return this;
    }

    public WaiterExecutionBuilder<InputT, OutputT, ErrorT> withAcceptors(List<WaiterAcceptor<OutputT, ErrorT>> acceptors) {
        this.acceptors = acceptors;
        return this;
    }

    public InputT getRequest() {
        return this.request;
    }

    public List<WaiterAcceptor<OutputT, ErrorT>> getAcceptorsList() {
        return this.acceptors;
    }

    public SdkFunction<InputT, OutputT> getSdkFunction() {
        return this.sdkFunction;
    }

    public PollingStrategy getPollingStrategy() {
        return this.pollingStrategy;
    }

    public WaiterExecution<InputT, OutputT, ErrorT> build() {
        return new WaiterExecution<>(this);
    }

}
