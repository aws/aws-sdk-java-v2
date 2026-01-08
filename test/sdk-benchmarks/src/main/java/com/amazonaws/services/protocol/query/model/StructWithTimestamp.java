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
package com.amazonaws.services.protocol.query.model;

import java.io.Serializable;


/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/StructWithTimestamp" target="_top">AWS API
 *      Documentation</a>
 */

public class StructWithTimestamp implements Serializable, Cloneable {

    private java.util.Date nestedTimestamp;

    /**
     * @param nestedTimestamp
     */

    public void setNestedTimestamp(java.util.Date nestedTimestamp) {
        this.nestedTimestamp = nestedTimestamp;
    }

    /**
     * @return
     */

    public java.util.Date getNestedTimestamp() {
        return this.nestedTimestamp;
    }

    /**
     * @param nestedTimestamp
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public StructWithTimestamp withNestedTimestamp(java.util.Date nestedTimestamp) {
        setNestedTimestamp(nestedTimestamp);
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
        if (getNestedTimestamp() != null)
            sb.append("NestedTimestamp: ").append(getNestedTimestamp());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof StructWithTimestamp == false)
            return false;
        StructWithTimestamp other = (StructWithTimestamp) obj;
        if (other.getNestedTimestamp() == null ^ this.getNestedTimestamp() == null)
            return false;
        if (other.getNestedTimestamp() != null && other.getNestedTimestamp().equals(this.getNestedTimestamp()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getNestedTimestamp() == null) ? 0 : getNestedTimestamp().hashCode());
        return hashCode;
    }

    @Override
    public StructWithTimestamp clone() {
        try {
            return (StructWithTimestamp) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
