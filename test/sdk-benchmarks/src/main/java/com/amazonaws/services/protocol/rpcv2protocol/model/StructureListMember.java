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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/StructureListMember" target="_top">AWS
 *      API Documentation</a>
 */

public class StructureListMember implements Serializable, Cloneable, StructuredPojo {

    private String a;

    private String b;

    /**
     * @param a
     */

    public void setA(String a) {
        this.a = a;
    }

    /**
     * @return
     */

    public String getA() {
        return this.a;
    }

    /**
     * @param a
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public StructureListMember withA(String a) {
        setA(a);
        return this;
    }

    /**
     * @param b
     */

    public void setB(String b) {
        this.b = b;
    }

    /**
     * @return
     */

    public String getB() {
        return this.b;
    }

    /**
     * @param b
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public StructureListMember withB(String b) {
        setB(b);
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
        if (getA() != null)
            sb.append("A: ").append(getA()).append(",");
        if (getB() != null)
            sb.append("B: ").append(getB());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof StructureListMember == false)
            return false;
        StructureListMember other = (StructureListMember) obj;
        if (other.getA() == null ^ this.getA() == null)
            return false;
        if (other.getA() != null && other.getA().equals(this.getA()) == false)
            return false;
        if (other.getB() == null ^ this.getB() == null)
            return false;
        if (other.getB() != null && other.getB().equals(this.getB()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getA() == null) ? 0 : getA().hashCode());
        hashCode = prime * hashCode + ((getB() == null) ? 0 : getB().hashCode());
        return hashCode;
    }

    @Override
    public StructureListMember clone() {
        try {
            return (StructureListMember) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.rpcv2protocol.model.transform.StructureListMemberMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
