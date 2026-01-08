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
package com.amazonaws.services.protocol.restjson.model;



/**
 * 
 */

public class ModeledException extends AmazonProtocolRestJsonException {
    private static final long serialVersionUID = 1L;

    private CapitalizedMembersStructure capitalizedMembers;

    private AllTypesStructure allTypes;

    /**
     * Constructs a new ModeledException with the specified error message.
     *
     * @param message
     *        Describes the error encountered.
     */
    public ModeledException(String message) {
        super(message);
    }

    /**
     * @param capitalizedMembers
     */

    @com.fasterxml.jackson.annotation.JsonProperty("CapitalizedMembers")
    public void setCapitalizedMembers(CapitalizedMembersStructure capitalizedMembers) {
        this.capitalizedMembers = capitalizedMembers;
    }

    /**
     * @return
     */

    @com.fasterxml.jackson.annotation.JsonProperty("CapitalizedMembers")
    public CapitalizedMembersStructure getCapitalizedMembers() {
        return this.capitalizedMembers;
    }

    /**
     * @param capitalizedMembers
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ModeledException withCapitalizedMembers(CapitalizedMembersStructure capitalizedMembers) {
        setCapitalizedMembers(capitalizedMembers);
        return this;
    }

    /**
     * @param allTypes
     */

    @com.fasterxml.jackson.annotation.JsonProperty("AllTypes")
    public void setAllTypes(AllTypesStructure allTypes) {
        this.allTypes = allTypes;
    }

    /**
     * @return
     */

    @com.fasterxml.jackson.annotation.JsonProperty("AllTypes")
    public AllTypesStructure getAllTypes() {
        return this.allTypes;
    }

    /**
     * @param allTypes
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ModeledException withAllTypes(AllTypesStructure allTypes) {
        setAllTypes(allTypes);
        return this;
    }

}
