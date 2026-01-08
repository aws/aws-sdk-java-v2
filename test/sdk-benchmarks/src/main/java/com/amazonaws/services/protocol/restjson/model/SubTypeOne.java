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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/SubTypeOne" target="_top">AWS API
 *      Documentation</a>
 */

public class SubTypeOne implements Serializable, Cloneable, StructuredPojo {

    private String subTypeOneMember;

    /**
     * @param subTypeOneMember
     */

    public void setSubTypeOneMember(String subTypeOneMember) {
        this.subTypeOneMember = subTypeOneMember;
    }

    /**
     * @return
     */

    public String getSubTypeOneMember() {
        return this.subTypeOneMember;
    }

    /**
     * @param subTypeOneMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SubTypeOne withSubTypeOneMember(String subTypeOneMember) {
        setSubTypeOneMember(subTypeOneMember);
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
        if (getSubTypeOneMember() != null)
            sb.append("SubTypeOneMember: ").append(getSubTypeOneMember());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof SubTypeOne == false)
            return false;
        SubTypeOne other = (SubTypeOne) obj;
        if (other.getSubTypeOneMember() == null ^ this.getSubTypeOneMember() == null)
            return false;
        if (other.getSubTypeOneMember() != null && other.getSubTypeOneMember().equals(this.getSubTypeOneMember()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getSubTypeOneMember() == null) ? 0 : getSubTypeOneMember().hashCode());
        return hashCode;
    }

    @Override
    public SubTypeOne clone() {
        try {
            return (SubTypeOne) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.restjson.model.transform.SubTypeOneMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
