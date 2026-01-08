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

import com.amazonaws.protocol.StructuredPojo;
import com.amazonaws.protocol.ProtocolMarshaller;

/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/GreetingStruct" target="_top">AWS API
 *      Documentation</a>
 */

public class GreetingStruct implements Serializable, Cloneable, StructuredPojo {

    private String hi;

    /**
     * @param hi
     */

    public void setHi(String hi) {
        this.hi = hi;
    }

    /**
     * @return
     */

    public String getHi() {
        return this.hi;
    }

    /**
     * @param hi
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public GreetingStruct withHi(String hi) {
        setHi(hi);
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
        if (getHi() != null)
            sb.append("Hi: ").append(getHi());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof GreetingStruct == false)
            return false;
        GreetingStruct other = (GreetingStruct) obj;
        if (other.getHi() == null ^ this.getHi() == null)
            return false;
        if (other.getHi() != null && other.getHi().equals(this.getHi()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getHi() == null) ? 0 : getHi().hashCode());
        return hashCode;
    }

    @Override
    public GreetingStruct clone() {
        try {
            return (GreetingStruct) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.rpcv2protocol.model.transform.GreetingStructMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
