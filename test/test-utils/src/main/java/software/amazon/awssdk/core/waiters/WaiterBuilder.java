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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class WaiterBuilder<InputT, OutputT, ErrorT extends RuntimeException> {

    private SdkFunction<InputT, OutputT> sdkFunction;

    private List<WaiterAcceptor<OutputT, ErrorT>> acceptors = new ArrayList<>();

    private PollingStrategy defaultPollingStrategy;

    private ExecutorService executorService;

    public WaiterBuilder<InputT, OutputT, ErrorT> withSdkFunction(SdkFunction<InputT, OutputT> sdkFunction) {
        this.sdkFunction = sdkFunction;
        return this;
    }

    public WaiterBuilder<InputT, OutputT, ErrorT> withAcceptors(WaiterAcceptor<OutputT, ErrorT>... acceptors) {
        Collections.addAll(this.acceptors, acceptors);
        return this;
    }

    public WaiterBuilder<InputT, OutputT, ErrorT> withDefaultPollingStrategy(PollingStrategy pollingStrategy) {
        this.defaultPollingStrategy = pollingStrategy;
        return this;
    }

    public WaiterBuilder<InputT, OutputT, ErrorT> withExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public List<WaiterAcceptor<OutputT, ErrorT>> getAcceptor() {
        return this.acceptors;
    }

    public SdkFunction<InputT, OutputT> getSdkFunction() {
        return this.sdkFunction;
    }

    PollingStrategy getDefaultPollingStrategy() {
        return this.defaultPollingStrategy;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public Waiter<InputT> build() {
        return new WaiterImpl<>(this);
    }

}
