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

package software.amazon.awssdk.services.sfn.builder;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.adapter.TypeAdapter;
import software.amazon.awssdk.services.sfn.model.CreateStateMachineRequest;

/**
 * Adapter from a StateMachine object to the JSON string. For the convenience overload in {@link CreateStateMachineRequest}.
 *
 */
@SdkInternalApi
public final class StateMachineAdapter implements TypeAdapter<StateMachine, String> {

    private static final StateMachineAdapter INSTANCE = new StateMachineAdapter();

    /**
     * @return The singleton instance of {@link StateMachineAdapter}.
     */
    public static StateMachineAdapter instance() {
        return INSTANCE;
    }

    @Override
    public String adapt(StateMachine stateMachine) {
        return stateMachine == null ? null : stateMachine.toJson();
    }
}
