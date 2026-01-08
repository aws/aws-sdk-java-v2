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
 * The available AWS access control policy actions for EC2ProtocolTests.
 */

public enum Ec2Actions implements Action {

    /** Represents any action executed on EC2ProtocolTests. */
    AllEc2Actions("ec2:*"),

    /** Action for the AllTypes operation. */
    AllTypes("ec2:AllTypes"),
    /** Action for the Ec2Types operation. */
    Ec2Types("ec2:Ec2Types"),
    /** Action for the IdempotentOperation operation. */
    IdempotentOperation("ec2:IdempotentOperation"),

    ;

    private final String action;

    private Ec2Actions(String action) {
        this.action = action;
    }

    public String getActionName() {
        return this.action;
    }

    public boolean isNotType() {
        return false;
    }
}
