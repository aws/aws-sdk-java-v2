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
 * The available AWS access control policy actions for RestXmlProtocolTests.
 */

public enum RestxmlActions implements Action {

    /** Represents any action executed on RestXmlProtocolTests. */
    AllRestxmlActions("restxml:*"),

    /** Action for the AllTypes operation. */
    AllTypes("restxml:AllTypes"),
    /** Action for the DeleteOperation operation. */
    DeleteOperation("restxml:DeleteOperation"),
    /** Action for the IdempotentOperation operation. */
    IdempotentOperation("restxml:IdempotentOperation"),
    /** Action for the MapOfStringToListOfStringInQueryParams operation. */
    MapOfStringToListOfStringInQueryParams("restxml:MapOfStringToListOfStringInQueryParams"),
    /** Action for the MembersInHeaders operation. */
    MembersInHeaders("restxml:MembersInHeaders"),
    /** Action for the MembersInQueryParams operation. */
    MembersInQueryParams("restxml:MembersInQueryParams"),
    /** Action for the MultiLocationOperation operation. */
    MultiLocationOperation("restxml:MultiLocationOperation"),
    /** Action for the OperationWithExplicitPayloadBlob operation. */
    OperationWithExplicitPayloadBlob("restxml:OperationWithExplicitPayloadBlob"),
    /** Action for the OperationWithGreedyLabel operation. */
    OperationWithGreedyLabel("restxml:OperationWithGreedyLabel"),
    /** Action for the OperationWithModeledContentType operation. */
    OperationWithModeledContentType("restxml:OperationWithModeledContentType"),
    /** Action for the QueryParamWithoutValue operation. */
    QueryParamWithoutValue("restxml:QueryParamWithoutValue"),
    /** Action for the RestXmlTypes operation. */
    RestXmlTypes("restxml:RestXmlTypes"),
    /** Action for the StreamingOutputOperation operation. */
    StreamingOutputOperation("restxml:StreamingOutputOperation"),

    ;

    private final String action;

    private RestxmlActions(String action) {
        this.action = action;
    }

    public String getActionName() {
        return this.action;
    }

    public boolean isNotType() {
        return false;
    }
}
