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
 * The available AWS access control policy actions for JsonProtocolTests.
 */

public enum RestjsonActions implements Action {

    /** Represents any action executed on JsonProtocolTests. */
    AllRestjsonActions("restjson:*"),

    /** Action for the AllTypes operation. */
    AllTypes("restjson:AllTypes"),
    /** Action for the DeleteOperation operation. */
    DeleteOperation("restjson:DeleteOperation"),
    /** Action for the FurtherNestedContainers operation. */
    FurtherNestedContainers("restjson:FurtherNestedContainers"),
    /** Action for the GetOperationWithBody operation. */
    GetOperationWithBody("restjson:GetOperationWithBody"),
    /** Action for the HeadOperation operation. */
    HeadOperation("restjson:HeadOperation"),
    /** Action for the IdempotentOperation operation. */
    IdempotentOperation("restjson:IdempotentOperation"),
    /** Action for the JsonValuesOperation operation. */
    JsonValuesOperation("restjson:JsonValuesOperation"),
    /** Action for the MapOfStringToListOfStringInQueryParams operation. */
    MapOfStringToListOfStringInQueryParams("restjson:MapOfStringToListOfStringInQueryParams"),
    /** Action for the MembersInHeaders operation. */
    MembersInHeaders("restjson:MembersInHeaders"),
    /** Action for the MembersInQueryParams operation. */
    MembersInQueryParams("restjson:MembersInQueryParams"),
    /** Action for the MultiLocationOperation operation. */
    MultiLocationOperation("restjson:MultiLocationOperation"),
    /** Action for the NestedContainers operation. */
    NestedContainers("restjson:NestedContainers"),
    /** Action for the OperationWithExplicitPayloadBlob operation. */
    OperationWithExplicitPayloadBlob("restjson:OperationWithExplicitPayloadBlob"),
    /** Action for the OperationWithExplicitPayloadStructure operation. */
    OperationWithExplicitPayloadStructure("restjson:OperationWithExplicitPayloadStructure"),
    /** Action for the OperationWithGreedyLabel operation. */
    OperationWithGreedyLabel("restjson:OperationWithGreedyLabel"),
    /** Action for the OperationWithModeledContentType operation. */
    OperationWithModeledContentType("restjson:OperationWithModeledContentType"),
    /** Action for the OperationWithNoInputOrOutput operation. */
    OperationWithNoInputOrOutput("restjson:OperationWithNoInputOrOutput"),
    /** Action for the QueryParamWithoutValue operation. */
    QueryParamWithoutValue("restjson:QueryParamWithoutValue"),
    /** Action for the StreamingInputOperation operation. */
    StreamingInputOperation("restjson:StreamingInputOperation"),
    /** Action for the StreamingOutputOperation operation. */
    StreamingOutputOperation("restjson:StreamingOutputOperation"),

    ;

    private final String action;

    private RestjsonActions(String action) {
        this.action = action;
    }

    public String getActionName() {
        return this.action;
    }

    public boolean isNotType() {
        return false;
    }
}
