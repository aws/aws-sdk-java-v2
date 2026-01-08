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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/SimpleScalarProperties"
 *      target="_top">AWS API Documentation</a>
 */

public class SimpleScalarPropertiesResult extends com.amazonaws.AmazonWebServiceResult<com.amazonaws.ResponseMetadata> implements Serializable, Cloneable {

    private Boolean trueBooleanValue;

    private Boolean falseBooleanValue;

    private Integer byteValue;

    private Double doubleValue;

    private Float floatValue;

    private Integer integerValue;

    private Long longValue;

    private Integer shortValue;

    private String stringValue;

    private java.nio.ByteBuffer blobValue;

    /**
     * @param trueBooleanValue
     */

    public void setTrueBooleanValue(Boolean trueBooleanValue) {
        this.trueBooleanValue = trueBooleanValue;
    }

    /**
     * @return
     */

    public Boolean getTrueBooleanValue() {
        return this.trueBooleanValue;
    }

    /**
     * @param trueBooleanValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withTrueBooleanValue(Boolean trueBooleanValue) {
        setTrueBooleanValue(trueBooleanValue);
        return this;
    }

    /**
     * @return
     */

    public Boolean isTrueBooleanValue() {
        return this.trueBooleanValue;
    }

    /**
     * @param falseBooleanValue
     */

    public void setFalseBooleanValue(Boolean falseBooleanValue) {
        this.falseBooleanValue = falseBooleanValue;
    }

    /**
     * @return
     */

    public Boolean getFalseBooleanValue() {
        return this.falseBooleanValue;
    }

    /**
     * @param falseBooleanValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withFalseBooleanValue(Boolean falseBooleanValue) {
        setFalseBooleanValue(falseBooleanValue);
        return this;
    }

    /**
     * @return
     */

    public Boolean isFalseBooleanValue() {
        return this.falseBooleanValue;
    }

    /**
     * @param byteValue
     */

    public void setByteValue(Integer byteValue) {
        this.byteValue = byteValue;
    }

    /**
     * @return
     */

    public Integer getByteValue() {
        return this.byteValue;
    }

    /**
     * @param byteValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withByteValue(Integer byteValue) {
        setByteValue(byteValue);
        return this;
    }

    /**
     * @param doubleValue
     */

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    /**
     * @return
     */

    public Double getDoubleValue() {
        return this.doubleValue;
    }

    /**
     * @param doubleValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withDoubleValue(Double doubleValue) {
        setDoubleValue(doubleValue);
        return this;
    }

    /**
     * @param floatValue
     */

    public void setFloatValue(Float floatValue) {
        this.floatValue = floatValue;
    }

    /**
     * @return
     */

    public Float getFloatValue() {
        return this.floatValue;
    }

    /**
     * @param floatValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withFloatValue(Float floatValue) {
        setFloatValue(floatValue);
        return this;
    }

    /**
     * @param integerValue
     */

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    /**
     * @return
     */

    public Integer getIntegerValue() {
        return this.integerValue;
    }

    /**
     * @param integerValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withIntegerValue(Integer integerValue) {
        setIntegerValue(integerValue);
        return this;
    }

    /**
     * @param longValue
     */

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    /**
     * @return
     */

    public Long getLongValue() {
        return this.longValue;
    }

    /**
     * @param longValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withLongValue(Long longValue) {
        setLongValue(longValue);
        return this;
    }

    /**
     * @param shortValue
     */

    public void setShortValue(Integer shortValue) {
        this.shortValue = shortValue;
    }

    /**
     * @return
     */

    public Integer getShortValue() {
        return this.shortValue;
    }

    /**
     * @param shortValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withShortValue(Integer shortValue) {
        setShortValue(shortValue);
        return this;
    }

    /**
     * @param stringValue
     */

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * @return
     */

    public String getStringValue() {
        return this.stringValue;
    }

    /**
     * @param stringValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withStringValue(String stringValue) {
        setStringValue(stringValue);
        return this;
    }

    /**
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * <p>
     * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content or position of the byte buffer will
     * be seen by all objects that have a reference to this object. It is recommended to call ByteBuffer.duplicate() or
     * ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer. This behavior will be changed in a future
     * major version of the SDK.
     * </p>
     * 
     * @param blobValue
     */

    public void setBlobValue(java.nio.ByteBuffer blobValue) {
        this.blobValue = blobValue;
    }

    /**
     * <p>
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}.
     * </p>
     * 
     * @return
     */

    public java.nio.ByteBuffer getBlobValue() {
        return this.blobValue;
    }

    /**
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * <p>
     * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content or position of the byte buffer will
     * be seen by all objects that have a reference to this object. It is recommended to call ByteBuffer.duplicate() or
     * ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer. This behavior will be changed in a future
     * major version of the SDK.
     * </p>
     * 
     * @param blobValue
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public SimpleScalarPropertiesResult withBlobValue(java.nio.ByteBuffer blobValue) {
        setBlobValue(blobValue);
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
        if (getTrueBooleanValue() != null)
            sb.append("TrueBooleanValue: ").append(getTrueBooleanValue()).append(",");
        if (getFalseBooleanValue() != null)
            sb.append("FalseBooleanValue: ").append(getFalseBooleanValue()).append(",");
        if (getByteValue() != null)
            sb.append("ByteValue: ").append(getByteValue()).append(",");
        if (getDoubleValue() != null)
            sb.append("DoubleValue: ").append(getDoubleValue()).append(",");
        if (getFloatValue() != null)
            sb.append("FloatValue: ").append(getFloatValue()).append(",");
        if (getIntegerValue() != null)
            sb.append("IntegerValue: ").append(getIntegerValue()).append(",");
        if (getLongValue() != null)
            sb.append("LongValue: ").append(getLongValue()).append(",");
        if (getShortValue() != null)
            sb.append("ShortValue: ").append(getShortValue()).append(",");
        if (getStringValue() != null)
            sb.append("StringValue: ").append(getStringValue()).append(",");
        if (getBlobValue() != null)
            sb.append("BlobValue: ").append(getBlobValue());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof SimpleScalarPropertiesResult == false)
            return false;
        SimpleScalarPropertiesResult other = (SimpleScalarPropertiesResult) obj;
        if (other.getTrueBooleanValue() == null ^ this.getTrueBooleanValue() == null)
            return false;
        if (other.getTrueBooleanValue() != null && other.getTrueBooleanValue().equals(this.getTrueBooleanValue()) == false)
            return false;
        if (other.getFalseBooleanValue() == null ^ this.getFalseBooleanValue() == null)
            return false;
        if (other.getFalseBooleanValue() != null && other.getFalseBooleanValue().equals(this.getFalseBooleanValue()) == false)
            return false;
        if (other.getByteValue() == null ^ this.getByteValue() == null)
            return false;
        if (other.getByteValue() != null && other.getByteValue().equals(this.getByteValue()) == false)
            return false;
        if (other.getDoubleValue() == null ^ this.getDoubleValue() == null)
            return false;
        if (other.getDoubleValue() != null && other.getDoubleValue().equals(this.getDoubleValue()) == false)
            return false;
        if (other.getFloatValue() == null ^ this.getFloatValue() == null)
            return false;
        if (other.getFloatValue() != null && other.getFloatValue().equals(this.getFloatValue()) == false)
            return false;
        if (other.getIntegerValue() == null ^ this.getIntegerValue() == null)
            return false;
        if (other.getIntegerValue() != null && other.getIntegerValue().equals(this.getIntegerValue()) == false)
            return false;
        if (other.getLongValue() == null ^ this.getLongValue() == null)
            return false;
        if (other.getLongValue() != null && other.getLongValue().equals(this.getLongValue()) == false)
            return false;
        if (other.getShortValue() == null ^ this.getShortValue() == null)
            return false;
        if (other.getShortValue() != null && other.getShortValue().equals(this.getShortValue()) == false)
            return false;
        if (other.getStringValue() == null ^ this.getStringValue() == null)
            return false;
        if (other.getStringValue() != null && other.getStringValue().equals(this.getStringValue()) == false)
            return false;
        if (other.getBlobValue() == null ^ this.getBlobValue() == null)
            return false;
        if (other.getBlobValue() != null && other.getBlobValue().equals(this.getBlobValue()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getTrueBooleanValue() == null) ? 0 : getTrueBooleanValue().hashCode());
        hashCode = prime * hashCode + ((getFalseBooleanValue() == null) ? 0 : getFalseBooleanValue().hashCode());
        hashCode = prime * hashCode + ((getByteValue() == null) ? 0 : getByteValue().hashCode());
        hashCode = prime * hashCode + ((getDoubleValue() == null) ? 0 : getDoubleValue().hashCode());
        hashCode = prime * hashCode + ((getFloatValue() == null) ? 0 : getFloatValue().hashCode());
        hashCode = prime * hashCode + ((getIntegerValue() == null) ? 0 : getIntegerValue().hashCode());
        hashCode = prime * hashCode + ((getLongValue() == null) ? 0 : getLongValue().hashCode());
        hashCode = prime * hashCode + ((getShortValue() == null) ? 0 : getShortValue().hashCode());
        hashCode = prime * hashCode + ((getStringValue() == null) ? 0 : getStringValue().hashCode());
        hashCode = prime * hashCode + ((getBlobValue() == null) ? 0 : getBlobValue().hashCode());
        return hashCode;
    }

    @Override
    public SimpleScalarPropertiesResult clone() {
        try {
            return (SimpleScalarPropertiesResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
