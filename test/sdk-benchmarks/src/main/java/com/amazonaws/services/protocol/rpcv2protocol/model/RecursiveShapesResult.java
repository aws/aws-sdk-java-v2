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

import java.io.Serializable;


/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RecursiveShapes" target="_top">AWS API
 *      Documentation</a>
 */

public class RecursiveShapesResult extends com.amazonaws.AmazonWebServiceResult<com.amazonaws.ResponseMetadata> implements Serializable, Cloneable {

    private RecursiveShapesInputOutputNested1 nested;

    /**
     * @param nested
     */

    public void setNested(RecursiveShapesInputOutputNested1 nested) {
        this.nested = nested;
    }

    /**
     * @return
     */

    public RecursiveShapesInputOutputNested1 getNested() {
        return this.nested;
    }

    /**
     * @param nested
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveShapesResult withNested(RecursiveShapesInputOutputNested1 nested) {
        setNested(nested);
        return this;
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     *
     * @return A string representation of this object.
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getNested() != null)
            sb.append("Nested: ").append(getNested());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof RecursiveShapesResult == false)
            return false;
        RecursiveShapesResult other = (RecursiveShapesResult) obj;
        if (other.getNested() == null ^ this.getNested() == null)
            return false;
        if (other.getNested() != null && other.getNested().equals(this.getNested()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getNested() == null) ? 0 : getNested().hashCode());
        return hashCode;
    }

    @Override
    public RecursiveShapesResult clone() {
        try {
            return (RecursiveShapesResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
