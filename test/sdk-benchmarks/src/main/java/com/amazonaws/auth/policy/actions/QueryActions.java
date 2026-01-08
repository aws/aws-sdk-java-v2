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
 * The available AWS access control policy actions for QueryProtocolTests.
 */

public enum QueryActions implements Action {

    /** Represents any action executed on QueryProtocolTests. */
    AllQueryActions("query:*"),

    /** Action for the AllTypes operation. */
    AllTypes("query:AllTypes"),
    /** Action for the IdempotentOperation operation. */
    IdempotentOperation("query:IdempotentOperation"),
    /** Action for the QueryTypes operation. */
    QueryTypes("query:QueryTypes"),

    ;

    private final String action;

    private QueryActions(String action) {
        this.action = action;
    }

    public String getActionName() {
        return this.action;
    }

    public boolean isNotType() {
        return false;
    }
}
