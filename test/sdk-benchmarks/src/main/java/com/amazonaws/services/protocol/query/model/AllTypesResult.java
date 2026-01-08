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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/AllTypes" target="_top">AWS API
 *      Documentation</a>
 */

public class AllTypesResult extends com.amazonaws.AmazonWebServiceResult<com.amazonaws.ResponseMetadata> implements Serializable, Cloneable {

    private String stringMember;

    private Integer integerMember;

    private Boolean booleanMember;

    private Float floatMember;

    private Double doubleMember;

    private Long longMember;

    private SimpleStruct simpleStructMember;

    private java.util.List<String> simpleList;

    private java.util.List<SimpleStruct> listOfStructs;

    private java.util.Map<String, String> mapOfStringToString;

    private java.util.Date timestampMember;

    private StructWithTimestamp structWithNestedTimestampMember;

    private java.nio.ByteBuffer blobArg;

    private java.util.Map<String, java.nio.ByteBuffer> blobMap;

    private java.util.List<java.nio.ByteBuffer> listOfBlobs;

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

    public AllTypesResult withStringMember(String stringMember) {
        setStringMember(stringMember);
        return this;
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

    public AllTypesResult withIntegerMember(Integer integerMember) {
        setIntegerMember(integerMember);
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

    public AllTypesResult withBooleanMember(Boolean booleanMember) {
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

    public AllTypesResult withFloatMember(Float floatMember) {
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

    public AllTypesResult withDoubleMember(Double doubleMember) {
        setDoubleMember(doubleMember);
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

    public AllTypesResult withLongMember(Long longMember) {
        setLongMember(longMember);
        return this;
    }

    /**
     * @param simpleStructMember
     */

    public void setSimpleStructMember(SimpleStruct simpleStructMember) {
        this.simpleStructMember = simpleStructMember;
    }

    /**
     * @return
     */

    public SimpleStruct getSimpleStructMember() {
        return this.simpleStructMember;
    }

    /**
     * @param simpleStructMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withSimpleStructMember(SimpleStruct simpleStructMember) {
        setSimpleStructMember(simpleStructMember);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<String> getSimpleList() {
        return simpleList;
    }

    /**
     * @param simpleList
     */

    public void setSimpleList(java.util.Collection<String> simpleList) {
        if (simpleList == null) {
            this.simpleList = null;
            return;
        }

        this.simpleList = new java.util.ArrayList<String>(simpleList);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setSimpleList(java.util.Collection)} or {@link #withSimpleList(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param simpleList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withSimpleList(String... simpleList) {
        if (this.simpleList == null) {
            setSimpleList(new java.util.ArrayList<String>(simpleList.length));
        }
        for (String ele : simpleList) {
            this.simpleList.add(ele);
        }
        return this;
    }

    /**
     * @param simpleList
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withSimpleList(java.util.Collection<String> simpleList) {
        setSimpleList(simpleList);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<SimpleStruct> getListOfStructs() {
        return listOfStructs;
    }

    /**
     * @param listOfStructs
     */

    public void setListOfStructs(java.util.Collection<SimpleStruct> listOfStructs) {
        if (listOfStructs == null) {
            this.listOfStructs = null;
            return;
        }

        this.listOfStructs = new java.util.ArrayList<SimpleStruct>(listOfStructs);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfStructs(java.util.Collection)} or {@link #withListOfStructs(java.util.Collection)} if you want
     * to override the existing values.
     * </p>
     * 
     * @param listOfStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withListOfStructs(SimpleStruct... listOfStructs) {
        if (this.listOfStructs == null) {
            setListOfStructs(new java.util.ArrayList<SimpleStruct>(listOfStructs.length));
        }
        for (SimpleStruct ele : listOfStructs) {
            this.listOfStructs.add(ele);
        }
        return this;
    }

    /**
     * @param listOfStructs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withListOfStructs(java.util.Collection<SimpleStruct> listOfStructs) {
        setListOfStructs(listOfStructs);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, String> getMapOfStringToString() {
        return mapOfStringToString;
    }

    /**
     * @param mapOfStringToString
     */

    public void setMapOfStringToString(java.util.Map<String, String> mapOfStringToString) {
        this.mapOfStringToString = mapOfStringToString;
    }

    /**
     * @param mapOfStringToString
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withMapOfStringToString(java.util.Map<String, String> mapOfStringToString) {
        setMapOfStringToString(mapOfStringToString);
        return this;
    }

    /**
     * Add a single MapOfStringToString entry
     *
     * @see AllTypesResult#withMapOfStringToString
     * @returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult addMapOfStringToStringEntry(String key, String value) {
        if (null == this.mapOfStringToString) {
            this.mapOfStringToString = new java.util.HashMap<String, String>();
        }
        if (this.mapOfStringToString.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.mapOfStringToString.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into MapOfStringToString.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult clearMapOfStringToStringEntries() {
        this.mapOfStringToString = null;
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

    public AllTypesResult withTimestampMember(java.util.Date timestampMember) {
        setTimestampMember(timestampMember);
        return this;
    }

    /**
     * @param structWithNestedTimestampMember
     */

    public void setStructWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
        this.structWithNestedTimestampMember = structWithNestedTimestampMember;
    }

    /**
     * @return
     */

    public StructWithTimestamp getStructWithNestedTimestampMember() {
        return this.structWithNestedTimestampMember;
    }

    /**
     * @param structWithNestedTimestampMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withStructWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
        setStructWithNestedTimestampMember(structWithNestedTimestampMember);
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
     * @param blobArg
     */

    public void setBlobArg(java.nio.ByteBuffer blobArg) {
        this.blobArg = blobArg;
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

    public java.nio.ByteBuffer getBlobArg() {
        return this.blobArg;
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
     * @param blobArg
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withBlobArg(java.nio.ByteBuffer blobArg) {
        setBlobArg(blobArg);
        return this;
    }

    /**
     * @return
     */

    public java.util.Map<String, java.nio.ByteBuffer> getBlobMap() {
        return blobMap;
    }

    /**
     * @param blobMap
     */

    public void setBlobMap(java.util.Map<String, java.nio.ByteBuffer> blobMap) {
        this.blobMap = blobMap;
    }

    /**
     * @param blobMap
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withBlobMap(java.util.Map<String, java.nio.ByteBuffer> blobMap) {
        setBlobMap(blobMap);
        return this;
    }

    /**
     * Add a single BlobMap entry
     *
     * @see AllTypesResult#withBlobMap
     * @returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult addBlobMapEntry(String key, java.nio.ByteBuffer value) {
        if (null == this.blobMap) {
            this.blobMap = new java.util.HashMap<String, java.nio.ByteBuffer>();
        }
        if (this.blobMap.containsKey(key))
            throw new IllegalArgumentException("Duplicated keys (" + key.toString() + ") are provided.");
        this.blobMap.put(key, value);
        return this;
    }

    /**
     * Removes all the entries added into BlobMap.
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult clearBlobMapEntries() {
        this.blobMap = null;
        return this;
    }

    /**
     * @return
     */

    public java.util.List<java.nio.ByteBuffer> getListOfBlobs() {
        return listOfBlobs;
    }

    /**
     * @param listOfBlobs
     */

    public void setListOfBlobs(java.util.Collection<java.nio.ByteBuffer> listOfBlobs) {
        if (listOfBlobs == null) {
            this.listOfBlobs = null;
            return;
        }

        this.listOfBlobs = new java.util.ArrayList<java.nio.ByteBuffer>(listOfBlobs);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setListOfBlobs(java.util.Collection)} or {@link #withListOfBlobs(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param listOfBlobs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withListOfBlobs(java.nio.ByteBuffer... listOfBlobs) {
        if (this.listOfBlobs == null) {
            setListOfBlobs(new java.util.ArrayList<java.nio.ByteBuffer>(listOfBlobs.length));
        }
        for (java.nio.ByteBuffer ele : listOfBlobs) {
            this.listOfBlobs.add(ele);
        }
        return this;
    }

    /**
     * @param listOfBlobs
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public AllTypesResult withListOfBlobs(java.util.Collection<java.nio.ByteBuffer> listOfBlobs) {
        setListOfBlobs(listOfBlobs);
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
        if (getIntegerMember() != null)
            sb.append("IntegerMember: ").append(getIntegerMember()).append(",");
        if (getBooleanMember() != null)
            sb.append("BooleanMember: ").append(getBooleanMember()).append(",");
        if (getFloatMember() != null)
            sb.append("FloatMember: ").append(getFloatMember()).append(",");
        if (getDoubleMember() != null)
            sb.append("DoubleMember: ").append(getDoubleMember()).append(",");
        if (getLongMember() != null)
            sb.append("LongMember: ").append(getLongMember()).append(",");
        if (getSimpleStructMember() != null)
            sb.append("SimpleStructMember: ").append(getSimpleStructMember()).append(",");
        if (getSimpleList() != null)
            sb.append("SimpleList: ").append(getSimpleList()).append(",");
        if (getListOfStructs() != null)
            sb.append("ListOfStructs: ").append(getListOfStructs()).append(",");
        if (getMapOfStringToString() != null)
            sb.append("MapOfStringToString: ").append(getMapOfStringToString()).append(",");
        if (getTimestampMember() != null)
            sb.append("TimestampMember: ").append(getTimestampMember()).append(",");
        if (getStructWithNestedTimestampMember() != null)
            sb.append("StructWithNestedTimestampMember: ").append(getStructWithNestedTimestampMember()).append(",");
        if (getBlobArg() != null)
            sb.append("BlobArg: ").append(getBlobArg()).append(",");
        if (getBlobMap() != null)
            sb.append("BlobMap: ").append(getBlobMap()).append(",");
        if (getListOfBlobs() != null)
            sb.append("ListOfBlobs: ").append(getListOfBlobs());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof AllTypesResult == false)
            return false;
        AllTypesResult other = (AllTypesResult) obj;
        if (other.getStringMember() == null ^ this.getStringMember() == null)
            return false;
        if (other.getStringMember() != null && other.getStringMember().equals(this.getStringMember()) == false)
            return false;
        if (other.getIntegerMember() == null ^ this.getIntegerMember() == null)
            return false;
        if (other.getIntegerMember() != null && other.getIntegerMember().equals(this.getIntegerMember()) == false)
            return false;
        if (other.getBooleanMember() == null ^ this.getBooleanMember() == null)
            return false;
        if (other.getBooleanMember() != null && other.getBooleanMember().equals(this.getBooleanMember()) == false)
            return false;
        if (other.getFloatMember() == null ^ this.getFloatMember() == null)
            return false;
        if (other.getFloatMember() != null && other.getFloatMember().equals(this.getFloatMember()) == false)
            return false;
        if (other.getDoubleMember() == null ^ this.getDoubleMember() == null)
            return false;
        if (other.getDoubleMember() != null && other.getDoubleMember().equals(this.getDoubleMember()) == false)
            return false;
        if (other.getLongMember() == null ^ this.getLongMember() == null)
            return false;
        if (other.getLongMember() != null && other.getLongMember().equals(this.getLongMember()) == false)
            return false;
        if (other.getSimpleStructMember() == null ^ this.getSimpleStructMember() == null)
            return false;
        if (other.getSimpleStructMember() != null && other.getSimpleStructMember().equals(this.getSimpleStructMember()) == false)
            return false;
        if (other.getSimpleList() == null ^ this.getSimpleList() == null)
            return false;
        if (other.getSimpleList() != null && other.getSimpleList().equals(this.getSimpleList()) == false)
            return false;
        if (other.getListOfStructs() == null ^ this.getListOfStructs() == null)
            return false;
        if (other.getListOfStructs() != null && other.getListOfStructs().equals(this.getListOfStructs()) == false)
            return false;
        if (other.getMapOfStringToString() == null ^ this.getMapOfStringToString() == null)
            return false;
        if (other.getMapOfStringToString() != null && other.getMapOfStringToString().equals(this.getMapOfStringToString()) == false)
            return false;
        if (other.getTimestampMember() == null ^ this.getTimestampMember() == null)
            return false;
        if (other.getTimestampMember() != null && other.getTimestampMember().equals(this.getTimestampMember()) == false)
            return false;
        if (other.getStructWithNestedTimestampMember() == null ^ this.getStructWithNestedTimestampMember() == null)
            return false;
        if (other.getStructWithNestedTimestampMember() != null
                && other.getStructWithNestedTimestampMember().equals(this.getStructWithNestedTimestampMember()) == false)
            return false;
        if (other.getBlobArg() == null ^ this.getBlobArg() == null)
            return false;
        if (other.getBlobArg() != null && other.getBlobArg().equals(this.getBlobArg()) == false)
            return false;
        if (other.getBlobMap() == null ^ this.getBlobMap() == null)
            return false;
        if (other.getBlobMap() != null && other.getBlobMap().equals(this.getBlobMap()) == false)
            return false;
        if (other.getListOfBlobs() == null ^ this.getListOfBlobs() == null)
            return false;
        if (other.getListOfBlobs() != null && other.getListOfBlobs().equals(this.getListOfBlobs()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getStringMember() == null) ? 0 : getStringMember().hashCode());
        hashCode = prime * hashCode + ((getIntegerMember() == null) ? 0 : getIntegerMember().hashCode());
        hashCode = prime * hashCode + ((getBooleanMember() == null) ? 0 : getBooleanMember().hashCode());
        hashCode = prime * hashCode + ((getFloatMember() == null) ? 0 : getFloatMember().hashCode());
        hashCode = prime * hashCode + ((getDoubleMember() == null) ? 0 : getDoubleMember().hashCode());
        hashCode = prime * hashCode + ((getLongMember() == null) ? 0 : getLongMember().hashCode());
        hashCode = prime * hashCode + ((getSimpleStructMember() == null) ? 0 : getSimpleStructMember().hashCode());
        hashCode = prime * hashCode + ((getSimpleList() == null) ? 0 : getSimpleList().hashCode());
        hashCode = prime * hashCode + ((getListOfStructs() == null) ? 0 : getListOfStructs().hashCode());
        hashCode = prime * hashCode + ((getMapOfStringToString() == null) ? 0 : getMapOfStringToString().hashCode());
        hashCode = prime * hashCode + ((getTimestampMember() == null) ? 0 : getTimestampMember().hashCode());
        hashCode = prime * hashCode + ((getStructWithNestedTimestampMember() == null) ? 0 : getStructWithNestedTimestampMember().hashCode());
        hashCode = prime * hashCode + ((getBlobArg() == null) ? 0 : getBlobArg().hashCode());
        hashCode = prime * hashCode + ((getBlobMap() == null) ? 0 : getBlobMap().hashCode());
        hashCode = prime * hashCode + ((getListOfBlobs() == null) ? 0 : getListOfBlobs().hashCode());
        return hashCode;
    }

    @Override
    public AllTypesResult clone() {
        try {
            return (AllTypesResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
