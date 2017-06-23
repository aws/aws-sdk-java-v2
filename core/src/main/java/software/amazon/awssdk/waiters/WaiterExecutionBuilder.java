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
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.annotation.SdkProtectedApi;

@SdkProtectedApi
public class WaiterExecutionBuilder<InputT extends AmazonWebServiceRequest, OutputT> {

    private SdkFunction<InputT, OutputT> sdkFunction;

    private InputT request;

    private PollingStrategy pollingStrategy;

    private List<WaiterAcceptor<OutputT>> acceptors = new ArrayList<WaiterAcceptor<OutputT>>();

    public WaiterExecutionBuilder<InputT, OutputT> withSdkFunction(SdkFunction sdkFunction) {
        this.sdkFunction = sdkFunction;
        return this;
    }

    public WaiterExecutionBuilder<InputT, OutputT> withRequest(InputT request) {
        this.request = request;
        return this;
    }


    public WaiterExecutionBuilder<InputT, OutputT> withPollingStrategy(PollingStrategy pollingStrategy) {
        this.pollingStrategy = pollingStrategy;
        return this;
    }

    public WaiterExecutionBuilder<InputT, OutputT> withAcceptors(List<WaiterAcceptor<OutputT>> acceptors) {
        this.acceptors = acceptors;
        return this;
    }

    public InputT getRequest() {
        return this.request;
    }

    public List<WaiterAcceptor<OutputT>> getAcceptorsList() {
        return this.acceptors;
    }

    public SdkFunction<InputT, OutputT> getSdkFunction() {
        return this.sdkFunction;
    }

    public PollingStrategy getPollingStrategy() {
        return this.pollingStrategy;
    }

    public WaiterExecution<InputT, OutputT> build() {
        return new WaiterExecution<InputT, OutputT>(this);
    }

}
