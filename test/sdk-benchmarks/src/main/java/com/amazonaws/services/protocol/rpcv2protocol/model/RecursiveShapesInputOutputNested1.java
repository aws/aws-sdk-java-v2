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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RecursiveShapesInputOutputNested1"
 *      target="_top">AWS API Documentation</a>
 */

public class RecursiveShapesInputOutputNested1 implements Serializable, Cloneable, StructuredPojo {

    private String foo;

    private RecursiveShapesInputOutputNested2 nested;

    /**
     * @param foo
     */

    public void setFoo(String foo) {
        this.foo = foo;
    }

    /**
     * @return
     */

    public String getFoo() {
        return this.foo;
    }

    /**
     * @param foo
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveShapesInputOutputNested1 withFoo(String foo) {
        setFoo(foo);
        return this;
    }

    /**
     * @param nested
     */

    public void setNested(RecursiveShapesInputOutputNested2 nested) {
        this.nested = nested;
    }

    /**
     * @return
     */

    public RecursiveShapesInputOutputNested2 getNested() {
        return this.nested;
    }

    /**
     * @param nested
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public RecursiveShapesInputOutputNested1 withNested(RecursiveShapesInputOutputNested2 nested) {
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
        if (getFoo() != null)
            sb.append("Foo: ").append(getFoo()).append(",");
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

        if (obj instanceof RecursiveShapesInputOutputNested1 == false)
            return false;
        RecursiveShapesInputOutputNested1 other = (RecursiveShapesInputOutputNested1) obj;
        if (other.getFoo() == null ^ this.getFoo() == null)
            return false;
        if (other.getFoo() != null && other.getFoo().equals(this.getFoo()) == false)
            return false;
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

        hashCode = prime * hashCode + ((getFoo() == null) ? 0 : getFoo().hashCode());
        hashCode = prime * hashCode + ((getNested() == null) ? 0 : getNested().hashCode());
        return hashCode;
    }

    @Override
    public RecursiveShapesInputOutputNested1 clone() {
        try {
            return (RecursiveShapesInputOutputNested1) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.rpcv2protocol.model.transform.RecursiveShapesInputOutputNested1Marshaller.getInstance().marshall(this,
                protocolMarshaller);
    }
}
