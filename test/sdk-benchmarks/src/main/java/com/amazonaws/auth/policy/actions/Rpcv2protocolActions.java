/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.auth.policy.actions;



import com.amazonaws.auth.policy.Action;

/**
 * The available AWS access control policy actions for RpcV2Protocol.
 */

public enum Rpcv2protocolActions implements Action {

    /** Represents any action executed on RpcV2Protocol. */
    AllRpcv2protocolActions("rpcv2protocol:*"),

    /** Action for the EmptyInputOutput operation. */
    EmptyInputOutput("rpcv2protocol:EmptyInputOutput"),
    /** Action for the Float16 operation. */
    Float16("rpcv2protocol:Float16"),
    /** Action for the FractionalSeconds operation. */
    FractionalSeconds("rpcv2protocol:FractionalSeconds"),
    /** Action for the GreetingWithErrors operation. */
    GreetingWithErrors("rpcv2protocol:GreetingWithErrors"),
    /** Action for the NoInputOutput operation. */
    NoInputOutput("rpcv2protocol:NoInputOutput"),
    /** Action for the OptionalInputOutput operation. */
    OptionalInputOutput("rpcv2protocol:OptionalInputOutput"),
    /** Action for the RecursiveShapes operation. */
    RecursiveShapes("rpcv2protocol:RecursiveShapes"),
    /** Action for the RpcV2CborDenseMaps operation. */
    RpcV2CborDenseMaps("rpcv2protocol:RpcV2CborDenseMaps"),
    /** Action for the RpcV2CborLists operation. */
    RpcV2CborLists("rpcv2protocol:RpcV2CborLists"),
    /** Action for the SimpleScalarProperties operation. */
    SimpleScalarProperties("rpcv2protocol:SimpleScalarProperties"),

    ;

    private final String action;

    private Rpcv2protocolActions(String action) {
        this.action = action;
    }

    public String getActionName() {
        return this.action;
    }

    public boolean isNotType() {
        return false;
    }
}
