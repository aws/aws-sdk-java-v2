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
package com.amazonaws.services.protocol.rpcv2protocol.model;



/**
 * <p>
 * This error is thrown when a request is invalid.
 * </p>
 */

public class ComplexErrorException extends RpcV2ProtocolException {
    private static final long serialVersionUID = 1L;

    private String topLevel;

    private ComplexNestedErrorData nested;

    /**
     * Constructs a new ComplexErrorException with the specified error message.
     *
     * @param message
     *        Describes the error encountered.
     */
    public ComplexErrorException(String message) {
        super(message);
    }

    /**
     * @param topLevel
     */

    @com.fasterxml.jackson.annotation.JsonProperty("TopLevel")
    public void setTopLevel(String topLevel) {
        this.topLevel = topLevel;
    }

    /**
     * @return
     */

    @com.fasterxml.jackson.annotation.JsonProperty("TopLevel")
    public String getTopLevel() {
        return this.topLevel;
    }

    /**
     * @param topLevel
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ComplexErrorException withTopLevel(String topLevel) {
        setTopLevel(topLevel);
        return this;
    }

    /**
     * @param nested
     */

    @com.fasterxml.jackson.annotation.JsonProperty("Nested")
    public void setNested(ComplexNestedErrorData nested) {
        this.nested = nested;
    }

    /**
     * @return
     */

    @com.fasterxml.jackson.annotation.JsonProperty("Nested")
    public ComplexNestedErrorData getNested() {
        return this.nested;
    }

    /**
     * @param nested
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ComplexErrorException withNested(ComplexNestedErrorData nested) {
        setNested(nested);
        return this;
    }

}
