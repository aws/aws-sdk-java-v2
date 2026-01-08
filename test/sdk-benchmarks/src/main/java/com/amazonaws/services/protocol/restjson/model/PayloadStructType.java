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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/PayloadStructType" target="_top">AWS API
 *      Documentation</a>
 */

public class PayloadStructType implements Serializable, Cloneable, StructuredPojo {

    private String payloadMemberOne;

    private String payloadMemberTwo;

    /**
     * @param payloadMemberOne
     */

    public void setPayloadMemberOne(String payloadMemberOne) {
        this.payloadMemberOne = payloadMemberOne;
    }

    /**
     * @return
     */

    public String getPayloadMemberOne() {
        return this.payloadMemberOne;
    }

    /**
     * @param payloadMemberOne
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PayloadStructType withPayloadMemberOne(String payloadMemberOne) {
        setPayloadMemberOne(payloadMemberOne);
        return this;
    }

    /**
     * @param payloadMemberTwo
     */

    public void setPayloadMemberTwo(String payloadMemberTwo) {
        this.payloadMemberTwo = payloadMemberTwo;
    }

    /**
     * @return
     */

    public String getPayloadMemberTwo() {
        return this.payloadMemberTwo;
    }

    /**
     * @param payloadMemberTwo
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PayloadStructType withPayloadMemberTwo(String payloadMemberTwo) {
        setPayloadMemberTwo(payloadMemberTwo);
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
        if (getPayloadMemberOne() != null)
            sb.append("PayloadMemberOne: ").append(getPayloadMemberOne()).append(",");
        if (getPayloadMemberTwo() != null)
            sb.append("PayloadMemberTwo: ").append(getPayloadMemberTwo());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof PayloadStructType == false)
            return false;
        PayloadStructType other = (PayloadStructType) obj;
        if (other.getPayloadMemberOne() == null ^ this.getPayloadMemberOne() == null)
            return false;
        if (other.getPayloadMemberOne() != null && other.getPayloadMemberOne().equals(this.getPayloadMemberOne()) == false)
            return false;
        if (other.getPayloadMemberTwo() == null ^ this.getPayloadMemberTwo() == null)
            return false;
        if (other.getPayloadMemberTwo() != null && other.getPayloadMemberTwo().equals(this.getPayloadMemberTwo()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getPayloadMemberOne() == null) ? 0 : getPayloadMemberOne().hashCode());
        hashCode = prime * hashCode + ((getPayloadMemberTwo() == null) ? 0 : getPayloadMemberTwo().hashCode());
        return hashCode;
    }

    @Override
    public PayloadStructType clone() {
        try {
            return (PayloadStructType) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        com.amazonaws.services.protocol.restjson.model.transform.PayloadStructTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
