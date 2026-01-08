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
package com.amazonaws.services.protocol.restxml.model;

import java.io.Serializable;


/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MembersInHeaders" target="_top">AWS API
 *      Documentation</a>
 */

public class MembersInHeadersResult extends com.amazonaws.AmazonWebServiceResult<com.amazonaws.ResponseMetadata> implements Serializable, Cloneable {

    private String stringMember;

    private Boolean booleanMember;

    private Integer integerMember;

    private Long longMember;

    private Float floatMember;

    private Double doubleMember;

    private java.util.Date timestampMember;

    /**
     * @param stringMember
     */

    public void setStringMember(String stringMember) {
        this.stringMember = stringMember;
    }

    /**
     * @return
     */

    public String getStringMember() {
        return this.stringMember;
    }

    /**
     * @param stringMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInHeadersResult withStringMember(String stringMember) {
        setStringMember(stringMember);
        return this;
    }

    /**
     * @param booleanMember
     */

    public void setBooleanMember(Boolean booleanMember) {
        this.booleanMember = booleanMember;
    }

    /**
     * @return
     */

    public Boolean getBooleanMember() {
        return this.booleanMember;
    }

    /**
     * @param booleanMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInHeadersResult withBooleanMember(Boolean booleanMember) {
        setBooleanMember(booleanMember);
        return this;
    }

    /**
     * @return
     */

    public Boolean isBooleanMember() {
        return this.booleanMember;
    }

    /**
     * @param integerMember
     */

    public void setIntegerMember(Integer integerMember) {
        this.integerMember = integerMember;
    }

    /**
     * @return
     */

    public Integer getIntegerMember() {
        return this.integerMember;
    }

    /**
     * @param integerMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInHeadersResult withIntegerMember(Integer integerMember) {
        setIntegerMember(integerMember);
        return this;
    }

    /**
     * @param longMember
     */

    public void setLongMember(Long longMember) {
        this.longMember = longMember;
    }

    /**
     * @return
     */

    public Long getLongMember() {
        return this.longMember;
    }

    /**
     * @param longMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInHeadersResult withLongMember(Long longMember) {
        setLongMember(longMember);
        return this;
    }

    /**
     * @param floatMember
     */

    public void setFloatMember(Float floatMember) {
        this.floatMember = floatMember;
    }

    /**
     * @return
     */

    public Float getFloatMember() {
        return this.floatMember;
    }

    /**
     * @param floatMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInHeadersResult withFloatMember(Float floatMember) {
        setFloatMember(floatMember);
        return this;
    }

    /**
     * @param doubleMember
     */

    public void setDoubleMember(Double doubleMember) {
        this.doubleMember = doubleMember;
    }

    /**
     * @return
     */

    public Double getDoubleMember() {
        return this.doubleMember;
    }

    /**
     * @param doubleMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInHeadersResult withDoubleMember(Double doubleMember) {
        setDoubleMember(doubleMember);
        return this;
    }

    /**
     * @param timestampMember
     */

    public void setTimestampMember(java.util.Date timestampMember) {
        this.timestampMember = timestampMember;
    }

    /**
     * @return
     */

    public java.util.Date getTimestampMember() {
        return this.timestampMember;
    }

    /**
     * @param timestampMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MembersInHeadersResult withTimestampMember(java.util.Date timestampMember) {
        setTimestampMember(timestampMember);
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
        if (getStringMember() != null)
            sb.append("StringMember: ").append(getStringMember()).append(",");
        if (getBooleanMember() != null)
            sb.append("BooleanMember: ").append(getBooleanMember()).append(",");
        if (getIntegerMember() != null)
            sb.append("IntegerMember: ").append(getIntegerMember()).append(",");
        if (getLongMember() != null)
            sb.append("LongMember: ").append(getLongMember()).append(",");
        if (getFloatMember() != null)
            sb.append("FloatMember: ").append(getFloatMember()).append(",");
        if (getDoubleMember() != null)
            sb.append("DoubleMember: ").append(getDoubleMember()).append(",");
        if (getTimestampMember() != null)
            sb.append("TimestampMember: ").append(getTimestampMember());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof MembersInHeadersResult == false)
            return false;
        MembersInHeadersResult other = (MembersInHeadersResult) obj;
        if (other.getStringMember() == null ^ this.getStringMember() == null)
            return false;
        if (other.getStringMember() != null && other.getStringMember().equals(this.getStringMember()) == false)
            return false;
        if (other.getBooleanMember() == null ^ this.getBooleanMember() == null)
            return false;
        if (other.getBooleanMember() != null && other.getBooleanMember().equals(this.getBooleanMember()) == false)
            return false;
        if (other.getIntegerMember() == null ^ this.getIntegerMember() == null)
            return false;
        if (other.getIntegerMember() != null && other.getIntegerMember().equals(this.getIntegerMember()) == false)
            return false;
        if (other.getLongMember() == null ^ this.getLongMember() == null)
            return false;
        if (other.getLongMember() != null && other.getLongMember().equals(this.getLongMember()) == false)
            return false;
        if (other.getFloatMember() == null ^ this.getFloatMember() == null)
            return false;
        if (other.getFloatMember() != null && other.getFloatMember().equals(this.getFloatMember()) == false)
            return false;
        if (other.getDoubleMember() == null ^ this.getDoubleMember() == null)
            return false;
        if (other.getDoubleMember() != null && other.getDoubleMember().equals(this.getDoubleMember()) == false)
            return false;
        if (other.getTimestampMember() == null ^ this.getTimestampMember() == null)
            return false;
        if (other.getTimestampMember() != null && other.getTimestampMember().equals(this.getTimestampMember()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getStringMember() == null) ? 0 : getStringMember().hashCode());
        hashCode = prime * hashCode + ((getBooleanMember() == null) ? 0 : getBooleanMember().hashCode());
        hashCode = prime * hashCode + ((getIntegerMember() == null) ? 0 : getIntegerMember().hashCode());
        hashCode = prime * hashCode + ((getLongMember() == null) ? 0 : getLongMember().hashCode());
        hashCode = prime * hashCode + ((getFloatMember() == null) ? 0 : getFloatMember().hashCode());
        hashCode = prime * hashCode + ((getDoubleMember() == null) ? 0 : getDoubleMember().hashCode());
        hashCode = prime * hashCode + ((getTimestampMember() == null) ? 0 : getTimestampMember().hashCode());
        return hashCode;
    }

    @Override
    public MembersInHeadersResult clone() {
        try {
            return (MembersInHeadersResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
