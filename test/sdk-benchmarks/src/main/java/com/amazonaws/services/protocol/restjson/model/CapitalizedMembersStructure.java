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

import java.io.Serializable;

import com.amazonaws.protocol.StructuredPojo;
import com.amazonaws.protocol.ProtocolMarshaller;

/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/CapitalizedMembersStructure"
 *      target="_top">AWS API Documentation</a>
 */

public class CapitalizedMembersStructure implements Serializable, Cloneable, StructuredPojo {

    private com.amazonaws.internal.SdkInternalList<String> sS;

    /**
     * @return
     */

    public java.util.List<String> getSS() {
        if (sS == null) {
            sS = new com.amazonaws.internal.SdkInternalList<String>();
        }
        return sS;
    }

    /**
     * @param sS
     */

    public void setSS(java.util.Collection<String> sS) {
        if (sS == null) {
            this.sS = null;
            return;
        }

        this.sS = new com.amazonaws.internal.SdkInternalList<String>(sS);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setSS(java.util.Collection)} or {@link #withSS(java.util.Collection)} if you want to override the
     * existing values.
     * </p>
     * 
     * @param sS
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public CapitalizedMembersStructure withSS(String... sS) {
        if (this.sS == null) {
            setSS(new com.amazonaws.internal.SdkInternalList<String>(sS.length));
        }
        for (String ele : sS) {
            this.sS.add(ele);
        }
        return this;
    }

    /**
     * @param sS
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public CapitalizedMembersStructure withSS(java.util.Collection<String> sS) {
        setSS(sS);
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
        if (getSS() != null)
            sb.append("SS: ").append(getSS());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof CapitalizedMembersStructure == false)
            return false;
        CapitalizedMembersStructure other = (CapitalizedMembersStructure) obj;
        if (other.getSS() == null ^ this.getSS() == null)
            return false;
        if (other.getSS() != null && other.getSS().equals(this.getSS()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getSS() == null) ? 0 : getSS().hashCode());
        return hashCode;
    }

    @Override
    public CapitalizedMembersStructure clone() {
        try {
            return (CapitalizedMembersStructure) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.restjson.model.transform.CapitalizedMembersStructureMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
