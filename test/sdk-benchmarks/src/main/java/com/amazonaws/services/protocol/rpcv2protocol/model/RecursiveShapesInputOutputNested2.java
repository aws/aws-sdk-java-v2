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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RecursiveShapesInputOutputNested2"
 *      target="_top">AWS API Documentation</a>
 */

public class RecursiveShapesInputOutputNested2 implements Serializable, Cloneable, StructuredPojo {

    private String bar;

    private RecursiveShapesInputOutputNested1 recursiveMember;

    /**
     * @param bar
     */

    public void setBar(String bar) {
        this.bar = bar;
    }

    /**
     * @return
     */

    public String getBar() {
        return this.bar;
    }

    /**
     * @param bar
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveShapesInputOutputNested2 withBar(String bar) {
        setBar(bar);
        return this;
    }

    /**
     * @param recursiveMember
     */

    public void setRecursiveMember(RecursiveShapesInputOutputNested1 recursiveMember) {
        this.recursiveMember = recursiveMember;
    }

    /**
     * @return
     */

    public RecursiveShapesInputOutputNested1 getRecursiveMember() {
        return this.recursiveMember;
    }

    /**
     * @param recursiveMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveShapesInputOutputNested2 withRecursiveMember(RecursiveShapesInputOutputNested1 recursiveMember) {
        setRecursiveMember(recursiveMember);
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
        if (getBar() != null)
            sb.append("Bar: ").append(getBar()).append(",");
        if (getRecursiveMember() != null)
            sb.append("RecursiveMember: ").append(getRecursiveMember());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof RecursiveShapesInputOutputNested2 == false)
            return false;
        RecursiveShapesInputOutputNested2 other = (RecursiveShapesInputOutputNested2) obj;
        if (other.getBar() == null ^ this.getBar() == null)
            return false;
        if (other.getBar() != null && other.getBar().equals(this.getBar()) == false)
            return false;
        if (other.getRecursiveMember() == null ^ this.getRecursiveMember() == null)
            return false;
        if (other.getRecursiveMember() != null && other.getRecursiveMember().equals(this.getRecursiveMember()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getBar() == null) ? 0 : getBar().hashCode());
        hashCode = prime * hashCode + ((getRecursiveMember() == null) ? 0 : getRecursiveMember().hashCode());
        return hashCode;
    }

    @Override
    public RecursiveShapesInputOutputNested2 clone() {
        try {
            return (RecursiveShapesInputOutputNested2) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.rpcv2protocol.model.transform.RecursiveShapesInputOutputNested2Marshaller.getInstance().marshall(this,
                protocolMarshaller);
    }
}
