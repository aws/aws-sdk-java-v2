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
 * A standard error for input validation failures. This should be thrown by services when a member of the input
 * structure falls outside of the modeled or documented constraints.
 * </p>
 */

public class ValidationException extends RpcV2ProtocolException {
    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * A list of specific failures encountered while validating the input. A member can appear in this list more than
     * once if it failed to satisfy multiple constraints.
     * </p>
     */
    private java.util.List<ValidationExceptionField> fieldList;

    /**
     * Constructs a new ValidationException with the specified error message.
     *
     * @param message
     *        Describes the error encountered.
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * <p>
     * A list of specific failures encountered while validating the input. A member can appear in this list more than
     * once if it failed to satisfy multiple constraints.
     * </p>
     * 
     * @return A list of specific failures encountered while validating the input. A member can appear in this list more
     *         than once if it failed to satisfy multiple constraints.
     */

    @com.fasterxml.jackson.annotation.JsonProperty("fieldList")
    public java.util.List<ValidationExceptionField> getFieldList() {
        return fieldList;
    }

    /**
     * <p>
     * A list of specific failures encountered while validating the input. A member can appear in this list more than
     * once if it failed to satisfy multiple constraints.
     * </p>
     * 
     * @param fieldList
     *        A list of specific failures encountered while validating the input. A member can appear in this list more
     *        than once if it failed to satisfy multiple constraints.
     */

    @com.fasterxml.jackson.annotation.JsonProperty("fieldList")
    public void setFieldList(java.util.Collection<ValidationExceptionField> fieldList) {
        if (fieldList == null) {
            this.fieldList = null;
            return;
        }

        this.fieldList = new java.util.ArrayList<ValidationExceptionField>(fieldList);
    }

    /**
     * <p>
     * A list of specific failures encountered while validating the input. A member can appear in this list more than
     * once if it failed to satisfy multiple constraints.
     * </p>
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setFieldList(java.util.Collection)} or {@link #withFieldList(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param fieldList
     *        A list of specific failures encountered while validating the input. A member can appear in this list more
     *        than once if it failed to satisfy multiple constraints.
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ValidationException withFieldList(ValidationExceptionField... fieldList) {
        if (this.fieldList == null) {
            setFieldList(new java.util.ArrayList<ValidationExceptionField>(fieldList.length));
        }
        for (ValidationExceptionField ele : fieldList) {
            this.fieldList.add(ele);
        }
        return this;
    }

    /**
     * <p>
     * A list of specific failures encountered while validating the input. A member can appear in this list more than
     * once if it failed to satisfy multiple constraints.
     * </p>
     * 
     * @param fieldList
     *        A list of specific failures encountered while validating the input. A member can appear in this list more
     *        than once if it failed to satisfy multiple constraints.
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ValidationException withFieldList(java.util.Collection<ValidationExceptionField> fieldList) {
        setFieldList(fieldList);
        return this;
    }

}
